// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Hashtable;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

public class CmdVariables
{
    // -----------------------------------------------------------------

    public static void commandVarCreate ( StringTokenizer t )
    {
        boolean success = false;
        if ( t.hasMoreTokens() )
        {
            // omit -
            t.nextToken();

            if ( t.hasMoreTokens() )
            {
                final String strAddress = t.nextToken();

                if ( t.hasMoreTokens() )
                {
                    success = true;
                    final String strExpression = Utils.unquote ( Utils.getRemainingText ( t, "\n" ) );
                    final long address = Utils.parseAddress ( strAddress );

                    final StackFrame hFrame =
                        Instance.s_threadManager.getFrameByAddress ( address );

                    try
                    {
                        final IDebugVariable hVariable =
                            Instance.s_debugVariableManager.createVariable (
                                strExpression, hFrame, Instance.getVirtualMachine() );

                        final String typeName = hVariable.getTypeName();

                        Messenger.println (
                            "^done,name=\"" + hVariable.getFullId() + "\""
                            + ",type=\"" + typeName + "\""
                            + ",numchild=\"" + hVariable.getChildrenCount() + "\"" );
                    }
                    catch ( Exception e )
                    {
                        Messenger.printError ( e.getMessage() );
                    }
                }
            }
        }

        if ( !success )
            Messenger.printError ( "\"-var-create\" takes 3 arguments." );
    }

    // -----------------------------------------------------------------

    public static void commandVarDelete ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String id = Utils.unquote ( t.nextToken() );

            try
            {
                Instance.s_debugVariableManager.deleteVariable ( id );
                Messenger.println ( "^done" );
            }
            catch ( XVariableNotFound e )
            {
                Messenger.printError ( e.getMessage() );
            }
        }
        else
            Messenger.printError ( "\"-var-delete\" takes an argument." );
    }

    // -----------------------------------------------------------------

    public static void commandVarSetFormat ( StringTokenizer t )
    {
        boolean correctInput = false;
        try
        {
            if ( t.hasMoreTokens() )
            {
                final String id = Utils.unquote ( t.nextToken() );
                if ( t.hasMoreTokens() )
                {
                    String formatSpec = t.nextToken();
                    if ( d_str2format.containsKey( formatSpec ) )
                    {
                        correctInput = true;
                        DebugVariableManager varManager = Instance.s_debugVariableManager;
                        IDebugVariable variable = varManager.findVariable( id );
                        IDebugVariable.Format format = d_str2format.get( formatSpec );
                        variable.setFormat( format );
                        Messenger.println ( "^done" );
                    }
                }
            }
        }
        catch ( XVariableNotFound e )
        {
            Messenger.printError ( "\"-var-set-format\" " + e.getMessage() );
        }
        catch ( ClassNotLoadedException e )
        {
            Messenger.printError ( "\"-var-set-format\" could not load class." );
        }
        catch ( Exception e )
        {
            Messenger.printError ( "\"-var-set-format\" " + e.getMessage() );
        }

        if ( !correctInput )
            Messenger.printError ( "\"-var-set-format\" takes two arguments name format-spec." );
    }

    // -----------------------------------------------------------------

    public static void commandVarInfoExpression ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            try
            {
                String id = Utils.unquote( t.nextToken() );
                IDebugVariable hVariable =
                    Instance.s_debugVariableManager.findVariable( id );
                String expression = hVariable.getExpression();
                Messenger.println( "^done,lang=\"Java\",exp=\"" + expression + "\"" );
            }
            catch ( XVariableNotFound e )
            {
                Messenger.printError ( "\"-var-info-expression\" " + e.getMessage() );
            }
            catch ( ClassNotLoadedException e )
            {
                Messenger.printError ( "\"-var-info-expression\" could not load class." );
            }
            catch ( Exception e )
            {
                Messenger.printError ( "\"-var-info-expression\" " + e.getMessage() );
            }
        }
        else
            Messenger.printError ( "\"-var-info-expression\" takes an argument." );

    }

    // -----------------------------------------------------------------

    public static void commandVarInfoNumChildren ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String id = Utils.unquote ( t.nextToken() );

            try
            {
                final IDebugVariable hVariable =
                    Instance.s_debugVariableManager.findVariable ( id );
                final Integer nChildren = hVariable.getChildrenCount();
                Messenger.println ( "^done,numchild=\"" + nChildren.toString() + "\"" );
            }
            catch ( XVariableNotFound e )
            {
                Messenger.printError ( "\"-var-info-num-children\" " + e.getMessage() );
            }
            catch ( ClassNotLoadedException e )
            {
                Messenger.printError ( "\"-var-info-num-children\" could not load class." );
            }
            catch ( Exception e )
            {
                Messenger.printError ( "\"-var-info-num-children\" " + e.getMessage() );
            }
        }
        else
            Messenger.printError ( "\"-var-info-num-children\" takes an argument." );
    }

    // -----------------------------------------------------------------

    public static void commandVarListChildren ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            String token = t.nextToken();
            if ( Utils.isIntNumber( token ) )
                token = t.nextToken();

            final String id = Utils.unquote ( token );

            try
            {
                final IDebugVariable hVariable =
                    Instance.s_debugVariableManager.findVariable ( id );

                final List< IDebugVariable > children = hVariable.getChildren();
                final Integer nChildren = children != null ? children.size() : 0;

                Message msg = new Message();
                msg.print ( "^done,numchild=\"" + nChildren.toString() + "\"" );
                msg.print ( ",children=[" );

                if ( children != null )
                {
                    Iterator< IDebugVariable > iChild = children.iterator();

                    boolean first = true;

                    while ( iChild.hasNext() )
                    {
                        IDebugVariable varChild = iChild.next();
                        final String varTypeName = varChild.getTypeName();
                        final String valueString = varChild.getValueString();

                        if ( first )
                            first = false;
                        else
                            msg.print ( "," );

                        msg.print ( "child={" );

                        msg.print ( "name=\"" + varChild.getFullId() + "\"," );
                        msg.print ( "type=\"" + varTypeName + "\"," );
                        msg.print ( "exp=\"" + varChild.getExpression() + "\"," );
                        msg.print ( "numchild=\"" + varChild.getChildrenCount() + "\"," );
                        msg.print ( "value=\"" + valueString + "\""  );

                        msg.print ( "}" );
                    }
                }

                msg.print ( "]" );
                msg.println();
                Messenger.print( msg );
            }
            catch ( XVariableNotFound e )
            {
                Messenger.printError ( e.getMessage() );
            }
            catch ( Exception e )
            {
                // probably it is impossible to list children, so dump error message
                Messenger.printError ( e.getMessage() );
            }
        }
        else
            Messenger.printError ( "\"-var-list-children\" takes an argument." );
    }

    // -----------------------------------------------------------------

    public static void commandVarInfoType ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            final String id = Utils.unquote ( t.nextToken() );

            try
            {
                final IDebugVariable hVariable =
                    Instance.s_debugVariableManager.findVariable ( id );
                final String typeName = hVariable.getTypeName();
                Messenger.println ( "^done,type=\"" + typeName + "\"" );
            }
            catch ( XVariableNotFound e )
            {
                Messenger.printError ( "\"-var-info-type\" " + e.getMessage() );
            }
            catch ( ClassNotLoadedException e )
            {
                Messenger.printError ( "\"-var-info-type\" could not load class." );
            }
            catch ( Exception e )
            {
                Messenger.printError ( "\"-var-info-type\" " + e.getMessage() );
            }
        }
        else
            Messenger.printError ( "\"-var-info-type\" takes an argument." );
    }

    // -----------------------------------------------------------------

    public static void commandVarEvaluateExpression ( StringTokenizer t )
    {
        if ( t.hasMoreTokens() )
        {
            // ignore format options if available -f <format_kind>
            String rawId = t.nextToken();
            if ( rawId.equals( "-f" ) )
            {
                t.nextToken();
                rawId = t.nextToken();
            }
            final String id = Utils.unquote ( rawId );

            try
            {
                final IDebugVariable hVariable =
                    Instance.s_debugVariableManager.findVariable ( id );
                final String valueString = hVariable.getValueString();
                Messenger.println ( "^done,value=\"" + valueString + "\"" );
            }
            catch ( XVariableNotFound e )
            {
                Messenger.printError ( "\"-var-evaluate-expression\" " + e.getMessage() );
            }
            catch ( ClassNotLoadedException e )
            {
                Messenger.printError ( "\"-var-evaluate-expression\" could not load class." );
            }
            catch ( Exception e )
            {
                Messenger.printError ( "\"-var-evaluate-expression\" " + e.getMessage() );
            }
        }
        else
            Messenger.printError ( "\"-var-evaluate-expression\" takes an argument." );
    }

    // -----------------------------------------------------------------

    public static void commandVarAssign ( StringTokenizer t )
    {
        Messenger.printError ( "\"-var-assign\" is not supported." );
    }

    // -----------------------------------------------------------------

    private static Hashtable< String, IDebugVariable.Format > initFormatMappings()
    {
        Hashtable< String, IDebugVariable.Format > mappings
            = new Hashtable< String, IDebugVariable.Format >();
        mappings.put( "natural", IDebugVariable.Format.Natural );
        mappings.put( "hexadecimal", IDebugVariable.Format.Hexadecimal );
        return mappings;
    }

    // -----------------------------------------------------------------

    private static Hashtable< String, IDebugVariable.Format > d_str2format = initFormatMappings();

}
