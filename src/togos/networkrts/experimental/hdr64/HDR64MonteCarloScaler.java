package togos.networkrts.experimental.hdr64;

import java.util.Random;

public class HDR64MonteCarloScaler
{
	final Random rand = new Random();
	
	public HDR64Buffer scale( HDR64Buffer in, int w, int h ) {
		// TODO: Flip instead of ignoring negatives
		if( w < 0 ) w = -w;
		if( h < 0 ) h = -h;
		
		HDR64Buffer out = new HDR64Buffer(w, h);
		int spp = 10;
		for( int y=0; y<h; ++y ) {
			for( int x=0; x<w; ++x ) {
				int sampleCount = 0;
				long v = 0;
				for( int i=0; i<spp; ++i ) {
					int ix = (int)((x + rand.nextDouble()) * in.width / w);
					int iy = (int)((y + rand.nextDouble()) * in.height / h);
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
