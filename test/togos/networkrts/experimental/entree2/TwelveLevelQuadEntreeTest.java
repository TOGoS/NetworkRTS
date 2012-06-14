package togos.networkrts.experimental.entree2;

public class TwelveLevelQuadEntreeTest extends QuadEntreeTest
{
	public void setUp() {
		entree = new QuadEntree( 0, 0, 1024, 1024, QuadEntreeNode.EMPTY, 11 );
	}
	
	public void testAddThingThatFitsInASubNode() {
		UpdateBuilder ub = new UpdateBuilder();
		ub.add( new SimpleWorldObject(512, 512, 256, Long.MAX_VALUE, 0) );
		entree = (QuadEntree)ub.applyAndClear(entree);
		
		assertEquals( 0, entree.root.objects.length );
		assertEquals( 0, entree.root.n0.objectCount );
		assertEquals( 0, entree.root.n1.objectCount );
		assertEquals( 0, entree.root.n2.objectCount );
		assertEquals( 1, entree.root.n3.objectCount );
		assertEquals( 1, entree.getObjectCount() );
	}
}
