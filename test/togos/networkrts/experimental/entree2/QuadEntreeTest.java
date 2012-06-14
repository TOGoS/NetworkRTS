package togos.networkrts.experimental.entree2;


public abstract class QuadEntreeTest extends EntreeTest<QuadEntree>
{
	static class SimpleWorldObject extends WorldObject {
		final long autoUpdateTime;
		final long flags;
		@Override public long getAutoUpdateTime() { return autoUpdateTime; }
		@Override public long getFlags() { return flags; }
		public SimpleWorldObject( double x, double y, double rad, long aat, long flags ) {
			super( x, y, rad );
			this.autoUpdateTime = aat;
			this.flags = flags;
		}
	}
	
	public void testAddThingThatFitsOnlyDirectly() {
		UpdateBuilder ub = new UpdateBuilder();
		ub.add( new SimpleWorldObject(512, 512, 512, Long.MAX_VALUE, 0) );
		entree = (QuadEntree)ub.applyAndClear(entree);
		
		assertEquals( 1, entree.root.objects.length );
		assertEquals( 0, entree.root.n0.objectCount );
		assertEquals( 0, entree.root.n1.objectCount );
		assertEquals( 0, entree.root.n2.objectCount );
		assertEquals( 0, entree.root.n3.objectCount );
		assertEquals( 1, entree.getObjectCount() );
	}
		
	public void testAddThingThatDoesntFit() {
		UpdateBuilder ub = new UpdateBuilder();
		ub.add( new SimpleWorldObject(512, 512, 2048, Long.MAX_VALUE, 0) );
		entree = (QuadEntree)ub.applyAndClear(entree);
		
		assertEquals( 0, entree.root.objectCount );
		assertEquals( 0, entree.getObjectCount() );
	}
}
