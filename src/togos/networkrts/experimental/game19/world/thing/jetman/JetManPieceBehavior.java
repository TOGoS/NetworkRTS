package togos.networkrts.experimental.game19.world.thing.jetman;

import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileBehavior;
import togos.networkrts.experimental.game19.world.World;

public class JetManPieceBehavior implements NonTileBehavior<BlargNonTile>
{
	public JetManPieceBehavior() { }
	
	@Override public NonTile update(final BlargNonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy + JetManBehavior.GRAVITY;
		
		BlockCollision c = BlockCollision.findCollisionWithRst(nt, world, BitAddresses.BLOCK_IWNT, Block.FLAG_SOLID);
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
}
