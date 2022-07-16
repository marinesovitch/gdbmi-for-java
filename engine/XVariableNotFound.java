// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

public class XVariableNotFound extends RuntimeException
{
    public XVariableNotFound ( String id )
    {
        super ( "Variable object not found: " + id );
    }

    private static final long serialVersionUID = 1L;
}

// ---------------------------------------------------------------------
