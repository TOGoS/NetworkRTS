package togos.networkrts.experimental.game19.world.thing.jetman;

import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class JetManPieceBehavior implements NonTileInternals<BlargNonTile>
{
	protected final Icon icon;
	protected final AABB aabb;
	
	public JetManPieceBehavior(Icon ic) {
		this.icon = ic;
		this.aabb = new AABB(-ic.imageWidth/2f, -ic.imageHeight/2f, -ic.imageWidth/2f, +ic.imageWidth/2f, +ic.imageHeight/2f, +ic.imageWidth/2f);
	}
	
	@Override public NonTile update(final BlargNonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy + JetManInternals.GRAVITY;
		
		BlockCollision c = BlockCollision.findCollisionWithRst(nt, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		if( c != null ) {
			if( c.correctionX != 0 && Math.abs(c.correctionX) < Math.abs(c.correctionY) ) {
				newX += c.correctionX;
				newVx *= -0.5;
			} else {
				newY += c.correctionY;
				newVy *= -0.5;
			}
			if( Math.abs(newVy) < 0.1 ) newVx = 0;
			if( c.correctionY < 0 && Math.abs(newVy) < 0.1 ) {
				newVy = 0;
			}
		}
		
		if( newVx == 0 && newVy == 0 ) return null;
		
		return nt.withPositionAndVelocity(time, newX, newY, newVx, newVy);
	}
	
	@Override public Icon getIcon() { return icon; }
	@Override public AABB getRelativePhysicalAabb() { return aabb; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override public long getNonTileAddressFlags() { return 0; }

}
