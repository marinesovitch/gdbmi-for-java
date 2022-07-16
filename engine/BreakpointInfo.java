package wingdbJavaDebugEngine;

public class BreakpointInfo
{
    BreakpointInfo( BreakpointHandle bkHandle )
    {
        d_handle = bkHandle;
    }

    BreakpointHandle d_handle;

    boolean d_pending = false;
    boolean d_multiple = false;

    Long d_address;

    String d_function;
    Long d_offset;

    String d_fileName;
    String d_fullFilePath;
    Integer d_lineNum;

    String d_what;

    String d_originalLocation;

    Integer d_hitCount;

    String getIndexString( boolean single )
    {
        String result = Integer.toString( d_handle.d_logicalIndex );
        Integer physicalIndex = d_handle.d_physicalIndex;
        if ( !single
            && ( physicalIndex != null )
            && ( physicalIndex != BreakpointHandle.InvalidPhysicalIndex ) )
        {
            result += '.' + Integer.toString( physicalIndex );
        }
        return result;
    }

    String getAddressString()
    {
        String result = null;
        if ( d_pending )
            result = "<PENDING>";
        else if ( d_multiple )
            result = "<MULTIPLE>";
        else if ( d_address != null )
            result = Utils.toAddressString( d_address );
        return result;
    }

    String getCliLocationString()
    {
        String result = getFileLocationString();
        if ( result == null )
        {
            result = getFuncOffsetLocationString();
            if ( result == null )
                result = getWhatLocationString();
        }
        return result;
    }

    private String getFileLocationString()
    {
        // 2 breakpoint     keep y   0x08048542 in _Z8funkcja3R10Struktura1i at bioFile.cpp:30
        // 3 breakpoint     keep y   <PENDING>  unloadedLibrarySrc.cpp:10
        String result = null;

        boolean isFunction = ( d_function != null );
        boolean isFilePath = ( ( d_fullFilePath != null ) || ( d_fileName != null ) );
        boolean isLineNum = ( d_lineNum != null );

        if ( isFilePath && isLineNum )
        {
            StringBuffer sb = new StringBuffer();
            if ( isFunction )
            {
                sb.append( "in " );
                sb.append( d_function );
                sb.append( " at " );
            }
            String sourcePath = ( d_fullFilePath != null ) ?  d_fullFilePath : d_fileName;
            sb.append( sourcePath );
            sb.append( ':' );
            sb.append( d_lineNum );

            result = sb.toString();
        }

        return result;
    }

    private String getFuncOffsetLocationString()
    {
        // 3 breakpoint     keep y   0x00b0fb63 <_dl_debug_state+3>
        String result = null;
        if ( d_function != null )
        {
            StringBuffer sb = new StringBuffer();
            sb.append( " <" );
            sb.append( d_function );

            if ( ( d_offset != null ) && ( d_offset != 0 ) )
            {
                sb.append( '+' );
                sb.append( d_offset );
            }

            sb.append( '>' );

            result = sb.toString();
        }

        return result;
    }

    private String getWhatLocationString()
    {
        // 2       breakpoint     keep y   0x00403847 exception catch
        // 3       breakpoint     keep y   0x0 exception uncaught
        // 3       read watchpoint   keep y   <PENDING>  Test.d_x
        String result = d_what;
        return result;
    }

}
