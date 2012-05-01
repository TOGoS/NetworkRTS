package togos.networkrts.experimental.entree;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

public class EntityPlaneCanvas extends Canvas
{
	private static final long serialVersionUID = 1;

	static class SimpleEntity implements PlaneEntity {
		public final String id;
		public final String planeId;
		public final double x, y, radius;
		public final int flags;
		public final Color color;
		
		public SimpleEntity( String id, String planeId, double x, double y, double radius, int flags, Color color ) {
			this.id = id;
			this.planeId = planeId;
			this.x = x; this.y = y;
			this.radius = radius;
			this.flags = flags | PlaneEntity.FLAG_EXISTS;
			this.color = color;
		}
		
		public int getFlags() {  return flags;  }
		public Object getId() {  return id;  }
		public double getMaxRadius() {  return radius;  }
		public Object getPlaneId() {  return planeId;  }
		public double getX() {  return x;  }
		public double getY() {  return y;  }
		public Color getColor() {  return color;  }
	}
	
	public EntityPlane plane = new QuadTreeEntityPlane(65536, 65536, EntityQuadTreeNode.EMPTY, 65536);
	
	public void paintLayer( int layer, final Graphics g ) {
		Rectangle gClip = g.getClipBounds();
		ClipRectangle wClip = new ClipRectangle( gClip.getMinX(), gClip.getMinY(), gClip.getWidth(), gClip.getHeight() );
		plane.eachEntity( wClip, layer << 1, (~layer << 1) & (0x7 << 1), new Iterated<SimpleEntity>() {
			public void item( SimpleEntity e ) {
				g.setColor( e.getColor() );
				
				g.fillOval(
					(int)(e.getX() - e.getMaxRadius()),
					(int)(e.getY() - e.getMaxRadius()),
					(int)(e.getMaxRadius() * 2),
					(int)(e.getMaxRadius() * 2)
				);
			}
		} );		
	}
	
	@Override
	public void paint( final Graphics g ) {
		paintLayer( 0, g );
		paintLayer( 1, g );
		paintLayer( 2, g );
		paintLayer( 3, g );
		paintLayer( 4, g );
		paintLayer( 5, g );
		paintLayer( 6, g );
		paintLayer( 7, g );
	}
	
	public static void main( String[] args ) {
		EntityPlaneCanvas c = new EntityPlaneCanvas();
		
		Color[] colors = new Color[] {
			Color.RED,
			Color.ORANGE,
			Color.YELLOW,
			Color.GREEN,
			Color.CYAN,
			Color.BLUE,
			Color.PINK,
			Color.WHITE
		};
		
		for( int i=0; i<65536*8; ++i ) {
			Random r = new Random();
			int layer = r.nextInt(8);
			
			c.plane = c.plane.update( new EntityPlaneUpdate(
				EntityPlaneUpdate.EMPTY_ENTITY_LIST,
				new SimpleEntity[] {
					new SimpleEntity(
						String.valueOf(i), "x",
						Math.random() * 32768, Math.random() * 32768,
						Math.random() * Math.random() * 128,
						layer << 1,
						colors[layer]
					),
				}
			) );
		}
		c.setBackground(Color.BLACK);
		c.setSize(32768, 32768);
		
		ScrollPane sp = new ScrollPane();
		sp.add(c);
		sp.setPreferredSize( new Dimension(512,512) );
		
		final Frame f = new Frame("EntityPlaneCanvas");
		f.add( sp );
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				f.dispose();
			}
		});
		f.setVisible(true);
	}
}
