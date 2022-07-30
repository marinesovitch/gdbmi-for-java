// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.ClassType;
import com.sun.jdi.Location;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ReferenceType;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.io.*;

// ---------------------------------------------------------------------

class SourceInfo
{
	SourceInfo(
		Namespace namespace,
		String sourceFileName )
	{
		d_namespace = namespace;
		d_sourceFileName = sourceFileName;
	}

	final Namespace d_namespace;
	final String d_sourceFileName;

	String getAbsolutePath()
	{
		String dirPath = d_namespace.getDirPath();
		String result = dirPath + '/' + d_sourceFileName;
		return result;
	}

	// ---------------------------------------------------------------------

	void addClass( ReferenceType hClass )
	{
		d_classes.put( hClass, null );
		d_allLocationsCache.add( hClass );
	}

	// ---------------------------------------------------------------------

	/*
		find
			- exactly specified line or
			- greater but only if the specified line is within range of class lines

		samples:
		specified line = 10
		range = 8, 9, 11, 12
		result = 11

		specified line = 10
		range = 8, 10, 11, 12
		result = 10

		specified line = 10
		range = 11, 12, 13
		result = null

		specified line = 10
		range = 4, 7, 9
		result = null

		use that mode for searching the set of lines of single class
	*/
	public enum EFitLineMode
	{
		// fit exactly specified line
		Exactly,

		// fit nearest line to that specified
		Nearest
	}

	public final synchronized List< Location > getLocationsForLine(
		EFitLineMode fitLineMode,
		Integer line )
	{
		List< Location > result = findLocationsInClasses(
			ESearchMode.LoadedClassesOnly, fitLineMode, line );
		if ( result == null )
		{
			result = findLocationsInClasses(
				ESearchMode.UnloadedClassesOnly, fitLineMode, line );
		}
		return result;
	}

	public final synchronized List< Location > getLocationsForLine(
		ReferenceType refType,
		EFitLineMode fitLineMode,
		Integer line )
	{
		List< Location > result = null;
		SortedMap< Integer, List< Location > > classLocations = getClassLocations( refType );
		if ( classLocations != null )
			result = findLocationsInRange( fitLineMode, line, classLocations );
		return result;
	}

	// ---------------------------------------------------------------------

	private enum ESearchMode
	{
		// try to find nearest line among loaded classes only, ignore unloaded
		LoadedClassesOnly,

		// try to find nearest line among unloaded classes only (load their locations),
		// ignore already loaded classes
		UnloadedClassesOnly
	}

	// ---------------------------------------------------------------------

	private final List< Location > findLocationsInClasses(
		ESearchMode searchMode,
		EFitLineMode fitLineMode,
		Integer line )
	{
		List< Location > result = null;

		/*
			we can't rely on location ranges to determine proper class which may contain
			specified 'line' and have to iterate linearly even through loaded classes, because
			ranges may overlap
		*/
		for ( Map.Entry< ReferenceType, SortedMap< Integer, List< Location > > > entry : d_classes.entrySet() )
		{
			boolean performSearch = false;
			SortedMap< Integer, List< Location > > classLocations = entry.getValue();
			if ( ( classLocations != null ) && ( searchMode == ESearchMode.LoadedClassesOnly ) )
				performSearch = true;
			else if ( ( classLocations == null ) && ( searchMode == ESearchMode.UnloadedClassesOnly ) )
			{
				performSearch = true;
				ReferenceType classRefType = entry.getKey();
				classLocations = getClassLocations( classRefType );
			}

			if ( performSearch )
			{
				assert classLocations != null;
				result = findLocationsInRange( fitLineMode, line, classLocations );
				if ( result != null )
					break;
			}
		}

		return result;
	}

	// ---------------------------------------------------------------------

	private final List< Location > findLocationsInRange(
		EFitLineMode fitLineMode,
		Integer line,
		SortedMap< Integer, List< Location > > line2locations )
	{
		List< Location > result = null;
		if ( fitLineMode == EFitLineMode.Nearest )
			result = findNearestLocationsInRange( line, line2locations );
		else
		{
			assert fitLineMode == EFitLineMode.Exactly;
			result = findExactLocationsInRange( line, line2locations );
		}
		return result;
	}

	private final List< Location > findExactLocationsInRange(
		Integer line,
		SortedMap< Integer, List< Location > > line2locations )
	{
		List< Location > result = line2locations.get( line );
		if ( result == null )
		{
			if ( isInRange( line, line2locations ) )
			{
				List< Location > locations = getNearestLocations( line, line2locations );
				if ( isLineWithinAcceptableMargin( line, locations ) )
				{
					/*
						line is in the specified range, but it doesn't contain any code Location,
						so return empty list to avoid searching in other ranges (classes)
					*/
					result = new ArrayList< Location >();
				}
			}
		}
		return result;
	}

	private final List< Location > findNearestLocationsInRange(
		Integer line,
		SortedMap< Integer, List< Location > > line2locations )
	{
		List< Location > result = null;
		// we try to search for location only if it is in the range of class (between
		// the first and the last line of the class source-code)
		if ( isInRange( line, line2locations ) )
		{
			List< Location > locations = getNearestLocations( line, line2locations );
			if ( isLineWithinAcceptableMargin( line, locations ) )
				result = locations;
		}

		return result;
	}

	private final boolean isInRange(
		Integer line,
		SortedMap< Integer, List< Location > > line2locations )
	{
		boolean result = false;
		if ( !line2locations.isEmpty() )
		{
			final Integer firstLine = line2locations.firstKey();
			final Integer lastLine = line2locations.lastKey();
			if ( ( firstLine <= line ) && ( line <= lastLine ) )
				result = true;
		}
		return result;
	}

	private final List< Location > getNearestLocations(
		Integer line,
		SortedMap< Integer, List< Location > > line2locations )
	{
		List< Location > result = null;
		final SortedMap< Integer, List< Location > > lowerBound
			= line2locations.tailMap ( line );
		if ( !lowerBound.isEmpty() )
		{
			final Integer nearestLine = lowerBound.firstKey();
			result = lowerBound.get ( nearestLine );
		}
		return result;
	}

	private final boolean isLineWithinAcceptableMargin(
		Integer lineNum, List< Location > locations )
	{
		/*
			we can't rely only on isInRange routine, because anonymous so called lambda
			classes may have really strange ranges, e.g. locations may have lines:
			[ 1, 124, 125, 126, 127 ]
			despite in the line 1 there is no code at all, and ahead to line 124 there
			is code of other classes
		*/
		boolean result = false;

		if ( Container.safeIsNotEmpty( locations ) )
		{
			final int AcceptableMargin = 2;
			for ( Location location : locations )
			{
				Integer locationLineNum = location.lineNumber();
				if ( Math.abs( locationLineNum - lineNum ) <= AcceptableMargin )
					result = true;
			}
		}
		return result;
	}

	// ---------------------------------------------------------------------

	public final synchronized SortedMap< Integer, List< Location > > getAllLocations()
	{
		if ( !d_allLocationsCache.isEmpty() )
		{
			for ( ReferenceType refType : d_allLocationsCache )
			{
				SortedMap< Integer, List< Location > > classLocations = getClassLocations( refType );
				if ( classLocations != null )
					updateAllLocations( classLocations );
			}
			d_allLocationsCache.clear();
		}
		return d_allLocations;
	}

	private final void updateAllLocations( SortedMap< Integer, List< Location > > classLocations )
	{
		for ( Map.Entry< Integer, List< Location > > entry : classLocations.entrySet() )
		{
			Integer lineNum = entry.getKey();
			List< Location > newLocations = entry.getValue();

			List< Location > lineLocations = d_allLocations.get( lineNum );
			if ( lineLocations == null )
				d_allLocations.put( lineNum, newLocations );
			else
				lineLocations.addAll( newLocations );
		}
	}

	// ---------------------------------------------------------------------

	public final synchronized SortedMap< Integer, List< Location > > getClassLocations(
		ReferenceType hClass )
	{
		SortedMap< Integer, List< Location > > locations = null;
		// if d_classes doesn't contain hClass it means that class is not included in that file
		// so first check...
		if ( d_classes.containsKey( hClass ) )
		{
			//...then try get
			locations = d_classes.get( hClass );
			if ( locations == null )
			{
				// class may belong to that file and is in d_classes, but we load its locations
				// only if they are really needed (lazy evaluation of expensive operation)
				locations = prepareClassLocations( hClass );
				d_classes.put( hClass, locations );
			}
		}
		return locations;
	}

	private final SortedMap< Integer, List< Location > > prepareClassLocations( ReferenceType hClass )
	{
		SortedMap< Integer, List< Location > > result
			= new TreeMap< Integer, List< Location > >(); ;
		try
		{
			final List< Location > classLocations = hClass.allLineLocations();
			for ( Location hLocation : classLocations )
			{
				final Integer hLineNumber = hLocation.lineNumber();

				if ( ! result.containsKey ( hLineNumber ) )
					result.put ( hLineNumber, new ArrayList< Location >() );

				List< Location > locationsForLine = result.get ( hLineNumber );
				locationsForLine.add ( hLocation );
			}
		}
		catch ( AbsentInformationException e )
		{
		}
		return result;
	}

	// ---------------------------------------------------------------------

	// contains all classes which belongs to this (SourceInfo) source file
	private Map< ReferenceType, SortedMap< Integer, List< Location > > > d_classes
		= new TreeMap< ReferenceType, SortedMap< Integer, List< Location > > >();

	private SortedMap< Integer, List< Location > > d_allLocations
		= new TreeMap< Integer, List< Location > >();

	// all classes which lines should be inserted to d_allLocations
	private List< ReferenceType > d_allLocationsCache
		= new ArrayList< ReferenceType >();

	// ---------------------------------------------------------------------

	static final String JavaSrcExtension = ".java";

} // SourceInfo
