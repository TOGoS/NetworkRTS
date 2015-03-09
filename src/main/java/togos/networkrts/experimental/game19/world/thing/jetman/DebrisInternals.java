package togos.networkrts.experimental.game19.world.thing.jetman;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.AbstractPhysicalNonTileInternals;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class DebrisInternals extends AbstractPhysicalNonTileInternals
{
	protected final Icon icon;
	protected final AABB aabb;
	
	public DebrisInternals(Icon ic) {
		this.icon = ic;
		this.aabb = new AABB(-ic.imageWidth/2f, -ic.imageHeight/2f, -ic.imageWidth/2f, +ic.imageWidth/2f, +ic.imageHeight/2f, +ic.imageWidth/2f);
	}
	
	@Override public BlargNonTile update(final BlargNonTile nt0, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		BlargNonTile nt = super.update(nt0, time, world, messages, updateContext);
		return nt.vx == 0 && nt.vy == 0 ? null : nt;
	}
	
	@Override public Icon getIcon() { return icon; }
	@Override public AABB getRelativePhysicalAabb() { return aabb; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override public long getNonTileAddressFlags() { return 0; }
}
