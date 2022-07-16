// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class LogicalAccessWatchpoint extends LogicalWatchpoint
{

    LogicalAccessWatchpoint (
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
        return Breakpoint.Kind.WatchpointAccess;
    }

    // -----------------------------------------------------------------

    protected List< PhysicalBreakpoint > allocPhysicalBreakpoints( ReferenceType refType )
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = new ArrayList< PhysicalBreakpoint >();

        final PhysicalBreakpoint readWatchpoint = new PhysicalReadWatchpoint (
                allocNextPhysicalHandle(), d_className, d_fieldName, refType, isTemporary() );
        breakpoints.add ( readWatchpoint );

        final PhysicalBreakpoint writeWatchpoint = new PhysicalWriteWatchpoint (
                allocNextPhysicalHandle(), d_className, d_fieldName, refType, isTemporary() );
        breakpoints.add ( writeWatchpoint );

        return breakpoints;
    }

    // ---------------------------------------------------------------------

    void accept( LogicalBreakpointVisitor visitor )
    {
        visitor.visitAccessWatchpoint( this );
    }

}
