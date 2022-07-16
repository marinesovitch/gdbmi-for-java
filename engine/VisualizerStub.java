package wingdbJavaDebugEngine;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class VisualizerStub implements IVisualizer
{
    public boolean isScalar()
    {
        return true;
    }

    public String getValueString(
        Type type,
        Value value,
        IVariableCallback varCallback )
    {
        return "";
    }

    public int getChildrenCount(
        Type type,
        Value value,
        IVariableCallback varCallback )
    {
        return 0;
    }

    public void generateChildren(
        Type type,
        Value value,
        IVariableCallback varCallback,
        int childrenCount )
    {
        // do nothing
    }

}
