package togos.networkrts.experimental.fastshader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;

import togos.noise.function.SimplexNoise;

public class DiagonizerDemo
{
	final int width = 2048;
	final int height = 1024;
	final int CYCLE_SIZE = 16;
	
	final int[] colorBuffer = new int[width*height];
	final float[] z = new float[width*height];
	
	final float[] diffuseR = new float[width*height];
	final float[] diffuseG = new float[width*height];
	final float[] diffuseB = new float[width*height];
	
	final float[] outputR = new float[width*height];
	final float[] outputG = new float[width*height];
	final float[] outputB = new float[width*height];
	
	//
	
	float lightR = 1, lightG = 1, lightB = 1;
	
	Vector3D v = new Vector3D();
	int[] mx = {1, 1};
	int[] my = {0, 1};
	float mz = -0.5f;
	int segmentSize = 2;
	int edx = 2, edy = 1;
	
	void initLight( int dx, int dy, float dz, float r, float g, float b ) {
		v.x = dx;
		v.y = dy;
		v.z = dz;
		v.normalize();
		
		segmentSize = Math.max(Math.abs(dx), Math.abs(dy));
		mx = new int[segmentSize];
		my = new int[segmentSize];
		edx = dx == 0 ? 1 : dx;
		edy = dy == 0 ? 1 : dy;
		mz = dz / segmentSize;
		
		int x = 0, y = 0;
		for( int i=0; i<segmentSize; ++i ) {
			int nx = dx * (i+1) / segmentSize;
			mx[i] = nx - x;
			x = nx;
			
			int ny = dy * (i+1) / segmentSize;
			my[i] = ny - y;
			y = ny;
		}
		this.lightR = r;
		this.lightG = g;
		this.lightB = b;
	}
	
	void clearOutput() {
		for( int i=width*height-1; i>=0; --i ) {
			outputR[i] = outputG[i] = outputB[i] = 0;
		}
	}
	
	class Vector3D {
		float x, y, z;
		
		void normalize() {
			float len = (float)Math.sqrt(x*x + y*y + z*z);
			if( len != 0 ) {
				z /= len;
				y /= len;
				z /= len;
			}
		}
	}
	
	static void crossProduct( Vector3D a, Vector3D b, Vector3D dest ) {
		// http://math.stackexchange.com/questions/137538/calculate-the-vector-normal-to-the-plane-by-given-points
		dest.x = a.y*b.z - a.z*b.y;
		dest.y = a.x*b.z - a.z*b.x;
		dest.z = a.x*b.y - a.y*b.x;
	}
	
	static float dotProduct( Vector3D a, Vector3D b ) {
		return a.x*b.x + a.y*b.y + a.z*b.z; 
	}
	
	Vector3D a = new Vector3D();
	Vector3D b = new Vector3D();
	Vector3D c = new Vector3D();
	
	void lightLine( int x, int y ) {
		a.x = -1; a.y  = 0;
		b.x =  0; b.y = -1;
		
		if( x < 0 || y < 0 || x >= width || y >= height ) return;
		
		float minZ = Float.NEGATIVE_INFINITY;
		
		int i = 0;
		int pIdx = x + y*width;
		
		while( x >= 0 && y >= 0 && x < width && y < height ) {
			pIdx = x + y*width;
			//colorBuffer[pIdx] = color;
			
			//System.err.println( String.format("% 3d % 3d % 8.3f", x, y, minZ));
			
			a.z = (x == 0) ? z[x+1 + y*width] - z[pIdx] : z[pIdx] - z[x-1 + y*width];
			b.z = (y == 0) ? z[x + (y+1)*width] - z[pIdx] : z[pIdx] - z[x + (y-1)*width];
			
			crossProduct( a, b, c );
			c.normalize();
			// Rather than completely cut off light when shaded,
			// allow an amount inversely proportional to the distance from minZ
			// This reduces stripeyness and makes things look slightly more natural.
			// Smaller multipliers cause a subsurface scattering-like effect.
			float brighten = (z[pIdx] - minZ) * 0.5f;
			float shade = dotProduct( c, v ) * 0.5f + 1 + ((brighten < 0 ) ? brighten : 0);
			if( shade < 0 ) shade = 0;
			
			//float slope = z[pIdx] - prevZ;
			//float shade = 0.5f + slope * 0.5f; //1 - (mz + 1/slope);
			outputR[pIdx] += shade * lightR * diffuseR[pIdx];
			outputG[pIdx] += shade * lightG * diffuseG[pIdx];
			outputB[pIdx] += shade * lightB * diffuseB[pIdx];
			
			if(  z[pIdx] >= minZ ) {
				minZ = z[pIdx];
			}
			
			minZ += mz;
			
			x += mx[i];
			y += my[i];
			i = (i+1) % segmentSize;
		}
	}
	
	void lightFromEdge( int x, int y, int edx, int edy ) {
		while( x >= 0 && x < width && y >= 0 && y <= height ) {
			lightLine(x, y);
			x += edx; y += edy;
		}
	}
	
	void light() {
		System.err.println("Applying light "+lightR+","+lightG+","+lightB);
		lightFromEdge( edx < 0 ? width-1 : 0, edy < 0 ? height-1 :   0, edx, 0 );
		lightFromEdge( edx < 0 ? width-1 : 0, edy < 0 ? height-2 : edy, 0, edy );
	}
	
	int color( int a, int r, int g, int b ) {
		return
			((a << 24)&0xFF000000) |
			((r << 16)&0x00FF0000) |
			((g <<  8)&0x0000FF00) |
			((b <<  0)&0x000000FF);
	}
	
	int color( int r, int g, int b ) {
		return color( 255, r, g, b );
	}
	
	int component( float v ) {
		v *= 255;
		return v < 0 ? 0 : v > 255 ? 255 : (int)v;
	}
	
	/**
	 * Approximately equivalent to (but much faster than) Math.pow( v, 1/2.2 )
	 */
	static final float fastGamma( float v ) {
		v *= 20;
		switch( (int)(v) ) {
		case 0: return 0.0f + (v - 0f) * 0.256225724166006f;
		case 1: return 0.256225724166006f + (v - 1f) * 0.0948934492555071f;
		case 2: return 0.351119173421513f + (v - 2f) * 0.0710592432996416f;
		case 3: return 0.422178416721155f + (v - 3f) * 0.0589780883311317f;
		case 4: return 0.481156505052286f + (v - 4f) * 0.0513640396676949f;
		case 5: return 0.532520544719981f + (v - 5f) * 0.0460120643614358f;
		case 6: return 0.578532609081417f + (v - 6f) * 0.0419908459921748f;
		case 7: return 0.620523455073592f + (v - 7f) * 0.038829835429302f;
		case 8: return 0.659353290502894f + (v - 8f) * 0.0362623278169302f;
		case 9: return 0.695615618319824f + (v - 9f) * 0.034124434520899f;
		case 10: return 0.729740052840723f + (v - 10f) * 0.0323091983675802f;
		case 11: return 0.762049251208303f + (v - 11f) * 0.030743479339119f;
		case 12: return 0.792792730547422f + (v - 12f) * 0.0293753589380957f;
		case 13: return 0.822168089485518f + (v - 13f) * 0.0281668382165122f;
		case 14: return 0.85033492770203f + (v - 14f) * 0.0270893870233362f;
		case 15: return 0.877424314725366f + (v - 15f) * 0.026121116193728f;
		case 16: return 0.903545430919094f + (v - 16f) * 0.0252449169649703f;
		case 17: return 0.928790347884065f + (v - 17f) * 0.024447199667204f;
		case 18: return 0.953237547551269f + (v - 18f) * 0.0237170162902004f;
		case 19: return 0.976954563841469f + (v - 19f) * 0.0230454361585308f;
		default: return v; // Out of range, so doesn't matter other than being < 0 or > 1
		}
	}
	
	float exposure = 0.5f;
	float adjustComponent( float v ) {
		return fastGamma( v * exposure );
	}
	
	void toRgb() {
		for( int i=width*height-1; i>=0; --i ) {
			colorBuffer[i] = color( 
				component( adjustComponent(outputR[i]) ),
				component( adjustComponent(outputG[i]) ),
				component( adjustComponent(outputB[i]) )
			);
		}
	}
	
	public static void main( String[] args ) throws Exception {
		DiagonizerDemo d = new DiagonizerDemo();
		
		Random rand = new Random();
		SimplexNoise sn = new SimplexNoise();
		
		System.err.println("Generating height field...");
		float z = rand.nextFloat()*2048;
		for( int y=0; y<d.height; ++y ) {
			for( int x=0; x<d.width; ++x ) {
				int pIdx = y*d.width + x;
				
				float hz = 
					(float)Math.abs(sn.apply( (float)x / 100, z, (float)y / 100 )) * 25 +
					sn.apply( (float)x / 200, z * 1.5f, (float)y / 200 ) * 50 +
					sn.apply( (float)x / 400, z * 2.5f, (float)y / 400 ) * 100;
				float ha = hz -
					(float)Math.abs(sn.apply( (float)x / 25, z, (float)y / 25 ) * 10);
				float hb = hz + 2 - Math.abs((sn.apply( (float)x / 100, z + 90, (float)y / 100 )) * 90);
					//(sn.apply( (float)x / 300, 0, (float)y / 300 )) * 200;
				
				float dirtLightness = (0.5f + sn.apply( x * 0.5f, y * 0.55f, z ));
				
				if( ha > hb ) {
					d.diffuseR[pIdx] = dirtLightness * 1.0f;
					d.diffuseG[pIdx] = dirtLightness * 0.8f;
					d.diffuseB[pIdx] = dirtLightness * 0.6f;
					d.z[pIdx] = ha;
				} else {
					d.diffuseR[pIdx] = 0.5f;
					d.diffuseG[pIdx] = 0.6f + sn.apply( x * 0.2f, y * 0.2f, z ) * 0.5f;
					d.diffuseB[pIdx] = 0.5f;
					d.z[pIdx] = hb;
				}
			}
		}
		
		d.clearOutput();
		
		// Sun
		d.initLight(  4, 1, -0.4f, 1.0f, 1.0f, 0.7f );
		d.light();
		
		// Ambient sky light
		d.initLight( 0, 1, -1.0f, 0.08f, 0.08f, 0.1f );
		d.light();
		
		System.err.println("Converting to 32-bit ARGB");
		long conversionBeginTime = System.currentTimeMillis();
		d.toRgb();
		long conversionEndTime = System.currentTimeMillis();
		System.err.println("Conversion took "+(conversionEndTime-conversionBeginTime)+" milliseconds");
		BufferedImage bi = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		bi.setRGB(0, 0, d.width, d.height, d.colorBuffer, 0, d.width);
		ImageIO.write(bi, "png", new File("test.png"));
	}
}
