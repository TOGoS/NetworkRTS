package togos.networkrts.experimental.game19.world.thing.jetman;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.msg.UploadSceneTask;
import togos.networkrts.experimental.game19.world.thing.AbstractPhysicalNonTileInternals;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class JetManHeadInternals extends AbstractPhysicalNonTileInternals
{
	public static final float MAX_HEALTH = 0.25f;
	public static final float MAX_BATTERY = 1;
	protected static final AABB aabb = new AABB(-3f/16, -2.5f/16, -3f/16, 3f/16, 2.5f/16, 3f/16);
	
	final long lastUpdateTime;
	final long lastViewSendTime;
	final long uplinkBitAddress;
	final boolean facingLeft;
	final float health;
	final float battery;
	final JetManIcons icons;
	
	public JetManHeadInternals(long uplinkBitAddress, boolean facingLeft, float health, float battery, JetManIcons icons, long lastUpdateTime, long lastViewSendTime) {
		this.uplinkBitAddress = uplinkBitAddress;
		this.facingLeft = facingLeft;
		this.health = health;
		this.battery = battery;
		this.icons = icons;
		this.lastUpdateTime = lastUpdateTime;
		this.lastViewSendTime = lastViewSendTime;
	}
	
	protected JetManHeadInternals withHealth( float h ) {
		return h == health ? this : new JetManHeadInternals(uplinkBitAddress, facingLeft, h, battery, icons, lastUpdateTime, lastViewSendTime);
	}
	
	protected JetManHeadInternals miniUpdate(
		final BlargNonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext,
		JetManCoreStats stats, boolean externallyPowered
	) {
		float newBattery = battery;
		boolean viewSendAttempted = false;
		
		if( time == lastUpdateTime ) {
			double viewUpdateWork = externallyPowered ? 0 : 0.0001;
			if( newBattery >= viewUpdateWork ) {
				updateContext.startAsyncTask(new UploadSceneTask(nt, world, uplinkBitAddress));
				updateContext.sendMessage(Message.create(uplinkBitAddress, uplinkBitAddress, MessageType.INCOMING_PACKET, stats));
				newBattery -= viewUpdateWork; // These transmissions cost something!
			}
			viewSendAttempted = true;
		} else if( lastViewSendTime < time ) {
			updateContext.sendMessage(Message.create(nt.baseBitAddress, MessageType.UPDATE, null));
		}
		
		for( Message m : messages ) {
			if( m.isApplicableTo(nt) ) {
				//Object p = m.payload;
				// Whatever messages head can act on
				// like turn vision on/off to save battery
			}
		}
		
		return new JetManHeadInternals(uplinkBitAddress, facingLeft, health, newBattery, icons, time, viewSendAttempted ? time : lastViewSendTime);
	}
	
	@Override public BlargNonTile update(
		final BlargNonTile nt0, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		final PhysicsResult pr = super.updatePhysics(nt0, time, world);
		
		double damageFactor = 0.001;
		float newHealth = health - (float)((pr.collisionVy*pr.collisionVy+pr.collisionVx*pr.collisionVx)*damageFactor);
		if( newHealth < 0 ) return null;
		
		return pr.nt.withInternals(
			withHealth(newHealth).miniUpdate(pr.nt, time, world, messages, updateContext, getStats(), false)
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
		return BitAddresses.PHYSINTERACT|BitAddresses.PICKUP;
	}
	
	protected JetManHeadInternals batteryDrained(float amount) {
		return new JetManHeadInternals(uplinkBitAddress, facingLeft, health, battery-amount, icons, lastUpdateTime, lastViewSendTime);
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
