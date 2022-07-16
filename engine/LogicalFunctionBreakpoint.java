// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------

class LogicalFunctionBreakpoint extends LogicalCommonBreakpoint
{
    // -----------------------------------------------------------------

    LogicalFunctionBreakpoint (
        BreakpointHandle handle,
        String rawMethodSpec,
        boolean temporary )
    {
        super ( handle, temporary );
        d_methodSpec = Utils.extractMethodNameFromSpec( rawMethodSpec );
        d_classFullName = Utils.extractClassNameFromSpec( d_methodSpec );
    }

    // ---------------------------------------------------------------------

    protected BreakpointInfo getBreakpointInfo()
    {
        /*
            samples:
            3       breakpoint     keep y   <PENDING>  f
        */
        BreakpointInfo bkInfo = new BreakpointInfo( getHandle() );
        bkInfo.d_function = d_methodSpec;

        return bkInfo;
    }

    // ---------------------------------------------------------------------

    protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints()
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = null;
        MethodManager methodManager = Instance.s_methodManager;
        final List< Integer > methodIndexes = methodManager.getMethodsByName( d_methodSpec );
        if ( ( methodIndexes != null ) && ( !methodIndexes.isEmpty() ) )
        {
            breakpoints = new ArrayList< PhysicalBreakpoint >();

            for ( Integer methodIndex : methodIndexes )
            {
                final PhysicalBreakpoint hBreakpoint = new PhysicalFunctionBreakpoint (
                    allocNextPhysicalHandle(), methodIndex, isTemporary() );
                breakpoints.add ( hBreakpoint );
            }
        }

        return breakpoints;
    }

    protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints ( ReferenceType refType )
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = null;

        if ( Utils.canMatchRefType( d_classFullName, refType ) )
            breakpoints = resolvePhysicalBreakpoints();

        return breakpoints;
    }

    // ---------------------------------------------------------------------

    void accept( LogicalBreakpointVisitor visitor )
    {
        visitor.visitFunctionBreakpoint( this );
    }

    // ---------------------------------------------------------------------

    // unmangled class/method name
    private String d_methodSpec;
    private String d_classFullName;

}
