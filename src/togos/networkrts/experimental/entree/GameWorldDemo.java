package togos.networkrts.experimental.entree;

import java.awt.Graphics2D;
import java.util.List;

import togos.networkrts.awt.Apallit;
import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.s64.GridNode64;
import togos.networkrts.tfunc.PositionFunction;
import togos.networkrts.tfunc.ScalarFunction;

public class GameWorldDemo extends Apallit
{
	private static final long serialVersionUID = 5975810881860478072L;
	
	static class GameEntity extends AbstractPlaneEntity implements AWTDrawableEntity
	{
		public static final int LAYER_0 = 0x01000000;
		public static final int LAYER_1 = 0x02000000;
		public static final int LAYER_2 = 0x04000000;
		public static final int LAYER_3 = 0x08000000;
		public static final int LAYER_4 = 0x10000000;
		public static final int LAYER_5 = 0x20000000;
		public static final int LAYER_6 = 0x40000000;
		public static final int LAYER_7 = 0x80000000;
		
		final int outerArmor = 100;
		final int innerArmor = 100;
		final int health     = 100;
		final List<GameEntity> subEntities;
		final ScalarFunction rotation;
		final double maxRadius;
		final AWTDrawable drawable;
		
		static int aggregateFlags( List<GameEntity> entities ) {
			int flags = 0;
			for( GameEntity e : entities ) {
				flags |= e.getFlags();
			}
			return flags;
		}
		
		static double aggregateMaxRadius( double maxRad, List<GameEntity> entities ) {
			for( GameEntity e : entities ) {
				double x = e.getX(), y = e.getY();
				double rad = Math.sqrt( x*x + y*y ) + e.maxRadius;
				if( rad > maxRad ) maxRad = rad;
			}
			return maxRad;
		}
		
		public GameEntity( Object planeId, long referenceTimestamp, PositionFunction pf, ScalarFunction rotation, int flags, AWTDrawable drawable, List<GameEntity> subEntities ) {
			super( planeId, referenceTimestamp, pf, rotation, flags | aggregateFlags(subEntities) );
			this.rotation = rotation;
			this.subEntities = subEntities;
			this.drawable = drawable;
			this.maxRadius = aggregateMaxRadius( 0.1, subEntities );
		}
		
		@Override public double getMaxRadius() {  return maxRadius;  }
		
		@Override
		public void draw(Graphics2D g2d, double x, double y, double scale, double rotation, long timestamp, int renderLayer) {
			drawable.draw(g2d, x, y, scale, rotation, timestamp, renderLayer);
			double sin = Math.sin(rotation);
			double cos = Math.cos(rotation);
			double[] pbuf = new double[3];
			for( GameEntity e : subEntities ) {
				e.position.getPosition( timestamp, pbuf );
				e.draw(g2d, x + cos * pbuf[0] - sin * pbuf[1], y + sin * pbuf[0] + cos * pbuf[1], scale, rotation, timestamp, renderLayer);
			}
		}
	}
	
	static class GameState {
		final double width, height;
		/** Size of entity and terrain root nodes */
		final double rootNodeSize;
		final EntityQuadTreeNode entities;
		final GridNode64 terrain;
		
		public GameState( double width, double height, double rootNodeSize, EntityQuadTreeNode entities, GridNode64 terrain ) {
			this.width = width;
			this.height = height;
			this.rootNodeSize = rootNodeSize;
			this.entities = entities;
			this.terrain = terrain;
		}
	}
	
	class GameWorldDrawer implements TimestampedPaintable {
		double cx, cy, zoom;
		
		@Override
		public void paint(long timestamp, int width, int height, Graphics2D g2d) {
			// TODO Auto-generated method stub
			
		}
	}
	
	@Override
	public void init() {
		
	}
	
	public static void main( String[] args ) {
		GameWorldDemo ad = new GameWorldDemo();
		ad.runWindowed();
	}
}
