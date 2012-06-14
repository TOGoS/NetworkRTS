package togos.networkrts.experimental.entree2;

import junit.framework.TestCase;

public class CatSortTest extends TestCase
{
	public void testSort() {
		Integer[] values = new Integer[24];
		int[] cats = new int[] {
			0,1,2,3,4,5,5,4,3,2,1,0,
			0,1,2,3,4,5,5,4,3,2,1,0
		};
		for( int i=0; i<cats.length; ++i ) {
			values[i] = Integer.valueOf(cats[i]);
		}
		
		int[] expectedResults = new int[] {
			0,1,0,0,1,1,2,2,2,2,3,3,
			3,3,4,4,4,4,5,5,5,5,1,0
		};
		
		CatSort.sort( values, cats, 2, 22 );
		
		for( int i=0; i<24; ++i ) {
			assertEquals( expectedResults[i], values[i].intValue() );
			assertEquals( expectedResults[i], cats[i] );
		}
	}
	
	public void testBooleanSort() {
		Boolean[] values = new Boolean[24];
		boolean[] cats = new boolean[] {
			true,false,true,false,true,false,true,false,true,false,true,false,
			true,false,true,false,true,false,true,false,true,false,true,false
		};
		for( int i=0; i<cats.length; ++i ) {
			values[i] = Boolean.valueOf(cats[i]);
		}
		
		boolean[] expectedResults = new boolean[] {
			true,false,false,false,false,false,false,false,false,false,false,false,
			true,true ,true ,true ,true ,true ,true ,true ,true ,true ,true ,false
		};
		
		assertEquals( 12, CatSort.sort( values, cats, 2, 22 ) );
		
		for( int i=0; i<24; ++i ) {
			assertEquals( expectedResults[i], values[i].booleanValue() );
			assertEquals( expectedResults[i], cats[i] );
		}
	}
	
	public void testBooleanSort2() {
		Boolean[] values = new Boolean[24];
		boolean[] cats = new boolean[] {
			true,false,true,false,true,false,true,false,true, false,true,false,
			true,false,true,false,true,false,true,false,false,false,true,false
		};
		for( int i=0; i<cats.length; ++i ) {
			values[i] = Boolean.valueOf(cats[i]);
		}
		
		boolean[] expectedResults = new boolean[] {
			true ,false,false,false,false,false,false,false,false,false,false,false,
			false,true ,true ,true ,true ,true ,true ,true ,true ,true ,true ,false
		};
		
		assertEquals( 13, CatSort.sort( values, cats, 2, 22 ) );
		
		for( int i=0; i<24; ++i ) {
			assertEquals( expectedResults[i], values[i].booleanValue() );
			assertEquals( expectedResults[i], cats[i] );
		}
	}
}
