package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRange;
import togos.networkrts.util.BitAddressUtil;

public class NonTile implements EntityRange
{
	public static class Icon {
		public final ImageHandle image;
		public final float imageX, imageY, imageWidth, imageHeight;
		public Icon( ImageHandle image, float x, float y, float w, float h ) {
			this.image = image;
			this.imageX = x; this.imageWidth  = w;
			this.imageY = y; this.imageHeight = h;
		}
	}
	
	public final long referenceTime;
	public final double x, y;
	public final AABB physicalAabb;
	public final long minBitAddress, maxBitAddress;
	public final long nextAutoUpdateTime;
	public final Icon icon;
	
	public NonTile( long referenceTime, double x, double y, AABB physicalAabb, long minBa, long maxBa, long nextAut, Icon icon ) {
		this.referenceTime = referenceTime;
		this.x = x; this.y = y;
		this.physicalAabb = physicalAabb;
		this.minBitAddress = minBa;
		this.maxBitAddress = maxBa;
		this.nextAutoUpdateTime = nextAut;
		this.icon = icon;
	}
	
	/** 'Centered cube bounding box' */
	static AABB ccbb( double x, double y, double diameter ) {
		return new AABB( x-diameter, y-diameter, -diameter, x+diameter, y+diameter, +diameter );
	}
	
	public static NonTile create( long referenceTime, double x, double y, ImageHandle image, float diameter ) {
		Icon icon = new Icon( image, -diameter/2, -diameter/2, diameter, diameter );
		return new NonTile( referenceTime, x, y, ccbb(x,y,diameter), BitAddressUtil.MAX_ADDRESS, BitAddressUtil.MIN_ADDRESS, Long.MAX_VALUE, icon );
	}
	
	@Override public AABB getAabb() { return physicalAabb; }
	@Override public long getMinBitAddress() { return minBitAddress; }
	@Override public long getMaxBitAddress() { return maxBitAddress; }
	@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }

	public NonTile withPosition(long referenceTime, double x, double y) {
		double dx = x-this.x, dy = y-this.y;
		return new NonTile(
			referenceTime, x, y, this.physicalAabb.shiftedBy(dx, dy, 0),
			minBitAddress, maxBitAddress, nextAutoUpdateTime, icon
		);
	}
}
