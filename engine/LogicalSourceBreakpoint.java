// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------

class LogicalSourceBreakpoint extends LogicalCommonBreakpoint
{
    // ---------------------------------------------------------------------

    LogicalSourceBreakpoint (
        BreakpointHandle handle,
        String sourceName,
        int lineNumber,
        boolean temporary )
    {
        super ( handle, temporary );
        d_sourceName = sourceName;
        d_lineNumber = lineNumber;
    }

    // -----------------------------------------------------------------

    protected BreakpointInfo getBreakpointInfo()
    {
        /*
            samples:
            3       breakpoint     keep y   <PENDING>  unloadedLibrarySrc.cpp:10
        */
        BreakpointInfo bkInfo = new BreakpointInfo( getHandle() );

        bkInfo.d_fileName = d_sourceName;
        bkInfo.d_fullFilePath = d_sourceName;
        bkInfo.d_lineNum = d_lineNumber;

        return bkInfo;
    }

    // -----------------------------------------------------------------

    protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints()
        throws Exception
    {
        List< Location > locations
            = Instance.s_sourceManager.getNearestLocations (
                d_sourceName, d_lineNumber );

        List< PhysicalBreakpoint > breakpoints = internalResolvePhysicalBreakpoints( locations );
        return breakpoints;
    }

    // ---------------------------------------------------------------------

    protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints ( ReferenceType hType )
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = null;

        if ( hType instanceof ClassType )
        {
            ClassType hClass = ( ClassType ) hType;

            List< Location > locations
                = Instance.s_sourceManager.tryGetNearestLocations (
                    d_sourceName, d_lineNumber, hClass );
            breakpoints = internalResolvePhysicalBreakpoints( locations );
        }

        return breakpoints;
    }

    // ---------------------------------------------------------------------

    private List< PhysicalBreakpoint > internalResolvePhysicalBreakpoints ( List< Location > locations )
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = null;

        if ( locations != null )
        {
            for ( Location location : locations )
            {
                Method method = location.method();
                if ( method != null )
                {
                    if ( breakpoints == null )
                        breakpoints = new ArrayList< PhysicalBreakpoint >();
                    final PhysicalBreakpoint hBreakpoint
                        = new PhysicalSourceBreakpoint( allocNextPhysicalHandle(), location, isTemporary() );
                    breakpoints.add ( hBreakpoint );
                }
            }
        }

        return breakpoints;
    }

    // ---------------------------------------------------------------------

    void accept( LogicalBreakpointVisitor visitor )
    {
        visitor.visitSourceBreakpoint( this );
    }

    // ---------------------------------------------------------------------

    private String d_sourceName;
    private int d_lineNumber;

}
