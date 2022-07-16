package wingdbJavaDebugEngine;

public class BreakpointHandle extends Object
{
    BreakpointHandle( int logicalIndex )
    {
        d_logicalIndex = logicalIndex;
    }

    BreakpointHandle(
        int logicalIndex,
        int physicalIndex )
    {
        this( logicalIndex );
        d_physicalIndex = physicalIndex;
    }

    BreakpointHandle( String handleStr )
    {
        int logicalHandleEndIndex = handleStr.lastIndexOf( '.' );
        if ( logicalHandleEndIndex == -1 )
        {
            d_logicalIndex = Integer.decode( handleStr );
        }
        else
        {
            String logicalHandleStr = handleStr.substring( 0, logicalHandleEndIndex );
            d_logicalIndex = Integer.decode( logicalHandleStr );

            String physicalHandleStr = handleStr.substring( logicalHandleEndIndex + 1 );
            d_physicalIndex = Integer.decode( physicalHandleStr );
        }
    }

    // -----------------------------------------------------------------

    public String toString()
    {
        String result = null;
        if ( d_logicalIndex != InvalidLogicalIndex )
        {
        	StringBuffer sb = new StringBuffer();
        	sb.append( d_logicalIndex );
            if ( d_physicalIndex != InvalidPhysicalIndex )
            {
            	sb.append( '.' );
            	sb.append( d_physicalIndex );
            }
            result = sb.toString();
        }
        else
            result = "<invalid_handle>";
        return result;
    }

    // -----------------------------------------------------------------

    final static int InvalidLogicalIndex = 0;
    final static int InvalidPhysicalIndex = 0;

    final static int FirstLogicalIndex = 1;
    final static int FirstPhysicalIndex = 1;

    // -----------------------------------------------------------------

    int d_logicalIndex = InvalidLogicalIndex;
    int d_physicalIndex = InvalidPhysicalIndex;

}
