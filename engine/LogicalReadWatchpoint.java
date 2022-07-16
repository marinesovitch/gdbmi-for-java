// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class LogicalReadWatchpoint extends LogicalWatchpoint
{
    // ---------------------------------------------------------------------

    LogicalReadWatchpoint (
        BreakpointHandle handle,
        String className,
        String fieldName,
        boolean temporary )
    {
        super ( handle, className, fieldName, temporary );
    }

    // -----------------------------------------------------------------

    Kind getKind()
    {
        return Breakpoint.Kind.WatchpointRead;
    }

    // ---------------------------------------------------------------------

    protected List< PhysicalBreakpoint > allocPhysicalBreakpoints( ReferenceType refType )
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = new ArrayList< PhysicalBreakpoint >();
        final PhysicalBreakpoint hBreakpoint = new PhysicalReadWatchpoint (
            allocNextPhysicalHandle(), d_className, d_fieldName, refType, isTemporary() );
        breakpoints.add ( hBreakpoint );
        return breakpoints;
    }

    // ---------------------------------------------------------------------

    void accept( LogicalBreakpointVisitor visitor )
    {
        visitor.visitReadWatchpoint( this );
    }

}
