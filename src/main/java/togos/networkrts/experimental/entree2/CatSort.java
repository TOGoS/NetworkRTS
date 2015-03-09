package togos.networkrts.experimental.entree2;

public class CatSort
{
	static final void swap( Object[] arr, int[] cats, int i1, int i2 ) {
		Object o1 = arr[i1]; int c1 = cats[i1];
		arr[i1] = arr[i2];   cats[i1] = cats[i2];
		arr[i2] = o1;        cats[i2] = c1;
	}

	static final void swap( Object[] arr, boolean[] cats, int i1, int i2 ) {
		Object o1 = arr[i1]; boolean c1 = cats[i1];
		arr[i1] = arr[i2];   cats[i1] = cats[i2];
		arr[i2] = o1;        cats[i2] = c1;
	}
	
	/**
	 * Sorts items by category, where category is an integer between 0 and 5 (inclusive).
	 * 
	 * e.g. if input categories are 0,1,2,3,4,5,0,1,2,3,4,5
	 * after this function returns, they will be 0,0,1,1,2,2,3,3,4,4,5,5
	 * 
	 * Only items at indexes between begin (inclusive) and end (exclusive)
	 * will be sorted.
	 * 
	 * @param items 
	 * @param cats
	 * @param off
	 * @param len
	 */
	
	public static void debug( int j, int[] cats, int begin, int end ) {
		for( int i=0; i<cats.length; ++i ) {
			if( i == begin ) System.err.print("[");
			else if( i == end ) System.err.print("]");
			else if( i == j ) System.err.print("|");
			else System.err.print(" ");
			System.err.print(cats[i]);
		}
		System.err.println();
	}
	
	public static void debug( int j, boolean[] cats, int begin, int end ) {
		for( int i=0; i<cats.length; ++i ) {
			if( i == begin ) System.err.print("[");
			else if( i == end ) System.err.print("]");
			else if( i == j ) System.err.print("|");
			else System.err.print(" ");
			System.err.print(cats[i] ? "X" : "O");
		}
		System.err.println();
	}
	
	public static void sort( Object[] items, int[] cats, int begin, int end ) {
		// Put all 0s at the beginning, 5s at the end
		//System.err.println("Sort 0/5 ");
		for( int i=begin; i<end; ) {
			//debug( i, cats, begin, end );
			switch( cats[i] ) {
			case( 0 ): swap( items, cats, begin++, i++ ); break;
			case( 5 ): swap( items, cats, --end, i ); break;
			default: ++i;
			}
		}
		// Put all 1s at the beginning, 4s at the end
		//System.err.println("Sort 1/4 ");
		for( int i=begin; i<end; ) {
			//debug( i, cats, begin, end );
			switch( cats[i] ) {
			case( 1 ): swap( items, cats, begin++, i++ ); break;
			case( 4 ): swap( items, cats, --end, i ); break;
			default: ++i;
			}
		}
		// Put all 2s at the beginning, 3s at the end
		//System.err.println("Sort 2/3 ");
		for( int i=begin; i<end; ) {
			//debug( i, cats, begin, end );
			switch( cats[i] ) {
			case( 2 ): swap( items, cats, begin++, i++ ); break;
			case( 3 ): swap( items, cats, --end, i ); break;
			default: ++i;
			}
		}
	}

	/**
	 * Sorts flases to the beginning, trues to the end.
	 * Returns the index after the last false in the results.
	 */
	public static int sort( Object[] items, boolean[] cats, int begin, int end ) {
		for( int i=begin; i<end; ) {
			if( cats[i] ) {
				swap( items, cats, --end, i );
			} else {
				++i;
			}
		}
		return end;
	}
}
