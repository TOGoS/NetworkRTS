package togos.networkrts.experimental.game19.graphics;

public abstract class AbstractSurface implements Surface
{
	final int clipLeft, clipTop, clipRight, clipBottom;
	
	protected AbstractSurface( int clipLeft, int clipTop, int clipRight, int clipBottom ) {
		this.clipLeft = clipLeft;
		this.clipTop = clipTop;
		this.clipRight = clipRight;
		this.clipBottom = clipBottom;
	}
	
	protected abstract AbstractSurface withClip( int left, int top, int right, int bottom );
	
	@Override public int getClipTop() { return clipTop; }
	@Override public int getClipLeft() { return clipLeft; }
	@Override public int getClipRight() { return clipRight; }
	@Override public int getClipBottom() { return clipBottom; }
	
	@Override public Surface intersectClip(int x, int y, int w, int h) {
		if( x <= clipLeft && y <= clipTop && x+w >= clipRight && y+h >= clipBottom ) return this;
		return withClip(
			Math.min(x, clipLeft),
			Math.min(y, clipTop),
			Math.max(x+w, clipRight),
			Math.max(y+h, clipBottom)
		);
	}
}
