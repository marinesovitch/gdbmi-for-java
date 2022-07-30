// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.event.*;

// ---------------------------------------------------------------------

interface IEventHandler
{
	void onResumeExecution();
	void onVirtualMachineInterrupted();
	
	void onGenericEvent ( Event event );
	
	enum EventStatus
	{
		Unrecognized,
		Stop,
		Resume
	}
	
	EventStatus onVirtualMachineStartEvent ( VMStartEvent e );
	EventStatus onVirtualMachineExitEvent ( VMDeathEvent e );
	EventStatus onVirtualMachineDisconnectEvent ( VMDisconnectEvent e );
	EventStatus onThreadStartEvent ( ThreadStartEvent e );
	EventStatus onThreadDeathEvent ( ThreadDeathEvent e );
	EventStatus onClassPrepareEvent ( ClassPrepareEvent e );
	EventStatus onClassUnloadEvent ( ClassUnloadEvent e );
	EventStatus onBreakpointEvent ( BreakpointEvent e );
	EventStatus onWatchpointEvent ( WatchpointEvent e );
	EventStatus onStepEvent ( StepEvent e );
	EventStatus onExceptionEvent ( ExceptionEvent e );
	EventStatus onMethodEntryEvent ( MethodEntryEvent e );
	EventStatus onMethodExitEvent ( MethodExitEvent e );
}

// ---------------------------------------------------------------------
