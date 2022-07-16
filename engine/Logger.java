// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

// ---------------------------------------------------------------------

public class Logger
{
    public Logger()
    {
        try
        {
            // WARNING! don't use drive letter, else it will
        	// throw exception if WinJDB starts on other drive
            d_logFile = new FileWriter ( "jgdb.log" );
            d_logWriter = new PrintWriter ( d_logFile );
            d_logWriter.println ( "WinJDB log start" );
            d_logWriter.flush();
        }
        catch ( IOException e )
        {
            Messenger.println ( e.getMessage() );
        }
    }

    private static Logger s_logger;

    static
    {
        try
        {
            s_logger = new Logger();
        }
        catch ( Exception err )
        {
        }
    }

    public static Logger begin()
    {
        try
        {
            final Long currentTime = System.currentTimeMillis();

            s_logger.d_logWriter.print ( "(" );
            s_logger.d_logWriter.print ( currentTime.toString() );
            s_logger.d_logWriter.print ( ") " );
        }
        catch ( Exception err )
        {
        }

        return s_logger;
    }

    public void end()
    {
        try
        {
            d_logWriter.println();
            d_logWriter.flush();
        }
        catch ( Exception err )
        {
        }
    }

    public Logger log ( String text )
    {
        try
        {
            d_logWriter.print ( text );
        }
        catch ( Exception err )
        {
        }

        return this;
    }

    public Logger log ( int v )
    {
        try
        {
            d_logWriter.print ( v );
        }
        catch ( Exception err )
        {
        }

        return this;
    }

    public Logger log ( long v )
    {
        try
        {
            d_logWriter.print ( v );
        }
        catch ( Exception err )
        {
        }

        return this;
    }

    public Logger log ( boolean v )
    {
        try
        {
            d_logWriter.print ( v ? "true" : "false" );
        }
        catch ( Exception err )
        {
        }

        return this;
    }

    public Logger log ( double v )
    {
        try
        {
            d_logWriter.print ( v );
        }
        catch ( Exception err )
        {
        }

        return this;
    }

    public Logger log ( Throwable v )
    {
        try
        {
            v.printStackTrace ( d_logWriter );
        }
        catch ( Exception err )
        {
        }

        return this;
    }

    private FileWriter d_logFile = null;
    private PrintWriter d_logWriter = null;
}

// ---------------------------------------------------------------------
