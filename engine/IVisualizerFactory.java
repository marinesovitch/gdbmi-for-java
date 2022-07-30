package wingdbJavaDebugEngine;

import org.python.util.PythonInterpreter; 
import org.python.core.*; 
import com.sun.jdi.*;

// ---------------------------------------------------------------------

public interface IVisualizerFactory
{
	String[] getSupportedTypes();
	IVisualizer allocVisualizer( VirtualMachine vm, String typeName );
}
