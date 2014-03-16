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
	static class BufferedImageWithFlipBits extends BufferedImage {
		public final int fWidth, fHeight;
		public BufferedImageWithFlipBits(int width, int height, int mode) {
			super(Math.abs(width), Math.abs(height), mode);
			fWidth = width;
			fHeight = height;
		}
	}

	public final ResourceHandle<BufferedImage> original;
	protected volatile boolean metadataInitialized;
	public volatile boolean isCompletelyOpaque;
	public volatile boolean isCompletelyTransparent;
	protected transient SoftReference<BufferedImageWithFlipBits>[] scaled;
	
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
	
	protected BufferedImageWithFlipBits scale( Getter<BufferedImage> populator, int width, int height ) throws ResourceNotFound {
		assert width != 0;
		assert height != 0;
		BufferedImage original = getOriginal(populator);
		ensureMetadataInitialized(original);
		final int dx0, dy0, dx1, dy1;
		if( width  < 0 ) { dx0 = -width ; dx1 = 0; } else { dx0 = 0; dx1 = width ; }
		if( height < 0 ) { dy0 = -height; dy1 = 0; } else { dy0 = 0; dy1 = height; }
		BufferedImageWithFlipBits b = new BufferedImageWithFlipBits(width, height, isCompletelyOpaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)b.getGraphics();
		// Interpolate only when scaling down
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			(b.getWidth() < original.getWidth() && b.getHeight() < original.getHeight()) ?
				RenderingHints.VALUE_INTERPOLATION_BICUBIC :
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage( original, dx0, dy0, dx1, dy1, 0, 0, original.getWidth(), original.getHeight(), null);
		return b;
	}
	
	public synchronized BufferedImage getScaled( Getter<BufferedImage> populator, int width, int height ) throws ResourceNotFound {
		int firstAvailableSlot = -1;
		int hash = (width ^ (height << 16)) & Integer.MAX_VALUE;
		
		if( scaled == null ) {
			@SuppressWarnings("unchecked")
			SoftReference<BufferedImageWithFlipBits>[] _scaled = (SoftReference<BufferedImageWithFlipBits>[])new SoftReference[31];
			scaled = _scaled;
		}
		
		for( int i=0; i<scaled.length; ++i ) {
			int idx = (hash + i) % scaled.length;
			SoftReference<BufferedImageWithFlipBits> s = scaled[idx];
			BufferedImageWithFlipBits b;
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
			
			if( b.fWidth == width && b.fHeight == height ) {
				return b;
			}
		}
		
		// If no slots were available, we'll overwrite whatever's in the first one
		if( firstAvailableSlot == -1 ) firstAvailableSlot = hash % scaled.length;
		
		// Oh no, doing work in synchronized block!
		// But maybe it will be okay.
		BufferedImageWithFlipBits b = scale( populator, width, height );
		scaled[firstAvailableSlot] = new SoftReference<BufferedImageWithFlipBits>(b); 
		return b;
	}
}