//---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

// ---------------------------------------------------------------------

class PrepareStopInfo extends LogicalBreakpointVisitor
{
    PrepareStopInfo( BreakpointHandle bkHandle )
    {
        d_bkHandle = bkHandle;
    }

    // -----------------------------------------------------------------

    String getResult()
    {
        return d_stopInfo;
    }

    // -----------------------------------------------------------------

    void visitReadWatchpoint( LogicalReadWatchpoint bkpt )
    {
        prepareWatchpointInfo( "read-watchpoint-trigger" );
    }

    void visitWriteWatchpoint( LogicalWriteWatchpoint bkpt )
    {
        prepareWatchpointInfo( "watchpoint-trigger" );
    }

    void visitAccessWatchpoint( LogicalAccessWatchpoint bkpt )
    {
        prepareWatchpointInfo( "access-watchpoint-trigger" );
    }

    private void prepareWatchpointInfo( String reason )
    {
        d_stopInfo = String.format(
            "reason=\"%s\",bkptno=\"%d\"",
            reason,
            d_bkHandle.d_logicalIndex );
    }

    // -----------------------------------------------------------------

    void visitCatchpoint( LogicalCatchpoint bkpt )
    {
        /*
            ~"Catchpoint 2 (exception uncaught), 0x00007ffff7b91cd0 in __cxa_begin_uncaught ()\n"
            ~"   from /usr/lib/libstdc++.so.6\n"
            *stopped,frame={addr="0x00007ffff7b91cd0",func="__cxa_throw",args=[],from="/usr/lib/libstdc++.so.6"},thread-id="1",stopped-threads="all",core="0"

            ~"Catchpoint 2 (exception caught), 0x00007ffff7b91cd0 in __cxa_begin_catch ()\n"
            ~"   from /usr/lib/libstdc++.so.6\n"
            *stopped,frame={addr="0x00007ffff7b91cd0",func="__cxa_begin_catch",args=[],from="/usr/lib/libstdc++.so.6"},thread-id="1",stopped-threads="all",core="0"

            WinJDB version:
            *stopped,frame={func="__cxa_begin_catch"},stopped-threads="all"
        */
        StringBuffer sb = new StringBuffer();

        String catchRoutineName = bkpt.getPredefinedRoutineName();
        sb.append( "frame={func=\"" );
        sb.append( catchRoutineName );
        sb.append( "\"" );

        sb.append( ",stopped-threads=\"all\"" );

        d_stopInfo = sb.toString();
    }

    // -----------------------------------------------------------------

    void visitBreakpoint( LogicalBreakpoint bkpt )
    {
        d_stopInfo = String.format (
            "reason=\"breakpoint-hit\",bkptno=\"%d\"",
            d_bkHandle.d_logicalIndex
        );
    }

    // -----------------------------------------------------------------

    private final BreakpointHandle d_bkHandle;
    private String d_stopInfo;
}

// ---------------------------------------------------------------------

public class Controller implements IEventHandler
{
    // ---------------------------------------------------------------------

    public EventStatus onVirtualMachineStartEvent ( VMStartEvent se )
    {
        Thread.yield();

        Message msg = new Message();
        msg.printCliMessage ( "[Java VM started]" );
        Messenger.print( msg );

        EventStatus result = d_bStartSuspended ? EventStatus.Stop : EventStatus.Resume;
        return result;
    }

    // ---------------------------------------------------------------------

    public EventStatus onVirtualMachineExitEvent ( VMDeathEvent e )
    {
        Message msg = new Message();
        msg.printCliMessage ( "[Java VM terminated]" );
        Messenger.print( msg );

        return EventStatus.Resume;
    }

    // ---------------------------------------------------------------------

    public EventStatus onVirtualMachineDisconnectEvent ( VMDisconnectEvent e )
    {
    	return EventStatus.Resume;
    }

    // ---------------------------------------------------------------------

    public EventStatus onThreadStartEvent ( ThreadStartEvent e )
    {
        Instance.s_threadManager.onThreadStart ( e.thread() );
        return EventStatus.Resume;
    }

    // ---------------------------------------------------------------------

    public EventStatus onThreadDeathEvent ( ThreadDeathEvent e )
    {
        Instance.s_threadManager.onThreadEnd ( e.thread() );
        return EventStatus.Resume;
    }

    // ---------------------------------------------------------------------

    public EventStatus onClassPrepareEvent ( ClassPrepareEvent event )
    {
        EventStatus result = EventStatus.Resume;

        Instance.s_sourceManager.addClass ( event.referenceType() );
        if ( Instance.s_breakpointManager.resolve ( event ) )
            result = prepareBreakpointBoundNotification();
        return result;
    }

    // ---------------------------------------------------------------------

    public EventStatus onClassUnloadEvent ( ClassUnloadEvent e )
    {
        return EventStatus.Resume;
    }

    // ---------------------------------------------------------------------

    public EventStatus onBreakpointEvent ( BreakpointEvent be )
    {
        return processBreakpointEvent( be );
    }

    // ---------------------------------------------------------------------

    public EventStatus onWatchpointEvent ( WatchpointEvent fwe )
    {
        return processBreakpointEvent( fwe );
    }

    // ---------------------------------------------------------------------

    public EventStatus onExceptionEvent ( ExceptionEvent ee )
    {
        return processBreakpointEvent( ee );
    }

    // ---------------------------------------------------------------------

    private EventStatus processBreakpointEvent( Event event )
    {
        EventStatus result = EventStatus.Stop;

        Thread.yield();

        EventRequest eventRequest = event.request();

        if ( ( eventRequest != null ) && checkBkCondition( eventRequest ) && checkPassCounter( eventRequest ) )
        {
            final int hitCount = ( Integer ) eventRequest.getProperty ( PhysicalBreakpoint.HitCountPropName );
            eventRequest.putProperty ( PhysicalBreakpoint.HitCountPropName, hitCount + 1 );

            final BreakpointHandle bkHandle
                = ( BreakpointHandle ) eventRequest.getProperty ( PhysicalBreakpoint.HandlePropName );

            PrepareStopInfo prepareStopInfo = new PrepareStopInfo( bkHandle );
            LogicalBreakpoint breakpoint = Instance.s_breakpointManager.getBreakpoint( bkHandle );
            breakpoint.accept( prepareStopInfo );
            d_stopInfo = prepareStopInfo.getResult();
        }
        else
        {
            // if condition set on breakpoint wasn't fulfilled or breakpoint has set pass counter
            // which didn't expired then dont' stop but continue execution
            result = EventStatus.Resume;
        }

        return result;
    }

    // ---------------------------------------------------------------------

    public EventStatus onStepEvent ( StepEvent se )
    {
        Thread.yield();
        d_stopInfo = "reason=\"end-stepping-range\"";
        return EventStatus.Stop;
    }

    // ---------------------------------------------------------------------

    public EventStatus onMethodEntryEvent ( MethodEntryEvent me )
    {
        Thread.yield();
        return EventStatus.Resume;
    }

    // ---------------------------------------------------------------------

    public EventStatus onMethodExitEvent ( MethodExitEvent me )
    {
        Thread.yield();

        return EventStatus.Resume;
    }

    // ---------------------------------------------------------------------

    public void onResumeExecution()
    {
        if ( d_breakpointBound )
        {
            d_breakpointBound = false;
            Messenger.println( "&\"observer_notify_solib_loaded() called\\n\"" );
        }
    }

    public void onVirtualMachineInterrupted()
    {
        if ( d_breakpointBound )
        {
            d_breakpointBound = false;
            d_stopInfo = "reason=\"shlib-event\"";
        }
        onDebuggerStopped ( true );
    }

    // ---------------------------------------------------------------------

    public void onGenericEvent ( Event event )
    {
    }

    // ---------------------------------------------------------------------

    public void onDebugBreak()
    {
        d_stopInfo = "reason=\"signal-received\",signal-name=\"SIGTRAP\"";
        onDebuggerStopped ( true );
    }

    // ---------------------------------------------------------------------

    public void onInterruptExited()
    {
        d_stopInfo = "reason=\"exited-signalled\",signal-name=\"SIGKILL\"";
        onDebuggerStopped ( false );
    }

    // ---------------------------------------------------------------------

    public void loadClasses()
    {
        VirtualMachine machine = Instance.s_connection.getVirtualMachine();
        final List< ReferenceType > classes = machine.allClasses();
        for ( ReferenceType type : classes )
        {
        	Instance.s_sourceManager.addClass( type );
        }
    }

    // ---------------------------------------------------------------------

    public void onDebuggerStopped ( boolean bPrintLocation )
    {
        // FIXME - what's that? to kick out?
        //Thread.yield();

        if ( d_stopInfo != null )
        {
            Message msg = new Message();
            msg.print ( "*stopped," + d_stopInfo );
            if ( bPrintLocation )
                printCurrentLocation( msg );
            msg.println();
            Messenger.print( msg );
            Messenger.printPrompt();
            d_stopInfo = null;
        }

        Instance.s_commandExecutor.notifyDebuggerStopped();
    }

    // ---------------------------------------------------------------------

    public EventStatus prepareBreakpointBoundNotification()
    {
        EventStatus result = EventStatus.Resume;
        SettingsManager settingsManager = Instance.s_settingsManager;
        boolean stopOnSolibEvents = settingsManager.isValue( "stop-on-solib-events", "1" );
        if ( stopOnSolibEvents )
        {
            result = EventStatus.Stop;
            d_breakpointBound = true;
        }
        else
        {
            boolean debugObserverEnabled = settingsManager.isValue( "debug", "observer 1" );
            if ( debugObserverEnabled )
                d_breakpointBound = true;
        }
        return result;
    }

    // ---------------------------------------------------------------------

    private void printFrame ( StackFrame hFrame, Message msg )
    {
        msg.format ( ",frame={" );

        Location hLocation = hFrame.location();
        Method method = hLocation.method();

        MethodManager methodManager = Instance.s_methodManager;

        final Long offset = MethodManager.getOffset ( hLocation );
        final long address = methodManager.methodoffset2address ( method, offset );
        final String addressStr = Utils.toAddressString( address );
        msg.format ( "addr=\"%s\"", addressStr );

        final String funcName = methodManager.method2manglid( method );
        msg.format ( ",func=\"%s\"", funcName );

        try
        {
            final String sourceName = hLocation.sourceName();
            msg.format ( ",file=\"%s\"", sourceName );

            final String absoluteSourcePath =
                Instance.s_sourceManager.getAbsolutePath( hLocation );
            if ( absoluteSourcePath != null )
                msg.format ( ",fullname=\"%s\"", absoluteSourcePath );

            msg.format ( ",line=\"%d\"", hLocation.lineNumber());
        }
        catch ( AbsentInformationException e )
        {
        }
        msg.print ( "}");
    }

    // ---------------------------------------------------------------------

    private void printCurrentLocation( Message msg )
    {
        final int threadId = Instance.s_threadManager.getCurrentThreadId();

        if ( threadId == ThreadManager.InvalidThread )
        {
            // no thread - stop on machine initialization, don't print anything
            // as it confuses WinGDB
            return;
        }

        final int currentFrameId =
            Instance.s_threadManager.getCurrentFrameIndex ( threadId );

        if ( currentFrameId == -1 )
        {
            // FIXME: print an error message or not?
            return;
        }

        try
        {
            final StackFrame frame =
                Instance.s_threadManager.getFrameById ( threadId, currentFrameId );

            if ( frame != null )
            {
                msg.format ( ",thread-id=\"%d\"", threadId );
                printFrame ( frame, msg );
            }
        }
        catch ( IncompatibleThreadStateException e )
        {
        }
        msg.println();
    }

    // ---------------------------------------------------------------------

    boolean checkBkCondition( EventRequest bkRequest )
    {
        boolean stop = true;
        final String condition = ( String ) bkRequest.getProperty ( PhysicalBreakpoint.ConditionPropName );
        if ( Utils.isNotEmpty( condition ) )
        {
            StackFrame currentFrame = Instance.s_threadManager.getCurrentFrame();
            if ( currentFrame != null )
            {
            	try
            	{
	                Expression expression
	                    = ExpressionParser.parse(
	                        condition, Instance.getVirtualMachine(), currentFrame );
	                Value value = expression.getValue();
	                String valueStr = Instance.getValueText ( value, currentFrame );
	                if ( ( valueStr == null ) || valueStr.isEmpty()
						|| ( valueStr.equals( Consts.NullValue ) ) || ( valueStr.equals( "null" ) )
	                    || ( valueStr.equals( "0" ) ) || ( valueStr.equals( "false" ) ) )
	                {
	                    stop = false;
	                }
            	}
            	catch ( XParseError e )
            	{
	                stop = false;
            	}
                catch ( Exception e )
                {
	                stop = false;
                }
            }
        }
        return stop;
    }

    // ---------------------------------------------------------------------

    boolean checkPassCounter( EventRequest bkRequest )
    {
        boolean stop = true;
        Integer passCounter = ( Integer ) bkRequest.getProperty ( PhysicalBreakpoint.PassCounterPropName );
        if ( passCounter != null )
        {
            if ( 1 < passCounter )
            {
                --passCounter;
                stop = false;
            }
            else
            {
                passCounter = null;
            }
            bkRequest.putProperty ( PhysicalBreakpoint.PassCounterPropName, passCounter );
        }
        return stop;
    }

    // ---------------------------------------------------------------------

    void runInitScript( String scriptPath )
    {
        CommandExecutor commandExecutor = Instance.s_commandExecutor;
        try
        {
            commandExecutor.enableScriptMode( true );
            if ( !scriptPath.isEmpty() )
            {
                FileInputStream fstream = new FileInputStream( scriptPath );
                DataInputStream in = new DataInputStream( fstream );
                BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
                String strLine;
                while ( ( strLine = br.readLine() ) != null )
                {
                    if ( !strLine.isEmpty() && ( strLine.trim().charAt( 0 ) != '#' ) )
                    {
                        Messenger.println ( strLine );
                        StringTokenizer st = new StringTokenizer( strLine );
                        Instance.s_commandExecutor.execute( st );
                    }
                }
                in.close();
            }
        }
        catch ( Exception e )
        {
            System.err.println( "Script execution error: " + e.getMessage() );
        }
        finally
        {
            commandExecutor.enableScriptMode( false );
        }
    }

    // ---------------------------------------------------------------------

    public Controller( String scriptPath ) throws Exception
    {
        Instance.s_controller = this;

        printBanner();

        try
        {
            Thread.currentThread().setPriority( Thread.NORM_PRIORITY );

            runInitScript( scriptPath );

            if ( !Instance.s_halted )
                Instance.s_commandExecutor.run();
        }
        catch ( VMDisconnectedException e )
        {
            Instance.s_eventDispatcher.flushEventsQueue();
        }
    }

    private void printBanner()
    {
        Message msg = new Message();
        msg.printBanner ( "WinGDB 2.2 Java Debugger for Visual Studio" );
        msg.printBanner ( "(C) 2010-2012 WinGDB.com" );
        Messenger.print( msg );
    }

    // ---------------------------------------------------------------------

    public void setStartSuspended ( boolean bStartSuspended )
    {
        d_bStartSuspended = bStartSuspended;
    }

    // ---------------------------------------------------------------------

    public static void main ( String argv[] )
    {
        //test();

        InstanceInitData initData = new InstanceInitData();
        final int DefaultConnectionTimeOut = 5000;
        initData.d_timeOut = DefaultConnectionTimeOut;

        String cmdLine = "";
        String javaParams = "";
        String initScript = "";
        int traceFlags = VirtualMachine.TRACE_NONE;
        String connectSpec = null;

        for ( int i = 0; i < argv.length; i++ )
        {
            String token = argv [ i ];

            if ( isCommonJVMParam( token ) )
            {
                javaParams = addParam ( javaParams, token );
            }
            else if ( token.equals( "-classpath" ) )
            {
                if ( i == ( argv.length - 1 ) )
                {
                    Messenger.println ( "No classpath specified." );
                    return;
                }
                javaParams = addParam( javaParams, token );
                javaParams = addParam( javaParams, argv[++i] );
            }
            else if ( token.equals( "-initscript" ) )
            {
				initScript = argv[++i];
            }
            else if ( token.equals( "-mainclass" ) )
            {
                initData.d_mainClassName = argv[++i];
            }
            else if ( token.equals( "-timeout" ) )
            {
                initData.d_timeOut = Integer.parseInt( argv[++i] );
            }
            else
            {
            	if ( cmdLine.isEmpty() )
            		initData.d_debuggedProgramPath = token;

                cmdLine = addParam ( "", token );

                for ( i++; i < argv.length; i++ )
                {
                    cmdLine = addParam ( cmdLine, argv[i] );
                }
                break;
            }
        }

        cmdLine = cmdLine.trim();
        javaParams = javaParams.trim();

        try
        {
            Instance.init( initData );
            new Controller( initScript );
        }
        catch ( Exception e )
        {
            Messenger.printError ( "Internal exception: " + e.toString() );
            e.printStackTrace();
        }
    }

    private static boolean isCommonJVMParam( String param )
    {
        if ( d_commonSwitches == null )
        {
            d_commonSwitches = new HashSet< String >( Arrays.asList( new String[] {
                "-v",
                "-noasyncgc",
                "-prof",
                "-verify",
                "-noverify",
                "-verifyremote",
                "-verbosegc" } ) );
        }

        if ( d_commonParams == null )
        {
            d_commonParams = new ArrayList< String >( Arrays.asList( new String[] {
                "-v:",
                "-verbose",
                "-D",
                "-X",
                "-ms",
                "-mx",
                "-ss",
                "-oss" } ) );
        }

        boolean result = d_commonSwitches.contains( param );
        if ( !result )
        {
            for ( String commonParam : d_commonParams )
            {
                if ( param.startsWith( commonParam ) )
                {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------

    private static String addParam ( String key, String value )
    {
        StringBuffer sb = new StringBuffer( key );
        if ( Utils.hasWhitespace( value ) || ( value.indexOf( ',' ) != -1 ) )
        {
            sb.append( '"' );
            CharacterIterator it = new StringCharacterIterator( value );
            for( char chr = it.first()
            	; chr != CharacterIterator.DONE
            	; chr = it.next()
            	)
            {
                if ( chr == '"' )
                    sb.append( '\\' );
                sb.append( chr );
            }
            sb.append( "\" " );
        }
        else
        {
            sb.append( value );
            sb.append( ' ' );
        }

        String result = sb.toString();
        return result;
    }

    // ---------------------------------------------------------------------

    private static void test()
    {
    }

    // ---------------------------------------------------------------------

    private boolean d_bStartSuspended = false;
    private String d_stopInfo = null;
    private boolean d_breakpointBound = false;

    private static Set< String > d_commonSwitches = null;
    private static List< String > d_commonParams = null;

    // ---------------------------------------------------------------------
}
