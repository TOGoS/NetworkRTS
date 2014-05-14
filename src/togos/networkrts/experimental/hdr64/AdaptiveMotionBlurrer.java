package togos.networkrts.experimental.hdr64;

import java.util.Random;

import togos.networkrts.util.RealTimeKeeper;

public class AdaptiveMotionBlurrer
{
	protected final RealTimeKeeper timeKeeper;
	protected final HDR64Buffer drawBuffer;
	protected final HDR64Buffer accumulationBuffer;
	
	public AdaptiveMotionBlurrer( RealTimeKeeper timeKeeper, HDR64Buffer drawBuffer, HDR64Buffer accumulationBuffer ) {
		assert drawBuffer.width == accumulationBuffer.width;
		assert drawBuffer.height == accumulationBuffer.height;
		this.timeKeeper = timeKeeper;
		this.drawBuffer = drawBuffer;
		this.accumulationBuffer = accumulationBuffer;
	}
	
	public interface Renderer {
		public void render( double t, HDR64Buffer into );
	}
	
	double[] ats = new double[] { 0.0, 0.6, 0.3, 0.9 };
	
	public int render( Renderer renderer, long tryToFinishBy ) {
		Random r = new Random();
		
		// Draw the first one directly onto the accumulation buffer to save some time
		renderer.render(ats[0], accumulationBuffer);
		int iterations = 1;
		
		while( timeKeeper.currentTime() < tryToFinishBy ) {
			renderer.render(iterations >= ats.length ? r.nextDouble() : ats[iterations], drawBuffer);
			HDR64Util.add(drawBuffer.data, accumulationBuffer.data);
			++iterations;
		} 
		
		return iterations;
	}
}
