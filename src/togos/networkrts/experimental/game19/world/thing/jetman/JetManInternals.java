package togos.networkrts.experimental.game19.world.thing.jetman;

import java.util.Random;

import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.Substances;
import togos.networkrts.experimental.game19.world.thing.pickup.SubstanceContainerInternals;
import togos.networkrts.experimental.game19.world.thing.pickup.SubstanceContainerType;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.Visitor;
import togos.networkrts.util.BitAddressUtil;

public class JetManInternals implements NonTileInternals<BlargNonTile>
{
	protected static final SubstanceContainerInternals DEFAULT_FUEL_TANK = SubstanceContainerInternals.filled(
		new SubstanceContainerType("Jetman fuel tank", new Icon[]{}, null, 0, 0.2), Substances.KEROSENE
	);
	
	protected static final AABB aabb = new AABB(-3f/16, -7f/16, -3f/16, 3f/16, 8f/16, 3f/16);
	public static final double GRAVITY = 0.002;
	
	public static final int S_FACING_LEFT = 0x01;
	public static final int S_BACK_THRUSTER_ON = 0x02;
	public static final int S_BOTTOM_THRUSTER_ON = 0x04;
	public static final int S_FEET_ON_GROUND = 0x8;
	
	final int walkFrame;
	final int thrustDir;
	final int stateFlags;
	final float suitHealth;
	final SubstanceContainerInternals fuelTank;
	final JetManHeadInternals headInternals;
	final JetManIcons icons;
	
	public JetManInternals(
		int walkFrame, int thrustDir, int state,
		float suitHealth,
		SubstanceContainerInternals fuelTank,
		JetManHeadInternals headInternals,
		JetManIcons icons
	) {
		this.walkFrame = walkFrame;
		this.thrustDir = thrustDir;
		this.stateFlags = state;
		this.suitHealth = suitHealth;
		this.fuelTank = fuelTank;
		this.headInternals = headInternals;
		this.icons = icons;
	}
	
	public boolean checkStateFlag(int flag) {
		return (stateFlags & flag) == flag;
	}
	
	public JetManCoreStats getStats() {
		return new JetManCoreStats(
			1  , suitHealth,
			(float)fuelTank.type.getCapacity(fuelTank.contents.substance), (float)fuelTank.contents.quantity,
			JetManHeadInternals.MAX_HEALTH, headInternals.health,
			JetManHeadInternals.MAX_BATTERY, headInternals.battery
		);
	}
	public JetManInternals(long clientId, JetManIcons icons) {
		this(0, -1, 0, 1, DEFAULT_FUEL_TANK, new JetManHeadInternals(clientId, false, JetManHeadInternals.MAX_HEALTH, JetManHeadInternals.MAX_BATTERY, icons), icons);
	}
	
	public static NonTile createJetMan( long id, long uplinkBitAddress, JetManIcons icons ) {
		return new BlargNonTile(id, 0, 0, 0, 0, 0, new JetManInternals(uplinkBitAddress, icons));
	}
	
	@Override public NonTile update(
		final BlargNonTile nt, long time, final World world,
		MessageSet messages, final NonTileUpdateContext updateContext
	) {
		headInternals.sendUpdate(nt, time, world, messages, updateContext, getStats());
		
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy;
		boolean feetOnGround = false;
		float newSuitHealth = suitHealth;
		SubstanceContainerInternals newFuelTank = fuelTank;
		
		BlockCollision c = BlockCollision.findCollisionWithRst(nt, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		if( c != null ) {
			double collisionDamage;
			if( c.correctionX != 0 && Math.abs(c.correctionX) < Math.abs(c.correctionY) ) {
				collisionDamage = newVx*newVx;
				newX += c.correctionX;
				newVx *= -0.5;
			} else {
				newY += c.correctionY;
				newVy *= -0.5;
				if( c.correctionY < 0 && Math.abs(newVy) < 0.1 ) {
					newVy = 0;
					feetOnGround = true;
					collisionDamage = 0;
				} else {
					collisionDamage = newVy*newVy;
				}
			}

			Block b = c.block;
			if( (b.flags & Block.FLAG_SPIKEY) == Block.FLAG_SPIKEY ) {
				// TODO: Unless he has steel boots!
				collisionDamage += 50;
			}
			newSuitHealth -= collisionDamage;
		}
		
		world.nonTiles.forEachEntity(EntityRanges.create(nt.getAabb(), BitAddresses.PHYSINTERACT, BitAddressUtil.MAX_ADDRESS), new Visitor<NonTile>() {
			@Override public void visit(NonTile v) {
				if( v == nt ) {
					// It's myself!
				} else 	if( (v.getBitAddress() & BitAddresses.PICKUP) == BitAddresses.PICKUP ) {
					System.err.println("Found a pickup: "+v);
					// TODO: And we have room for it; otherwise don't bother
					updateContext.sendMessage(Message.create(v, MessageType.REQUEST_PICKUP, nt.getBitAddress(), null));
				}
			}
		});
		
		JetManHeadInternals newHeadInternals = headInternals;
		
		if( newSuitHealth < 0 ) {
			Random rand = new Random();
			Icon[] pieceIcons = new Icon[] { icons.leg1, icons.leg2, icons.torso, icons.jetpack };
			for( int j=0; j<4; ++j ) {
				Icon ic = pieceIcons[j];
				updateContext.addNonTile(new BlargNonTile(0, time, newX, newY,
					newVx+newVx*rand.nextGaussian()+newVy*rand.nextGaussian(),
					newVy+newVy*rand.nextGaussian()+newVx*rand.nextGaussian(),
					new JetManPieceBehavior(ic)
				));
			}
			
			// TODO: Some amount of remaining damage goes to head
			
			return new BlargNonTile(nt.id, time, newX, newY, newVx, newVy, newHeadInternals);
		}
		
		int newThrustDir = thrustDir;
		for( Message m : messages ) {
			if( m.isApplicableTo(nt) ) {
				switch( m.type ) {
				case INCOMING_PACKET:
					Object p = m.payload;
					if( p instanceof Number ) {
						int _wd = ((Number)p).intValue();
						if( _wd >= -1 && _wd <= 7 ) {
							newThrustDir = _wd;
						}
					}
					break;
				case INCOMING_ITEM:
					Object item = m.payload;
					if( item instanceof SubstanceContainerInternals ) {
						SubstanceContainerInternals sci = (SubstanceContainerInternals)item;
						if( sci.contents.substance.equals(newFuelTank.contents.substance) ) {
							// Yay fuel!
							// TODO: Only fill tank, leaving remaining
							// TODO: Send happy chat messages back to client
							double cap = newFuelTank.getCapacity();
							double delta = Math.min(cap - newFuelTank.contents.quantity, sci.contents.quantity);
							System.err.println("Got "+delta+sci.contents.substance.unitOfMeasure.abbreviation+" of fuel, woohoo");
							newFuelTank = newFuelTank.add( delta );
						}
					} else {
						System.err.println("Don't know what to do with "+item);
					}
					break;
				}
			}
		}
		boolean facingLeft = checkStateFlag(S_FACING_LEFT);
		boolean goUp = false, goForward =  false;
		// Change our direction!
		switch( newThrustDir ) {
		case  0:
			facingLeft = false;
			goForward = true;
			break;
		case 1:
			facingLeft = false;
			goForward = true;
			break;
		case  2:
			break;
		case  3:
			facingLeft = true;
			goForward = true;
			break;
		case  4:
			facingLeft = true;
			goForward = true;
			break;
		case  5:
			facingLeft = true;
			goForward = true;
			goUp = true;
			break;
		case  6:
			goUp = true;
			break;
		case  7:
			facingLeft = false;
			goForward = true;
			goUp = true;
			break;
		default:
		}
		
		boolean jetForward = false, jetUp = false;
		if( goUp && newFuelTank.contents.quantity >= 0.01 ) {
			newFuelTank = newFuelTank.add(-0.01);
			newVy -= 0.002;
			jetUp = true;
		} else {
			newVy += GRAVITY;
		}
		if( feetOnGround ) {
			double walkSpeed = 0.05;
			if( goForward && Math.abs(newVx) <= walkSpeed ) {
				newVx = facingLeft ? -walkSpeed : walkSpeed; 
			} else if( !goForward && Math.abs(newVx) <= walkSpeed ) {
				newVx = 0;
			} else {
				newVx *= 0.6;
			}
		} else {
			if( goForward && newFuelTank.contents.quantity >= 0.01 ) {
				newFuelTank = newFuelTank.add(-0.01);
				newVx += 0.003 * (facingLeft ? -1 : 1);
				jetForward = true;
			}
		}
		
		int newWalkState = walkFrame + ((feetOnGround && newVx != 0) ? 1 : 0);
		if( (newWalkState>>2) >= icons.walking.length ) {
			newWalkState = 0;
		}
		
		int stateFlags =
			(facingLeft   ? S_FACING_LEFT        : 0) |
			(jetUp        ? S_BOTTOM_THRUSTER_ON : 0) |
			(jetForward   ? S_BACK_THRUSTER_ON   : 0) |
			(feetOnGround ? S_FEET_ON_GROUND     : 0);
		
		return nt.withPositionAndVelocity(time, newX, newY, newVx, newVy).withInternals(
			new JetManInternals(newWalkState, newThrustDir, stateFlags, newSuitHealth, newFuelTank, newHeadInternals, icons)
		);
	}
	
	@Override public Icon getIcon() {
		boolean jetUp        = checkStateFlag(S_BOTTOM_THRUSTER_ON);
		boolean jetForward   = checkStateFlag(S_BACK_THRUSTER_ON);
		boolean feetOnGround = checkStateFlag(S_FEET_ON_GROUND);
		boolean facingLeft   = checkStateFlag(S_FACING_LEFT);
		
		Icon icon =
			jetUp && jetForward ? icons.jetUpAndForward :
			jetUp               ? icons.jetUp :
			feetOnGround        ? icons.walking[walkFrame>>2] :
			jetForward          ? icons.jetForward :
			icons.fall0;
		if( facingLeft ) icon = JetManIcons.flipped(icon);
		return icon;
	}
	
	@Override public AABB getRelativePhysicalAabb() { return aabb; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override public long getNonTileAddressFlags() {
		return BitAddresses.PHYSINTERACT;
	}
}
