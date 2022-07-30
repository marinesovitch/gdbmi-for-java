// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.StringTokenizer;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

public class CmdStack
{
	// -----------------------------------------------------------------

	public static void commandStackSelectFrame ( StringTokenizer t )
	{
		final int threadId = Instance.s_threadManager.getCurrentThreadId();

		if ( threadId == ThreadManager.InvalidThread )
		{
			Messenger.printError ( "Current thread not set." );
			return;
		}

		if ( ! t.hasMoreTokens() )
		{
			Messenger.printError ( "Usage: FRAME_SPEC" );
			return;
		}

		int nFrame = 0;
		String idFrame = t.nextToken();

		try
		{
			nFrame = Integer.decode ( idFrame );
		}
		catch ( NumberFormatException e )
		{
			Messenger.printError ( "Usage: FRAME_SPEC" );
			return;
		}

		if ( nFrame < 0 )
		{
			// this matches GDB behavior (wrap-around)
			nFrame += Integer.MAX_VALUE;
			++nFrame;
		}

		try
		{
			Instance.s_threadManager.setCurrentFrameIndex ( threadId, nFrame );
		}
		catch ( IncompatibleThreadStateException e )
		{
			Messenger.printError ( "Current thread isn't suspended." );
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			Messenger.printError ( "End of stack." );
		}
	}

	// -----------------------------------------------------------------

	public static void commandStackListFrames ( StringTokenizer t )
	{
		// the arguments here, they are only the limit of frames

		final int threadId = Instance.s_threadManager.getCurrentThreadId();

		if ( threadId == ThreadManager.InvalidThread )
		{
			Messenger.printError ( "No current thread." );
			return;
		}

		List< StackFrame > stack = null;

		try
		{
			stack = Instance.s_threadManager.getFrames ( threadId );
		}
		catch ( IncompatibleThreadStateException e )
		{
		}

		if ( stack == null )
		{
			Messenger.printError ( "Thread is not running (no stack)." );
			return;
		}

		final int nFrames = stack.size();

		Message msg = new Message();
		msg.print ( "^done,stack=[" );

		for ( int i = 0; i < nFrames; ++i )
		{
			StackFrame frame = stack.get(i);
			dumpFrame ( i, frame, msg );
		}

		msg.println ("]");
		Messenger.print( msg );
	}

	// ---------------------------------------------------------------------

	public static void commandStackListLocals ( StringTokenizer t )
	{
		final int threadId = Instance.s_threadManager.getCurrentThreadId();

		if ( threadId == ThreadManager.InvalidThread )
		{
			Messenger.printError ( "No current thread." );
			return;
		}

		try
		{
			final int currentFrameIndex =
				Instance.s_threadManager.getCurrentFrameIndex ( threadId );

			final StackFrame hFrame =
				Instance.s_threadManager.getFrameById ( threadId, currentFrameIndex );

			if ( hFrame == null )
				throw new AbsentInformationException();

			final ObjectReference localThis = hFrame.thisObject();
			final List< LocalVariable > localVars = hFrame.visibleVariables();

			boolean isLocalThis = ( localThis != null );
			boolean areLocalVars = ( localVars != null ) && !localVars.isEmpty();

			if ( isLocalThis || areLocalVars )
			{
				Message msg = new Message();
				msg.print ( "^done,locals=[" );

				if ( printLocalThis( localThis, msg ) )
					msg.print ( "," );
				printLocalVariables( hFrame, localVars, msg );

				msg.println ( "]" );
				Messenger.print( msg );
			}
			else
			{
				Messenger.printError ( "No local variables" );
			}

		}
		catch ( AbsentInformationException e )
		{
			Messenger.printError ( "Local variable information not available." );
		}
		catch ( IncompatibleThreadStateException e )
		{
			Messenger.printError ( "Current thread is not suspended." );
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			Messenger.printError ( "No such frame." );
		}
	}

	// ---------------------------------------------------------------------

	public static void commandStackListArguments ( StringTokenizer t )
	{
		if ( Instance.s_dalvik )
		{
			// Dalvik never returns any arguments (isArgument is apparently
			// not working, as it returns false always - what a crap), but
			// this operation costs a lot of time.
			// Better just print error and spare that time.

			Messenger.printError ( "This operation is not supported by this VM." );
			return;
		}

		final int threadId = Instance.s_threadManager.getCurrentThreadId();

		if ( threadId == ThreadManager.InvalidThread )
		{
			Messenger.printError ( "No current thread." );
			return;
		}

		try
		{
			final ThreadReference currentThread =
				Instance.s_threadManager.getThreadById ( threadId );

			final List< StackFrame > frames = currentThread.frames();

			Message msg = new Message();
			msg.print ( "^done,stack-args=[" );
			int level = 0;

			for ( Iterator< StackFrame > iFrame = frames.iterator(); iFrame.hasNext(); ++level )
			{
				final StackFrame hFrame = iFrame.next();
				List< LocalVariable > vars = null;
				Map< LocalVariable, Value > values = null;

				try
				{
					vars = hFrame.visibleVariables();
					values = hFrame.getValues ( vars );
				}
				catch ( Exception e )
				{
				}

				if ( level != 0 )
					msg.print ( "," );

				msg.print ( "frame={level=\"" );
				msg.print ( Integer.toString ( level ) );
				msg.print ( "\",args=[" );

				if ( ( vars != null ) && ( values != null ) )
				{
					for ( Iterator< LocalVariable > iVariable = vars.iterator(); iVariable.hasNext(); )
					{
						final LocalVariable hVariable = iVariable.next();

						if ( hVariable.isArgument() )
						{
							final Value hValue = values.get ( hVariable );
							printLocalVariableValue ( hFrame, hVariable, hValue, msg );

							if ( iVariable.hasNext() )
								msg.print ( "," );
						}
					}
				}

				msg.print ( "]}" );
			}

			msg.println ( "]" );
			Messenger.print( msg );
		}
		catch ( IncompatibleThreadStateException e )
		{
			Messenger.printError ( "Current thread isn't suspended." );
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			Messenger.printError ( "No such frame." );
		}
	}

	// ---------------------------------------------------------------------

	private static void dumpFrame ( int frameNumber, StackFrame hFrame, Message msg )
	{
		if ( frameNumber > 0 )
			msg.print ( "," );

		msg.print ( "frame={" );

		msg.printProperty ( "level", frameNumber );

		Location hLocation = null;

		try
		{
			hLocation = hFrame.location();
		}
		catch ( InvalidStackFrameException e )
		{
		}

		if ( hLocation != null )
		{
			final Method hMethod = hLocation.method();

			MethodManager methodManager = Instance.s_methodManager;
			final Long offset = MethodManager.getOffset ( hLocation );
			final long address = methodManager.methodoffset2address ( hMethod, offset );

			msg.print ( "," );
			msg.printAddressProperty ( "addr", address );
			msg.print ( "," );
			msg.printProperty ( "func", hMethod.name() );

			try
			{
				final String sourceName = hLocation.sourceName();
				msg.print ( "," );
				msg.printProperty ( "file", sourceName );
			}
			catch ( AbsentInformationException e )
			{
			}

			final String absoluteSourcePath =
				Instance.s_sourceManager.getAbsolutePath( hLocation );

			if ( absoluteSourcePath != null )
			{
				msg.print ( "," );
				msg.printProperty ( "fullname", absoluteSourcePath );
			}

			msg.print ( "," );
			msg.printProperty ( "line", hLocation.lineNumber() );
		}

		msg.print ( "}" );
	}

	// -----------------------------------------------------------------

	private static boolean printLocalThis( ObjectReference localThis, Message msg )
	{
		boolean result = false;
		if ( localThis != null )
		{
			String name = "this";

			ReferenceType refType = localThis.referenceType();
			String type = refType.name();

			String value = null;

			printVariableValue( name, type, value, msg );

			result = true;
		}
		return result;
	}

	private static void printLocalVariables(
		StackFrame hFrame,
		List< LocalVariable > vars,
		Message msg )
	{
		final Map< LocalVariable, Value > values = hFrame.getValues ( vars );

		for ( Iterator< LocalVariable > iVariable = vars.iterator(); iVariable.hasNext(); )
		{
			final LocalVariable hVariable = iVariable.next();
			final Value hValue = values.get ( hVariable );

			printLocalVariableValue ( hFrame, hVariable, hValue, msg );

			if ( iVariable.hasNext() )
				msg.print ( "," );
		}
	}

	private static void printLocalVariableValue (
		StackFrame stackFrame,
		LocalVariable hVariable,
		Value hValue,
		Message msg )
	{
		final String variableName = hVariable.name();
		final String typeName = hVariable.typeName();

		if ( ! isScalarTypeName ( typeName ) )
			printVariableValue ( variableName, typeName, null, msg );
		else
		{
			try
			{
				final Type type = hVariable.type();
				final String value = Instance.getValueText( variableName, stackFrame, type, hValue, true );
				printVariableValue( variableName, typeName, value, msg );
			}
			catch ( Exception ex )
			{
				printVariableValue ( variableName, typeName, null, msg );
			}
		}
	}

	private static void printVariableValue (
		String name, String type, String value, Message msg )
	{
		msg.print ( "{" );

		msg.printProperty ( "name", name );
		msg.print ( "," );

		msg.printProperty ( "type", type );

		if ( value != null )
		{
			msg.print ( "," );
			msg.printProperty ( "value", value );
		}

		msg.print ( "}" );
	}

	// -----------------------------------------------------------------

	private static boolean isScalarTypeName ( String typeName )
	{
		return d_scalarTypes.contains ( typeName );
	}

	// -----------------------------------------------------------------

	private static Set< String > initScalarTypes()
	{
		Set< String > scalarTypes
			= new TreeSet< String >(
				Arrays.asList( new String[] {
					Consts.BooleanClassName,
					Consts.CharacterClassName,
					Consts.ByteClassName,
					Consts.ShortClassName,
					Consts.IntegerClassName,
					Consts.LongClassName,
					Consts.FloatClassName,
					Consts.DoubleClassName,
					Consts.BooleanTypeName,
					Consts.CharacterTypeName,
					Consts.ByteTypeName,
					Consts.ShortTypeName,
					Consts.IntegerTypeName,
					Consts.LongTypeName,
					Consts.FloatTypeName,
					Consts.DoubleTypeName
		 } ) );

		return scalarTypes;
	}

	private static Set< String > d_scalarTypes = initScalarTypes();
}

// ---------------------------------------------------------------------
