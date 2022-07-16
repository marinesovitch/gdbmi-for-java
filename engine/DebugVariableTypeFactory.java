package wingdbJavaDebugEngine;

import java.util.Map;
import java.util.TreeMap;

import com.sun.jdi.*;

// -----------------------------------------------------------------

class DebugVariableTypeFactory
{
    DebugVariableTypeFactory()
    {
    }

    // -----------------------------------------------------------------

    public enum TypeKind
    {
        Char,
        Int
    }

    synchronized Type getType( TypeKind typeKind )
    {
        Type result = d_kind2type.get( typeKind );
        if ( result == null )
            result = addType( typeKind );
        return result;
    }

    // -----------------------------------------------------------------

    private Type addType( TypeKind typeKind )
    {
        Type type = allocType( typeKind );
        d_kind2type.put( typeKind, type );
        return type;
    }

    private Type allocType( TypeKind typeKind )
    {
        Type result = null;

        VirtualMachine vm = Instance.s_connection.getVirtualMachine();
        Value tempValue = null;
        switch ( typeKind )
        {
            case Char:
                tempValue = vm.mirrorOf( '0' );
                break;

            case Int:
                tempValue = vm.mirrorOf( 0 );
                break;

            default:
                assert false : "Unknown kind of type!";
        }

        if ( tempValue != null )
            result = tempValue.type();

        return result;
    }

    // -----------------------------------------------------------------

    private Map< TypeKind, Type > d_kind2type = new TreeMap< TypeKind, Type >();

}
