package wingdbJavaDebugEngine;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

interface IVisualizerManager
{ 
	void addDirectory( String visualizersDirPath ) throws Exception;

	void loadModule( String moduleName, String registrator ) throws Exception;
	
	boolean isCustomType( Type type );

	IVisualizer getVisualizer( Type type );

}
