// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

// ---------------------------------------------------------------------

public abstract class PhysicalWatchpoint extends PhysicalBreakpoint
{

    protected PhysicalWatchpoint (
        BreakpointHandle handle,
        String className,
        String fieldName,
        ReferenceType classType,
        boolean temporary )
    {
        super ( handle, temporary );
        d_className = className;
        d_fieldName = fieldName;
        d_classType = classType;
    }

    // -----------------------------------------------------------------

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
        bkInfo.d_hitCount = getHitCount();
        return bkInfo;
    }


    protected EventRequest createConcrete()
    {
        final Field field = d_classType.fieldByName ( d_fieldName );

        EventRequestManager eventRequestManager =
            Instance.getVirtualMachine().eventRequestManager();

        return eventRequestManager.createAccessWatchpointRequest ( field );
    }

    // -----------------------------------------------------------------

    final protected String d_className;
    final protected String d_fieldName;
    final protected ReferenceType d_classType;

}
