package togos.networkrts.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.imageio.ImageIO;

import togos.networkrts.repo.BitprintFileRepository;

public class StorageUtil
{
	public static String storeSerialized( BitprintFileRepository br, Object obj ) throws IOException {
		File temp = br.tempFile();
		FileOutputStream fos = new FileOutputStream(temp);
		try {
			ObjectOutputStream oos = new ObjectOutputStream(fos); 
			oos.writeObject(obj);
			oos.close();
		} finally {
			fos.close();
		}
		return br.store(temp, true);
	}
	
	public static String storeImage( BitprintFileRepository br, BufferedImage img ) throws IOException {
		File temp = br.tempFile();
		ImageIO.write(img, "png", temp);
		return br.store(temp, true);
	}
}
