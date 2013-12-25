package togos.networkrts.experimental.qt2drender;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.qt2drender.Renderer.RenderNode;

public class ImageHandle
{
	public final BufferedImage image;
	public final boolean hasTranslucentPixels;
	// Big fat memory leak:
	final List<BufferedImage> scales = new ArrayList<BufferedImage>();
	
	public ImageHandle( BufferedImage image ) {
		this.image = image;
		
		findTranslucentPixels: {
			for( int y=0; y<image.getHeight(); ++y ) for( int x=0; x<image.getWidth(); ++x ) {
				if( (image.getRGB(y, x) & 0xFF000000) != 0xFF000000 ) {
					hasTranslucentPixels = true;
					break findTranslucentPixels;
				}
			}
			hasTranslucentPixels = false;
		}
	}
	
	public BufferedImage optimized( int w, int h ) {
		//if( image.getWidth() == w && image.getHeight() == h ) return image;
		
		for( BufferedImage scale : scales ) {
			if( scale.getWidth() == w && scale.getHeight() == h ) return scale;
		}
		
		BufferedImage scale = new BufferedImage(w, h, hasTranslucentPixels ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		scale.getGraphics().drawImage( image, 0, 0, w, h, 0, 0, image.getWidth(), image.getHeight(), null);
		scales.add(scale);
		return scale;
	}

	RenderNode renderNode = null;
	public synchronized RenderNode asOpaqueRenderNode() {
		if( renderNode == null ) {
			renderNode = new RenderNode( null, 0, 0, 0, 0, RenderNode.EMPTY_SPRITE_LIST, this, null, null, null, null );			
		}
		return renderNode;
	}
}
