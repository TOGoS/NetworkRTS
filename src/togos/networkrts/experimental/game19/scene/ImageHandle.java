package togos.networkrts.experimental.game19.scene;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceHandle;
import togos.networkrts.util.ResourceNotFound;

public class ImageHandle
{
	public final ResourceHandle<BufferedImage> original;
	protected volatile boolean metadataInitialized;
	public volatile boolean isCompletelyOpaque;
	public volatile boolean isCompletelyTransparent;
	protected transient SoftReference<BufferedImage>[] scaled;
	
	public ImageHandle( ResourceHandle<BufferedImage> unscaled ) {
		this.original = unscaled;
	}
	
	protected void ensureMetadataInitialized( BufferedImage image ) {
		if( !metadataInitialized ) {
			assert image != null;
			
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
			
			this.isCompletelyTransparent = isCompletelyTransparent;
			this.isCompletelyOpaque = isCompletelyOpaque;
			metadataInitialized = true;
		}
	}
	
	protected BufferedImage getOriginal( Getter<BufferedImage> populator ) throws ResourceNotFound {
		BufferedImage image = original.getValue(populator);
		ensureMetadataInitialized(image);
		return image;
	}
	
	protected BufferedImage scale( Getter<BufferedImage> populator, int width, int height ) throws ResourceNotFound {
		BufferedImage original = getOriginal(populator);
		ensureMetadataInitialized(original);
		BufferedImage b = new BufferedImage(width, height, isCompletelyOpaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)b.getGraphics();
		//g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // That's for primitive geometry
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage( original, 0, 0, width, height, 0, 0, original.getWidth(), original.getHeight(), null);
		return b;
	}
	
	public synchronized BufferedImage getScaled( Getter<BufferedImage> populator, int width, int height ) throws ResourceNotFound {
		int firstAvailableSlot = -1;
		int hash = (width ^ (height << 16));
		
		if( scaled == null ) {
			@SuppressWarnings("unchecked")
			SoftReference<BufferedImage>[] _scaled = (SoftReference<BufferedImage>[])new SoftReference[31];
			scaled = _scaled;
		}
		
		for( int i=0; i<scaled.length; ++i ) {
			int idx = (hash + i) % scaled.length;
			SoftReference<BufferedImage> s = scaled[idx];
			BufferedImage b;
			if( s == null ) {
				// Nobody's even tried to fill this slot, therefore our scaled
				// image hasn't yet been generated, so exit the loop.
				if( firstAvailableSlot == -1 ) firstAvailableSlot = idx;
				break;
			}
			
			b = s.get();
			if( b == null ) {
				// Can't tell if this would have been our slot or not,
				// but if we don't find out image elsewhere, we can use it.
				if( firstAvailableSlot == -1 ) firstAvailableSlot = idx;
				continue;
			}
			
			if( b.getWidth() == width && b.getHeight() == height ) {
				return b;
			}
		}
		
		// If no slots were available, we'll overwrite whatever's in the first one
		if( firstAvailableSlot == -1 ) firstAvailableSlot = hash % scaled.length;
		
		// Oh no, doing work in synchronized block!
		// But maybe it will be okay.
		BufferedImage b = scale( populator, width, height );
		scaled[firstAvailableSlot] = new SoftReference<BufferedImage>(b); 
		return b;
	}
}