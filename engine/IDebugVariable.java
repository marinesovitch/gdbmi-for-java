// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.List;

// ---------------------------------------------------------------------

interface IDebugVariable
{
    public String getFullId();
    public String getChildId();
    public String getExpression();

    public String getTypeName();
    public boolean isScalar();
    public void setFormat( Format format );
    public String getValueString();

    public int getChildrenCount();
    public List< IDebugVariable > getChildren() throws Exception;
    public IDebugVariable getChild ( String childId ) throws Exception;
    public IDebugVariable getChildByPath ( String path ) throws Exception;

    public enum Format
    {
        Natural,
        Hexadecimal,
        Terminator
    }
}

// ---------------------------------------------------------------------
