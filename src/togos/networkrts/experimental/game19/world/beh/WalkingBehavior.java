package togos.networkrts.experimental.game19.world.beh;

import java.util.Collection;
import java.util.List;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockBehavior;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.util.BitAddressUtil;

public class WalkingBehavior implements BlockBehavior
{
	public final long nextStepTime;
	public final long stepInterval;
	public final int walkDirection;
	
	public WalkingBehavior( long stepInterval, long nextStepTime, int walkDir ) {
		this.stepInterval = stepInterval;
		this.nextStepTime = nextStepTime;
		this.walkDirection = walkDir;
	}
	
	@Override public long getMinBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
	@Override public long getMaxBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
	@Override public long getNextAutoUpdateTime() { return walkDirection == -1 ? Long.MAX_VALUE : nextStepTime; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time,	Collection<Message> messages, List<Action> results ) {
		int newWalkDir = walkDirection;
		
		for( Message m : messages ) {
			System.err.println("Got your message lol");
			
			if( BitAddressUtil.rangeContains( m, b.bitAddress ) ) {
				System.err.println("U tellin me to walk lol");
				if( m.type == MessageType.INCOMING_PACKET ) {
					if( m.payload instanceof Integer ) {
						int d = ((Integer)m.payload).intValue();
						newWalkDir = d;
					} else {
						System.err.println("Unrecognized payload type "+m.payload.getClass());
					}
				}
			}
		}
		
		if( walkDirection != newWalkDir ) {
			b = b.withBehavior( new WalkingBehavior(stepInterval, nextStepTime, newWalkDir) );
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
			final Block b1 = new Block( b0.bitAddress, b0.imageHandle, new WalkingBehavior(stepInterval, time+stepInterval, newWalkDir) );
			results.add( new MoveBlockAction(b0, x, y, b1, destX, destY, new FlagBasedCellSuitabilityChecker(0, BitAddresses.BLOCK_SOLID)) );
		}
		
		return b0;
	}
}
