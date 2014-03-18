package togos.networkrts.experimental.game19.world.thing.jetman;


import java.util.Random;

import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.physics.BlockStackCollision;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTile.Icon;
import togos.networkrts.experimental.game19.world.msg.UploadSceneTask;
import togos.networkrts.experimental.game19.world.NonTileBehavior;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.util.BitAddressUtil;

public class JetManBehavior implements NonTileBehavior {
	public static final double GRAVITY = 0.002;
	
	final long messageBitAddress;
	final long uplinkBitAddress;
	final JetManState state;
	final JetManIcons icons;
	
	public JetManBehavior(long id, long uplinkBitAddress, JetManState state, JetManIcons icons) {
		this.messageBitAddress = id;
		this.uplinkBitAddress = uplinkBitAddress;
		this.state = state;
		this.icons = icons;
	}
	
	public JetManBehavior(long id, long clientId, JetManIcons icons) {
		this(id, clientId, JetManState.DEFAULT, icons);
	}
	
	protected JetManBehavior withState(JetManState ps) {
		return new JetManBehavior(messageBitAddress, uplinkBitAddress, ps, icons);
	}
	
	public static NonTile createJetMan( long bitAddress, long uplinkBitAddress, JetManIcons icons ) {
		return new NonTile(0, 0, 0, 0, 0,
			new AABB(-3/16f, -7/16f, -3/16f, 3/16f, 8/16f, 3/16f),
			bitAddress, bitAddress, 1,
			icons.walking[0], 
			new JetManBehavior(bitAddress, uplinkBitAddress, icons)
		);
	}
	
	@Override public NonTile update(final NonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		updateContext.startAsyncTask(new UploadSceneTask(nt, world, uplinkBitAddress));
		
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy;
		boolean feetOnGround = false;
		double newSuitHealth = state.suitHealth, newFuel = state.fuel;
		
		// TODO: Collision detection!
		BlockStackCollision c = BlockStackCollision.findCollisionWithRst(nt, world, BitAddresses.BLOCK_SOLID);
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
			for( Block b : c.blockStack.getBlocks() ) {
				if( (b.bitAddress & BitAddresses.BLOCK_SHARP) == BitAddresses.BLOCK_SHARP ) {
					// TODO: Unless he has steel boots!
					collisionDamage += 50;
				}
			}
			newSuitHealth -= collisionDamage;
		}
		
		if( newSuitHealth < 0 ) {
			Random rand = new Random();
			Icon[] pieceIcons = new Icon[] { icons.leg1, icons.leg2, icons.torso, icons.jetpack };
			for( int j=0; j<4; ++j ) {
				Icon ic = pieceIcons[j];
				updateContext.addNonTile(new NonTile(time, newX, newY,
					newVx+newVx*rand.nextGaussian()+newVy*rand.nextGaussian(), newVy+newVy*rand.nextGaussian()+newVx*rand.nextGaussian(),
					new AABB(-ic.imageWidth/2f, -ic.imageHeight/2f, -ic.imageWidth/2f, +ic.imageWidth/2f, +ic.imageHeight/2f, +ic.imageWidth/2f),
					0, 0, time+1, ic,
					new JetManPieceBehavior()
				));
			}
			
			return new NonTile(time, newX, newY, newVx, newVy,
				new AABB(-3f/16, -2.5f/16, -3f/16, 3f/16, 2.5f/16, 3f/16),
				messageBitAddress, messageBitAddress, time+1, icons.head,
				new JetManHeadBehavior(messageBitAddress, uplinkBitAddress, new JetManHeadState(state.facingLeft, newSuitHealth+0.25, 1), icons)
			);
		}
		
		int newThrustDir = state.thrustDir;
		for( Message m : messages ) {
			if( m.isApplicableTo(nt) && BitAddressUtil.rangeContains(m, messageBitAddress)) {
				Object p = m.payload;
				if( p instanceof Number ) {
					int _wd = ((Number)p).intValue();
					if( _wd >= -1 && _wd <= 7 ) {
						newThrustDir = _wd;
					}
				}
			}
		}
		boolean facingLeft = state.facingLeft;
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
		if( goUp && newFuel >= 0.01 ) {
			newFuel -= 0.01;
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
			if( goForward && newFuel >= 0.01 ) {
				newFuel -= 0.01;
				newVx += 0.003 * (facingLeft ? -1 : 1);
				jetForward = true;
			}
		}
		
		int newWalkState = state.walkState + ((feetOnGround && newVx != 0) ? 1 : 0);
		if( (newWalkState>>2) >= icons.walking.length ) {
			newWalkState = 0;
		}
		
		JetManState newState = new JetManState(newWalkState, newThrustDir, facingLeft, newSuitHealth, newFuel);
		if( newFuel != state.fuel || newSuitHealth != state.suitHealth ) {
			// TODO: Some way to send status info to the client
			//System.err.println(String.format("Jetman s:%4.3f f:%8.3f",newSuitHealth,newFuel));
		}
		
		Icon newIcon =
			jetUp && jetForward ? icons.jetUpAndForward :
			jetUp               ? icons.jetUp :
			feetOnGround        ? icons.walking[newWalkState>>2] :
			jetForward          ? icons.jetForward :
			icons.fall0;
		if( facingLeft ) newIcon = JetManIcons.flipped(newIcon);
		
		return nt.withIcon(newIcon).withPositionAndVelocity(time, newX, newY, newVx, newVy).withBehavior(withState(newState));
	}
}
