package togos.networkrts.experimental.qt2drender;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceNotFound;

public class ImageHandle implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final int FLAG_FLAGS_POPULATED = 1;
	public final int FLAG_TRANSPARENT     = 2;
	public final int FLAG_OPAQUE          = 4;
	
	public static final ImageHandle[] EMPTY_ARRAY = new ImageHandle[0];
	
	public final String imageUri;
	
	protected SoftReference<BufferedImage> image;
	protected int flags;
	protected transient ImageHandle[] single;
	protected transient List<SoftReference<BufferedImage>> scales;

	public ImageHandle( String imageUri ) {
		this.imageUri = imageUri;
	}
	
	public BufferedImage getImage(Getter<BufferedImage> getter) throws ResourceNotFound {
		BufferedImage img = image == null ? null : image.get();
		if( img == null ) {
			img = getter.get(imageUri);
			init(img);
		}
		return img;
	}
	
	public void init( BufferedImage image ) {
		assert image != null;
		this.image = new SoftReference<BufferedImage>(image);
		
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
		
		flags = FLAG_FLAGS_POPULATED;
		if( isCompletelyOpaque ) flags |= FLAG_OPAQUE;
		if( isCompletelyTransparent ) flags |= FLAG_TRANSPARENT;
	}
	
	protected void ensureFlagsPopulated( Getter<BufferedImage> source )
		throws ResourceNotFound
	{
		if( (flags & FLAG_FLAGS_POPULATED) == 0 ) {
			getImage(source);
		}
	}
	
	public boolean isCompletelyOpaque( Getter<BufferedImage> source )
		throws ResourceNotFound
	{
		ensureFlagsPopulated( source );
		return (flags & FLAG_OPAQUE) == FLAG_OPAQUE;
	}
	
	public boolean isCompletelyTransparent( Getter<BufferedImage> source )
		throws ResourceNotFound
	{
		ensureFlagsPopulated( source );
		return (flags & FLAG_TRANSPARENT) == FLAG_TRANSPARENT;
	}
	
	public synchronized BufferedImage optimized( Getter<BufferedImage> imageSource, int w, int h )
		throws ResourceNotFound 
	{
		//if( image.getWidth() == w && image.getHeight() == h ) return image;
		synchronized(this) {
			if( scales == null ) scales = new ArrayList<SoftReference<BufferedImage>>();
		}
		
		synchronized(scales) {
			for( SoftReference<BufferedImage> scaleRef : scales ) {
				BufferedImage scale = scaleRef.get();
				if( scale != null && scale.getWidth() == w && scale.getHeight() == h ) return scale;
			}
		}
		
		BufferedImage image = getImage(imageSource);
		
		BufferedImage scale = new BufferedImage(w, h, isCompletelyOpaque(imageSource) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)scale.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage( image, 0, 0, w, h, 0, 0, image.getWidth(), image.getHeight(), null);
		synchronized( scales ) {
			// TODO: fill in one of the empty spaces
			scales.add(new SoftReference<BufferedImage>(scale));
		}
		return scale;
	}

	QTRenderNode renderNode = null;
	public synchronized QTRenderNode asOpaqueRenderNode() {
		if( renderNode == null ) {
			renderNode = new QTRenderNode( null, 0, 0, 0, 0, QTRenderNode.EMPTY_SPRITE_LIST, this.single, null, null, null, null );			
		}
		return renderNode;
	}
	
	// This kind of thing is necessary because Java's serialization stuff is dumb.
	public synchronized ImageHandle[] getSingle() {
		if( single == null ) single = new ImageHandle[]{ this };
		return single;
	}
}
