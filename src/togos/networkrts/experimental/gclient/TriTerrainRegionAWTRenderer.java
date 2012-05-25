package togos.networkrts.experimental.gclient;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.sim1.world.Handle;
import togos.networkrts.experimental.sim1.world.TerrainRegion;
import togos.networkrts.experimental.sim1.world.TerrainType;
import togos.networkrts.resource.ResourceLoader;

/** This is a single-threaded class due to it uses
 * instance variables to hold some arrays and stuff. */
public class TriTerrainRegionAWTRenderer implements TimestampedPaintable
{
	Handle[] emptyHandleArray = new Handle[0];
	
	ResourceLoader l = new ResourceLoader();
	
	TerrainRegion subregion1 = new TerrainRegion(
		Handle.getHardAnonymousInstance( new Handle[] {
			Handle.getHardAnonymousInstance( new TerrainType(Color.BLUE) ), 
			Handle.getHardAnonymousInstance( new TerrainType(Color.YELLOW) ),
			Handle.getHardAnonymousInstance( new TerrainType(Color.GREEN) ),
		}),
		Handle.NULL_HANDLE,
		new byte[] {
			1, 1, 1, 1, 1, 1, 1, 0,
			0, 0, 0, 1, 1, 1, 0, 1,
			0, 0, 1, 1, 0, 0, 1, 0,
			1, 0, 1, 0, 0, 1, 0, 0,
			0, 2, 2, 2, 0, 3, 1, 0,
			0, 2, 2, 2, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 1,
			0, 0, 0, 0, 0, 0, 0, 0
		}, new byte[] {
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
		}
	);
	TerrainRegion region = new TerrainRegion(
		Handle.getHardAnonymousInstance( new Handle[] {
			Handle.getHardAnonymousInstance( new TerrainType(Color.BLUE) ), 
			Handle.getHardAnonymousInstance( new TerrainType(Color.YELLOW) ),
			Handle.getHardAnonymousInstance( new TerrainType(Color.GREEN) ),
		}),
		Handle.getHardAnonymousInstance( new Handle[] {
			Handle.getHardAnonymousInstance( subregion1 ), 
		}),
		new byte[] {
			1, 1, 1, 1, 1, 1, 1, 0,
			0, 0, 0, 1, 1, 1, 0, 1,
			0, 0, 1, 1, 0, 0, 1, 0,
			1, 0, 1, 0, 0, 1, 0, 0,
			0, 2, 2, 2, 0, 3, 1, 0,
			0, 2, 2, 2, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 1,
			0, 0, 0, 0, 0, 0, 0, 0
		}, new byte[] {
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
			0, 1, 2, 3, 0, 1, 2, 3,
		}
	);
	
	/** Size of the root TerrainRegion on the screen and
	 * whether or not it points up: */
	boolean pointUp;
	double size = 256;
	
	int[] polyPointsX = new int[3];
	int[] polyPointsY = new int[3];
	
	protected void paint1( TerrainRegion r, TerrainType[] terrainTypes, TerrainRegion[] subregions, int o, Graphics2D g2d, double ox, double oy, double m ) {
		g2d.setColor( terrainTypes[r.terrainTypeIndexes[o]&0xFF].color );
		polyPointsX[0] = (int)(ox-0.0625*m);
		polyPointsX[1] = (int)(ox);
		polyPointsX[2] = (int)(ox+0.0625*m);
		polyPointsY[0] = (int)(oy-0.0625*m);
		polyPointsY[1] = (int)(oy+0.0625*m);
		polyPointsY[2] = (int)(oy-0.0625*m);
		g2d.fillPolygon(polyPointsX, polyPointsY, 3);
		
		if( subregions != null ) {
			TerrainRegion sr = subregions[r.subregionIndexes[o]&0xFF];
			if( sr != TerrainRegion.NULL_REGION ) {
				paint64( sr, g2d, ox, oy, m*0.125 );
			}
		}
	}
	
	protected void paint4( TerrainRegion r, TerrainType[] terrainTypes, TerrainRegion[] subregions, int o, Graphics2D g2d, double ox, double oy, double m ) {
		paint1( r, terrainTypes, subregions, o+0, g2d, ox-0.0625*m, oy-0.0625*m,  m );
		paint1( r, terrainTypes, subregions, o+1, g2d, ox+       0, oy+0.0625*m,  m );
		paint1( r, terrainTypes, subregions, o+2, g2d, ox+0.0625*m, oy-0.0625*m,  m );
		paint1( r, terrainTypes, subregions, o+3, g2d, ox+       0, oy-0.0625*m, -m );
	}
	
	protected void paint16( TerrainRegion r, TerrainType[] terrainTypes, TerrainRegion[] subregions, int o, Graphics2D g2d, double ox, double oy, double m ) {
		paint4( r, terrainTypes, subregions, o+ 0, g2d, ox-0.125*m, oy-0.125*m,  m );
		paint4( r, terrainTypes, subregions, o+ 4, g2d, ox+      0, oy+0.125*m,  m );
		paint4( r, terrainTypes, subregions, o+ 8, g2d, ox+0.125*m, oy-0.125*m,  m );
		paint4( r, terrainTypes, subregions, o+12, g2d, ox+      0, oy-0.125*m, -m );
	}
	
	protected void paint64( TerrainRegion r, Graphics2D g2d, double ox, double oy, double m ) {
		l.precache(r.terrainTypePaletteHandle);
		l.precache(r.subregionPaletteHandle);
		
		Handle[] subregionHandles = (Handle[])l.getValue(r.subregionPaletteHandle, emptyHandleArray);
		Handle[] terrainTypeHandles = (Handle[])l.getValue(r.terrainTypePaletteHandle, emptyHandleArray);
		
		TerrainType[] terrainTypes = new TerrainType[256];
		TerrainRegion[] subregions = null;

		for( int i=0; i<terrainTypeHandles.length; ++i ) {
			l.precache(terrainTypeHandles[i]);
		}
		if( m >= 192 || m <= -192 ) {
			subregions = new TerrainRegion[256];
			for( int i=0; i<subregionHandles.length; ++i ) {
				l.precache(subregionHandles[i]);
			}
			for( int i=0; i<subregionHandles.length; ++i ) {
				subregions[i] = (TerrainRegion)l.getValue(subregionHandles[i], TerrainRegion.NULL_REGION);
			}
			for( int i=subregionHandles.length; i<256; ++i ) {
				subregions[i] = TerrainRegion.NULL_REGION;
			}
		}
		for( int i=0; i<terrainTypeHandles.length; ++i ) {
			terrainTypes[i] = (TerrainType)l.getValue(terrainTypeHandles[i], TerrainRegion.NULL_REGION);
		}
		for( int i=terrainTypeHandles.length; i<256; ++i ) {
			terrainTypes[i] = TerrainType.NULL_TERRAIN_TYPE;
		}
		
		paint16( r, terrainTypes, subregions,  0, g2d, ox-0.25*m, oy-0.25*m,  m );
		paint16( r, terrainTypes, subregions, 16, g2d, ox+     0, oy+0.25*m,  m );
		paint16( r, terrainTypes, subregions, 32, g2d, ox+0.25*m, oy-0.25*m,  m );
		paint16( r, terrainTypes, subregions, 48, g2d, ox+     0, oy-0.25*m, -m );
	}
	
	long origTimestamp = 0;
	
	public void paint( long timestamp, int width, int height, Graphics2D g2d ) {
		if( region == null ) return;
		
		if( origTimestamp == 0 ) origTimestamp = timestamp;
		size = 128 + (timestamp - origTimestamp)/128;
		
		AffineTransform t = g2d.getTransform();
		paint64( region, g2d, 0, 0, pointUp ? -size : size );
		g2d.setTransform(t);
	}
}
