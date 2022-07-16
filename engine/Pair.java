// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

class Pair< TFirst, TSecond >
{
    public Pair()
    {
    }

    public Pair( TFirst i_first, TSecond i_second )
    {
        first = i_first;
        second = i_second;
    }

    public String toString()
    {
        return "(" + first.toString() + ", " + second.toString() + ")";
    }

    public TFirst first;
    public TSecond second;
}
