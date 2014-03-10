package togos.networkrts.experimental.game19;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import togos.networkrts.experimental.game19.demo.ServerClientDemo.Scene;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.Layer.VisibilityClip;
import togos.networkrts.experimental.game19.scene.QuadTreeLayerData;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceNotFound;

public class Renderer
{
	protected final ResourceContext resourceContext;
	protected boolean drawBackgrounds = true;
	
	public Renderer( ResourceContext resourceContext ) {
		this.resourceContext = resourceContext;
	}
	
	protected void draw( ImageHandle ih, int x, int y, int width, int height, Graphics g ) {
		try {
			g.drawImage( ih.getScaled(resourceContext.imageGetter, width, height), x, y, null );
		} catch( ResourceNotFound e ) {
			System.err.println("Couldn't load image "+ih.original.getUri());
			g.setColor( Color.PINK );
			g.fillRect( x+1, y+1, width-2, height-2 );
		}
	}
	
	protected void _drawLayerData( TileLayerData tileData, double lx, double ly, double ldist, Graphics g, double scale, double scx, double scy ) {
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
		final Getter<BufferedImage> imageGetter = resourceContext.imageGetter;
		final BufferedImage[] shadeImages = resourceContext.getShadeOverlays(tileSize); 
		
		// Left-handed coordinate system FTW
		
		int sy = osy;
		for( int shadeIndex=0, y=0; y<tileData.height; ++y, sy += tileSize ) {
			int sx = osx;
			for( int x=0; x<tileData.width; ++x, sx += tileSize, ++shadeIndex ) {
				
				if( shades[shadeIndex] == TileLayerData.SHADE_NONE ) {
					g.setColor( Color.BLACK );
					g.fillRect( sx, sy, tileSize, tileSize );
					continue;
				}
				
				// TODO: find first visible cell from top instead of starting at 0
				for( int z=0; z<tileData.depth; ++z ) {
					BlockStack cc = tileData.blockStacks[x + (tileData.width)*y + (tileData.width*tileData.height)*z];
					if( cc != null ) for( Block b : cc.getBlocks() ) {
						ImageHandle ih = b.imageHandle;
						if( ih.isCompletelyTransparent ) continue;
						try {
							g.drawImage( ih.getScaled(imageGetter,tileSize,tileSize), sx, sy, null );
						} catch( ResourceNotFound e ) {
							System.err.println("Couldn't load image "+ih.original.getUri());
							g.setColor( Color.PINK );
							g.fillRect( sx+1, sy+1, tileSize-2, tileSize-2 );
						}
					}
				}
				
				if( shades[shadeIndex] != TileLayerData.SHADE_ALL ) {
					g.drawImage( shadeImages[shades[shadeIndex]], sx, sy, null );
				}
			}
		}
	}
	
	protected void drawRstNode( RSTNode node, double x, double y, double size, Graphics g, int clipMinX, int clipMinY, int clipMaxX, int clipMaxY ) {
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
			
			final Getter<BufferedImage> imageGetter = resourceContext.imageGetter;
			for( Block b : node.getBlocks() ) {
				ImageHandle ih = b.imageHandle;
				if( ih.isCompletelyTransparent ) continue;
				try {
					g.drawImage( ih.getScaled(imageGetter,isize,isize), ix, iy, null );
				} catch( ResourceNotFound e ) {
					System.err.println("Couldn't load image "+ih.original.getUri());
					g.setColor( Color.PINK );
					g.fillRect( ix+1, iy+1, isize-2, isize-2 );
				}
			}
			break;
		default:
			System.err.println("Node type not handled by renderer: "+node.getNodeType());
		}
	}
	
	protected void _drawLayerData( Layer layer, double lx, double ly, double ldist, Graphics g, double scale, double scx, double scy ) {
		Object ld = layer.data;
		if( ld instanceof TileLayerData ) {
			_drawLayerData( (TileLayerData)ld, lx+layer.dataOffsetX, ly+layer.dataOffsetY, ldist, g, scale, scx, scy );
		} else if( ld instanceof QuadTreeLayerData ) {
			double dscale = scale / ldist;
			int x = (int)Math.round(scx + (lx + layer.dataOffsetX) * dscale);
			int y = (int)Math.round(scy + (ly + layer.dataOffsetY) * dscale);
			int nodeSize = (int)Math.round( ((QuadTreeLayerData)ld).size * dscale );
			Rectangle r = g.getClipBounds();
			drawRstNode( ((QuadTreeLayerData)ld).node, x, y, nodeSize, g, r.x, r.y, r.x+r.width, r.y+r.height );
		} else {
			System.err.println("Don't know how to draw "+(ld == null ? "null" : ld.getClass())+" layer data");
		}
	}
	
	protected void _draw( Layer layer, double lx, double ly, double ldist, Graphics g, double scale, double scx, double scy ) {
		if( ldist <= 0 ) {
			throw new RuntimeException("Distance must be positive, but "+ldist+" was given");
		}

		if( (drawBackgrounds || !layer.nextIsBackground) && layer.next != null ) {
			_draw( layer.next, lx + layer.nextOffsetX, ly + layer.nextOffsetY, ldist + layer.nextParallaxDistance, g, scale, scx, scy );
		} else {
			// Draw the background a solid color:
			Rectangle r = g.getClipBounds();
			g.setColor(Color.BLUE);
			g.fillRect(r.x, r.y, r.width, r.height);
		}
		
		_drawLayerData( layer, lx, ly, ldist, g, scale, scx, scy );
	}
	
	/**
	 * 
	 * @param layer
	 * @param lx x position on screen at which to center the object, not taking parallax into account
	 * @param ly y position on screen at which to center the object, not taking parallax into account
	 * @param ldist distance behind screen to object
	 * @param g
	 * @param scale rendering scale
	 * @param scx x position on screen where parallax offset = 0 (usually center)
	 * @param scy y position on screen where parallax offset = 0 (usually center)
	 */
	public void draw( Layer layer, double lx, double ly, double ldist, Graphics g, double scale, double scx, double scy ) {
		if( layer.visibilityClip != null ) {
			Shape oldClip = g.getClipBounds();
			
			double scaleOnScreen = scale / ldist;
			
			VisibilityClip vc = layer.visibilityClip;
			
			int sx = (int)Math.round((lx+vc.minX)*scaleOnScreen+scx);
			int sy = (int)Math.round((ly+vc.minY)*scaleOnScreen+scy);
			g.clipRect(
				sx, sy,
				(int)Math.round((lx+vc.maxX)*scaleOnScreen+scx) - sx,
				(int)Math.round((ly+vc.maxY)*scaleOnScreen+scy) - sy
			);
			
			_draw( layer, lx, ly, ldist, g, scale, scx, scy );
			
			g.setClip(oldClip);
		} else {
			_draw( layer, lx, ly, ldist, g, scale, scx, scy );
		}
	}
	
	public void draw( Scene s, double x, double y, double dist, Graphics g, double scale, double scx, double scy ) {
		draw( s.layer, x, y, dist, g, scale, scx, scy );
		
		double sscale = scale/dist;
		for( NonTile nt : s.nonTiles ) {
			draw(
				nt.icon.image,
				(int)(scx+sscale*(x+nt.x+nt.icon.imageX)),
				(int)(scy+sscale*(y+nt.y-nt.icon.imageY)),
				(int)(sscale*nt.icon.imageWidth),
				(int)(sscale*nt.icon.imageHeight),
				g
			);
		}
	}
}
