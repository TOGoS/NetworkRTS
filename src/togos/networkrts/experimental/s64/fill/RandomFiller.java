package togos.networkrts.experimental.s64.fill;

import java.util.Random;

import togos.networkrts.experimental.s64.Block;
import togos.networkrts.experimental.s64.GridNode64;

public class RandomFiller implements GridNode64Filler
{
	GridNode64[] values;
	
	public RandomFiller( GridNode64[] values ) {
		this.values = values;
	}
	
	public RandomFiller( Block[] blocks ) {
		this.values = new GridNode64[blocks.length];
		for( int i=0; i<blocks.length; ++i ) values[i] = blocks[i].getHomogeneousNode();
	}
	
	int hash( int a) {
	   a = (a+0x7ed55d16) + (a<<12);
	   a = (a^0xc761c23c) ^ (a>>19);
	   a = (a+0x165667b1) + (a<<5);
	   a = (a+0xd3a2646c) ^ (a<<9);
	   a = (a+0xfd7046c5) + (a<<3);
	   a = (a^0xb55a4f09) ^ (a>>16);
	   return a;
	}
	
	Random r = new Random();
	
	public GridNode64 getNode(double x, double y, double size) {
		return values[ r.nextInt(values.length) ];
		
		/*
		long lx = Double.doubleToLongBits( x );
		long ly = Double.doubleToLongBits( y );
		long ls = Double.doubleToLongBits( size );
		int hash = hash(
			hash( (int)lx+0 ^ (int)(lx>>32) ) ^
			hash( (int)ly+1 ^ (int)(ly>>32) ) ^
			hash( (int)ls+2 ^ (int)(ls>>32) )							
		);
		
		hash = hash * hash;
		
		//hash *= hash * lx * ly * ls;
		//hash = hash & 0x7FFFFFFFFFFFFFFFl;
		hash = hash & 0x7FFFFFFF;
		return values[(int)(hash % values.length)];
		*/
	}
}
