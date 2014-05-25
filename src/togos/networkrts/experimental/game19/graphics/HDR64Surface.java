package togos.networkrts.experimental.game19.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import togos.blob.InputStreamable;
import togos.networkrts.experimental.hdr64.HDR64Buffer;
import togos.networkrts.experimental.hdr64.HDR64Drawable;
import togos.networkrts.experimental.hdr64.HDR64IO;
import togos.networkrts.experimental.hdr64.HDR64MonteCarloScaler;
import togos.networkrts.experimental.hdr64.HDR64Util;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceNotFound;

public class HDR64Surface extends AbstractSurface
{
	static class HDRImageLoader {
		protected final Getter<InputStreamable> blobSource;

		// TODO: Like a more regular soft cache thing
		Map<String, HDR64Buffer> bufs = new WeakHashMap<String,HDR64Buffer>();
		HDR64MonteCarloScaler scaler = new HDR64MonteCarloScaler();
		
		public HDRImageLoader( Getter<InputStreamable> bs ) {
			blobSource = bs;
		}
		
		public HDR64Drawable getImage( String urn, int w, int h ) throws ResourceNotFound {
			// TODO: key without having to cat strings, I suppose
			final String scaledKey = urn + ";" + w+"x"+h;
			
			HDR64Buffer scaledBuf = bufs.get(scaledKey);
			if( scaledBuf != null ) return scaledBuf;
			
			final String origKey = urn + ";orig";
			HDR64Buffer origBuf = bufs.get(origKey);
			if( origBuf == null ) {
				InputStreamable imageData = blobSource.get(urn);
				BufferedImage bImg;
				try {
					InputStream is = imageData.openInputStream();
					try {
						bImg = ImageIO.read(ImageIO.createImageInputStream(is));
					} finally {
						is.close();
					}
				} catch( IOException e ) {
					throw new ResourceNotFound("IO error while reading buffered image from "+urn, e);
				}
				origBuf = HDR64IO.toHdr64Buffer(bImg, 0);
				bufs.put(origKey, origBuf);
				bufs.put(urn + ";" + origBuf.width+"x"+origBuf.height, origBuf);
			}
			
			scaledBuf = scaler.scale(origBuf, w, h);
			bufs.put(scaledKey, scaledBuf);
			return scaledBuf;
		}
	}
	
	protected final HDR64Buffer buffer;
	protected final HDRImageLoader imageLoader;
	
	public HDR64Surface( HDR64Buffer buffer, HDRImageLoader il, int clipLeft, int clipTop, int clipRight, int clipBottom ) {
		super(0, 0, buffer.width, buffer.height);
		this.buffer = buffer;
		this.imageLoader = il;
		assert clipLeft   >= 0;
		assert clipTop    >= 0;
		assert clipRight  <= buffer.width;
		assert clipBottom <= buffer.height;
	}
	
	public HDR64Surface( HDR64Buffer buffer, HDRImageLoader il ) {
		this( buffer, il, 0, 0, buffer.width, buffer.height );
	}
	
	public static HDR64Surface create( HDR64Buffer buffer, Getter<InputStreamable> imageGetter ) {
		return new HDR64Surface(buffer, new HDRImageLoader(imageGetter));
	}
	
	@Override protected AbstractSurface withClip( int left, int top, int right, int bottom ) {
		return new HDR64Surface( buffer, imageLoader, left, top, right, bottom );
	}
	
	@Override public void fillRect( int x, int y, int w, int h, long hdr64Color ) {
		int x0 = Math.max(x,clipLeft), x1 = Math.min(x+w, clipRight);
		int y0 = Math.max(y,clipTop) , y1 = Math.min(y+h, clipBottom);
		for( int fy=y0; fy<y1; ++fy ) for( int fx=x0, idx=fy*buffer.width+fx; fx<x1; ++fx, ++idx ) {
			buffer.data[idx] = hdr64Color;
		}
	}
	
	protected final long notFoundColor = HDR64Util.hdr(1, 1, 0, 1); 
	
	@Override public void drawImage( int x, int y, int w, int h, String imageUrn ) {
		try {
			imageLoader.getImage(imageUrn, w, h).draw(buffer, x, y, clipLeft, clipTop, clipRight, clipBottom );
		} catch( ResourceNotFound e ) {
			fillRect( x, y, w, h, notFoundColor );
		}
	}
}
