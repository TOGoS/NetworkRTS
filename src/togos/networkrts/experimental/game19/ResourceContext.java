package togos.networkrts.experimental.game19;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.qt2drender.Blackifier;
import togos.networkrts.repo.BlobRepository;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ImageGetter;
import togos.networkrts.util.ResourceHandlePool;

public class ResourceContext
{
	protected final BlobRepository repo;
	public final ImageGetter imageGetter;
	public final ResourceHandlePool resourceHandlePool;
	
	public ResourceContext( File storageDir ) {
		repo = new BlobRepository(storageDir);
		imageGetter = new ImageGetter(repo.toBlobGetter());
		resourceHandlePool = new ResourceHandlePool();
	}
	
	Map<File,String> imageUrns = new HashMap<File,String>();
	public String storeImage( File f ) throws IOException {
		String urn = imageUrns.get(f);
		if( urn == null ) {
			imageUrns.put(f, urn = repo.store( f, false ));
		}
		return urn;
	}
	
	public ImageHandle storeImageHandle( File f ) throws IOException {
		return new ImageHandle( resourceHandlePool.<BufferedImage>get(storeImage(f)) );
	}
	
	protected BufferedImage[] shadeOverlays;
	protected int shadeOverlaySize;
	public synchronized BufferedImage[] getShadeOverlays( int size ) {
		if( shadeOverlays == null || size != shadeOverlaySize ) {
			shadeOverlays = new BufferedImage[16];
			for( int i=0; i<16; ++i ) shadeOverlays[i] = Blackifier.makeShadeOverlay(
				size,
				(i & TileLayerData.SHADE_TL) != 0 ? 1 : 0,
				(i & TileLayerData.SHADE_TR) != 0 ? 1 : 0,
				(i & TileLayerData.SHADE_BL) != 0 ? 1 : 0,
				(i & TileLayerData.SHADE_BR) != 0 ? 1 : 0
			); 
		}
		return shadeOverlays;
	}
}
