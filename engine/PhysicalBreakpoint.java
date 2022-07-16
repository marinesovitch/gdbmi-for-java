// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

// ---------------------------------------------------------------------

abstract class PhysicalBreakpoint extends Breakpoint
{
    protected PhysicalBreakpoint ( BreakpointHandle handle, boolean temporary )
    {
        super( handle, temporary );
    }

    // -----------------------------------------------------------------

    public void create()
    {
        d_request = createConcrete();
        //breakpoint will stop the whole VM, not the current thread only
        d_request.setSuspendPolicy( EventRequest.SUSPEND_ALL );
        d_request.putProperty ( HandlePropName, d_handle );
        d_request.putProperty ( HitCountPropName, 0 );
        d_request.putProperty ( ConditionPropName, null );
        d_request.putProperty ( PassCounterPropName, null );
    }

    public void remove()
    {
        if ( d_request != null )
        {
            EventRequestManager eventRequestManager =
                Instance.getVirtualMachine().eventRequestManager();

            eventRequestManager.deleteEventRequest ( d_request );
            d_request = null;
        }
    }

    // -----------------------------------------------------------------

    boolean isEnabled()
    {
        return d_request.isEnabled();
    }

    void setEnabled( boolean enabled )
    {
        if ( d_request != null )
        {
            if ( enabled )
                d_request.enable();
            else
                d_request.disable();
        }
    }

    // -----------------------------------------------------------------

    public void setPassCount( int passCount )
    {
        if ( d_request != null )
        {
            Integer passCounter = passCount;
            /*
                pass counter makes sense only if user wants to ignore/pass
                breakpoint at least once, i.e. pass counter has to be equal
                or greater than 2
            */
            if ( passCounter <= 1 )
                passCounter = null;
            d_request.putProperty( PassCounterPropName, passCounter );
        }
    }

    public synchronized void setCondition( String condition )
    {
        if ( d_request != null )
        {
            d_request.putProperty( ConditionPropName, condition );
        }
    }

    // -----------------------------------------------------------------

    public EventRequest getRequest()
    {
        return d_request;
    }

    // -----------------------------------------------------------------

    protected abstract EventRequest createConcrete();

    // -----------------------------------------------------------------

    protected Integer getHitCount()
    {
        Integer result = ( Integer ) d_request.getProperty ( PhysicalBreakpoint.HitCountPropName );
        return result;
    }

    // -----------------------------------------------------------------

    //handle of breakpoint (logical and physical index count from 1)
    //one logical breakpoint may have many physical breakpoints
    final public static String HandlePropName = "Handle";

    //all hits since last reset (user may reset hit counter e.g. through GUI)
    final public static String HitCountPropName = "HitCount";

    //current condition which break
    final public static String ConditionPropName = "Condition";

    /*
        how many times breakpoint hit should be ignored until it stops VM.
        if there is condition, then first check condition and if it is true
        then decrement PassCounter
    */
    final public static String PassCounterPropName = "PassCounter";

    // -----------------------------------------------------------------

    private EventRequest d_request = null;

}
