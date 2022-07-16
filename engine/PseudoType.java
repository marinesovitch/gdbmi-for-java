// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

// ---------------------------------------------------------------------

public class PseudoType implements Type
{
    public PseudoType ( VirtualMachine virtualMachine, String typeName )
    {
        d_virtualMachine = virtualMachine;
        d_typeName = typeName;
    }

    // Accessible

    public boolean isPackagePrivate()
    {
        return false;
    }

    public boolean isPrivate()
    {
        return false;
    }

    public boolean isProtected()
    {
        return false;
    }

    public boolean isPublic()
    {
        return true;
    }

    public int modifiers()
    {
        return 0;
    }

    // Comparable< ReferenceType >

    public int compareTo ( Type t )
    {
        if ( t instanceof PseudoType )
            return t.name().compareTo ( d_typeName );
        else
            return -1;
    }

    // Mirror

    public String toString()
    {
        return d_typeName;
    }

    public VirtualMachine virtualMachine()
    {
        return d_virtualMachine;
    }

    // Type

    public String name()
    {
        return d_typeName;
    }

    public String signature()
    {
        return d_typeName;
    }

    private VirtualMachine d_virtualMachine;
    private String d_typeName;
}

// ---------------------------------------------------------------------
