// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.StringTokenizer;
import java.util.concurrent.locks.*;
import java.util.*;
import java.io.*;

// ---------------------------------------------------------------------

/*
    WinJDB executes commands in two modes
    0) script mode
    in such mode executor gets commands one-by-one from input stream and
    has to wait for asynchronous commands until they complete
    e.g. when runs command -exec-continue -exec-next etc. then cannot get
    next command until asynchronous command completes e.g. debugger stops
    at breakpoint

    1) console mode
    in such mode executor read commands without any blocking, but when
    asynchronous command is running it ignores all commands except one
    which interrupts debugging (visible for user in Visual Studio as
    "Break All")
*/
public class CommandExecutor
{
    public void run()
    {
        Messenger.printPrompt();

        BufferedReader cmdStream
            = new BufferedReader(
                new InputStreamReader( System.in ) );

        boolean stop = false;
        do
        {
        	try
        	{
	            String command = cmdStream.readLine();
	            if ( command != null )
	            {
	                StringTokenizer st = new StringTokenizer( command );
	                if ( st.hasMoreTokens() )
	                {
	                    execute( st );
	                    if ( Instance.s_halted )
	                        stop = true;
	                }
	                else
	                {
	                    Messenger.printPrompt();
	                }
	            }
	            else
	            {
	                stop = true;
	            }
        	}
        	catch ( IOException e )
        	{
        		Messenger.printWarning( "execution loop exception: " + e.getMessage() );
        		stop = true;
        	}
        }
        while ( !stop );
    }

    // ---------------------------------------------------------------------

    public void execute ( StringTokenizer t )
    {
        final String cmdName = t.nextToken().toLowerCase();

        if ( ! canIgnoreCommand( cmdName ) )
        {
            if ( cmdName.equals ( "info" ) )
                executeInfoCommand( t );
            else if ( cmdName.equals ( "maint" ) )
                executeMaintCommand( t );
            else if ( cmdName.equals ( "thread" ) )
                executeThreadCommand( t );
            else
                Instance.s_commandExecutor.internalExecute ( cmdName, t );
        }
    }

    private boolean canIgnoreCommand( String cmdName )
    {
        boolean result = false;
        if ( d_asyncCmdRunning && isConsoleMode() )
            result = ! isInterruptCommand( cmdName );
        return result;
    }

    private void executeInfoCommand ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String infoCmdName = t.nextToken();
            final String effectiveCmdName = "-info-" + infoCmdName;
            Instance.s_commandExecutor.internalExecute ( effectiveCmdName, t );
        }
        else
        {
            Messenger.printError ( "Info subcommand name required." );
        }
    }

    private void executeMaintCommand ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String maintCmdName = t.nextToken();

            if ( maintCmdName.equals ( "info" ) )
            {
                if ( t.hasMoreTokens() )
                {
                    final String maintInfoCmdName = t.nextToken();
                    final String effectiveCmdName = "-maint-info-" + maintInfoCmdName;
                    Instance.s_commandExecutor.internalExecute ( effectiveCmdName, t );
                }
                else
                {
                    Messenger.printError ( "Maint info subcommand name required." );
                }
            }
            else
            {
                final String effectiveCmdName = "-maint-" + maintCmdName;
                Instance.s_commandExecutor.internalExecute ( effectiveCmdName, t );
            }
        }
        else
        {
            Messenger.printError ( "Maint subcommand name required." );
        }
    }

    private void executeThreadCommand ( StringTokenizer t )
    {
        // it can be only "thread apply n info frame m"
        // which is translated to "-info-frame m n"

        final List< String > tokens = Utils.getTokens ( t );

        if ( tokens.size() == 5 )
        {
            final String effectiveCmdName = "-info-frame";
            final String effectiveCmdArgs = tokens.get ( 4 ) + " " + tokens.get ( 1 );
            StringTokenizer t2 = new StringTokenizer ( effectiveCmdArgs );

            Instance.s_commandExecutor.internalExecute ( effectiveCmdName, t2 );
        }
        else
        {
            Messenger.printError ( "usage: thread apply n info frame m" );
        }
    }

    private void internalExecute ( String name, StringTokenizer args )
    {
        CommandManager commandManager = Instance.s_commandManager;
        boolean printPrompt = true;
        if ( commandManager.existCommand ( name ) )
        {
            final CommandManager.CommandInfo hCommandInfo = commandManager.getCommand ( name );

            if ( hCommandInfo.hasTrait( CommandManager.CommandInfo.DontPrintPrompt )
                 || hCommandInfo.hasTrait( CommandManager.CommandInfo.Asynchronous ) )
            {
                printPrompt = false;
            }

            try
            {
                hCommandInfo.d_handler.executeCommand ( args );
                if ( hCommandInfo.hasTrait( CommandManager.CommandInfo.Asynchronous ) )
                    d_asyncCmdRunning = true;
            }
            catch ( Exception e )
            {
                // if asynchronous command fails, then d_asyncCmdRunning remains false
                Messenger.printError( e.getMessage() );
            }
        }
        else
        {
            Messenger.printError ( "Unrecognized command." );
        }
        if ( printPrompt )
            Messenger.printPrompt();
        if ( d_asyncCmdRunning && isScriptMode() )
        {
            //in script mode wait until asynchronous command completes
            waitDebuggerStopped();
        }
    }

    // ---------------------------------------------------------------------

    public void enableScriptMode( boolean enable )
    {
        if ( enable )
            d_execMode = EExecutionMode.Script;
        else
            d_execMode = EExecutionMode.Console;
    }

    public synchronized void notifyDebuggerStopped()
    {
	    if ( d_asyncCmdRunning )
	    {
	        if ( isConsoleMode() )
	        {
	            d_asyncCmdRunning = false;
            }
            else
            {
                // it waits until d_stopExecutor.await() is called
                d_stopExecutorLock.lock();
    	        try
    	        {
                    d_asyncCmdRunning = false;
	                // set free waitDebuggerStopped(), let it know that debugger stopped
	                // so asynchronous command completed
	                // and d_stopExecutor.await(); will be released and go farther
                    d_stopExecutor.signal();
                }
                catch ( Exception e )
                {
                }
    	        finally
    	        {
	                d_stopExecutorLock.unlock();
    	        }
            }
        }
    }

    private void waitDebuggerStopped()
    {
        // now locks d_stopExecutorLock, but it will be lockable again in notifyDebuggerStopped()
        // after call to d_stopExecutor.await();
        d_stopExecutorLock.lock();
	    try
	    {
            while ( d_asyncCmdRunning )
            {
                d_stopExecutor.await();
            }
	    }
        catch ( InterruptedException e )
        {
        }
	    finally
	    {
            d_stopExecutorLock.unlock();
	    }
    }

    // ---------------------------------------------------------------------

    private boolean isConsoleMode()
    {
        boolean result = ( d_execMode == EExecutionMode.Console );
        return result;
    }

    private boolean isScriptMode()
    {
        boolean result = ( d_execMode == EExecutionMode.Script );
        return result;
    }

    private boolean isInterruptCommand( String cmdName )
    {
        boolean result = cmdName.equals( Consts.InterruptCmdName );
        return result;
    }

    // ---------------------------------------------------------------------

    private enum EExecutionMode
    {
        Console,
        Script
    };

    private EExecutionMode d_execMode = EExecutionMode.Console;

    private boolean d_asyncCmdRunning = false;
    private Lock d_stopExecutorLock = new ReentrantLock();
    private Condition d_stopExecutor = d_stopExecutorLock.newCondition();

}
