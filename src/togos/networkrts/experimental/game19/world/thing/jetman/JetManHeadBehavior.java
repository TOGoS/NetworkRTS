package togos.networkrts.experimental.game19.world.thing.jetman;

import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileBehavior;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.msg.UploadSceneTask;
import togos.networkrts.util.BitAddressUtil;

public class JetManHeadBehavior implements NonTileBehavior
{
	final long messageBitAddress;
	final long uplinkBitAddress;
	final JetManHeadState state;
	final JetManIcons icons;
	
	public JetManHeadBehavior(long id, long uplinkBitAddress, JetManHeadState state, JetManIcons icons) {
		this.messageBitAddress = id;
		this.uplinkBitAddress = uplinkBitAddress;
		this.state = state;
		this.icons = icons;
	}
	
	protected JetManHeadBehavior withState(JetManHeadState ps) {
		return new JetManHeadBehavior(messageBitAddress, uplinkBitAddress, ps, icons);
	}
	
	@Override public NonTile update(final NonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy + JetManBehavior.GRAVITY;
		float newSuitHealth = state.health, newBattery = state.battery;
		
		if( newBattery >= 0.0001 ) {
			updateContext.startAsyncTask(new UploadSceneTask(nt, world, uplinkBitAddress));
			updateContext.sendMessage(Message.create(uplinkBitAddress, uplinkBitAddress, MessageType.INCOMING_PACKET, state.getStats()));
			newBattery -= 0.0001; // These transmissions cost something!
		}
		
		// TODO: Collision detection!
		BlockCollision c = BlockCollision.findCollisionWithRst(nt, world, BitAddresses.BLOCK_IWNT, Block.FLAG_SOLID);
		if( c != null ) {
			double collisionDamage;
			if( c.correctionX != 0 && Math.abs(c.correctionX) < Math.abs(c.correctionY) ) {
				collisionDamage = newVx*newVx;
				newX += c.correctionX;
				newVx *= -0.5;
			} else {
				collisionDamage = newVy*newVy;
				newY += c.correctionY;
				newVy *= -0.5;
			}
			if( Math.abs(newVy) < 0.03 ) newVx = 0;
			if( c.correctionY < 0 && Math.abs(newVy) < 0.03 ) {
				newVy = 0;
			}
			newSuitHealth -= collisionDamage;
		}
		
		if( newSuitHealth < 0 ) return null; // Immediate death!
		
		for( Message m : messages ) {
			if( m.isApplicableTo(nt) && BitAddressUtil.rangeContains(m, messageBitAddress)) {
				//Object p = m.payload;
				// Whatever messages head can act on
				// like turn vision on/off to save battery
			}
		}
		boolean facingLeft = state.facingLeft;
		
		JetManHeadState newState = new JetManHeadState(facingLeft, newSuitHealth, newBattery);
		if( newBattery != state.battery || newSuitHealth != state.health ) {
			// TODO: Some way to send status info to the client
			System.err.println(String.format("Jetman s:%4.3f f:%8.3f",newSuitHealth,newBattery));
		}
		
		Icon newIcon = icons.head;
		if( facingLeft ) newIcon = JetManIcons.flipped(newIcon);
		
		return nt.withIcon(newIcon).withPositionAndVelocity(time, newX, newY, newVx, newVy).withBehavior(withState(newState));
	}
}
