// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;
import java.util.regex.*;
import com.sun.jdi.*;

// ---------------------------------------------------------------------

public class CmdInfo
{
    // -----------------------------------------------------------------

    public static void commandInfoTarget ( StringTokenizer t )
    {
        StringBuffer sst = new StringBuffer();

        sst.append ( "Symbols from \\\"" );
        sst.append ( Instance.s_debuggedProgramPath );
        sst.append ( "\\\".\\n\n" );
        sst.append ( "Local exec file:\\n\n" );
        sst.append ( "\\t`" );
        sst.append ( Instance.s_debuggedProgramPath );
        sst.append ( "', file type java.\\n\n" );
        sst.append ( "\\tEntry point: 0x0000000000000000\\n\n" );
        sst.append ( "\\t0x0000000000000000 - " );
        sst.append ( Utils.toAddressString( Long.MAX_VALUE ) );
        sst.append ( " is " );
        sst.append ( SectionName );
        sst.append ( "\\n\n" );

        Message msg = new Message();
        msg.printCliMessage ( sst.toString() );
        msg.println ( "^done" );
        Messenger.print( msg );
    }

    // -----------------------------------------------------------------

    public static void commandInfoSharedLibrary ( StringTokenizer t )
    {
        Message msg = new Message();
        msg.printCliMessage ( "No shared libraries loaded at this time.\\n" );
        msg.println ( "^done" );
        Messenger.print( msg );
    }

    // ---------------------------------------------------------------------

    public static void commandInfoThreads ( StringTokenizer t )
    {
        final List< ThreadManager.ThreadInfo > threads =
            Instance.s_threadManager.getThreads();

        final int currentThreadId = Instance.s_threadManager.getCurrentThreadId();

        for ( Iterator< ThreadManager.ThreadInfo > iThread = threads.iterator();
              iThread.hasNext(); )
        {
            try
            {
                ThreadManager.ThreadInfo threadInfo = iThread.next();

                final boolean bCurrent = ( threadInfo.d_id == currentThreadId );

                StringBuffer sst = new StringBuffer();

                sst.append ( bCurrent ? "* " : "  " );
                sst.append ( threadInfo.d_id );
                sst.append ( " " );
                sst.append ( threadInfo.d_thread.name() );
                sst.append ( "  " );

                final StackFrame hFrame = threadInfo.d_thread.frame ( 0 );
                final Location hLocation = hFrame.location();

                final String methodName =
                    Instance.s_methodManager.method2manglid ( hLocation.method() );

                final int lineNumber = hLocation.lineNumber();
                String sourceName = null;

                if ( lineNumber != -1 )
                    try
                    {
                        sourceName = hLocation.sourceName();
                    }
                    catch ( AbsentInformationException ex )
                    {
                    }

                if ( lineNumber != -1 && sourceName != null )
                {
                    sst.append ( methodName );
                    sst.append ( " () at " );
                    sst.append ( sourceName );
                    sst.append ( ":" );
                    sst.append ( lineNumber );
                }
                else
                {
                    final Long offset = MethodManager.getOffset ( hLocation );
                    final Long address = Instance.s_methodManager.methodoffset2address (
                        hLocation.method(), offset );

                    sst.append ( Utils.toAddressString( address ) );
                    sst.append ( " in " );
                    sst.append ( methodName );
                }

                Message msg = new Message();
                msg.printCliMessage ( sst.toString() );
                Messenger.print( msg );
            }
            catch ( Exception e )
            {
            }
        }

        Message msg = new Message();
        msg.println ( "^done" );
        Messenger.print( msg );
    }

    // -----------------------------------------------------------------

    public static void commandInfoSymbol ( StringTokenizer t )
    {
        if ( ! t.hasMoreTokens() )
        {
            Messenger.printError ( "Argument required." );
            return;
        }

        final String strAddress = t.nextToken();
        final long address = Utils.parseAddress ( strAddress );

        StringBuffer sst = new StringBuffer();

        final Method hMethod = Instance.s_methodManager.address2method ( address );

        if ( hMethod != null )
        {
            final long offset = Instance.s_methodManager.address2offset ( address );
            final String methodId = Instance.s_methodManager.method2manglid ( hMethod );

            sst.append ( methodId );
            sst.append ( " + " );
            sst.append ( offset );
            sst.append ( " in section " );
            sst.append ( SectionName );
            sst.append ( "\\n" );
        }
        else
        {
            sst.append ( "No symbol matches " + strAddress + ".\\n" );
        }

        Message msg = new Message();
        msg.printCliMessage ( sst.toString() );
        msg.println ( "^done" );
        Messenger.print( msg );
    }

    // -----------------------------------------------------------------

    public static void commandInfoFrame ( StringTokenizer t )
    {
        String strFrameId = null;
        String strThreadId = null;

        if ( t.hasMoreTokens() )
            strFrameId = t.nextToken();

        if ( t.hasMoreTokens() )
            strThreadId = t.nextToken();

        final int threadId = (
            strThreadId != null ?
                Integer.decode ( strThreadId )
                : Instance.s_threadManager.getCurrentThreadId() );

        final int frame = (
            strFrameId != null ?
                Integer.decode ( strFrameId )
                : Instance.s_threadManager.getCurrentFrameIndex ( threadId ) );

        try
        {
            final StackFrame hFrame =
                Instance.s_threadManager.getFrameById ( threadId, frame );

            final long frameAddress =
                Instance.s_threadManager.getAddressByFrame ( hFrame );

            final String strFrameAddress =
                Utils.toAddressString( frameAddress );

            StringBuffer sst = new StringBuffer();

            sst.append ( "Stack frame at " );
            sst.append ( strFrameAddress );
            sst.append ( ":\\n\n" );

            // VS will see this as J#
            sst.append ( " source language Java.\\n\n" );

            sst.append ( " Arglist at " );
            sst.append ( strFrameAddress );
            sst.append ( ",\\n\n" );

            sst.append ( " Locals at " );
            sst.append ( strFrameAddress );
            sst.append ( ",\\n\n" );

            Message msg = new Message();
            msg.printCliMessage ( sst.toString() );
            msg.println ( "^done" );
            Messenger.print( msg );
        }
        catch ( IncompatibleThreadStateException e )
        {
            Messenger.printError ( "Thread is not suspended." );
        }
    }

    // -----------------------------------------------------------------

    public static void commandInfoLine ( StringTokenizer t )
    {
        if ( ! t.hasMoreTokens() )
        {
            Messenger.printError ( "Argument required." );
            return;
        }

    	String strLocation = Utils.getRemainingText( t );

        String output = null;

        if ( strLocation.charAt ( 0 ) == '*' )
        {
            output = commandInfoLineAddress( strLocation );
        }
        else
        {
            Matcher matcher = s_rxFileNumLocation.matcher( strLocation );
            if ( matcher.matches() )
            {
    	        String fileName = matcher.group( 1 );
    	        int lineNum = Integer.parseInt( matcher.group( 2 ) );
                output = commandInfoLineLocation( fileName, lineNum );
            }
            else
            {
                output = "Unsupported location format.";
            }
        }

        Message msg = new Message();
        msg.printCliMessage ( output );
        msg.println ( "^done" );
        Messenger.print( msg );
    }

    private static String commandInfoLineAddress( String strLocation )
    {
        String result = "";

        final String strAddress = strLocation.substring ( 1 );
        final long address = Utils.parseAddress ( strAddress );

        final Method hMethod = Instance.s_methodManager.address2method ( address );
        if ( hMethod != null )
        {
            final long offset = Instance.s_methodManager.address2offset ( address );
            final Location location = hMethod.locationOfCodeIndex ( offset );

            if ( location != null )
                result = resolveLocation( location );
        }

        if ( result.isEmpty() )
        {
            StringBuffer sb = new StringBuffer();
            sb.append ( "No line number information available for address " );
            sb.append ( strAddress );
            sb.append ( "\\n" );
            result = sb.toString();
        }

    	return result;
    }

    private static String commandInfoLineLocation( String fileName, int lineNum )
    {
    	String result = "";

	    List< Location > locations
		    = Instance.s_sourceManager.getExactLocations( fileName, lineNum );
	    if ( ( locations != null ) && !locations.isEmpty() )
	    {
		    Location location = locations.get( 0 );
            result = resolveLocation( location );
	    }

        if ( result.isEmpty() )
            result = "No line number information available.\\n";

    	return result;
    }

	private static String resolveLocation( Location location )
    {
		String result = "";

        final int lineNumber = location.lineNumber();

        try
        {
            Method hMethod = location.method();

            final List< Location > lineLocations = hMethod.locationsOfLine ( lineNumber );

            long minOffset = Long.MAX_VALUE;
            long maxOffset = Long.MIN_VALUE;

            for ( Iterator< Location > iLocation = lineLocations.iterator(); iLocation.hasNext(); )
            {
                final Location lineLocation = iLocation.next();
                final Long lineOffset = MethodManager.getOffset ( lineLocation );

                if ( lineOffset < minOffset )
                    minOffset = lineOffset;

                if ( lineOffset > maxOffset )
                    maxOffset = lineOffset;
            }

            final long startAddress =
                Instance.s_methodManager.methodoffset2address ( hMethod, minOffset );

            final long endAddress =
                Instance.s_methodManager.methodoffset2address ( hMethod, maxOffset );

            final String methodId = Instance.s_methodManager.method2manglid ( hMethod );

            StringBuffer sst = new StringBuffer();

            sst.append ( "Line " );
            sst.append ( location.lineNumber() );
            sst.append ( " of \\\"" );
            sst.append ( location.sourceName() );
            sst.append ( "\\\" starts at address " );
            sst.append ( Utils.toAddressString( startAddress ) );
            sst.append ( " <" );
            sst.append ( methodId );
            sst.append ( "+" );
            sst.append ( minOffset );
            sst.append ( "> and ends at " );
            sst.append ( Utils.toAddressString( endAddress ) );
            sst.append ( " <" );
            sst.append ( methodId );
            sst.append ( "+" );
            sst.append ( maxOffset );
            sst.append ( ">.\\n" );
            result = sst.toString();
        }
        catch ( AbsentInformationException e )
        {
        }
        return result;
	}

    // -----------------------------------------------------------------

    public static void commandMaintDemangle ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String manglid = t.nextToken();
            final String demangled = Instance.s_methodManager.demangle ( manglid );

            Message msg = new Message();
            msg.printCliMessage ( demangled );
            msg.println ( "^done" );
            Messenger.print( msg );
        }
        else
        {
            Messenger.printError (
                "\"maintenance demangle\" takes an argument to demangle." );
        }
    }

    // -----------------------------------------------------------------

    public static void commandWhatis ( StringTokenizer t )
    {
        if ( ! t.hasMoreTokens() )
        {
            Messenger.printError ( "Argument is required." );
            return;
        }

        final String expression = Utils.unquote ( t.nextToken() );

        if ( expression.endsWith ( "()" ) )
        {
            final String functionName =
                expression.substring ( 0, expression.length() - 2 );

            final Method hMethod = Instance.s_methodManager.manglid2method ( functionName );

            if ( hMethod != null )
            {
                final String returnTypeName = hMethod.returnTypeName();

                StringBuffer sst = new StringBuffer();
                sst.append ( "type = " );
                sst.append ( returnTypeName );
                sst.append ( "\\n" );

                Message msg = new Message();
                msg.printCliMessage ( sst.toString() );
                msg.println ( "^done" );
                Messenger.print( msg );
            }
            else
            {
                StringBuffer sst = new StringBuffer();
                sst.append ( "No symbol \\\"" );
                sst.append ( functionName );
                sst.append ( "\\\" in current context.\\n" );

                Messenger.printError ( sst.toString() );
            }
        }
    }

    // -----------------------------------------------------------------

    public static void commandFileListExecSourceFiles ( StringTokenizer t )
    {
        Message msg = new Message();
        msg.print ( "^done,files=[" );

        SourceTraverser sourceTraverser = Instance.s_sourceManager.getAllSources();

        boolean firstSrc = true;
        while ( sourceTraverser.hasNext() )
        {
            if ( firstSrc )
                firstSrc = false;
            else
                msg.print ( "," );

            final SourceInfo hInfo = sourceTraverser.getNext();
            final String srcFileName = hInfo.d_sourceFileName;
            final String absoluteSrcPath = hInfo.getAbsolutePath();

            assert absoluteSrcPath != null;
            String fileInfoStr = "{file=\"" + srcFileName + "\",fullname=\"" + absoluteSrcPath + "\"}";
            msg.print ( fileInfoStr );
        }

        msg.println ( "]" );
        Messenger.print( msg );
    }

    // -----------------------------------------------------------------

    public static void commandSymbolListLines ( StringTokenizer t )
    {
        if ( ! t.hasMoreTokens() )
        {
            Messenger.printError ( "Argument is required." );
            return;
        }

        final String fileName = Utils.unquote ( t.nextToken() );

        final List< Location > locations =
            Instance.s_sourceManager.getAllLocationsForFile ( fileName );

        StringBuffer sst = new StringBuffer();

        sst.append ( "^done,lines=[" );

        if ( locations != null )
        {
        	boolean firstLocation = true;
	        for ( Location location : locations )
	        {
                MethodManager methodManager = Instance.s_methodManager;
	            final Method hMethod = location.method();
                final Long offset = MethodManager.getOffset ( location );
	            final long address = methodManager.methodoffset2address ( hMethod, offset );

	            if ( firstLocation )
	            	firstLocation = false;
	            else
	                sst.append ( "," );

	            sst.append ( "{pc=\"" );
	            sst.append ( Utils.toAddressString( address ) );
	            sst.append ( "\",line=\"" );
	            sst.append ( location.lineNumber() );

	            sst.append ( "\"}" );
	        }
        }

        sst.append ( "]" );

        Messenger.println ( sst.toString() );
    }

    // -----------------------------------------------------------------

    public static void commandDataEvaluateExpression( StringTokenizer t )
    {
        try
        {
            int threadId = Instance.s_threadManager.getCurrentThreadId();

            if ( threadId == ThreadManager.InvalidThread )
            {
                Messenger.printError( "no current thread" );
                return;
            }
            else
            {
                final int currentFrameId =
                    Instance.s_threadManager.getCurrentFrameIndex ( threadId );

                if ( currentFrameId == ThreadManager.InvalidThread )
                {
                    Messenger.printError( "no current frame" );
                    return;
                }
                else
                {
                    final StackFrame currentFrame =
                        Instance.s_threadManager.getFrameById ( threadId, currentFrameId );

                    if ( currentFrame != null )
                    {
						String expression = null;
						if ( t.hasMoreTokens() )
						    expression = Utils.getRemainingText( t );
						if ( ( expression == null ) || expression.isEmpty() )
						{
			                Messenger.printError ( "Empty expression." );
						}
						else
						{
						    String valueStr = null;
                            Matcher matcher = s_rxSizeOfExpression.matcher( expression );
                            if ( matcher.matches() )
                            {
	                            String typeName = matcher.group( 1 );
	                            int typeSize = getTypeSize( typeName );
                                valueStr = Integer.toString( typeSize );
					        }
					        else
					        {
                                Expression hExpr = ExpressionParser.parse(
                                    expression, Instance.getVirtualMachine(), currentFrame );
                                Type hType = hExpr.getType();
                                Value hValue = hExpr.getValue();
                                valueStr = Instance.getValueText( expression, currentFrame, hType, hValue, false );
                            }
                            String resultStr = "^done,value=\"" + valueStr + '"';
                            Messenger.println( resultStr );
                        }
					}
                }
            }
        }
        catch ( InvocationException e )
        {
            Messenger.printError (
                "Exception in evaluate: " + e.exception().referenceType().name() );
        }
        catch ( Exception e )
        {
            String exMessage = e.getMessage();

            if ( exMessage == null )
                Messenger.printError ( "Exception in evaluate: " + exMessage );
            else
            {
                final String s = e.toString();
                Messenger.printError ( "Exception in evaluate: " + s );
            }
        }
    }

    // -----------------------------------------------------------------

    static int getTypeSize( String typeName )
    	throws Exception
    {
        if ( s_typeName2size.isEmpty() )
        {
            s_typeName2size.put( "boolean", 1 );
            s_typeName2size.put( "byte", 1 );
            s_typeName2size.put( "char", 2 );
            s_typeName2size.put( "short", 2 );
            s_typeName2size.put( "int", 4 );
            s_typeName2size.put( "long", 8 );

            s_typeName2size.put( "java.lang.Boolean", 1 );
            s_typeName2size.put( "java.lang.Byte", 1 );
            s_typeName2size.put( "java.lang.Character", 2 );
            s_typeName2size.put( "java.lang.Short", 2 );
            s_typeName2size.put( "java.lang.Integer", 4 );
            s_typeName2size.put( "java.lang.Long", 8 );
            s_typeName2size.put( "java.lang.Float", 4 );
            s_typeName2size.put( "java.lang.Double", 8 );

            // unsigned types are unsupported by Java, but user may want
            // format e.g. watch values with unsigned format-specifier
            s_typeName2size.put( "unsigned char", 2 );
            s_typeName2size.put( "unsigned short", 2 );
            s_typeName2size.put( "unsigned int", 4 );
            s_typeName2size.put( "unsigned long", 8 );
            s_typeName2size.put( "float", 4 );
            s_typeName2size.put( "double", 8 );
        }

        if ( !s_typeName2size.containsKey( typeName ) )
            throw new Exception( "Unknown type " + typeName );
        int result = s_typeName2size.get( typeName );
        return result;
    }

    // -----------------------------------------------------------------

	public static final String SectionName = ".android";

    // -----------------------------------------------------------------

	private static Pattern s_rxFileNumLocation = Pattern.compile( "^(.*)\\:(\\d+)$" );
	private static Pattern s_rxSizeOfExpression = Pattern.compile( "^\\\"sizeof\\((.*)\\)\\\"$" );

    private static Map< String, Integer > s_typeName2size =
        new TreeMap< String, Integer >();

}

// ---------------------------------------------------------------------
