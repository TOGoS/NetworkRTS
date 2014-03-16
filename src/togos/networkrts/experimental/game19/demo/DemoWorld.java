package togos.networkrts.experimental.game19.demo;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.QuadRSTNode;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.RSTUtil;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.experimental.game19.world.gen.SolidNodeFiller;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.shape.TCircle;

public class DemoWorld
{

	public static World initWorld( ResourceContext rc ) throws IOException {
		ImageHandle brickImage = rc.storeImageHandle(new File("tile-images/dumbrick1.png"));
		//ImageHandle dudeImage = resourceContext.storeImageHandle(new File("tile-images/dude.png"));
		//ImageHandle ballImage = resourceContext.storeImageHandle(new File("tile-images/stupid-ball.png"));
		
		Block bricks = new Block(BitAddresses.BLOCK_SOLID|BitAddresses.BLOCK_OPAQUE, brickImage, NoBehavior.instance);
		
		int worldSizePower = 24;
		int worldDataOrigin = -(1<<(worldSizePower-1));
		
		RSTNode n = QuadRSTNode.createHomogeneous(bricks.stack, worldSizePower);
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		
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
		return new World(n, worldSizePower, nonTiles);
	}

}
