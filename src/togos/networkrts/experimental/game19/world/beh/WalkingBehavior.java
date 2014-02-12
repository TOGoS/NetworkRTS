package togos.networkrts.experimental.game19.world.beh;

import java.util.List;

import togos.networkrts.experimental.game18.sim.IDUtil;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockBehavior;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;

public class WalkingBehavior implements BlockBehavior
{
	public final long blockId;
	public final long nextStepTime;
	public final long stepInterval;
	public final int walkDirection;
	
	public final ImageHandle visibleImage;
	public final ImageHandle invisibleImage;
	
	public WalkingBehavior( long blockId, long stepInterval, long nextStepTime, int walkDir, ImageHandle visibleImage, ImageHandle invisibleImage ) {
		this.blockId = blockId;
		this.stepInterval = stepInterval;
		this.nextStepTime = nextStepTime;
		this.walkDirection = walkDir;
		this.visibleImage = visibleImage;
		this.invisibleImage = invisibleImage;
	}
	
	@Override public long getMinId() { return blockId; }
	@Override public long getMaxId() { return blockId; }
	@Override public long getNextAutoUpdateTime() { return walkDirection == -1 ? Long.MAX_VALUE : nextStepTime; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time,	Message[] messages, List<Action> results ) {
		boolean visibility = b.imageHandle == visibleImage;
		
		int newWalkDir = walkDirection;
		boolean newVisibility = visibility;
		
		for( Message m : messages ) {
			if( IDUtil.rangeContains( m.minId, m.maxId, blockId ) ) {
				if( m.type == MessageType.INCOMING_PACKET ) {
					if( m.payload instanceof Integer ) {
						int d = ((Integer)m.payload).intValue();
						if( d < 128 ) newWalkDir = d;
						else if( d == 129 ) newVisibility = true;
						else if( d == 130 ) newVisibility = false;
					} else {
						System.err.println("Unrecognized payload type "+m.payload.getClass());
					}
				}
			}
		}
		
		if( walkDirection != newWalkDir || newVisibility != visibility ) {
			b = new Block( newVisibility ? visibleImage : invisibleImage, b.flags, new WalkingBehavior(blockId, stepInterval, nextStepTime, newWalkDir, visibleImage, invisibleImage) );
		}
		
		int destX, destY;
		switch( newWalkDir ) {
		case -1: destX = x ; destY = y; break;
		case 0: destX = x+1; destY = y  ; break;
		case 2: destX = x  ; destY = y+1; break;
		case 4: destX = x-1; destY = y  ; break;
		case 6: destX = x  ; destY = y-1; break;
		default:
			System.err.println("Unrecognized walk command "+newWalkDir);
			return b;
		}
		
		final Block b0 = b;
		
		if( (destX != x || destY != y) && time >= nextStepTime ) {
			// TODO: Create and use some kind of relative move action
			final Block b1 = new Block( b0.imageHandle, b0.flags, new WalkingBehavior(blockId, stepInterval, time+stepInterval, newWalkDir, visibleImage, invisibleImage) );
			results.add( new MoveBlockAction(b0, x, y, b1, destX, destY, new FlagBasedCellSuitabilityChecker(0, Block.FLAG_SOLID)) );
		}
		
		return b0;
	}
}
