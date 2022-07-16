package wingdbJavaDebugEngine;

import com.sun.jdi.Type;

// ---------------------------------------------------------------------

class VisualizerManagerStub implements IVisualizerManager
{

    public void addDirectory( String visualizersDirPath )
    {
    }

    public void loadModule( String moduleName, String registrator )
    {
    }

    public boolean isCustomType( Type type )
    {
        return false;
    }

    public IVisualizer getVisualizer( Type type )
    {
        return d_visualizerStub;
    }

    private VisualizerStub d_visualizerStub = new VisualizerStub();

}
