package wingdbJavaDebugEngine;

import com.sun.jdi.*;

class DebugNullVariable extends DebugScalarVariable
{
    DebugNullVariable(
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

    public String getValueString()
    {
        String result = Consts.NullValue;
        return result;
    }

}
