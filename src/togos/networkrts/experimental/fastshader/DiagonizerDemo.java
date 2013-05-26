package togos.networkrts.experimental.fastshader;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import togos.noise.function.SimplexNoise;

public class DiagonizerDemo
{
	final int width = 1440;
	final int height = 900;
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
			float brighten = (z[pIdx] - minZ) * 0.2f;
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
	
	int component( double v ) {
		v *= 255;
		return v < 0 ? 0 : v > 255 ? 255 : (int)v;
	}
	
	void toRgb() {
		float exposure = 0.5f;
		float gamma = 2.2f;
		for( int i=width*height-1; i>=0; --i ) {
			colorBuffer[i] = color( 
				component( Math.pow( outputR[i] * exposure, gamma ) ),
				component( Math.pow( outputG[i] * exposure, gamma ) ),
				component( Math.pow( outputB[i] * exposure, gamma ) )
			);
		}
	}
	
	public static void main( String[] args ) throws Exception {
		DiagonizerDemo d = new DiagonizerDemo();
		
		SimplexNoise sn = new SimplexNoise();
		
		System.err.println("Generating height field...");
		for( int y=0; y<d.height; ++y ) {
			for( int x=0; x<d.width; ++x ) {
				int pIdx = y*d.width + x;
				
				float hz = 
					(float)Math.abs(sn.apply( (float)x / 100, 0, (float)y / 100 )) * 25 +
					sn.apply( (float)x / 200, 0, (float)y / 200 ) * 100;
				float ha = hz -
					(float)Math.abs(sn.apply( (float)x / 25, 0, (float)y / 25 ) * 10);
				float hb = hz + 1 - Math.abs((sn.apply( (float)x / 100, 90, (float)y / 100 )) * 45);
					//(sn.apply( (float)x / 300, 0, (float)y / 300 )) * 200;
				
				if( ha > hb ) {
					d.diffuseR[pIdx] = 1;
					d.diffuseG[pIdx] = 1;
					d.diffuseB[pIdx] = 1;
					d.z[pIdx] = ha;
				} else {
					d.diffuseR[pIdx] = 0.5f;
					d.diffuseG[pIdx] = 1.0f;
					d.diffuseB[pIdx] = 0.5f;
					d.z[pIdx] = hb;
				}
			}
		}
		
		d.clearOutput();
		d.initLight(  2, 1, -0.4f, 1.0f, 0.7f, 0.4f );
		d.light();
		/*
		d.initLight(  1, 3, -0.7f, 0.9f, 0.7f, 0.5f );
		d.light();
		d.initLight(  1, 2, -0.6f, 0.8f, 0.7f, 0.6f );
		d.light();
		d.initLight(  1, 1, -0.5f, 0.7f, 0.7f, 0.7f );
		d.light();
		d.initLight(  2, 1, -0.5f, 0.6f, 0.7f, 0.8f );
		d.light();
		d.initLight(  3, 1, -0.5f, 0.5f, 0.7f, 0.9f );
		d.light();
		*/
		d.initLight( -1, 2, -0.4f, 0.4f, 0.7f, 1.0f );
		d.light();
		
		System.err.println("Converting to 32-bit ARGB");
		d.toRgb();
		BufferedImage bi = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		bi.setRGB(0, 0, d.width, d.height, d.colorBuffer, 0, d.width);
		ImageIO.write(bi, "png", new File("test.png"));
	}
}
