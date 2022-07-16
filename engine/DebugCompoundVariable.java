package wingdbJavaDebugEngine;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

abstract class DebugCompoundVariable extends DebugVariable
{
    protected DebugCompoundVariable (
        String parentId,
        String childId,
        String expression,
        StackFrame frame,
        Type type,
        IDebugVariableValue value )
    {
        super( parentId, childId, expression, frame, type, value );
    }

    // -----------------------------------------------------------------

    public boolean isScalar()
    {
        return false;
    }

    // -----------------------------------------------------------------

    public int getChildrenCount()
    {
        if ( d_childrenCount == null )
            d_childrenCount = calcChildrenCount();
        return d_childrenCount;
    }

    public List< IDebugVariable > getChildren() throws Exception
    {
        ensureChildren();
        return d_children;
    }

    public IDebugVariable getChild ( String childId ) throws Exception
    {
        ensureChildren();

        IDebugVariable result = d_id2child.get ( childId );
        return result;
    }

    public IDebugVariable getChildByPath ( String path ) throws Exception
    {
        IDebugVariable result = null;

        final int firstDotPos = path.indexOf ( '.' );
        if ( firstDotPos != -1 )
        {
            final String id = path.substring ( 0, firstDotPos );
            final String rest = path.substring ( firstDotPos + 1 );
            final IDebugVariable hChild = getChild ( id );
            if ( hChild != null )
                result = hChild.getChildByPath ( rest );
        }
        else
        {
            result = getChild ( path );
        }

        return result;
    }

    // -----------------------------------------------------------------

    abstract protected Integer calcChildrenCount();

    protected void ensureChildren() throws Exception
    {
        assert d_value != null;
        if ( d_children == null )
        {
            d_children = new ArrayList< IDebugVariable >();
            int childrenCount = getChildrenCount();
            if ( 0 < childrenCount )
            {
                d_id2child = new HashMap< String, IDebugVariable >();
                generateChildren();
            }
        }
    }

    protected abstract void generateChildren() throws Exception;

    protected void addChild(
        String childId,
        String expression,
        Type type,
        Value value )
    {
        DebugVariableFactory varFactory = Instance.s_debugVariableFactory;
        String parentId = getFullId();
        IDebugVariable varChild
            = varFactory.createVariable(
                parentId,
                childId,
                expression,
                d_frame,
                type,
                value );

        storeChild( childId, varChild );
    }

    protected void addChild(
        String childId,
        String expression,
        DebugVariableTypeFactory.TypeKind typeKind,
        Object value )
    {
        DebugVariableFactory varFactory = Instance.s_debugVariableFactory;
        String parentId = getFullId();
        IDebugVariable varChild
            = varFactory.createVariable(
                parentId,
                childId,
                expression,
                d_frame,
                typeKind,
                value );

        storeChild( childId, varChild );
    }

    private void storeChild( String childId, IDebugVariable varChild )
    {
        d_children.add( varChild );
        d_id2child.put( childId, varChild );
    }

    // -----------------------------------------------------------------

    protected Integer d_childrenCount;
    protected List< IDebugVariable > d_children;
    protected Map< String, IDebugVariable > d_id2child;

}
