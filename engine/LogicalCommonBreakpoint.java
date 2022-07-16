package wingdbJavaDebugEngine;

// ---------------------------------------------------------------------

abstract class LogicalCommonBreakpoint extends LogicalBreakpoint
{

    protected LogicalCommonBreakpoint (
        BreakpointHandle handle,
        boolean temporary )
    {
        super( handle, temporary );
    }

    // -----------------------------------------------------------------

    Kind getKind()
    {
        return Breakpoint.Kind.Breakpoint;
    }

    // ---------------------------------------------------------------------

    void accept( LogicalBreakpointVisitor visitor )
    {
        visitor.visitCommonBreakpoint( this );
    }

}
