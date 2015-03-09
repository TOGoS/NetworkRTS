package togos.networkrts.experimental.game19.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import togos.blob.InputStreamable;
import togos.networkrts.experimental.game19.io.CerealWorldIO;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.qt2drender.Blackifier;
import togos.networkrts.repo.BitprintFileRepository;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ImageGetter;
import togos.networkrts.util.Repository;
import togos.networkrts.util.ResourceHandlePool;

public class ResourceContext
{
	protected final BitprintFileRepository repo;
	public final ImageGetter imageGetter;
	public final ResourceHandlePool resourceHandlePool;
	
	public ResourceContext( File storageDir ) {
		repo = new BitprintFileRepository(storageDir);
		imageGetter = new ImageGetter(repo.toBlobGetter());
		resourceHandlePool = new ResourceHandlePool();
	}
	
	Map<File,String> fileUrns = new HashMap<File,String>();
	public String storeFile( File f ) throws IOException {
		String urn = fileUrns.get(f);
		if( urn == null ) {
			fileUrns.put(f, urn = repo.store( f, false ));
		}
		return urn;
	}
	
	public Repository<byte[]> getByteArrayRepository() {
		return repo.toByteArrayRepository();
	}
	
	public Getter<InputStreamable> getBlobGetter() {
		return repo.toBlobGetter();
	}
	
	//// Image stuff; maybe belongs in a subclass?
	
	protected final WeakHashMap<String,ImageHandle> imageHandleCache = new WeakHashMap<String,ImageHandle>();
	protected final ResourceHandlePool imageResourceHandlePool = new ResourceHandlePool();
	public synchronized ImageHandle getImageHandle(String uri) {
		ImageHandle ih = imageHandleCache.get(uri);
		if( ih == null ) {
			ih = new ImageHandle(imageResourceHandlePool.<BufferedImage>get(uri));
			imageHandleCache.put(uri, ih);
		}
		return ih;
	}
	
	protected static final String SHADE_IMAGE_URI_PREFIX = "corner-blackener:";
	
	public static final String[] SHADE_IMAGE_URIS = new String[16];
	static {
		for( int i=0; i<SHADE_IMAGE_URIS.length; ++i ) {
			SHADE_IMAGE_URIS[i] = SHADE_IMAGE_URI_PREFIX+i;
		}
	}
	
	public static int parseShadeImageUrn(String urn) {
		return urn.startsWith(SHADE_IMAGE_URI_PREFIX) ?
			Integer.parseInt(urn.substring(SHADE_IMAGE_URI_PREFIX.length())) : -1;
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
			shadeOverlaySize = size;
		}
		return shadeOverlays;
	}
	
	protected CerealWorldIO cerealWorldIo;
	public CerealWorldIO getCerealWorldIo() {
		if( cerealWorldIo == null ) cerealWorldIo = new CerealWorldIO(getByteArrayRepository());
		return cerealWorldIo;
	}
}
