package wingdbJavaDebugEngine;

import org.python.util.PythonInterpreter; 
import org.python.core.*; 
import com.sun.jdi.*;

// ---------------------------------------------------------------------

public interface IVisualizer
{
    boolean isScalar();

    String getValueString( 
        Type type, 
        Value value,
        IVariableCallback varCallback ) 
            throws Exception;
    
    int getChildrenCount( 
        Type type, 
        Value value, 
        IVariableCallback varCallback ) 
            throws Exception;

    void generateChildren( 
        Type type, 
        Value value, 
        IVariableCallback varCallback, 
        int childrenCount ) 
            throws Exception;

}
