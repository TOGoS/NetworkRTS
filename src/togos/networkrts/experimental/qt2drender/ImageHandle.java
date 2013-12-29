package togos.networkrts.experimental.qt2drender;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.qt2drender.Renderer.RenderNode;

public class ImageHandle implements Serializable
{
	// TODO: mayube image handles should just have URIs
	// and then be lazily populated
	private static final long serialVersionUID = 1L;

	public static final ImageHandle[] EMPTY_ARRAY = new ImageHandle[0];
	
	public final BufferedImage image;
	public final boolean isCompletelyOpaque;
	public final boolean isCompletelyTransparent;
	// Big fat memory leak:
	protected final List<BufferedImage> scales = new ArrayList<BufferedImage>();
	public final ImageHandle[] single = new ImageHandle[]{ this };
	
	public ImageHandle( BufferedImage image ) {
		this.image = image;
		
		boolean isCompletelyTransparent = true;
		boolean isCompletelyOpaque = true;
		
		for( int y=0; y<image.getHeight(); ++y ) for( int x=0; x<image.getWidth(); ++x ) {
			switch( (image.getRGB(x, y) & 0xFF000000) ) {
			case 0xFF000000:
				isCompletelyTransparent = false;
				break;
			case 0x00000000:
				isCompletelyOpaque = false;
				break;
			default:
				isCompletelyTransparent = false;
				isCompletelyOpaque = false;
				break;
			}
			
			if( !isCompletelyTransparent && !isCompletelyOpaque ) break;
		}
		
		this.isCompletelyOpaque = isCompletelyOpaque;
		this.isCompletelyTransparent = isCompletelyTransparent;
	}
	
	public BufferedImage optimized( int w, int h ) {
		//if( image.getWidth() == w && image.getHeight() == h ) return image;
		
		for( BufferedImage scale : scales ) {
			if( scale.getWidth() == w && scale.getHeight() == h ) return scale;
		}
		
		BufferedImage scale = new BufferedImage(w, h, isCompletelyOpaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)scale.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage( image, 0, 0, w, h, 0, 0, image.getWidth(), image.getHeight(), null);
		scales.add(scale);
		return scale;
	}

	RenderNode renderNode = null;
	public synchronized RenderNode asOpaqueRenderNode() {
		if( renderNode == null ) {
			renderNode = new RenderNode( null, 0, 0, 0, 0, RenderNode.EMPTY_SPRITE_LIST, this.single, null, null, null, null );			
		}
		return renderNode;
	}
}
