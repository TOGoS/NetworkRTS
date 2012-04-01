package togos.networkrts.gclient;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

public class TestAWTPainter implements AWTPainter
{
	static int m2u( int m ) {
		return m * 1024;
	}
	
	Polygon p = new Polygon( new int[] { 0, m2u(10), m2u(10) }, new int[] { 0, 0, -m2u(10) }, 3 );

	public void paint( Graphics2D g2d, long timestamp ) {
		g2d.setColor(Color.WHITE);
		//AffineTransform t = g2d.getTransform();
		g2d.rotate( 2 * Math.PI * (timestamp & 1023) / 1000f );
		g2d.fillPolygon( p );
		//g2d.setTransform(t);
	}
}
