package togos.networkrts.experimental.qt2drender;

public interface Display
{
	public void resetClip();
	public void draw( ImageHandle ih, float x, float y, float w, float h );
	public void clip( float x, float y, float w, float h );
	public void saveClip();
	public void restoreClip();
	public boolean hitClip(float screenX, float screenY, float screenNodeSize, float screenNodeSize2);
}
