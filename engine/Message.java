// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.*;
import java.io.PrintWriter;
import java.io.StringWriter;

// ---------------------------------------------------------------------

/*
    class needed to atomize messages and dump they in synchronized
    way through Messenger

    it can be used in comfortable way just like System.out - all
    routines like format, print, println and so on are supported
    due to 'extending' PrintWriter
*/
public class Message extends PrintWriter
{

    Message()
    {
    	this( new StringWriter() );
    }

    Message( StringWriter outputBuffer )
    {
    	super( outputBuffer );
    	d_outputBuffer = outputBuffer;
    }

    // ---------------------------------------------------------------------

    String getText()
    {
        flush();

        String result = d_outputBuffer.toString();

        try
        {
        	d_outputBuffer.close();
        }
        catch ( Exception e )
        {
        }
        close();

        return result;
    }

    void log()
    {
        Logger.begin().log ( d_outputBuffer.toString() ).end();
    }

    // ---------------------------------------------------------------------

    void printCliMessage ( String cliMessage )
    {
        if ( cliMessage != null )
        {
            StringTokenizer st = new StringTokenizer ( cliMessage, "\n" );

            while ( st.hasMoreTokens() )
            {
                final String token = st.nextToken();
                format ( "~\"%s\"\n", token );
            }
        }
    }

    void printProperty ( String name, String value )
    {
        format ( "%s=\"%s\"", name, value );
    }

    void printProperty ( String name, int value )
    {
        format ( "%s=\"%d\"", name, value );
    }

    void printAddressProperty ( String name, long value )
    {
        format ( "%s=\"%s\"", name, Utils.toAddressString( value ) );
    }

    void printBanner ( String text )
    {
        format ( "~\"%s\\n\"\n", text );
    }

    // ---------------------------------------------------------------------

    private StringWriter d_outputBuffer = new StringWriter();
}
