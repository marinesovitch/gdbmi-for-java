package wingdbJavaDebugEngine;

import java.util.List;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class DebugScalarVariable extends DebugVariable
{
    DebugScalarVariable(
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
        return true;
    }

    // -----------------------------------------------------------------

    public int getChildrenCount()
    {
        return 0;
    }

    public List< IDebugVariable > getChildren() throws Exception
    {
        return null;
    }

    public IDebugVariable getChild ( String childId ) throws Exception
    {
        return null;
    }

    public IDebugVariable getChildByPath ( String path ) throws Exception
    {
        return null;
    }

}
