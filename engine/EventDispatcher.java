// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

// ---------------------------------------------------------------------

public class EventDispatcher implements Runnable
{
	// ---------------------------------------------------------------------

	EventDispatcher ( IEventHandler eventHandler )
	{
		d_eventHandler = eventHandler;
		d_eventProcessingThread = new Thread ( this, "EventProcessor" );
		d_eventProcessingThread.start();
	}

	// ---------------------------------------------------------------------

	public void run()
	{
		VirtualMachine virtualMachine = Instance.getVirtualMachine();
		EventQueue queue = virtualMachine.eventQueue();

		while ( d_bIsConnected )
		{
			processEvents( queue );
		}

		synchronized ( this )
		{
			d_bIsSessionRunning = false;
			notifyAll();
		}
	}

	// ---------------------------------------------------------------------

	private void processEvents( EventQueue queue )
	{
		try
		{
			EventSet eventSet = queue.remove();
			if ( processEventLoop( eventSet ) )
			{
				resumeVirtualMachine( eventSet );
			}
			else
			{
				suspendVirtualMachine( eventSet );
			}
		}
		catch ( VMDisconnectedException e )
		{
			flushEventsQueue();
		}
		catch ( InterruptedException e )
		{
		}
	}

	// ---------------------------------------------------------------------

	private boolean processEventLoop( EventSet eventSet )
	{
		boolean resume = true;
		for ( EventIterator iEvent = eventSet.eventIterator()
			; iEvent.hasNext()
			;
			)
		{
			Event event = iEvent.nextEvent();
			IEventHandler.EventStatus eventResult = processEvent ( event );
			if ( eventResult == IEventHandler.EventStatus.Stop )
				resume = false;
		}
		return resume;
	}

	// ---------------------------------------------------------------------

	private IEventHandler.EventStatus processEvent ( Event event )
	{
		d_eventHandler.onGenericEvent ( event );

		IEventHandler.EventStatus result = processBreakpointEvent ( event );

		if ( result == IEventHandler.EventStatus.Unrecognized )
		{
			result = processStepEvent ( event );
			if ( result == IEventHandler.EventStatus.Unrecognized )
			{
				result = processVirtualMachineEvent ( event );
				if ( result == IEventHandler.EventStatus.Unrecognized )
				{
					throw new InternalError ( "unknown event type" );
				}
			}
		}

		return result;
	}

	// ---------------------------------------------------------------------

	private IEventHandler.EventStatus processBreakpointEvent ( Event event )
	{
		IEventHandler.EventStatus result = IEventHandler.EventStatus.Unrecognized;

		if ( event instanceof BreakpointEvent )
			result = d_eventHandler.onBreakpointEvent ( ( BreakpointEvent ) event );
		else if ( event instanceof WatchpointEvent )
			result = d_eventHandler.onWatchpointEvent ( ( WatchpointEvent ) event );
		else if ( event instanceof ExceptionEvent )
			result = d_eventHandler.onExceptionEvent ( ( ExceptionEvent ) event );

		return result;
	}

	// ---------------------------------------------------------------------

	private IEventHandler.EventStatus processStepEvent ( Event event )
	{
		IEventHandler.EventStatus result = IEventHandler.EventStatus.Unrecognized;

		if ( event instanceof StepEvent )
			result = d_eventHandler.onStepEvent ( ( StepEvent ) event );
		else if ( event instanceof MethodEntryEvent )
			result = d_eventHandler.onMethodEntryEvent ( ( MethodEntryEvent ) event );
		else if ( event instanceof MethodExitEvent )
			result = d_eventHandler.onMethodExitEvent ( ( MethodExitEvent ) event );

		return result;
	}

	// ---------------------------------------------------------------------

	private IEventHandler.EventStatus processVirtualMachineEvent ( Event event )
	{
		IEventHandler.EventStatus result = IEventHandler.EventStatus.Unrecognized;

		if ( event instanceof ClassPrepareEvent )
			result = d_eventHandler.onClassPrepareEvent ( ( ClassPrepareEvent ) event );
		else if ( event instanceof ClassUnloadEvent )
			result = d_eventHandler.onClassUnloadEvent ( ( ClassUnloadEvent ) event );
		else if ( event instanceof ThreadStartEvent )
			result = d_eventHandler.onThreadStartEvent ( ( ThreadStartEvent ) event );
		else if ( event instanceof ThreadDeathEvent )
			result = d_eventHandler.onThreadDeathEvent ( ( ThreadDeathEvent ) event );
		else if ( event instanceof VMStartEvent )
			result = d_eventHandler.onVirtualMachineStartEvent ( ( VMStartEvent ) event );
		else
			result = onExitEvent ( event );

		return result;
	}

	// ---------------------------------------------------------------------

	private IEventHandler.EventStatus onExitEvent ( Event event )
	{
		IEventHandler.EventStatus result = IEventHandler.EventStatus.Unrecognized;
		if ( event instanceof VMDisconnectEvent )
		{
			result = onVirtualMachineDisconnectEvent( ( VMDisconnectEvent ) event );
		}
		else if ( event instanceof VMDeathEvent )
		{
			result = onVirtualMachineExitEvent( ( VMDeathEvent ) event );
		}
		return result;
	}

	// ---------------------------------------------------------------------

	private void resumeVirtualMachine( EventSet eventSet )
	{
		Instance.resumeExecution( eventSet );
	}

	private void suspendVirtualMachine( EventSet eventSet )
	{
		final int suspendPolicy = eventSet.suspendPolicy();
		if ( suspendPolicy == EventRequest.SUSPEND_ALL )
		{
			updateActiveThread ( eventSet );
			d_eventHandler.onVirtualMachineInterrupted();
		}
	}

	// ---------------------------------------------------------------------

	synchronized void flushEventsQueue()
	{
		EventQueue queue = Instance.getVirtualMachine().eventQueue();
		while ( d_bIsConnected )
		{
			try
			{
				EventSet eventSet = queue.remove();
				for ( EventIterator iEvent = eventSet.eventIterator()
					; iEvent.hasNext()
					;
					)
				{
					Event event = iEvent.nextEvent();
					onExitEvent( event );
				}
			}
			catch ( InterruptedException e )
			{
			}
			catch ( InternalError e )
			{
			}
		}
	}

	// ---------------------------------------------------------------------

	private void updateActiveThread ( EventSet set )
	{
		if ( ! set.isEmpty() )
		{
			final Event event = set.iterator().next();

			ThreadReference activeThread = null;
			if ( event instanceof ClassPrepareEvent )
				activeThread = ( ( ClassPrepareEvent ) event ).thread();
			else if ( event instanceof LocatableEvent )
				activeThread = ( ( LocatableEvent ) event ).thread();
			else if ( event instanceof ThreadStartEvent )
				activeThread = ( ( ThreadStartEvent ) event ).thread();
			else if ( event instanceof ThreadDeathEvent )
				activeThread = ( ( ThreadDeathEvent ) event ).thread();
			else if ( event instanceof VMStartEvent )
				activeThread = ( ( VMStartEvent ) event ).thread();

			if ( activeThread != null )
			{
				final int threadId = Instance.s_threadManager.getIdByThread ( activeThread );
				Instance.s_threadManager.setCurrentThreadId ( threadId );
			}
			else
				Instance.s_threadManager.setCurrentThreadId ( ThreadManager.InvalidThread );
		}
	}

	// ---------------------------------------------------------------------

	public IEventHandler.EventStatus onVirtualMachineExitEvent ( VMDeathEvent event )
	{
		d_bIsVirtualMachineRunning = false;
		IEventHandler.EventStatus result = d_eventHandler.onVirtualMachineExitEvent ( event );
		return result;
	}

	// ---------------------------------------------------------------------

	private IEventHandler.EventStatus onVirtualMachineDisconnectEvent ( VMDisconnectEvent event )
	{
		IEventHandler.EventStatus result = IEventHandler.EventStatus.Resume;
		d_bIsConnected = false;
		if ( d_bIsVirtualMachineRunning )
		{
			result = d_eventHandler.onVirtualMachineDisconnectEvent ( event );
		}
		Instance.halt();
		return result;
	}

	// ---------------------------------------------------------------------

	synchronized void stop()
	{
		d_bIsConnected = false;
		d_eventProcessingThread.interrupt();

		while ( d_bIsSessionRunning )
		{
			try
			{
				wait();
			}
			catch ( InterruptedException e )
			{
			}
		}
	}

	// ---------------------------------------------------------------------

	private boolean d_bIsVirtualMachineRunning = true;
	private boolean d_bIsSessionRunning = true;
	private volatile boolean d_bIsConnected = true;

	private IEventHandler d_eventHandler;
	private Thread d_eventProcessingThread;

	// ---------------------------------------------------------------------
}
