// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

/*
    CAUTION!!!
    All messages dumped to output have to go through Messenger to
    ensure proper synchronization, else messages may get mixed and
    WinGDB will not able to parse them
*/
public class Messenger
{

    static synchronized void print ( String message )
    {
        /*
            WARNING!!!
            This is the ONLY place where System.out is used
            DON'T send any message directly to default output, but only with use
            of Messenger routines
        */
        System.out.print ( message );
    }

    static synchronized void println ( String rawMessage )
    {
        String message = String.format( "%s\n", rawMessage );
        print( message );
    }

    static synchronized void print ( Message message )
    {
        String text = message.getText();
        print ( text );
    }

    static synchronized void println ( Message message )
    {
        message.println();
        print ( message );
    }

    static synchronized void printWarning ( String warningMessage )
    {
        String message = String.format( "&\"warning: \"%s\"\n", warningMessage );
        print( message );
    }

    static synchronized void printError ( String errorMessage )
    {
        final String escaped = Utils.escapeValue ( errorMessage, false );
        final String message = String.format( "^error,msg=\"%s\"\n", escaped );
        print( message );
    }

    static synchronized void printPrompt()
    {
        println( "(gdb) " );
        flush();
    }

    static synchronized void printException( Exception e )
    {
        Message msg = new Message();
        msg.printCliMessage( e.getMessage() );
        msg.printCliMessage( e.toString() );
        print( msg );
    }

    static synchronized void flush()
    {
        System.out.flush();
    }

}
