package togos.networkrts.experimental.game19.graphics;

public interface Surface
{
	public int getClipTop();
	public int getClipLeft();
	public int getClipRight();
	public int getClipBottom();
	public Surface intersectClip( int x, int y, int w, int h );
	public void fillRect( int x, int y, int w, int h, long hdr64Color );
	public void drawImage( int x, int y, int w, int h, String imageUrn );
}
