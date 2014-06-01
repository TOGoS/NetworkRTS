package togos.networkrts.experimental.hdr64;

import java.util.Random;

public class HDR64MonteCarloScaler
{
	protected final int samplePointCount = 32; 
	
	float[] samplePoints = new float[samplePointCount*2];
	
	public HDR64MonteCarloScaler() {
		final Random rand = new Random();
		for( int i=0; i<samplePoints.length; ++i ) {
			samplePoints[i] = rand.nextFloat();
		}
	}
	
	public HDR64Buffer scale( HDR64Buffer in, int w, int h ) {
		// TODO: Flip instead of ignoring negatives
		int sx0, sx1, sy0, sy1;
		
		if( w < 0 ) {
			sx0 = in.width;
			sx1 = -1;
			w = -w;
		} else {
			sx0 = 0;
			sx1 = in.width;
		}
		if( h < 0 ) {
			sy0 = in.height;
			sy1 = -1;
			h = -h;
		} else {
			sy0 = 0;
			sy1 = in.height;
		}
		
		if( h < 0 ) h = -h;
		
		HDR64Buffer out = new HDR64Buffer(w, h);
		int spp = samplePointCount;
		for( int y=0; y<h; ++y ) {
			for( int x=0; x<w; ++x ) {
				int sampleCount = 0;
				long v = 0;
				for( int i=spp*2-1; i>=0; ) {
					int ix = (int)(sx0 + (x + samplePoints[i--]) * (sx1-sx0) / w);
					int iy = (int)(sy0 + (y + samplePoints[i--]) * (sy1-sy0) / h);
					long sample = in.data[ix + iy*in.width];
					if( (sample & HDR64Util.HDR_ALPHA_MASK) != 0 ) {
						++sampleCount;
						v += sample;
					}
				}
				if( sampleCount >= spp/2 ) {
					v = HDR64Util.divide(v, sampleCount) | HDR64Util.HDR_ALPHA_MASK;
				} else {
					v = 0;
				}
				out.data[x + y*w] = v;
			}
		}
		return out;
	}
}
