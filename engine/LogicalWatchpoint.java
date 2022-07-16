// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import java.util.List;

// ---------------------------------------------------------------------

/*
    notation notice regarding terminology mismatch (GDB vs JDI)
    GDB
    access  == read or write    / "-break-watch -a"
    read    == read             / "-break-watch -r"
    write   == write            / "-break-watch"

    JDI
    access == read          / AccessWatchpointRequest
    modification == write   / ModificationWatchpointRequest

    in names of classes we use GDB terminology, i.e.
    LogicalReadWatchpoint utilizes AccessWatchpointRequest
    LogicalWriteWatchpoint utilizes ModificationWatchpointRequest
*/
abstract class LogicalWatchpoint extends LogicalBreakpoint
{

    protected LogicalWatchpoint (
        BreakpointHandle handle,
        String className,
        String fieldName,
        boolean temporary )
    {
        super ( handle, temporary );
        d_className = className;
        d_fieldName = fieldName;
    }

    // ---------------------------------------------------------------------

    protected BreakpointInfo getBreakpointInfo()
    {
        /*
            samples:
            2       read watchpoint keep y                     x2
            3       acc watchpoint keep y                     y
            4       watchpoint  keep y                    z

            2       acc watchpoint     keep y   Test.d_x
            3       read watchpoint   keep y   <PENDING>  Test.d_x

            bkpt={number="2",type="read watchpoint",disp="keep",enabled="y",
                addr="",what="y",times="0",original-location="y"},
            bkpt={number="3",type="watchpoint",disp="keep",enabled="y",
                addr="",what="x2",times="0",original-location="x2"},
            bkpt={number="4",type="acc watchpoint",disp="keep",enabled="y",
                addr="",what="x",times="0",original-location="x"}]}
        */
        BreakpointInfo bkInfo = new BreakpointInfo( getHandle() );
        String classField = d_className + '.' + d_fieldName;
        bkInfo.d_what = classField;
        bkInfo.d_originalLocation = classField;
        return bkInfo;
    }

    // -----------------------------------------------------------------

    protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints()
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = null;

        SourceManager sourceManager = Instance.s_sourceManager;
	    List< ReferenceType > refTypes = sourceManager.getClassByName( d_className );
	    for ( ReferenceType refType : refTypes )
	    {
	        if ( matchRefType( refType ) )
	        {
                breakpoints = allocPhysicalBreakpoints( refType );
	            break;
	        }
        }

        return breakpoints;
    }

    protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints ( ReferenceType refType )
        throws Exception
    {
        List< PhysicalBreakpoint > breakpoints = null;
        if ( Utils.equalTypes( d_className, refType ) && matchRefType( refType ) )
            breakpoints = allocPhysicalBreakpoints( refType );
        return breakpoints;
    }

    private boolean matchRefType( ReferenceType refType )
    {
        Field field = refType.fieldByName( d_fieldName );
        boolean result = ( field != null );
        return result;
    }

    abstract protected List< PhysicalBreakpoint > allocPhysicalBreakpoints( ReferenceType refType )
        throws Exception;

    // ---------------------------------------------------------------------

    void accept( LogicalBreakpointVisitor visitor )
    {
        visitor.visitWatchpoint( this );
    }

    // ---------------------------------------------------------------------

    protected String d_className;
    protected String d_fieldName;

}
