// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

class SourceTraverser
{
	SourceTraverser( Namespaces namespaces )
	{
		d_namespaces = namespaces;

		d_namespaceIndex = 0;
		d_namespaceCount = d_namespaces.size();

		initNamespace();
	}

	boolean hasNext()
	{
		boolean result = ( d_namespace != null );
		return result;
	}

	SourceInfo getNext()
	{
		SourceInfo result = d_namespace.get( d_srcIndex );
		++d_srcIndex;
		if ( d_srcIndex == d_srcCount )
		{
			++d_namespaceIndex;
			if ( !initNamespace() )
				d_namespace = null;
		}
		return result;
	}

	private boolean initNamespace()
	{
		boolean result = false;
		if ( d_namespaceIndex < d_namespaceCount )
		{
			d_namespace = d_namespaces.get( d_namespaceIndex );

			d_srcIndex = 0;
			d_srcCount = d_namespace.size();

			assert 0 < d_srcCount;
			result = true;
		}
		return result;
	}

	// ---------------------------------------------------------------------

	private final Namespaces d_namespaces;

	private int d_namespaceIndex;
	private int d_namespaceCount;

	private Namespace d_namespace;

	private int d_srcIndex;
	private int d_srcCount;

}
