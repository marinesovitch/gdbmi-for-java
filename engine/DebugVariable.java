// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;

// ---------------------------------------------------------------------

abstract class DebugVariable implements IDebugVariable
{
    protected DebugVariable (
        String parentId,
        String childId,
        String expression,
        StackFrame frame,
        Type type,
        IDebugVariableValue value )
    {
        d_parentId = parentId;
        d_childId = childId;
        d_expression = Utils.escapeValue( expression, true );
        d_frame = frame;
        d_type = type;
        d_value = value;
    }

    // -----------------------------------------------------------------

    public String getFullId()
    {
        String result = "";
        if ( d_parentId != null )
            result = d_parentId + '.';
        result += d_childId;
        return result;
    }

    public String getChildId()
    {
        return d_childId;
    }

    public String getExpression()
    {
        return d_expression;
    }

    // -----------------------------------------------------------------

    public String getTypeName()
    {
        String result = d_type.name();
        return result;
    }

    public void setFormat( Format format )
    {
        d_format = format;
    }

    public String getValueString()
    {
        String result = d_value.getRawString();
        return result;
    }

    // -----------------------------------------------------------------

    protected final String d_parentId;
    protected final String d_childId;
    protected final String d_expression;

    protected final StackFrame d_frame;
    protected final Type d_type;
    protected final IDebugVariableValue d_value;

    protected Format d_format = Format.Natural;

}
