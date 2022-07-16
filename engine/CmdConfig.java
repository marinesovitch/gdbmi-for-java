// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

// ---------------------------------------------------------------------

public class CmdConfig
{
    // -----------------------------------------------------------------

    public static void commandListFeatures ( StringTokenizer t )
    {
        List< String > features
            = new ArrayList< String >( Arrays.asList( new String[] {
                "frozen-varobjs",
                "pending-breakpoints",
                "shlib-observer",
                "python"
                } ) );

        StringBuffer sb = new StringBuffer();
        sb.append( "^done,features=[" );
        boolean first = true;
        for ( String feature : features )
        {
            if ( first )
                first = false;
            else
                sb.append( ',' );

            sb.append( '\"' );
            sb.append( feature );
            sb.append( '\"' );
        }
        sb.append( "]" );

        final String commandOutput = sb.toString();
        Messenger.println( commandOutput );
    }

    // -----------------------------------------------------------------

    public static void commandGdbSet ( StringTokenizer t )
    {
        boolean success = false;

        if ( t.hasMoreTokens() )
        {
            final String variableName = t.nextToken();

            String variableValue = Utils.getRemainingText( t );
            if ( Utils.isNotEmpty( variableValue ) )
            {
                Instance.s_settingsManager.setValue ( variableName, variableValue );
                Messenger.println( "^done" );

                success = true;
            }
        }

        if ( !success )
            Messenger.printError ( "Argument required." );
    }

    // -----------------------------------------------------------------

    public static void commandGdbShow ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String variableName = t.nextToken();
            final String variableValue = Instance.s_settingsManager.getValue ( variableName  );

            Messenger.println ( "^done,value=\"" + variableValue.trim() + "\"" );
        }
        else
        {
            Messenger.printError ( "Argument required." );
        }
    }

    // -----------------------------------------------------------------

    public static void commandEnvironmentDirectory ( StringTokenizer t )
    {
        while ( t.hasMoreTokens() )
        {
            final String token = Utils.unquote ( Utils.getRemainingText( t ) );

            if ( token == "-r" )
                Instance.s_sourceManager.clearSourceDirectories();
            else
                Instance.s_sourceManager.addSourceDirectories ( token );
        }

        final String currentSourcesPathString = Instance.s_sourceManager.getSourcePath();

        Message msg = new Message();
        msg.format ( "^done,source-path=\"%s\"\n", currentSourcesPathString );
        Messenger.print( msg );
    }

    // -----------------------------------------------------------------

    public static void commandInternalCommand ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String internalOrder = t.nextToken();
            if ( internalOrder.equals( "enable-script-mode" ) )
                Instance.s_commandExecutor.enableScriptMode( true );
            else if ( internalOrder.equals( "disable-script-mode" ) )
            	Instance.s_commandExecutor.enableScriptMode( false );
            else
                Messenger.printError( "unknown internal order" );
        }
        else
            Messenger.printError( "empty internal order" );
    }

    // -----------------------------------------------------------------

    public static void commandEnablePrettyPrinting( StringTokenizer t )
    {
        try
        {
            IVisualizerManager visualizerManager = new VisualizerManager();
            Instance.s_visualizerManager = visualizerManager;
        }
        catch ( Exception e )
        {
            Messenger.printWarning( e.toString() );
        }
        Messenger.println ( "^done" );
    }

    // -----------------------------------------------------------------

    public static void commandAddVisualizersDirectory( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            try
            {
                final String directory = Utils.unquote ( Utils.getRemainingText( t ) );
                Instance.s_visualizerManager.addDirectory( directory );
            }
            catch ( Exception e )
            {
                Messenger.printWarning( e.toString() );
            }
            Messenger.println ( "^done" );
        }
        else
        {
            Messenger.printError ( "Argument required." );
        }
    }

    // -----------------------------------------------------------------

    public static void commandLoadVisualizer( StringTokenizer t )
    {
        boolean notEnoughArguments = true;
        try
        {
            if ( t.hasMoreTokens() )
            {
                final String moduleName = t.nextToken();
                if ( t.hasMoreTokens() )
                {
                    final String registrator = t.nextToken();
                    notEnoughArguments = false;
                    Instance.s_visualizerManager.loadModule( moduleName, registrator );
                }
            }
        }
        catch ( Exception e )
        {
            Messenger.printWarning( e.toString() );
        }

        if ( notEnoughArguments )
        {
            Messenger.printError ( "Two arguments required." );
        }
        else
        {
            Messenger.println ( "^done" );
        }
    }

}

// ---------------------------------------------------------------------
