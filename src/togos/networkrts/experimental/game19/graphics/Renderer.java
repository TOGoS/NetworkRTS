package togos.networkrts.experimental.game19.graphics;

import java.awt.Color;
import java.awt.image.BufferedImage;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.Layer.LayerLink;
import togos.networkrts.experimental.game19.scene.Layer.VisibilityClip;
import togos.networkrts.experimental.game19.scene.QuadTreeLayerData;
import togos.networkrts.experimental.game19.scene.Scene;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.game19.util.ResourceContext;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.hdr64.HDR64Demo;

public class Renderer
{
	protected final ResourceContext resourceContext;
	protected boolean drawBackgrounds = true;
	
	public Renderer( ResourceContext resourceContext ) {
		this.resourceContext = resourceContext;
	}
	
	protected void _drawLayerData( TileLayerData tileData, double lx, double ly, double ldist, Surface g, double scale, double scx, double scy, float minZ, float maxZ ) {
		double dTileSize = scale / ldist;
		int osx = (int)Math.round(scx + lx * dTileSize);
		int osy = (int)Math.round(scy + ly * dTileSize);
		int tileSize = (int)Math.round(dTileSize);
		if( tileSize == 0 ) {
			// Too tiny to draw anything!
			return;
		}
		if( tileSize < 0 ) {
			throw new RuntimeException("Tile size ended up being negative with scale / distance = "+scale +" / "+ldist);
		}
		
		int[] shades = tileData.getShades(0); // TODO: need to determine reference layer somehow
		final BufferedImage[] shadeImages = resourceContext.getShadeOverlays(tileSize); 
		
		// Left-handed coordinate system FTW
		
		int sy = osy;
		for( int shadeIndex=0, y=0; y<tileData.height; ++y, sy += tileSize ) {
			int sx = osx;
			for( int x=0; x<tileData.width; ++x, sx += tileSize, ++shadeIndex ) {
				
				if( shades[shadeIndex] == TileLayerData.SHADE_NONE ) {
					g.fillRect( sx, sy, tileSize, tileSize, HDR64Demo.hdr(0, 0, 0) );
					continue;
				}
				
				// TODO: find first visible cell from top instead of starting at 0
				for( int z=0; z<tileData.depth; ++z ) {
					BlockStack cc = tileData.blockStacks[x + (tileData.width)*y + (tileData.width*tileData.height)*z];
					if( cc != null ) for( Block b : cc.getBlocks() ) {
						Icon ic = b.icon;
						if( ic.imageZ <= minZ || ic.imageZ > maxZ ) continue;
						g.drawImage(sx, sy, tileSize, tileSize, ic.imageUri);
					}
				}
				
				if( shades[shadeIndex] != TileLayerData.SHADE_ALL ) {
					// TODO: Fix somehow
					//g.drawImage( shadeImages[shades[shadeIndex]], sx, sy, null );
				}
			}
		}
	}
	
	protected void drawRstNode( RSTNode node, double x, double y, double size, Surface g, int clipMinX, int clipMinY, int clipMaxX, int clipMaxY ) {
		if( x >= clipMaxX || y >= clipMaxY || x+size <= clipMinX || y+size <= clipMinY ) return;
		
		switch( node.getNodeType() ) {
		case QUADTREE:
			RSTNode[] subNodes = node.getSubNodes();
			double subSize = size/2;
			drawRstNode( subNodes[0], x        , y        , subSize, g, clipMinX, clipMinY, clipMaxX, clipMaxY );
			drawRstNode( subNodes[1], x+subSize, y        , subSize, g, clipMinX, clipMinY, clipMaxX, clipMaxY );
			drawRstNode( subNodes[2], x        , y+subSize, subSize, g, clipMinX, clipMinY, clipMaxX, clipMaxY );
			drawRstNode( subNodes[3], x+subSize, y+subSize, subSize, g, clipMinX, clipMinY, clipMaxX, clipMaxY );
			break;
		case BLOCKSTACK:
			int ix = (int)Math.floor(x);
			int iy = (int)Math.floor(y);
			int isize = (int)Math.ceil(size);
			
			for( Block b : node.getBlocks() ) {
				Icon ic = b.icon;
				g.drawImage(ix, iy, isize, isize, ic.imageUri);
			}
			break;
		default:
			System.err.println("Node type not handled by renderer: "+node.getNodeType());
		}
	}
	
	protected void _drawLayerData( Layer layer, double lx, double ly, double ldist, Surface g, double scale, double scx, double scy, float minZ, float maxZ ) {
		Object ld = layer.data;
		if( ld instanceof TileLayerData ) {
			_drawLayerData( (TileLayerData)ld, lx+layer.dataOffsetX, ly+layer.dataOffsetY, ldist, g, scale, scx, scy, minZ, maxZ );
		} else if( ld instanceof QuadTreeLayerData ) {
			double dscale = scale / ldist;
			int x = (int)Math.round(scx + (lx + layer.dataOffsetX) * dscale);
			int y = (int)Math.round(scy + (ly + layer.dataOffsetY) * dscale);
			int nodeSize = (int)Math.round( ((QuadTreeLayerData)ld).size * dscale );
			drawRstNode( ((QuadTreeLayerData)ld).node, x, y, nodeSize, g,
				g.getClipLeft(), g.getClipTop(), g.getClipRight(), g.getClipBottom() );
		} else {
			System.err.println("Don't know how to draw "+(ld == null ? "null" : ld.getClass())+" layer data");
		}
	}
	
	protected Color awtColor(int color) {
		// TODO: cache or something?
		return new Color(color);
	}
	
	protected void fillClip( Surface g, long hdr64Color ) {
		g.fillRect(
			g.getClipLeft(), g.getClipTop(),
			g.getClipRight()-g.getClipLeft(),
			g.getClipBottom()-g.getClipTop(),
			hdr64Color
		);
	}
	
	public void draw( Layer layer, double lx, double ly, double ldist, Surface g, double scale, double scx, double scy, float minZ, float maxZ ) {
		// Draw everything at z such that minZ <= z < maxZ
		if( minZ == maxZ ) return;
		
		if( ldist <= 0 ) {
			throw new RuntimeException("Distance must be positive, but "+ldist+" was given");
		}
		
		LayerLink next = layer.next;
		drawBackground: if( minZ == Float.NEGATIVE_INFINITY ) {
			if( next != null ) {
				if( drawBackgrounds || !next.isBackground ) {
					// TODO: Will want to call something on ResourceContext
					// to indicate that we want it
					Layer nextLayer = next.layer.getValueIfImmediatelyAvailable();
					if( nextLayer != null ) {
						draw( nextLayer, lx + next.offsetX, ly + next.offsetY, ldist + next.distance, g, scale, scx, scy );
						break drawBackground;
					}
				}
				
				fillClip(g, HDR64Demo.intToHdr(next.altColor, 0));
				break drawBackground;
			}
			
			// Draw the background a solid color:
			fillClip(g, HDR64Demo.hdr(0, 0, 1));
		}
		
		_drawLayerData( layer, lx, ly, ldist, g, scale, scx, scy, minZ, maxZ );
	}
	
	public void draw( Layer layer, double lx, double ly, double ldist, Surface g, double scale, double scx, double scy ) {
		draw( layer, lx, ly, ldist, g, scale, scx, scy, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY );
	}
	
	protected void _draw( Scene s, double x, double y, double dist, Surface g, double scale, double scx, double scy ) {
		double sscale = scale/dist;
		float prevZ = Float.NEGATIVE_INFINITY;
		for( NonTile nt : s.nonTiles ) {
			Icon icon = nt.getIcon();
			
			if( icon.imageZ > prevZ ) {
				draw( s.layer, x, y, dist, g, scale, scx, scy, prevZ, icon.imageZ );
			}
			
			g.drawImage(
				(int)(scx+sscale*(x+nt.getX()+icon.imageX)),
				(int)(scy+sscale*(y+nt.getY()+icon.imageY)),
				(int)Math.ceil(sscale*icon.imageWidth),
				(int)Math.ceil(sscale*icon.imageHeight),
				icon.imageUri
			);
			
			prevZ = icon.imageZ;
		}
		draw( s.layer, x, y, dist, g, scale, scx, scy, prevZ, Float.POSITIVE_INFINITY );
	}
	
	public void draw( Scene s, double x, double y, double dist, Surface g, double scale, double scx, double scy ) {
		if( s.visibilityClip != null ) {
			double scaleOnScreen = scale / dist;
			
			VisibilityClip vc = s.visibilityClip;
			
			int sx = (int)Math.round((x+vc.minX)*scaleOnScreen+scx);
			int sy = (int)Math.round((y+vc.minY)*scaleOnScreen+scy);
			g = g.intersectClip(
				sx, sy,
				(int)Math.round((x+vc.maxX)*scaleOnScreen+scx) - sx,
				(int)Math.round((y+vc.maxY)*scaleOnScreen+scy) - sy
			);
			
			_draw( s, x, y, dist, g, scale, scx, scy );
		}
		
		_draw( s, x, y, dist, g, scale, scx, scy );
	}
}
