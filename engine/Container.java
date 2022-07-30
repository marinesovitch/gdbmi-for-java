package wingdbJavaDebugEngine;

import java.util.List;
import java.util.Map;
import java.util.Set;

class Container
{
	static < TContainer extends List< ? > > boolean safeIsNotEmpty( TContainer container )
	{
		boolean result = ( container != null ) && !container.isEmpty();
		return result;
	}

	static < TContainer extends Set< ? > > boolean safeIsNotEmpty( TContainer container )
	{
		boolean result = ( container != null ) && !container.isEmpty();
		return result;
	}

	static < TContainer extends Map< ?, ? > > boolean safeIsNotEmpty( TContainer container )
	{
		boolean result = ( container != null ) && !container.isEmpty();
		return result;
	}

	// ---------------------------------------------------------------------

/*
	// TODO
	static < TKey, TValue, TMap extends Map< TKey, TValue > >
		TValue ensureMapEntry( TMap map, TKey key, Class< TValue > valueCreator )
	{
		TValue result = map.get( key );
		if ( result == null )
		{
			try
			{
				result = valueCreator.newInstance();
				map.put( key, result );
			}
			catch( Exception e )
			{
			}
		}
		return result;
	}
*/

}
