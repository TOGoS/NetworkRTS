package togos.networkrts.experimental.game19.world.thing.jetman;

import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.msg.UploadSceneTask;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class JetManHeadInternals implements NonTileInternals<BlargNonTile>
{
	public static final float MAX_HEALTH = 0.25f;
	public static final float MAX_BATTERY = 1;
	protected static final AABB aabb = new AABB(-3f/16, -2.5f/16, -3f/16, 3f/16, 2.5f/16, 3f/16);
	
	final long lastUpdateTime;
	final long uplinkBitAddress;
	final boolean facingLeft;
	final float health;
	final float battery;
	final JetManIcons icons;
	
	public JetManHeadInternals(long uplinkBitAddress, boolean facingLeft, float health, float battery, JetManIcons icons, long lastUpdateTime) {
		this.uplinkBitAddress = uplinkBitAddress;
		this.facingLeft = facingLeft;
		this.health = health;
		this.battery = battery;
		this.icons = icons;
		this.lastUpdateTime = lastUpdateTime;
	}
	
	public void sendUpdate(
		final BlargNonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext,
		JetManCoreStats stats
	) {
		updateContext.startAsyncTask(new UploadSceneTask(nt, world, uplinkBitAddress));
		updateContext.sendMessage(Message.create(uplinkBitAddress, uplinkBitAddress, MessageType.INCOMING_PACKET, stats));
	}
	
	@Override public NonTile update(
		final BlargNonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy + JetManInternals.GRAVITY;
		float newSuitHealth = health, newBattery = battery;
		
		if( newBattery >= 0.0001 ) {
			sendUpdate(nt, time, world, messages, updateContext, getStats());
			newBattery -= 0.0001; // These transmissions cost something!
		}
		
		BlockCollision c = BlockCollision.findCollisionWithRst(nt, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
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
			Block b = c.block;
			if( (b.flags & Block.FLAG_SPIKEY) == Block.FLAG_SPIKEY ) {
				collisionDamage += 50;
			}
			newSuitHealth -= collisionDamage;
		}
		
		if( newSuitHealth < 0 ) return null; // Immediate death!
		
		for( Message m : messages ) {
			if( m.isApplicableTo(nt) ) {
				//Object p = m.payload;
				// Whatever messages head can act on
				// like turn vision on/off to save battery
			}
		}
		
		return nt.withPositionAndVelocity(time, newX, newY, newVx, newVy).withInternals(
			new JetManHeadInternals(uplinkBitAddress, facingLeft, newSuitHealth, newBattery, icons, time)
		);
	}
	
	public JetManCoreStats getStats() {
		return new JetManCoreStats(
			0, 0,
			0, 0,
			MAX_HEALTH, health,
			MAX_BATTERY, battery
		);
	}

	@Override public Icon getIcon() { return facingLeft ? JetManIcons.flipped(icons.head) : icons.head; }
	@Override public AABB getRelativePhysicalAabb() { return aabb; }
	
	protected boolean isResting() {
		return false;  
	}
	
	@Override public long getNextAutoUpdateTime() {
		return isResting() ? Long.MAX_VALUE : lastUpdateTime+1;
	}
	@Override public long getNonTileAddressFlags() {
		return BitAddresses.PHYSINTERACT|BitAddresses.PICKUP|BitAddresses.UPPHASE2;
	}
	
	protected JetManHeadInternals batteryDrained(float amount) {
		return new JetManHeadInternals(uplinkBitAddress, facingLeft, health, battery-amount, icons, lastUpdateTime);
	}
	
	public JetManHeadInternals sendToClient(long myAddress, Object payload, UpdateContext ctx) {
		if( battery > 0.0001 ) {
			ctx.sendMessage(Message.create(uplinkBitAddress, MessageType.INCOMING_PACKET, myAddress, payload)); 
			return batteryDrained(0.0001f);
		}
		return this;
		// Cannot send update; battery too low!
	}
}
