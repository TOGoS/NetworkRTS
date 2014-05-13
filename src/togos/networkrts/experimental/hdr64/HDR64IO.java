package togos.networkrts.experimental.hdr64;

import java.awt.image.BufferedImage;

public class HDR64IO
{
	public static HDR64Drawable toHdr64Drawable( BufferedImage img, int shift ) {
		int width = img.getWidth(), height = img.getHeight();
		int[] argbRow = new int[width];
		HDR64Buffer hdrBuf = new HDR64Buffer(width,height);
		for( int y=0; y<height; ++y ) {
			img.getRGB(0, y, width, 1, argbRow, 0, width);
			for( int x=0, off=width*y; x<width; ++x, ++off ) {
				hdrBuf.data[off] = HDR64Util.intToHdr(argbRow[x], shift);
			}
		}
		return hdrBuf;
	}

	public static BufferedImage toBufferedImage( HDR64Buffer img, int shift, BufferedImage buf, int mode ) {
		if( buf == null || buf.getWidth() != img.width || buf.getHeight() != img.height || buf.getType() != mode) {
			buf = new BufferedImage(img.width, img.height, mode);
		}
		int[] row = new int[img.width];
		for( int y=img.height-1; y>=0; --y ) {
			for( int x=img.width-1, idx=y*img.width+x; x>=0; --x, --idx ) {
				row[x] = HDR64Util.hdrToInt(img.data[idx], shift);
			}
			buf.setRGB(0, y, img.width, 1, row, 0, row.length);
		}
		return buf;
	}
}
