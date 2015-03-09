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
	
	double[] ats = new double[] { 0.5, 0.0, 1.0, 0.3, 0.7, 0.15, 0.85, 0.4, 0.6 };
	
	public int render( Renderer renderer, long tryToFinishBy ) {
		Random r = new Random();
		
		// Draw the first one directly onto the accumulation buffer to save some time
		long preRender = timeKeeper.currentTime();
		renderer.render(ats[0], accumulationBuffer);
		long postRender = timeKeeper.currentTime();
		int iterations = 1;
		
		long renderTime = postRender - preRender;
		
		while( timeKeeper.currentTime() < tryToFinishBy-renderTime ) {
			preRender = timeKeeper.currentTime();
			renderer.render(iterations >= ats.length ? r.nextDouble() : ats[iterations], drawBuffer);
			HDR64Util.add(drawBuffer.data, accumulationBuffer.data);
			postRender = timeKeeper.currentTime();
			renderTime = ( renderTime + (postRender - preRender) ) / 2;
			++iterations;
		} 
		
		return iterations;
	}
}
