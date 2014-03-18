package togos.networkrts.experimental.game19.demo;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.Layer.LayerLink;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.QuadRSTNode;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.RSTNodeUpdater;
import togos.networkrts.experimental.game19.world.RSTUtil;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.experimental.game19.world.gen.SolidNodeFiller;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.shape.TCircle;
import togos.networkrts.experimental.shape.TRectangle;
import togos.networkrts.util.SoftResourceHandle;

public class DemoWorld
{
	protected static Icon loadBlockIcon(ResourceContext rc, String filename, float frontZ) throws IOException {
		ImageHandle ih = rc.storeImageHandle(new File(filename));
		return new Icon(ih, -0.5f, -0.5f, frontZ, 1f, 1f);
	}
	
	public static World initWorld( ResourceContext rc ) throws IOException {
		Icon brickImage = loadBlockIcon(rc, "tile-images/dumbrick1.png", 0.5f);
		Icon dirtImage = loadBlockIcon(rc, "tile-images/dirt0.png", 0.5f);
		Icon grassImage = loadBlockIcon(rc, "tile-images/grass0.png", 0.5f);
		Icon treeImage = loadBlockIcon(rc, "tile-images/tree0.png", -0.4f);
		
		final Block bricks = new Block(BitAddresses.BLOCK_SOLID|BitAddresses.BLOCK_OPAQUE, brickImage, NoBehavior.instance);
		final Block dirt = new Block(BitAddresses.BLOCK_SOLID|BitAddresses.BLOCK_OPAQUE, dirtImage, NoBehavior.instance);
		final Block grass = new Block(0, grassImage, NoBehavior.instance);
		final Block tree = new Block(0, treeImage, NoBehavior.instance);
		
		int worldSizePower = 24;
		int worldDataOrigin = -(1<<(worldSizePower-1));
		
		RSTNode n = QuadRSTNode.createHomogeneous(bricks.stack, worldSizePower);
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 0, 20, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 4, 20, 1 ), new SolidNodeFiller( dirt.stack ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 3, 20, 1 ), new RSTNodeUpdater() {
			final RSTNode treeAndGrass = BlockStackRSTNode.create(new Block[] { tree, grass } );
			final Random r = new Random();
			@Override public RSTNode update(RSTNode oldNode, int x, int y, int sizePower) {
				double v = r.nextDouble();
				return v < 0.1 ? bricks.stack : v < 0.25 ? treeAndGrass : grass.stack;
			}
		});
		
		Random r = new Random();
		//for( int i=0; i<100; ++i ) {
		//	n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( r.nextGaussian()*20, r.nextGaussian()*20, r.nextDouble()*8 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		//}
		for( int i=0; i<10; ++i ) {
			double sx = 0, sy = 0, dir = 0, rad = 4;
			while( rad > 1 ) {
				n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( sx, sy, rad ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
				sx += Math.cos(dir);
				sy += Math.sin(dir);
				dir += r.nextGaussian() * 0.1;
				rad *= (0.99 + r.nextGaussian()*0.02);
			}
		}

			
		EntitySpatialTreeIndex<NonTile> nonTiles = new EntitySpatialTreeIndex<NonTile>();
		return new World(n, worldSizePower, nonTiles,
			new LayerLink(true, new SoftResourceHandle<Layer>("urn:sha1:blah"), 0, 0, 0, 0xFF001122)
		);
	}

}
