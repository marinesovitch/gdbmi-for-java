// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.ClassType;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

// ---------------------------------------------------------------------

class Namespace
{
    Namespace(
        String packageName,
        String dirPath )
    {
        d_packageName = packageName;
        d_dirPath = dirPath;
    }

    // ---------------------------------------------------------------------

    String getPackageName()
    {
        return d_packageName;
    }

    String getDirPath()
    {
        return d_dirPath;
    }

    // ---------------------------------------------------------------------

    int size()
    {
        int result = d_sources.size();
        return result;
    }

    SourceInfo get( int index )
    {
        SourceInfo result = d_sources.get( index );
        return result;
    }

    // ---------------------------------------------------------------------

    void addSourceFile( String srcFileName )
    {
        final Integer srcIndex = d_sources.size();

        SourceInfo sourceInfo = new SourceInfo( this, srcFileName );
        d_sources.add ( sourceInfo );

        d_filename2source.put( srcFileName, srcIndex );
    }

    SourceInfo getSourceInfo( String srcFileName )
    {
        SourceInfo result = null;

        final Integer srcIndex = d_filename2source.get( srcFileName );
        if ( srcIndex != null )
            result = d_sources.get ( srcIndex );

        return result;
    }

    // ---------------------------------------------------------------------

    void addClass( String mainClassName, ReferenceType refType )
    {
        /*
            we are interested in main class name, so
            extractClassPackageAndName has cut internal
            class suffix, all behind first $
        */
        String sourceFileName = mainClassName + SourceInfo.JavaSrcExtension;
        if ( !associateClass( refType, sourceFileName ) )
        {
            //if ( !refType.isPublic() )
            d_unassociatedClasses.push( refType );
        }
    }

    void ensureAssociateClass( ReferenceType refType, String sourceFileName )
    {
        if ( !d_class2source.containsKey( refType ) )
        {
            d_unassociatedClasses.remove( refType );
            associateClass( refType, sourceFileName );
        }
    }

    private boolean associateClass( ReferenceType refType, String sourceFileName )
    {
        boolean result = false;

        Integer srcIndex = d_filename2source.get( sourceFileName );
        if ( srcIndex != null )
        {
            SourceInfo sourceInfo = d_sources.get( srcIndex );
            sourceInfo.addClass( refType );

            d_class2source.put( refType, srcIndex );

            result = true;
        }

        return result;
    }

    // ---------------------------------------------------------------------

    List< Location > getAllLocationsForFile( String srcFileName )
    {
        List< Location > result = null;

        final SourceInfo sourceInfo = getSourceInfo( srcFileName );
        if ( sourceInfo != null )
        {
            result = new ArrayList< Location >();

            final Iterator< SortedMap.Entry< Integer, List< Location > > > iEntry
                = sourceInfo.getAllLocations().entrySet().iterator();

            while ( iEntry.hasNext() )
            {
                final List< Location > locationsForLine = iEntry.next().getValue();

                if ( ! locationsForLine.isEmpty() )
                {
                    final Location location = locationsForLine.get ( 0 );
                    result.add ( location );
                }
            }
        }

        return result;
    }

    List< Location > getLocations( String srcFileName, SourceInfo.EFitLineMode fitLineMode, int line )
    {
        List< Location > result = null;

        final SourceInfo sourceInfo = getSourceInfo( srcFileName );
        if ( sourceInfo != null )
        {
            result = sourceInfo.getLocationsForLine( fitLineMode, line );
            if ( result == null )
                result = findLocationsInUnassociatedClasses( sourceInfo, fitLineMode, line );
        }

        return result;
    }

    List< Location > tryGetNearestLocations(
        String srcFileName, int line, ClassType refType )
    {
        List< Location > result = null;

        final SourceInfo sourceInfo = getSourceInfo( srcFileName );
        if ( sourceInfo != null )
        {
            result = sourceInfo.getLocationsForLine( refType, SourceInfo.EFitLineMode.Nearest, line );
            if ( result == null )
            {
                // if it is non-internal/non-public class then it may be still unassociated
                // to that SourceInfo - try match class with source file
                if ( tryMatchUnassociatedClass( sourceInfo, refType ) )
                {
                    result = sourceInfo.getLocationsForLine(
                        refType, SourceInfo.EFitLineMode.Nearest, line );
                }
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------

    private List< Location > findLocationsInUnassociatedClasses(
        SourceInfo sourceInfo,
        SourceInfo.EFitLineMode fitLineMode,
        int line )
    {
        List< Location > result = null;

        String sourceFileName = sourceInfo.d_sourceFileName;

        boolean stop = false;
        do
        {
            ReferenceType refType = getNextUnassociatedClass( sourceFileName );
            if ( refType != null )
            {
                result = sourceInfo.getLocationsForLine( refType, fitLineMode, line );
                if ( result != null )
                    stop = true;
            }
            else
            {
                stop = true;
            }
        }
        while ( !stop );

        return result;
    }

    private ReferenceType getNextUnassociatedClass( String expectedSourceFileName )
    {
    	ReferenceType result = null;
        boolean stop = false;
        do
        {
            if ( !d_unassociatedClasses.isEmpty() )
            {
                ReferenceType refType = d_unassociatedClasses.pop();
                try
                {
                    String sourceFileName = refType.sourceName();
                    if ( sourceFileName != null )
                    {
                        if ( associateClass( refType, sourceFileName ) )
                        {
                            if ( sourceFileName.equalsIgnoreCase( expectedSourceFileName ) )
                            {
                                result = refType;
                                stop = true;
                            }
                        }
                    }
                }
                catch( Exception e )
                {
                }
            }
            else
            {
                stop = true;
            }
        }
        while ( !stop );
        return result;
    }

    boolean tryMatchUnassociatedClass( SourceInfo sourceInfo, ReferenceType refType )
    {
        boolean result = false;

        if ( d_unassociatedClasses.remove( refType ) )
        {
            try
            {
                String srcFileName = refType.sourceName();
                if ( associateClass( refType, srcFileName ) )
                {
                    String expectedSrcFileName = sourceInfo.d_sourceFileName;
                    if ( srcFileName.equalsIgnoreCase( expectedSrcFileName ) )
                        result = true;
                }
            }
            catch( Exception e )
            {
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------

    // name of package e.g. com.sun.jdi or java.util
    final private String d_packageName;

    // absolute directory path, e.g. for package com.wingdb.winjdb it could
    // be c:/sources/foo/com/wingdb/winjdb
    final private String d_dirPath;

    // ---------------------------------------------------------------------

    private List< SourceInfo > d_sources
        = new ArrayList< SourceInfo >();

    private Map< String, Integer > d_filename2source
        = new TreeMap< String, Integer >( String.CASE_INSENSITIVE_ORDER );

    private Map< ReferenceType, Integer > d_class2source
        = new HashMap< ReferenceType, Integer >();

    /*
        all non-public classes for which source file name hasn't been
        determined yet
        all they belong to package kept by this Namespace

        ReferenceType.sourceName() is very slow so we call this
        in lazy-evaluation mode
    */
    private LinkedList< ReferenceType > d_unassociatedClasses
        = new LinkedList< ReferenceType >();

} // Namespace
