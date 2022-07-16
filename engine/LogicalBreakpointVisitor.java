// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

class LogicalBreakpointVisitor
{
    protected LogicalBreakpointVisitor()
    {
    }

    // -----------------------------------------------------------------

    void visitCodeBreakpoint( LogicalCodeBreakpoint bkpt )
    {
        visitCommonBreakpoint( bkpt );
    }

    void visitFunctionBreakpoint( LogicalFunctionBreakpoint bkpt )
    {
        visitCommonBreakpoint( bkpt );
    }

    void visitSourceBreakpoint( LogicalSourceBreakpoint bkpt )
    {
        visitCommonBreakpoint( bkpt );
    }

    void visitCommonBreakpoint( LogicalCommonBreakpoint bkpt )
    {
        visitBreakpoint( bkpt );
    }

    // -----------------------------------------------------------------

    void visitReadWatchpoint( LogicalReadWatchpoint bkpt )
    {
        visitWatchpoint( bkpt );
    }

    void visitWriteWatchpoint( LogicalWriteWatchpoint bkpt )
    {
        visitWatchpoint( bkpt );
    }

    void visitAccessWatchpoint( LogicalAccessWatchpoint bkpt )
    {
        visitWatchpoint( bkpt );
    }

    void visitWatchpoint( LogicalWatchpoint bkpt )
    {
        visitBreakpoint( bkpt );
    }

    // -----------------------------------------------------------------

    void visitCatchpoint( LogicalCatchpoint bkpt )
    {
        visitBreakpoint( bkpt );
    }

    // -----------------------------------------------------------------

    void visitBreakpoint( LogicalBreakpoint bkpt )
    {
    }

}
