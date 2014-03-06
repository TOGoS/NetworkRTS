package togos.networkrts.experimental.game19;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.LayerData;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceNotFound;

public class Renderer
{
	protected final ResourceContext resourceContext;
	
	public Renderer( ResourceContext resourceContext ) {
		this.resourceContext = resourceContext;
	}
	
	protected void _draw( Layer layer, double lx, double ly, double ldist, Graphics g, double scale, double scx, double scy ) {
		if( ldist <= 0 ) {
			throw new RuntimeException("Distance must be positive, but "+ldist+" was given");
		}

		if( layer.next != null ) {
			// TODO
			// If we want really large backgrounds, probably want to use quadtrees.
		}
		
		// For now just draw the background a solid color:
		Rectangle r = g.getClipBounds();
		g.setColor(Color.BLUE);
		g.fillRect(r.x, r.y, r.width, r.height);
		
		double dTileSize = scale / ldist;
		int osx = (int)Math.round(scx + (lx + layer.dataOffsetX) * dTileSize);
		int osy = (int)Math.round(scy + (ly + layer.dataOffsetY) * dTileSize);
		int tileSize = (int)Math.round(dTileSize);
		if( tileSize == 0 ) {
			// Too tiny to draw anything!
			return;
		}
		if( tileSize < 0 ) {
			throw new RuntimeException("Tile size ended up being negative with scale / distance = "+scale +" / "+ldist);
		}
		
		int[] shades = layer.data.getShades(0); // TODO: need to determine reference layer somehow
		final Getter<BufferedImage> imageGetter = resourceContext.imageGetter;
		final BufferedImage[] shadeImages = resourceContext.getShadeOverlays(tileSize); 
		
		// Left-handed coordinate system FTW
		
		int sy = osy;
		for( int shadeIndex=0, y=0; y<layer.data.height; ++y, sy += tileSize ) {
			int sx = osx;
			for( int x=0; x<layer.data.width; ++x, sx += tileSize, ++shadeIndex ) {
				
				if( shades[shadeIndex] == LayerData.SHADE_NONE ) {
					g.setColor( Color.BLACK );
					g.fillRect( sx, sy, tileSize, tileSize );
					continue;
				}
				
				// TODO: find first visible cell from top instead of starting at 0
				for( int z=0; z<layer.data.depth; ++z ) {
					BlockStack cc = layer.data.blockStacks[x + (layer.data.width)*y + (layer.data.width*layer.data.height)*z];
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
				
				if( shades[shadeIndex] != LayerData.SHADE_ALL ) {
					g.drawImage( shadeImages[shades[shadeIndex]], sx, sy, null );
				}
			}
		}
	}
	
	public void draw( Layer layer, double lx, double ly, double ldist, Graphics g, double scale, double scx, double scy ) {
		Shape oldClip = g.getClipBounds();
		
		double dTileSize = scale / ldist;
		int osx = (int)Math.round(scx + (lx + layer.dataOffsetX) * dTileSize);
		int osy = (int)Math.round(scy + (ly + layer.dataOffsetY) * dTileSize);
		int tileSize = (int)Math.round(dTileSize);
		
		g.clipRect(osx, osy, tileSize*layer.data.width, tileSize*layer.data.height);
		
		_draw( layer, lx, ly, ldist, g, scale, scx, scy );
		g.setClip(oldClip);
	}
}
