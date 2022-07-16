package wingdbJavaDebugEngine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

import com.sun.jdi.*;

import org.python.util.PythonInterpreter;
import org.python.core.*;

// ---------------------------------------------------------------------

public class VisualizerManager implements IVisualizerManager, IVisualizerManagerCallback
{
    VisualizerManager()
    {
        d_interp.exec( "import sys" );
    }

    // ---------------------------------------------------------------------

    public void addDirectory( String visualizersDirPath ) throws Exception
    {
        String cmdAddDir = "sys.path.insert ( 0, '" + visualizersDirPath + "' )";
        d_interp.exec( cmdAddDir );
    }

    public void loadModule( String moduleName, String registrator ) throws Exception
    {
        Message msg = new Message();
        try
        {
            IVisualizerFactory visualizerFactory = loadVisualizerFactory( moduleName, registrator );
            List< String > supportedTypes = Arrays.asList( visualizerFactory.getSupportedTypes() );

            for ( String supportedType : supportedTypes )
                d_type2factory.put( supportedType, visualizerFactory );

            msg.printCliMessage( "Loaded visualizer " + moduleName );
            msg.printCliMessage( "Supported types " + supportedTypes.toString() );
        }
        catch ( Exception e )
        {
            msg.printCliMessage( "Error while loading visualizer " + moduleName + '/' + registrator );
            msg.printCliMessage( e.getMessage() );
            msg.printCliMessage( e.toString() );
        }
        Messenger.print( msg );
    }

    private IVisualizerFactory loadVisualizerFactory( String moduleName, String registrator )
    {
        String cmdImport = "from " + moduleName + " import " + registrator;
        d_interp.exec( cmdImport );
        PyFunction registratorFunc = d_interp.get( registrator, PyFunction.class );
        IVisualizerManagerCallback visualizerManagerCallback = this;
        PyObject pyVisualizerManagerCallback = Py.java2py( visualizerManagerCallback );
        PyObject visualizerObject = registratorFunc.__call__( pyVisualizerManagerCallback );
        IVisualizerFactory result
            = ( IVisualizerFactory )visualizerObject.__tojava__( IVisualizerFactory.class );
        return result;
    }

    // ---------------------------------------------------------------------

    public boolean isCustomType( Type type )
    {
        String typeName = type.name();
        boolean result = d_type2factory.containsKey( typeName );
        return result;
    }

    public IVisualizer getVisualizer( Type type )
    {
        assert isCustomType( type );
        String typeName = type.name();
        IVisualizer result = d_type2visualizer.get( typeName );
        if ( result == null )
            result = allocVisualizer( typeName );
        return result;
    }

    private IVisualizer allocVisualizer( String typeName )
    {
        IVisualizer visualizer = null;
        try
        {
            IVisualizerFactory visualizerFactory = d_type2factory.get( typeName );
            VirtualMachine vm = Instance.s_connection.getVirtualMachine();
            visualizer = visualizerFactory.allocVisualizer( vm, typeName );
        }
        catch( Exception e )
        {
            Messenger.printException( e );
            visualizer = new VisualizerStub();
        }
        d_type2visualizer.put( typeName, visualizer );
        return visualizer;
    }

    // ---------------------------------------------------------------------
    // IVisualizerManagerCallback

    public void addSupportedEntryType( IVisualizerFactory visualizerFactory, String entryType )
    {
        assert !d_type2factory.containsKey( entryType );
        d_type2factory.put( entryType, visualizerFactory );
    }

    // ---------------------------------------------------------------------

    private PythonInterpreter d_interp = new PythonInterpreter();
    private Map< String, IVisualizerFactory > d_type2factory
        = new HashMap< String, IVisualizerFactory >();
    private Map< String, IVisualizer > d_type2visualizer
        = new HashMap< String, IVisualizer >();

}
