package togos.networkrts.experimental.shape;

import junit.framework.TestCase;

public class TCircleTest extends TestCase
{
	public void testRouterTransmissionLike() {
		assertEquals( RectIntersector.INCLUDES_SOME, new TCircle( 0, 0, 1500 ).rectIntersection(-1, -1501, 2, 2) );
		assertEquals( RectIntersector.INCLUDES_SOME, new TCircle( 0, -1500, 1500 ).rectIntersection(-1, -1, 2, 2) );
	}
}
