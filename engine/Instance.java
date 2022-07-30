// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.event.EventSet;
import java.util.*;

// ---------------------------------------------------------------------

class Instance
{
	// ---------------------------------------------------------------------

	static String s_debuggedProgramPath = "<unknown>";
	static String s_mainClassName = "<unknown>";
	static String s_arguments = "";

	// ---------------------------------------------------------------------

	static CommandManager s_commandManager = new CommandManager();
	static CommandExecutor s_commandExecutor = new CommandExecutor();
	static BreakpointManager s_breakpointManager = new BreakpointManager();
	static MethodManager s_methodManager = new MethodManager();
	static SourceManager s_sourceManager = new SourceManager();
	static ThreadManager s_threadManager = new ThreadManager();
	static IVisualizerManager s_visualizerManager = new VisualizerManagerStub();
	static DebugVariableTypeFactory s_debugVariableTypeFactory = new DebugVariableTypeFactory();
	static DebugVariableValueFactory s_debugVariableValueFactory = new DebugVariableValueFactory();
	static DebugVariableFactory s_debugVariableFactory
		= new DebugVariableFactory( s_debugVariableTypeFactory, s_debugVariableValueFactory );
	static DebugVariableManager s_debugVariableManager = new DebugVariableManager( s_debugVariableFactory );
	static SettingsManager s_settingsManager = new SettingsManager();

	static VirtualMachineConnector s_connection = null;
	static Controller s_controller = null;
	static EventDispatcher s_eventDispatcher = null;

	// ---------------------------------------------------------------------

	static boolean s_halted = false;
	static boolean s_dalvik = false;

	// ---------------------------------------------------------------------

	// list of classes/files to ignore at stepping
	private static List< String > d_predefinedStepFilters
		= new ArrayList< String >( Arrays.asList( new String[] {
			"java.*",
			"javax.*",
			"sun.*",
			"com.sun.*",
			"dalvik.*" } ) );


	// ---------------------------------------------------------------------

	static void init( InstanceInitData initData )
	{
		s_debuggedProgramPath = initData.d_debuggedProgramPath;
		s_mainClassName = initData.d_mainClassName;
		s_connection = new VirtualMachineConnector( initData.d_timeOut );
	}

	// ---------------------------------------------------------------------

	static VirtualMachine getVirtualMachine()
	{
		return s_connection.getVirtualMachine();
	}

	// ---------------------------------------------------------------------

	static synchronized void resumeExecution( EventSet eventSet )
	{
		Instance.s_threadManager.onResumeExecution();
		Instance.s_controller.onResumeExecution();
		if ( eventSet != null )
			eventSet.resume();
		else
			getVirtualMachine().resume();
	}

	// ---------------------------------------------------------------------

	static void halt()
	{
		if ( s_connection != null )
		{
			try
			{
				s_connection.close();
			}
			catch ( VMDisconnectedException e )
			{
			}
		}

		s_halted = true;
	}

	// ---------------------------------------------------------------------

	static void setStepFilters ( StepRequest request )
	{
		for ( String stepFilter : d_predefinedStepFilters )
		{
			request.addClassExclusionFilter( stepFilter );
		}
	}

	// ---------------------------------------------------------------------

	public static Value invokeMethodInValue (
		Value srcValue,
		String methodName,
		List< Value > args,
		ThreadReference hThread )
	{
		if ( srcValue instanceof ObjectReference )
		{
			final ObjectReference hObjectReference = ( ObjectReference ) srcValue;
			final ReferenceType hClass = hObjectReference.referenceType();
			try
			{
				final Method hMethod
					= Instance.s_methodManager.matchMethod( hClass, methodName, args );
				if ( hMethod != null )
				{
					if ( hThread == null )
					{
						final int threadId = Instance.s_threadManager.getCurrentThreadId();

						if ( threadId == ThreadManager.InvalidThread )
							return null;

						hThread = s_threadManager.getThreadById ( threadId );
					}

					final Value hResult = hObjectReference.invokeMethod (
						hThread, hMethod, args, 0 );

					s_threadManager.onResumeExecution();

					return hResult;
				}
			}
			catch ( InvalidTypeException e )
			{
			}
			catch ( ClassNotLoadedException e )
			{
			}
			catch ( IncompatibleThreadStateException e )
			{
			}
			catch ( InvocationException e )
			{
			}
		}

		return null;
	}

	public static String getValueText ( Value hValue, StackFrame hFrame ) throws XParseError
	{
		if ( hValue == null )
			return "null";

		if ( ! ( hValue instanceof ObjectReference ) )
			return hValue.toString();

		if ( hValue instanceof ArrayReference )
			return hValue.toString();

		if ( hValue instanceof StringReference )
		{
			StringReference hStringRefValue = ( StringReference ) hValue;
			return hStringRefValue.value();
		}

		List< Value > args = new ArrayList< Value >();
		final Value hTextValue =
			invokeMethodInValue ( hValue, "toString", args, hFrame.thread() );

		if ( hTextValue == null )
			return "null";

		if ( hTextValue instanceof StringReference )
		{
			final StringReference hStringRefValue = ( StringReference ) hValue;
			return hStringRefValue.value();
		}
		else
			return hValue.toString();
	}

	// ---------------------------------------------------------------------

	public static String getValueText(
		String expression,
		StackFrame frame,
		Type type,
		Value value,
		boolean scalarOnly )
			throws Exception
	{
		String result = null;
		IDebugVariable var = s_debugVariableFactory.createVariable( expression, frame, type, value );
		if ( !scalarOnly || var.isScalar() )
			result = var.getValueString();
		return result;
	}

}
