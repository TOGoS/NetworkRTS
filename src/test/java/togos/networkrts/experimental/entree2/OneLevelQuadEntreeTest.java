package togos.networkrts.experimental.entree2;

public class OneLevelQuadEntreeTest extends QuadEntreeTest
{
	public void setUp() {
		entree = new QuadEntree( 0, 0, 1024, 1024, QuadEntreeNode.EMPTY, 0 );
	}
	
	public void testAddThingThatFitsInASubNode() {
		// It *would* fit as a sub-node if we allowed subdivision,
		// but since this tree has maxSubdivision=0,
		// it should end up in the root node anyway.
		WorldUpdateBuilder ub = new WorldUpdateBuilder();
		ub.add( new SimpleWorldObject(512, 512, 256, Long.MAX_VALUE, 0) );
		entree = (QuadEntree)ub.applyAndClear(entree);
		
		assertEquals( 1, entree.root.objects.length );
		assertEquals( 0, entree.root.n0.objectCount );
		assertEquals( 0, entree.root.n1.objectCount );
		assertEquals( 0, entree.root.n2.objectCount );
		assertEquals( 0, entree.root.n3.objectCount );
		assertEquals( 1, entree.getObjectCount() );
	}
}
