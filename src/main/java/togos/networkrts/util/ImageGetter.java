package togos.networkrts.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import togos.blob.InputStreamable;

public class ImageGetter implements Getter<BufferedImage>
{
	protected final Getter<InputStreamable> blobGetter;
	public ImageGetter( Getter<InputStreamable> blobGetter ) {
		this.blobGetter = blobGetter;
	}
	
	@Override public BufferedImage get( String uri ) throws ResourceNotFound {
		InputStreamable imageBlob = blobGetter.get(uri);
		try {
			InputStream is = imageBlob.openInputStream();
			try {
				return ImageIO.read(is);
			} finally {
				is.close();
			}
		} catch( IOException e ) {
			throw new ResourceNotFound(uri, e);
		}
	}
}