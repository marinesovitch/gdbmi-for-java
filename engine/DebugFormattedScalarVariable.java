package wingdbJavaDebugEngine;

import java.util.Vector;

import com.sun.jdi.*;

// -----------------------------------------------------------------

abstract class DebugFormattedScalarVariable extends DebugScalarVariable
{
    protected DebugFormattedScalarVariable(
        String parentId,
        String childId,
        String expression,
        StackFrame frame,
        Type type,
        IDebugVariableValue value )
    {
        super( parentId, childId, expression, frame, type, value );

        d_values = new Vector< String >();
        int formatsCount = IDebugVariable.Format.Terminator.ordinal();
        d_values.setSize( formatsCount );
    }

    // -----------------------------------------------------------------

    public String getValueString()
    {
        int valueIndex = d_format.ordinal();
        String result = d_values.get( valueIndex );
        if ( result == null )
        {
            result = prepareValueString();
            d_values.set( valueIndex, result );
        }
        return result;
    }

    protected abstract String prepareValueString();

    // -----------------------------------------------------------------

    private Vector< String > d_values;

}
