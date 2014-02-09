package togos.networkrts.experimental.game19;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceNotFound;

public class Renderer
{
	protected final Getter<BufferedImage> imageLoader;
	
	public Renderer( Getter<BufferedImage> imageLoader ) {
		this.imageLoader = imageLoader;
	}
	
	public void draw( Layer layer, double lx, double ly, double ldist, Graphics g, double scale, double scx, double scy ) {
		if( ldist <= 0 ) {
			throw new RuntimeException("Distance must be positive, but "+ldist+" was given");
		}
		
		if( layer.next != null ) {
			// TODO
			// If we want really large backgrounds, probably want to use quadtrees.
		}
		
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
		
		// Left-handed coordinate system FTW
		
		int sy = osy;
		for( int y=0; y<layer.data.height; ++y, sy += tileSize ) {
			int sx = osx;
			for( int x=0; x<layer.data.width; ++x, sx += tileSize ) {
				
				// TODO: find first visible cell from top instead of starting at 0
				for( int z=0; z<layer.data.depth; ++z ) {
					BlockStack cc = layer.data.blockStacks[x + (layer.data.width)*y + (layer.data.width*layer.data.height)*z];
					for( Block b : cc.blocks ) {
						ImageHandle ih = b.imageHandle;
						if( ih.isCompletelyTransparent ) continue;
						try {
							g.drawImage( ih.getScaled(imageLoader,tileSize,tileSize), sx, sy, null );
						} catch( ResourceNotFound e ) {
							System.err.println("Couldn't load image "+ih.original.getUri());
							g.setColor( Color.PINK );
							g.fillRect( sx+1, sy+1, tileSize-2, tileSize-2 );
						}
					}
				}
			}
		}
	}
}
