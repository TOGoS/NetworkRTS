package togos.networkrts.experimental.game18;

import togos.networkrts.experimental.qt2drender.demo.ImageHandlePool;
import togos.networkrts.repo.BitprintFileRepository;
import togos.networkrts.util.ResourceHandlePool;

public class StorageContext
{
	static final ImageHandlePool theImageHandlePool = new ImageHandlePool();
	static final ResourceHandlePool theResourceHandlePool = new ResourceHandlePool();
	
	public final BitprintFileRepository blobRepository;
	public final ImageHandlePool imageHandlePool = theImageHandlePool;
	public final ResourceHandlePool resourceHandlePool = theResourceHandlePool;
	
	public StorageContext( BitprintFileRepository blobRepo ) {
		this.blobRepository = blobRepo;
	}
}
