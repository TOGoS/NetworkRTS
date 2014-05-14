package togos.networkrts.experimental.game19.world.thing.bomb;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.AbstractPhysicalNonTileInternals;
import togos.networkrts.experimental.game19.world.thing.BlockWand;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class OMGBombInternals extends AbstractPhysicalNonTileInternals
{
	protected final Icon icon;
	protected final AABB aabb;
	
	public OMGBombInternals(Icon ic) {
		this.icon = ic;
		this.aabb = new AABB(-ic.imageWidth/2f, -ic.imageHeight/2f, -ic.imageWidth/2f, +ic.imageWidth/2f, +ic.imageHeight/2f, +ic.imageWidth/2f);
	}
	
	@Override public BlargNonTile update(final BlargNonTile nt0, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		final PhysicsResult pr = super.updatePhysics(nt0, time, world);
		if( pr.getCollisionSpeed() > 0 ) {
			BlockWand.apply(new BlockWand.Application(
				pr.nt.x, pr.nt.y, 3, false, BlockStackRSTNode.EMPTY
			), updateContext);
			return null;
		} else {
			return pr.nt;
		}
	}
	
	@Override public Icon getIcon() { return icon; }
	@Override public AABB getRelativePhysicalAabb() { return aabb; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override public long getNonTileAddressFlags() { return 0; }
}
