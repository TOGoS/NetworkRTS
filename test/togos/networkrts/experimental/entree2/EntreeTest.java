package togos.networkrts.experimental.entree2;

import junit.framework.TestCase;
import togos.networkrts.experimental.entree.ClipRectangle;
import togos.networkrts.experimental.netsim2.Sink;

public abstract class EntreeTest<EntreeClass extends Entree> extends TestCase
{
	EntreeClass entree;
	
	static class SimpleWorldObject extends WorldObject {
		final long autoUpdateTime;
		final long flags;
		final double maxRadius;
		@Override public long getAutoUpdateTime() { return autoUpdateTime; }
		@Override public long getFlags() { return flags; }
		@Override public double getMaxRadius() { return maxRadius; }
		public SimpleWorldObject( double x, double y, double rad, long aat, long flags ) {
			super( x, y );
			this.autoUpdateTime = aat;
			this.flags = flags;
			this.maxRadius = rad;
		}
	}
	
	protected static void clear( boolean[] arr ) {
		for( int i=0; i<arr.length; ++i ) arr[i]=false;
	}
	
	protected int findEntities( long flags, long autoUpdateTime, double x, double y, double w, double h ) throws Exception {
		final int[] counter = new int[1];
		entree.forEachObject(flags, autoUpdateTime, new ClipRectangle(x, y, w, h), new Sink<WorldObject>() {
			public void give(WorldObject p) throws Exception {
				++counter[0];
			};
		});
		return counter[0];
	}
	
	public void testAddMultipleThingsAndGetThemAndThenRemoveThemAndStuff() throws Exception {
		WorldUpdateBuilder ub = new WorldUpdateBuilder();
		final SimpleWorldObject o0 = new SimpleWorldObject(128,  64,  32, Long.MAX_VALUE, 1);
		final SimpleWorldObject o1 = new SimpleWorldObject(128, 128,  32, Long.MAX_VALUE, 2);
		final SimpleWorldObject o2 = new SimpleWorldObject(256, 256,  64, Long.MAX_VALUE, 4);
		final SimpleWorldObject o3 = new SimpleWorldObject( 64, 768,  64, Long.MAX_VALUE, 8);
		
		////
		
		ub.add( o0 );
		ub.add( o1 );
		entree = ub.applyAndClear(entree);
		
		assertEquals( 2, entree.getObjectCount() );
		final boolean[] found = new boolean[4];
		clear(found);
		entree.forEachObject(0, Long.MAX_VALUE, new ClipRectangle(0,0,1024,1024), new Sink<WorldObject>() {
			@Override public void give(WorldObject p) {
				if( p == o0 ) {
					found[0] = true;
				} else if( p == o1 ) {
					found[1] = true;
				}
			}
		});
		assertTrue( found[0] );
		assertTrue( found[1] );
		
		// We should find nothing in the right half, though:
		
		clear(found);
		entree.forEachObject(0, Long.MAX_VALUE, new ClipRectangle(512,0,1024,1024), new Sink<WorldObject>() {
			@Override public void give(WorldObject p) {
				if( p == o0 ) {
					found[0] = true;
				} else if( p == o1 ) {
					found[1] = true;
				}
			}
		});
		assertFalse( found[0] );
		assertFalse( found[1] );
		
		////
		
		ub.add( o2 );
		ub.add( o3 );
		entree = ub.applyAndClear(entree);

		assertEquals( 4, entree.getObjectCount() );
		clear(found);
		entree.forEachObject(0, Long.MAX_VALUE, new ClipRectangle(0,0,1024,1024), new Sink<WorldObject>() {
			@Override public void give(WorldObject p) {
				if( p == o0 ) {
					found[0] = true;
				} else if( p == o1 ) {
					found[1] = true;
				} else if( p == o2 ) {
					found[2] = true;
				} else if( p == o3 ) {
					found[3] = true;
				}
			}
		});
		assertEquals( true, found[0] );
		assertEquals( true, found[1] );
		assertEquals( true, found[2] );
		assertEquals( true, found[3] );
		
		ub.remove( o1 );
		entree = ub.applyAndClear(entree);
		
		assertEquals( 3, entree.getObjectCount() );
		assertEquals( 3, findEntities(0, Long.MAX_VALUE, 0, 0, 1024, 1024) );
		
		ub.remove( o2 );
		ub.remove( o3 );
		entree = ub.applyAndClear(entree);
		
		assertEquals( 1, entree.getObjectCount() );
		assertEquals( 1, findEntities(0, Long.MAX_VALUE, 0, 0, 1024, 1024) );
		
		entree.forEachObject(0, Long.MAX_VALUE, new ClipRectangle(0,0,1024,1024), new Sink<WorldObject>() {
			@Override public void give(WorldObject p) {
				assertSame( o0, p );
			}
		});
	}
	
	public void testFlagFilters() throws Exception {
		WorldUpdateBuilder ub = new WorldUpdateBuilder();
		ub.add( new SimpleWorldObject(64, 32, 32, Long.MAX_VALUE, 0x01) );
		ub.add( new SimpleWorldObject(64, 64, 32, Long.MAX_VALUE, 0x02) );
		ub.add( new SimpleWorldObject(32, 64, 32, Long.MAX_VALUE, 0x04) );
		ub.add( new SimpleWorldObject(32, 96, 32, Long.MAX_VALUE, 0x08) );
		ub.add( new SimpleWorldObject(64, 16, 32, Long.MAX_VALUE, 0x08) );
		entree = ub.applyAndClear(entree);
		assertEquals( 1, findEntities( 0x01, Long.MAX_VALUE, 0, 0, 128, 128 ) );
		assertEquals( 1, findEntities( 0x02, Long.MAX_VALUE, 0, 0, 128, 128 ) );
		assertEquals( 1, findEntities( 0x04, Long.MAX_VALUE, 0, 0, 128, 128 ) );
		assertEquals( 2, findEntities( 0x08, Long.MAX_VALUE, 0, 0, 128, 128 ) );
		// None have both 2 and 8
		assertEquals( 0, findEntities( 0x06, Long.MAX_VALUE, 0, 0, 128, 128 ) );
		// None have 0x10
		assertEquals( 0, findEntities( 0x11, Long.MAX_VALUE, 0, 0, 128, 128 ) );
	}
	
	public void testUpdateTimeFilters() throws Exception {
		WorldUpdateBuilder ub = new WorldUpdateBuilder();
		ub.add( new SimpleWorldObject(64, 32, 32, 100, 0x01) );
		ub.add( new SimpleWorldObject(64, 64, 32, 100, 0x02) );
		ub.add( new SimpleWorldObject(32, 64, 32, 200, 0x04) );
		ub.add( new SimpleWorldObject(32, 96, 32, 300, 0x08) );
		ub.add( new SimpleWorldObject(64, 16, 32, 400, 0x08) );
		entree = ub.applyAndClear(entree);
		assertEquals( 0, findEntities( 0x00,   0, 0, 0, 128, 128 ) );
		assertEquals( 2, findEntities( 0x00, 100, 0, 0, 128, 128 ) );
		assertEquals( 3, findEntities( 0x00, 200, 0, 0, 128, 128 ) );
		assertEquals( 4, findEntities( 0x00, 300, 0, 0, 128, 128 ) );
		assertEquals( 5, findEntities( 0x00, 400, 0, 0, 128, 128 ) );
		assertEquals( 5, findEntities( 0x00, Long.MAX_VALUE, 0, 0, 128, 128 ) );
	}
}
