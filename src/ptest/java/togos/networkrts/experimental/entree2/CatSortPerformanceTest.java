package togos.networkrts.experimental.entree2;

import java.util.Random;

public class CatSortPerformanceTest
{
	protected static Random rand = new Random();
	protected static void randomize( int[] arr ) {
		for( int i=0; i<arr.length; ++i ) arr[i] = rand.nextInt();
	}
	protected static void randomize( boolean[] arr ) {
		for( int i=0; i<arr.length; ++i ) arr[i] = rand.nextBoolean();
	}
	
	public static void main( String[] args ) {
		Object[] items = new Object[65536];
		for( int i=0; i<items.length; ++i ) {
			items[i] = new Object();
		}
		int[] iVals = new int[items.length];
		boolean[] bVals = new boolean[items.length];
		
		for( int i=0; i<10; ++i ) {
			randomize(iVals);
			CatSort.sort(items, iVals, 0, 2048 );
			randomize(bVals);
			CatSort.sort(items, bVals, 0, 2048 );
		}
		
		long iTime = 0;
		long bTime = 0;
		long begin;
		for( int i=0; i<1000; ++i ) {
			randomize(iVals);
			begin = System.currentTimeMillis();
			CatSort.sort(items, iVals, 0, items.length );
			iTime += (System.currentTimeMillis() - begin);
			
			randomize(bVals);
			begin = System.currentTimeMillis();
			CatSort.sort(items, bVals, 0, items.length );
			bTime += (System.currentTimeMillis() - begin);
		}
		
		System.err.println(String.format("%20s: %10dms", "Int", iTime));
		System.err.println(String.format("%20s: %10dms", "Bool", bTime));
	}
}
