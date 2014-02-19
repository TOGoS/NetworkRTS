package togos.networkrts.experimental.game19.world.beh;

import java.util.List;
import java.util.Random;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockBehavior;
import togos.networkrts.experimental.game19.world.BlockDynamics;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.util.BitAddressUtil;

public class RandomWalkBehavior implements BlockBehavior
{
	public final int stepInterval;
	public final long nextStepTime;
	
	public RandomWalkBehavior( int stepInterval, long nextStepTime ) {
		this.stepInterval = stepInterval;
		this.nextStepTime = nextStepTime;
	}
	
	@Override public long getMinBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
	@Override public long getMaxBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
	@Override public long getNextAutoUpdateTime() { return nextStepTime; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time,	Message[] messages, List<Action> results ) {
		if( time < nextStepTime ) return b;
		
		// TODO: replace with some reproducable pseudo-random
		Random r = new Random();
		
		int destX, destY;
		switch( r.nextInt(4) ) {
		case 0: destX = x+1; destY = y  ; break;
		case 1: destX = x  ; destY = y+1; break;
		case 2: destX = x-1; destY = y  ; break;
		case 3: destX = x  ; destY = y-1; break;
		default: throw new RuntimeException("Unpossible!");
		}
		
		Block newBlock = new Block( b.bitAddress, b.imageHandle, new RandomWalkBehavior(stepInterval, time+stepInterval), BlockDynamics.NONE );
		
		results.add( new MoveBlockAction(b, x, y, newBlock, destX, destY, new FlagBasedCellSuitabilityChecker(0, BitAddresses.BLOCK_SOLID) ) );
		return b;
	}
}
