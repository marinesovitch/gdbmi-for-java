package wingdbJavaDebugEngine;

class BreakpointHandleFactory
{
    //use only for allocation logical handles
    BreakpointHandleFactory()
    {
        d_logicalIndex = BreakpointHandle.FirstLogicalIndex;
    }

    //use only for allocation physical handles
    BreakpointHandleFactory( BreakpointHandle logicalHandle )
    {
        d_logicalIndex = logicalHandle.d_logicalIndex;
        d_physicalIndex = BreakpointHandle.FirstPhysicalIndex;
    }

    // -----------------------------------------------------------------

    BreakpointHandle allocNextLogicalHandle()
    {
        BreakpointHandle result = new BreakpointHandle( d_logicalIndex );
        ++d_logicalIndex;
        return result;
    }

    // -----------------------------------------------------------------

    BreakpointHandle allocNextPhysicalHandle()
    {
        assert d_logicalIndex != BreakpointHandle.InvalidLogicalIndex;
        BreakpointHandle result = new BreakpointHandle( d_logicalIndex, d_physicalIndex );
        ++d_physicalIndex;
        return result;
    }

    // -----------------------------------------------------------------

    private int d_logicalIndex = BreakpointHandle.InvalidLogicalIndex;
    private int d_physicalIndex = BreakpointHandle.InvalidPhysicalIndex;

}
