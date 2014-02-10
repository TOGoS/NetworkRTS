package togos.networkrts.experimental.game19.world.beh;

import java.util.List;

import togos.networkrts.experimental.game18.sim.IDUtil;
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
	
	public WalkingBehavior( long blockId, long stepInterval, long nextStepTime, int walkDir ) {
		this.blockId = blockId;
		this.stepInterval = stepInterval;
		this.nextStepTime = nextStepTime;
		this.walkDirection = walkDir;
	}
	
	@Override public long getMinId() { return blockId; }
	@Override public long getMaxId() { return blockId; }
	@Override public long getNextAutoUpdateTime() { return walkDirection == -1 ? Long.MAX_VALUE : nextStepTime; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time,	Message[] messages, List<Action> results ) {
		int newWalkDir = walkDirection;
		
		for( Message m : messages ) {
			if( IDUtil.rangeContains( m.minId, m.maxId, blockId ) ) {
				if( m.type == MessageType.INCOMING_PACKET ) {
					if( m.payload instanceof Integer ) {
						newWalkDir = ((Integer)m.payload).intValue();
					} else {
						System.err.println("Unrecognized payload type "+m.payload.getClass());
					}
				}
			}
		}
		
		if( time < nextStepTime ) {
			if( walkDirection == newWalkDir ) return b;
			
			return b.withBehavior( new WalkingBehavior(blockId, stepInterval, nextStepTime, newWalkDir) );
		}
		
		int destX, destY;
		switch( newWalkDir ) {
		case -1: destX = x; destY = y; break;
		case 0: destX = x+1; destY = y  ; break;
		case 2: destX = x  ; destY = y+1; break;
		case 4: destX = x-1; destY = y  ; break;
		case 6: destX = x  ; destY = y-1; break;
		default: throw new RuntimeException("Unpossible!");
		}
		
		Block newBlock = new Block( b.imageHandle, b.flags, new WalkingBehavior(blockId, stepInterval, nextStepTime+stepInterval, newWalkDir) );
		if( destX != x || destY != y ) {
			results.add( new MoveBlockAction(newBlock, x, y, newBlock, destX, destY, new FlagBasedCellSuitabilityChecker(0, Block.FLAG_SOLID)) );
		}
		return newBlock;
	}
}
