// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.Iterator;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.*;
import java.text.ParseException;

import com.sun.jdi.VirtualMachine;

// ---------------------------------------------------------------------

public class CmdBreakpoints
{
    // -----------------------------------------------------------------

    public static void commandBreakInsert ( StringTokenizer t )
    {
        boolean bPending = false;
        boolean temporary = false;

        while ( t.hasMoreTokens() )
        {
            String token = t.nextToken();

            if ( token.equals ( "-f" ) )
                bPending = true;
            else if ( token.equals ( "-t" ) )
                temporary = true;
            else
            {
                try
                {
                    LogicalBreakpoint hBreakpoint = createBreakpointBySpec( token, temporary );

                    if ( hBreakpoint != null )
                    {
                        final String breakpointDescription = hBreakpoint.getMiDescription();

                        Message msg = new Message();
                        msg.print ( "^done,bkpt=" );
                        msg.println ( breakpointDescription );
                        Messenger.print( msg );
                    }
                }
                catch ( Exception e )
                {
                    final String errorMessage = e.getMessage();
                    Messenger.printError ( errorMessage );
                    e.printStackTrace();
                }
            }
        }
    }

    // -----------------------------------------------------------------

    public static void commandWatchInsert( StringTokenizer t )
    {
        boolean correctInput = false;

        Breakpoint.Kind kind = Breakpoint.Kind.WatchpointWrite;
        boolean flagTemporary = false;

        if ( t.hasMoreTokens() )
        {
            String field = null;

            String token = t.nextToken();
            if ( token.startsWith( "-" ) )
            {
                if ( token.equals ( "-r" ) )
                    kind = Breakpoint.Kind.WatchpointRead;
                else if ( token.equals ( "-a" ) )
                    kind = Breakpoint.Kind.WatchpointAccess;
                else if ( token.equals ( "-t" ) )
                    flagTemporary = true;
            }
            else
            {
                field = token;
            }

            if ( ( field == null ) && t.hasMoreTokens() )
                field = t.nextToken();

            if ( field != null )
            {
                correctInput = true;
                try
                {
                    LogicalWatchpoint hWatchpoint = createWatchpoint( kind, field, flagTemporary );
                    if ( hWatchpoint != null )
                    {
                        StringBuffer sb = new StringBuffer();
                        sb.append( "^done," );
                        final String watchpointKindStr = getWatchpointDescription( hWatchpoint );
                        sb.append( watchpointKindStr );
                        sb.append( "=");
                        final String bkptMiDescription = hWatchpoint.getMiDescription();
                        sb.append( bkptMiDescription );
                        final String breakpointDescription = sb.toString();
                        Messenger.println( breakpointDescription );
                    }
                    else
                    {
                        Messenger.printError ( "VM doesn't support watchpoints of that kind." );
                    }
                }
                catch ( Exception e )
                {
                    final String errorMessage = e.getMessage();
                    Messenger.printError ( errorMessage );
                    e.printStackTrace();
                }
            }
        }

        if ( ! correctInput )
            Messenger.printError ( "\"-break-watch\" takes '[-a|-r] field'." );
    }

    // -----------------------------------------------------------------

    public static void commandCatch( StringTokenizer t )
    {
        LogicalBreakpoint catchpoint = null;

        String catchKindStr = t.nextToken();

        Breakpoint.Kind catchKind = Breakpoint.Kind.Unknown;
        if ( catchKindStr.equals( "catch" ) )
        {
            catchKind = Breakpoint.Kind.CatchpointCatch;
        }
        else if ( catchKindStr.equals( "uncaught" ) )
        {
            catchKind = Breakpoint.Kind.CatchpointUncaught;
        }
        if ( catchKind != Breakpoint.Kind.Unknown )
        {
            try
            {
            	catchpoint = Instance.s_breakpointManager.createCatchpoint( catchKind );
            }
            catch ( Exception e )
            {
                Messenger.printError( "Cannot create catchpoint " + e.getMessage() );
            }
        }
    }

    // -----------------------------------------------------------------

    public static void commandBreakDelete ( StringTokenizer t )
    {
        commandBreakOperation ( t, EBreakpointOperation.BP_OPERATION_DELETE );
    }

    // -----------------------------------------------------------------

    public static void commandBreakEnable ( StringTokenizer t )
    {
        commandBreakOperation ( t, EBreakpointOperation.BP_OPERATION_ENABLE );
    }

    // -----------------------------------------------------------------

    public static void commandBreakDisable ( StringTokenizer t )
    {
        commandBreakOperation ( t, EBreakpointOperation.BP_OPERATION_DISABLE );
    }

    // -----------------------------------------------------------------

    public static void commandBreakSetCondition( StringTokenizer t )
    {
        commandBreakOperation ( t, EBreakpointOperation.BP_OPERATION_SET_CONDITION );
    }

    // -----------------------------------------------------------------

    public static void commandBreakSetPassCount( StringTokenizer t )
    {
        commandBreakOperation ( t, EBreakpointOperation.BP_OPERATION_SET_PASS_COUNT );
    }

    // -----------------------------------------------------------------

    public static void commandInfoBreak ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String strBreakpointId = t.nextToken();
            final BreakpointHandle bkHandle = new BreakpointHandle( strBreakpointId );
            final int breakpointId = bkHandle.d_logicalIndex;
            printBreakpoint ( breakpointId );
        }
        else
            printBreakpoints ( false );
    }

    // -----------------------------------------------------------------

    public static void commandMaintInfoBreak ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String strBreakpointId = t.nextToken();
            final BreakpointHandle bkHandle = new BreakpointHandle( strBreakpointId );
            final int breakpointId = bkHandle.d_logicalIndex;
            printBreakpoint ( breakpointId );
        }
        else
            printBreakpoints ( true );
    }

    // -----------------------------------------------------------------

    private static void printBreakpoints ( boolean bMaint )
    {
        Iterator< LogicalBreakpoint > it =
            Instance.s_breakpointManager.getBreakpoints().iterator();

        Message msg = new Message();

        while ( it.hasNext() )
        {
            final LogicalBreakpoint hBreakpoint = it.next();

            if ( bMaint || ( 0 < hBreakpoint.getLogicalIndex() ) )
            {
                final String bpDescription = hBreakpoint.getCliDescription();
                msg.printCliMessage ( bpDescription );
            }
        }

        msg.println ( "^done" );

        Messenger.print( msg );
    }

    // -----------------------------------------------------------------

    private static void printBreakpoint ( int bkLogicalIndex )
    {
    	BreakpointHandle bkHandle = new BreakpointHandle( bkLogicalIndex );
        LogicalBreakpoint hBreakpoint =
            Instance.s_breakpointManager.getBreakpoint ( bkHandle );

        Message msg = new Message();

        final String bpDescription = hBreakpoint.getCliDescription();
        msg.printCliMessage ( bpDescription );
        msg.println ( "^done" );

        Messenger.print( msg );
    }

    // ---------------------------------------------------------------------

    private enum EBreakpointOperation
    {
        BP_OPERATION_ENABLE,
        BP_OPERATION_DISABLE,
        BP_OPERATION_DELETE,
        BP_OPERATION_SET_CONDITION,
        BP_OPERATION_SET_PASS_COUNT
    };

    // ---------------------------------------------------------------------

    private static void commandBreakOperation (
        StringTokenizer t, EBreakpointOperation eOperation )
    {
        if ( t.hasMoreTokens() )
        {
            try
            {
                final String breakpointHandleStr = t.nextToken();
                final BreakpointManager breakpointManager = Instance.s_breakpointManager;
                final BreakpointHandle bkHandle = new BreakpointHandle( breakpointHandleStr );

                boolean bResult = false;

                switch ( eOperation )
                {
                    case BP_OPERATION_ENABLE:
                        bResult = breakpointManager.enable ( bkHandle, true );
                        break;

                    case BP_OPERATION_DISABLE:
                        bResult = breakpointManager.enable ( bkHandle, false );
                        break;

                    case BP_OPERATION_DELETE:
                        bResult = breakpointManager.remove ( bkHandle );
                        break;

                    case BP_OPERATION_SET_CONDITION:
                        bResult = setCondition( bkHandle, t );
                        break;

                    case BP_OPERATION_SET_PASS_COUNT:
                        bResult = setPassCount( bkHandle, t );
                        break;

                   default:
                       break;
                }

                Message msg = new Message();
                if ( ! bResult )
                    msg.format (
                        "~\"No breakpoint number %d.\\n\"\n", bkHandle.d_logicalIndex );

                msg.println ( "^done" );
                Messenger.print( msg );
            }
            catch ( ParseException e )
            {
                Messenger.printWarning( e.getMessage() );
            }
            catch ( NumberFormatException e )
            {
                Messenger.printWarning( "bad breakpoint number or incorrect pass count" );
            }
        }
    }

    // ---------------------------------------------------------------------

    private static boolean setCondition( BreakpointHandle bkHandle, StringTokenizer t )
    	throws ParseException
    {
        if ( t.hasMoreTokens() )
        {
            String condition = Utils.getRemainingText( t );
            if ( !condition.isEmpty() )
            {
                boolean result = Instance.s_breakpointManager.setCondition( bkHandle, condition );
                return result;
            }
        }
        throw new ParseException( "condition missed", 0 );
    }

    // ---------------------------------------------------------------------

    private static boolean setPassCount( BreakpointHandle bkHandle, StringTokenizer t )
		throws ParseException
    {
        if ( t.hasMoreTokens() )
        {
            String strPassCount = t.nextToken();
            int passCount = Integer.decode( strPassCount );
            boolean result = Instance.s_breakpointManager.setPassCount( bkHandle, passCount );
            return result;
        }
        else
        {
        	throw new ParseException( "pass-count missed", 0 );
        }
    }

    // ---------------------------------------------------------------------

    private static LogicalBreakpoint createBreakpointBySpec( String bpSpec, boolean temporary )
        throws Exception
    {
        /*
            If specified, location, can be one of:
            function
            filename:linenum
            *address

            address may be obtained from MethodManager with use
            of Method and codeIndex
         */
        LogicalBreakpoint hBreakpoint = null;

        final BreakpointManager breakpointManager = Instance.s_breakpointManager;
        if ( bpSpec.charAt ( 0 ) == '*' )
        {
            final String strAddress = bpSpec.substring ( 1 );
            final long address = Utils.parseAddress ( strAddress );

            hBreakpoint = breakpointManager.createCodeBreakpoint( address, temporary );
        }
        else
        {
            Matcher matcher = s_rxFileNumLocation.matcher( bpSpec );
            if ( matcher.matches() )
            {
    	        String fileName = matcher.group( 1 );
    	        int lineNum = Integer.parseInt( matcher.group( 2 ) );
                hBreakpoint
                    = breakpointManager.createSourceBreakpoint (
                        fileName, lineNum, temporary );
            }
            else
            {
                // uncaught routine may be unmangled (WinGDB can't mangle routine
                // name introduced by user in dialog Function Breakpoint)
                hBreakpoint = breakpointManager.createFunctionBreakpoint ( bpSpec, temporary );
            }
        }

        return hBreakpoint;
    }

    // ---------------------------------------------------------------------

    private static LogicalWatchpoint createWatchpoint(
    	Breakpoint.Kind watchpointKind,
        String classField,
        boolean flagTemporary ) throws Exception
    {
        LogicalWatchpoint watchpoint = null;

        final int endOfClassName = classField.lastIndexOf( '.' );
        if ( endOfClassName != -1 )
        {
            String className = classField.substring( 0, endOfClassName );
            String fieldName = classField.substring( endOfClassName + 1 );

            VirtualMachine vm = Instance.s_connection.getVirtualMachine();
            BreakpointManager bm = Instance.s_breakpointManager;
            switch ( watchpointKind )
            {
                case WatchpointRead:
                    if ( vm.canWatchFieldAccess() )
                        watchpoint = bm.createReadWatchpoint( className, fieldName, flagTemporary );
                    break;

                case WatchpointWrite:
                    if ( vm.canWatchFieldModification() )
                        watchpoint = bm.createWriteWatchpoint( className, fieldName, flagTemporary );
                    break;

                case WatchpointAccess:
                    if ( vm.canWatchFieldAccess() && vm.canWatchFieldModification() )
                        watchpoint = bm.createAccessWatchpoint( className, fieldName, flagTemporary );
                    break;

                default:
                    throw new Exception( "Unknown watchpoint kind!" );
            }
        }
        else
        {
            throw new Exception( "Class containing field must be specified." );
        }
        return watchpoint;
    }

    // ---------------------------------------------------------------------

    private static String getWatchpointDescription( LogicalWatchpoint hWatchpoint )
    	throws Exception
    {
        Breakpoint.Kind kind = hWatchpoint.getKind();
        String result = d_watchpoint2kindStr.get( kind );
        return result;
    }

    private static Hashtable< Breakpoint.Kind, String > initWatchpointKinds()
    {
        Hashtable< Breakpoint.Kind, String > mappings = new Hashtable< Breakpoint.Kind, String >();

        mappings.put( Breakpoint.Kind.WatchpointRead, "rwpt" );
        mappings.put( Breakpoint.Kind.WatchpointWrite, "wpt" );
        mappings.put( Breakpoint.Kind.WatchpointAccess, "awpt" );

        return mappings;
    }

    // -----------------------------------------------------------------

	private static Pattern s_rxFileNumLocation = Pattern.compile( "^(.*)\\:(\\d+)$" );

    private static Hashtable< Breakpoint.Kind, String > d_watchpoint2kindStr = initWatchpointKinds();

}
