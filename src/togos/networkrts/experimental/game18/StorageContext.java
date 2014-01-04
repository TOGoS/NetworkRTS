package togos.networkrts.experimental.game18;

import togos.networkrts.experimental.qt2drender.demo.ImageHandlePool;
import togos.networkrts.repo.BlobRepository;
import togos.networkrts.util.ResourceHandlePool;

public class StorageContext
{
	static final ImageHandlePool theImageHandlePool = new ImageHandlePool();
	static final ResourceHandlePool theResourceHandlePool = new ResourceHandlePool();
	
	public final BlobRepository blobRepository;
	public final ImageHandlePool imageHandlePool = theImageHandlePool;
	public final ResourceHandlePool resourceHandlePool = theResourceHandlePool;
	
	public StorageContext( BlobRepository blobRepo ) {
		this.blobRepository = blobRepo;
	}
}
