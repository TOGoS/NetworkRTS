package togos.networkrts.experimental.entree2;

import java.util.Collections;
import java.util.Random;

import togos.networkrts.experimental.entree2.EntreeTest.SimpleWorldObject;

public class EntreeModificationPerformanceTest
{
	protected static long addAndRemoveSomeStuff( Entree<SimpleWorldObject> entree, int objectCount ) {
		Random rand = new Random();
		SimpleWorldObject[] objects = new SimpleWorldObject[objectCount];
		WorldObjectUpdate<SimpleWorldObject>[] additions = new WorldObjectUpdate[objectCount];
		WorldObjectUpdate<SimpleWorldObject>[] removals = new WorldObjectUpdate[objectCount];
		for( int i=0; i<objectCount; ++i ) {
			objects[i] = new SimpleWorldObject(rand.nextDouble()*1024, rand.nextDouble()*1024, rand.nextDouble()*rand.nextDouble()*rand.nextDouble()*512, rand.nextLong(), rand.nextLong());
			additions[i] = WorldObjectUpdate.addition(objects[i]);
			removals[i] = WorldObjectUpdate.addition(objects[i]);
		}
		
		long begin = System.currentTimeMillis();
		for( int i=0; i<10; ++i ) {
			// Add them all!
			entree = entree.update(additions, 0, objectCount);
			// Remove half of them!
			entree = entree.update(removals, 0, objectCount/2);
			// Remove the other half of them!
			entree = entree.update(removals, objectCount/2, objectCount-objectCount/2);
		}
		
		return System.currentTimeMillis() - begin;
	}
	
	public static void main( String[] args ) {
		QuadEntree<SimpleWorldObject> quadEntree12 = new QuadEntree<EntreeTest.SimpleWorldObject>(0, 0, 1024, 1024, QuadEntreeNode.EMPTY, 11 );
		SetEntree<SimpleWorldObject> setEntree = new SetEntree<EntreeTest.SimpleWorldObject>(Collections.EMPTY_SET);
		
		int objectCount = 2048;
		
		addAndRemoveSomeStuff(quadEntree12, objectCount);
		addAndRemoveSomeStuff(setEntree, objectCount);
		
		long quadTreeTime = 0;
		long setTime = 0;
		for( int i=0; i<100; ++i ) {
			quadTreeTime += addAndRemoveSomeStuff(quadEntree12, objectCount);
			setTime      += addAndRemoveSomeStuff(setEntree   , objectCount);
		}
		
		System.out.println(String.format("%20s: % 10dms", "QuadEntree", quadTreeTime));
		System.out.println(String.format("%20s: % 10dms", "SetEntree", setTime));
	}
}
