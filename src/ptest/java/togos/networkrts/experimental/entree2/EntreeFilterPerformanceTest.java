package togos.networkrts.experimental.entree2;

import java.util.Collections;
import java.util.Random;

import togos.networkrts.experimental.entree2.EntreeTest.SimpleWorldObject;
import togos.networkrts.experimental.netsim2.Sink;
import togos.networkrts.experimental.shape.TRectangle;

public class EntreeFilterPerformanceTest
{
	static Random rand = new Random(123123);
	static final int[] counter = new int[1]; // Forces Java to actually run our code
	
	protected static WorldObjectUpdate[] generateAdditions( int objectCount ) {
		WorldObjectUpdate<SimpleWorldObject>[] additions = new WorldObjectUpdate[objectCount];
		for( int i=0; i<objectCount; ++i ) {
			additions[i] = WorldObjectUpdate.addition(new SimpleWorldObject(rand.nextDouble()*1024, rand.nextDouble()*1024, rand.nextDouble()*rand.nextDouble()*rand.nextDouble()*512, rand.nextLong(), rand.nextLong()));
		}
		return additions;
	}
	
	protected static long findSomeEntities( Entree<SimpleWorldObject> entree, long requireFlags, long maxAutoUpdateTime, double x, double y, double rad )
		throws Exception
	{
		TRectangle cr = new TRectangle(x-rad, y-rad, rad*2, rad*2);
		long begin = System.currentTimeMillis();
		for( int i=0; i<100; ++i ) {
			entree.forEachObject(requireFlags, maxAutoUpdateTime, cr, new Sink<EntreeTest.SimpleWorldObject>() {
				@Override public void give(SimpleWorldObject p) throws Exception {
					++counter[0];
				}
			});
		}
		return System.currentTimeMillis() - begin;
	}
	
	public static void main( String[] args ) throws Exception {
		Entree<SimpleWorldObject> quadEntree12 = new QuadEntree<EntreeTest.SimpleWorldObject>(0, 0, 1024, 1024, QuadEntreeNode.EMPTY, 11 );
		Entree<SimpleWorldObject> setEntree = new SetEntree<EntreeTest.SimpleWorldObject>(Collections.EMPTY_SET);
		
		int objectCount = 10000;
		
		WorldObjectUpdate[] additions = generateAdditions(objectCount);
		
		quadEntree12 = quadEntree12.update(additions, 0, additions.length);
		setEntree     = setEntree.update(additions, 0, additions.length);
		
		findSomeEntities( quadEntree12, 0x7, Long.MAX_VALUE, 512, 512, 512 );
		findSomeEntities( setEntree   , 0x7, Long.MAX_VALUE, 512, 512, 512 );
		
		long quadTreeTime = 0;
		long setTime = 0;
		for( int i=0; i<100; ++i ) {
			long flags  = rand.nextInt() & (0x7 << rand.nextInt(24));
			long maxAut = rand.nextLong();
			double x = rand.nextDouble()*1024;
			double y = rand.nextDouble()*1024;
			double rad = rand.nextDouble()*128;
			quadTreeTime += findSomeEntities( quadEntree12, flags, maxAut, x, y, rad );
			setTime      += findSomeEntities( setEntree,    flags, maxAut, x, y, rad );
		}
		
		System.out.println(String.format("%20s: % 10dms", "QuadEntree", quadTreeTime));
		System.out.println(String.format("%20s: % 10dms", "SetEntree", setTime));
	}
}
