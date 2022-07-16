// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class LogicalWriteWatchpoint extends LogicalWatchpoint
{

    LogicalWriteWatchpoint (
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
        return Breakpoint.Kind.WatchpointWrite;
    }

    // ---------------------------------------------------------------------

    protected List< PhysicalBreakpoint > allocPhysicalBreakpoints( ReferenceType refType )
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = new ArrayList< PhysicalBreakpoint >();
        final PhysicalBreakpoint hBreakpoint = new PhysicalWriteWatchpoint (
            allocNextPhysicalHandle(), d_className, d_fieldName, refType, isTemporary() );
        breakpoints.add ( hBreakpoint );
        return breakpoints;
    }

    // ---------------------------------------------------------------------

    void accept( LogicalBreakpointVisitor visitor )
    {
        visitor.visitWriteWatchpoint( this );
    }

}
