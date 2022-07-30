// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.List;
import java.util.StringTokenizer;


import com.sun.jdi.*;
import com.sun.jdi.request.*;

// ---------------------------------------------------------------------

public class CmdExec
{
	// -----------------------------------------------------------------

	public static void commandExecArguments ( StringTokenizer t )
	{
		Instance.s_arguments = t.nextToken ( "\n" );
		Messenger.println ( "^done" );
	}

	// -----------------------------------------------------------------

	public static void commandExecRun ( StringTokenizer t ) throws Exception
	{
		throw new Exception(
			"This debugger does not support -exec-run. Use target and -exec-continue" );
	}

	// -----------------------------------------------------------------

	public static void commandExecContinue ( StringTokenizer t )
	{
		Messenger.println ( "^running" );
		Messenger.printPrompt();
		Instance.resumeExecution( null );
	}

	// -----------------------------------------------------------------

	public static void commandExecInterrupt ( StringTokenizer t )
	{
		final VirtualMachine vm = Instance.getVirtualMachine();

		try
		{
			vm.suspend();
			Instance.s_controller.onDebugBreak();
		}
		catch ( Exception ex )
		{
			Instance.s_controller.onInterruptExited();
		}
	}

	// -----------------------------------------------------------------

	public static void commandExecFinish ( StringTokenizer t ) throws Exception
	{
		commandExecStepOperation ( StepRequest.STEP_LINE, StepRequest.STEP_OUT );
	}

	// -----------------------------------------------------------------

	public static void commandExecStep ( StringTokenizer t ) throws Exception
	{
		commandExecStepOperation ( StepRequest.STEP_LINE, StepRequest.STEP_INTO );
	}

	// -----------------------------------------------------------------

	public static void commandExecStepInstruction ( StringTokenizer t ) throws Exception
	{
		commandExecStepOperation ( StepRequest.STEP_MIN, StepRequest.STEP_INTO );
	}

	// -----------------------------------------------------------------

	public static void commandExecNext ( StringTokenizer t ) throws Exception
	{
		commandExecStepOperation ( StepRequest.STEP_LINE, StepRequest.STEP_OVER );
	}

	// -----------------------------------------------------------------

	public static void commandExecNextInstruction ( StringTokenizer t ) throws Exception
	{
		commandExecStepOperation ( StepRequest.STEP_MIN, StepRequest.STEP_OVER );
	}

	// -----------------------------------------------------------------

	private static void doRun (
		VirtualMachineConnector connection,
		boolean bStartSuspended,
		boolean bAttach )
			throws Exception
	{
		if ( !bAttach )
		{
			connection.setArgument (
				"main",
				Instance.s_mainClassName + " " + Instance.s_arguments
			);

			// FIXME - what about arguments? we set them by -exec-arguments
			// they shall be moved to the connector: connection.setArgument
		}

		try
		{
			if ( connection.create() )
			{
				connection.init();
				Instance.s_controller.setStartSuspended ( bStartSuspended );
				if ( bAttach )
				{
					Instance.s_controller.loadClasses();
					Instance.s_threadManager.loadThreads();
					Instance.s_breakpointManager.resolveAll();
				}
				Instance.s_eventDispatcher = new EventDispatcher ( Instance.s_controller );
			}
		}
		catch ( Exception e )
		{
			String errorMessage = e.getMessage() + "\n";
			errorMessage
				+= "\n"
				+ "Please ensure that DDMS and/or Eclipse is NOT running, also any other "
				+ "development environment which may start DDMS - just close them all.\n";
			throw new Exception( errorMessage );
		}
	}

	// -----------------------------------------------------------------

	private static void commandExecStepOperation ( int unit, int kind ) throws Exception
	{
		final int threadId = Instance.s_threadManager.getCurrentThreadId();

		if ( threadId == ThreadManager.InvalidThread )
		{
			throw new Exception( "no current thread" );
		}

		final ThreadReference hThread = Instance.s_threadManager.getThreadById ( threadId );

		if ( hThread == null )
		{
			throw new Exception( "cannot obtain current thread (" + threadId + ")" );
		}

		EventRequestManager eventRequestManager
			= Instance.getVirtualMachine().eventRequestManager();
		List< StepRequest > steps = eventRequestManager.stepRequests();
		for ( StepRequest step : steps )
		{
			ThreadReference stepThread = step.thread();
			if ( stepThread.equals ( hThread ) )
			{
				eventRequestManager.deleteEventRequest ( step );
				break;
			}
		}

		StepRequest stepRequest
			= eventRequestManager.createStepRequest ( hThread, unit, kind );
		stepRequest.addCountFilter ( 1 );
		if ( kind != StepRequest.STEP_OUT )
			Instance.setStepFilters ( stepRequest );
		stepRequest.enable();
		Messenger.println ( "^running" );
		Messenger.printPrompt();
		Instance.resumeExecution( null );
	}

	// -----------------------------------------------------------------

	public static void commandThreadSelect( StringTokenizer t )
	{
		if ( t.hasMoreTokens() )
		{
			try
			{
				String strThreadId = t.nextToken();
				int threadId = Integer.decode( strThreadId );
				Instance.s_threadManager.setCurrentThreadId( threadId );
			}
			catch ( NumberFormatException e )
			{
				Messenger.printWarning( "bad thread identifier" );
			}
		}
		else
		{
			Messenger.printWarning( "thread identifier missing" );
		}
	}

	// -----------------------------------------------------------------

	public static void commandTarget ( StringTokenizer t )
	{
		try
		{
			if ( ! t.hasMoreTokens() )
			{
				Messenger.printError ( "Argument required." );
				return;
			}

			boolean success = false;

			final String targetType = t.nextToken();

			if ( targetType.equals ( "launch" ) )
				success = launchTarget( t );
			else if ( targetType.equals ( "attach" ) )
				success = attachTarget( t );
			else
				Messenger.printError ( "Unsupported target kind." );

			if ( success )
				printInfoBanner();
		}
		catch ( Exception e )
		{
			Messenger.printError ( "command target error: " + e.getMessage() );
		}
	}

	private static boolean launchTarget( StringTokenizer t )
		throws Exception
	{
		boolean result = false;
		if ( t.hasMoreTokens() )
		{
			final String className = Utils.unquote ( t.nextToken() );

			StringBuffer rawArguments = new StringBuffer();
			rawArguments.append ( "main=" );
			rawArguments.append ( className );
			rawArguments.append ( ',' );

			final String rawClassPath =
				Instance.s_settingsManager.getValue ( "solib-search-path" );
			final String classPath = Utils.quoteTokens( rawClassPath, ";" );

			if ( ! classPath.isEmpty() )
			{
				rawArguments.append ( "options=-classpath " );
				rawArguments.append ( classPath );
				rawArguments.append ( ',' );
			}

			VirtualMachineConnector connection = Instance.s_connection;
			VirtualMachineConnector.EKind connectorKind = VirtualMachineConnector.EKind.Launch;
			String rawArgumentsStr = rawArguments.toString();
			connection.setTargetDescription ( connectorKind, rawArgumentsStr );
			doRun ( connection, true, false );
			Messenger.println ( "^done" );
			result = true;
		}
		else
		{
			Messenger.printError ( "Class name is required for the launch target." );
		}
		return result;
	}

	private static boolean attachTarget( StringTokenizer t )
		throws Exception
	{
		boolean result = false;
		if ( t.hasMoreTokens() )
		{
			boolean canAttach = false;
			final String attachKindStr = Utils.unquote ( t.nextToken() );
			final EAttachKind attachKind = str2attachKind( attachKindStr );
			VirtualMachineConnector.EKind connectorKind = VirtualMachineConnector.EKind.Unknown;
			StringBuffer rawArguments = new StringBuffer();
			if ( attachKind == EAttachKind.SharedMemory )
			{
				connectorKind = VirtualMachineConnector.EKind.AttachSharedMemory;
				canAttach = prepareAttachSharedMemory( t, rawArguments );
			}
			else if ( attachKind == EAttachKind.Socket )
			{
				connectorKind = VirtualMachineConnector.EKind.AttachSocket;
				canAttach = prepareAttachSocket( t, rawArguments );
			}
			else if ( attachKind == EAttachKind.Process )
			{
				connectorKind = VirtualMachineConnector.EKind.AttachProcess;
				canAttach = prepareAttachProcess( t, rawArguments );
			}
			else
				Messenger.printError( "Unexpected attach kind, expected shmem|socket|pid." );

			if ( canAttach )
			{
				rawArguments.append ( ',' );

				VirtualMachineConnector connection = Instance.s_connection;
				String rawArgumentsStr = rawArguments.toString();
				connection.setTargetDescription ( connectorKind, rawArgumentsStr );
				doRun ( connection, false, true );
				Messenger.println ( "^done" );

				result = true;
			}
		}
		else
		{
			Messenger.printError ( "Kind (shmem|socket|pid) is required for the attach." );
		}
		return result;
	}

	private static boolean prepareAttachSharedMemory( StringTokenizer t, StringBuffer rawArguments )
	{
		boolean result = false;
		if ( t.hasMoreTokens() )
		{
			String address = Utils.unquote ( t.nextToken() );
			rawArguments.append ( "name=" );
			rawArguments.append ( address );
			result = true;
		}
		else
		{
			Messenger.printError( "Address is required for 'shmem' kind of attach." );
		}
		return result;
	}

	private static boolean prepareAttachSocket( StringTokenizer t, StringBuffer rawArguments )
	{
		boolean result = false;
		if ( t.hasMoreTokens() )
		{
			String hostname = Utils.unquote ( t.nextToken() );
			rawArguments.append ( "hostname=" );
			rawArguments.append ( hostname );
			if ( t.hasMoreTokens() )
			{
				String port = Utils.unquote ( t.nextToken() );
				rawArguments.append ( ",port=" );
				rawArguments.append ( port );
			}
			result = true;
		}
		else
		{
			Messenger.printError( "Hostname is required for 'socket' kind of attach." );
		}
		return result;
	}

	private static boolean prepareAttachProcess( StringTokenizer t, StringBuffer rawArguments )
	{
		boolean result = false;
		if ( t.hasMoreTokens() )
		{
			String pid = Utils.unquote ( t.nextToken() );
			rawArguments.append ( "pid=" );
			rawArguments.append ( pid );
			result = true;
		}
		else
		{
			Messenger.printError( "Process id is required for 'pid' kind of attach." );
		}
		return result;
	}

	private static void printInfoBanner()
	{
		Message msg = new Message();

		VirtualMachineConnector vmc = Instance.s_connection;
		String jdiVersionStr = vmc.getJdiVersionString();
		msg.printBanner ( "JDI: " + jdiVersionStr );

		VirtualMachine vm = Instance.getVirtualMachine();
		msg.printBanner ( "VM: " + vm.name() );
		msg.printBanner ( "JRE: " + vm.version() );
		msg.printBanner ( "VM description: " + Utils.escapeValue( vm.description(), false ) );

		Messenger.print( msg );

		if ( vm.name().equals ( "DalvikVM" ) )
			Instance.s_dalvik = true;
	}

	private enum EAttachKind
	{
		Unknown,
		SharedMemory,
		Socket,
		Process
	};

	private static EAttachKind str2attachKind( String attachKindStr )
	{
		if ( attachKindStr.equals( "shmem" ) )
			return EAttachKind.SharedMemory;
		else if ( attachKindStr.equals(  "socket" ) )
			return EAttachKind.Socket;
		else if ( attachKindStr.equals( "pid" ) )
			return EAttachKind.Process;
		else
			return EAttachKind.Unknown;
	}

	// -----------------------------------------------------------------

	public static void commandGdbExit ( StringTokenizer t )
	{
		if ( Instance.s_eventDispatcher != null )
			Instance.s_eventDispatcher.stop();
		Instance.halt();
		Messenger.println ( "^exit" );
		Messenger.flush();
	}

	// -----------------------------------------------------------------
}

// ---------------------------------------------------------------------
