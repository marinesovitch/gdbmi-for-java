package wingdbJavaDebugEngine;

import com.sun.jdi.Value;

interface IDebugVariableValue
{
    public boolean isNull();
    public String getRawString();
    public Value getNativeValue();
}
