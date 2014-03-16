package togos.networkrts.experimental.game19.world.thing.jetman;


import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTile.Icon;
import togos.networkrts.experimental.game19.world.msg.UploadSceneTask;
import togos.networkrts.experimental.game19.world.NonTileBehavior;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.util.BitAddressUtil;

public class JetManBehavior implements NonTileBehavior {
	final long messageBitAddress;
	final long clientBitAddress;
	final JetManState state;
	final JetManIcons jetManIcons;
	
	public JetManBehavior(long id, long clientId, JetManState playerState, JetManIcons jetManIcons) {
		this.messageBitAddress = id;
		this.clientBitAddress = clientId;
		this.state = playerState;
		this.jetManIcons = jetManIcons;
	}
	
	public JetManBehavior(long id, long clientId, JetManIcons jetManIcons) {
		this(id, clientId, JetManState.DEFAULT, jetManIcons);
	}
	
	protected JetManBehavior withState(JetManState ps) {
		return new JetManBehavior(messageBitAddress, clientBitAddress, ps, jetManIcons);
	}
	
	static class Collision {
		final BlockStack blockStack;
		final double correctionX, correctionY;
		
		public Collision( BlockStack bs, double correctionX, double correctionY ) {
			this.blockStack = bs;
			this.correctionX = correctionX;
			this.correctionY = correctionY;
		}
		
		protected static double jaque( double leftOverlap, double rightOverlap ) {
			if( leftOverlap < 0 && rightOverlap < 0 ) return 0;
			return leftOverlap < rightOverlap ? -leftOverlap : rightOverlap;
		}
		
		public static Collision forOverlap( BlockStack bs, AABB a, int blockX, int blockY, int blockSize ) {
			//return new Collision( bs, 0, blockY-a.maxY);
			return new Collision( bs,
				jaque( a.maxX - blockX, (blockX+blockSize) - a.minX ),
				jaque( a.maxY - blockY, (blockY+blockSize) - a.minY )
			);
		}
	}
	
	protected static Collision findCollisionWithRst(AABB a, RSTNode rst, int rstX, int rstY, int rstSizePower, long tileFlags) {
		if( a.maxX <= rstX || a.maxY <= rstY ) return null;
		int rstSize = 1<<rstSizePower;
		if( a.minX >= rstX+rstSize || a.minY >= rstY+rstSize ) return null;
		if( (rst.getMaxBitAddress() & tileFlags) == 0 ) return null; 
		
		switch( rst.getNodeType() ) {
		case QUADTREE:
			RSTNode[] subNodes = rst.getSubNodes();
			int subSizePower = rstSizePower-1;
			int subSize = 1<<subSizePower;
			Collision c;
			if( (c = findCollisionWithRst(a, subNodes[0], rstX        , rstY        , subSizePower, tileFlags)) != null ) return c;
			if( (c = findCollisionWithRst(a, subNodes[1], rstX+subSize, rstY        , subSizePower, tileFlags)) != null ) return c;
			if( (c = findCollisionWithRst(a, subNodes[2], rstX        , rstY+subSize, subSizePower, tileFlags)) != null ) return c;
			if( (c = findCollisionWithRst(a, subNodes[3], rstX+subSize, rstY+subSize, subSizePower, tileFlags)) != null ) return c;
			return null;
		case BLOCKSTACK:
			return Collision.forOverlap(rst, a, rstX, rstY, rstSize);
		default:
			throw new UnsupportedOperationException("Unrecognized RST node type "+rst.getNodeType());
		}
	}
	
	protected static Collision findCollisionWithRst(NonTile nt, World w, long tileFlag) {
		int rad = 1<<(w.rstSizePower-1);
		return findCollisionWithRst(nt.physicalAabb, w.rst, -rad, -rad, w.rstSizePower, tileFlag);
	}
	
	@Override public NonTile update(final NonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		updateContext.startAsyncTask(new UploadSceneTask(nt, world, clientBitAddress));
		
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy;
		boolean feetOnGround = false;
		double newSuitHealth = state.suitHealth, newFuel = state.fuel;
		
		// TODO: Collision detection!
		Collision c = findCollisionWithRst(nt, world, BitAddresses.BLOCK_SOLID);
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
				if( c.correctionY < 0 && Math.abs(newVy) < 0.1 ) {
					newVy = 0;
					feetOnGround = true;
				}
			}
			newSuitHealth -= collisionDamage;
		}
		
		// TODO: make arms and legs go flying!
		
		if( newSuitHealth < 0 ) return null; // Immediate death!
		
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
			newVy += 0.002;
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
		if( (newWalkState>>2) >= jetManIcons.walking.length ) {
			newWalkState = 0;
		}
		
		JetManState newState = new JetManState(newWalkState, newThrustDir, facingLeft, newSuitHealth, newFuel);
		if( newFuel != state.fuel || newSuitHealth != state.suitHealth ) {
			// TODO: Some way to send status info to the client
			//System.err.println(String.format("Jetman s:%4.3f f:%8.3f",newSuitHealth,newFuel));
		}
		
		Icon newIcon =
			jetUp && jetForward ? jetManIcons.jetUpAndForward :
			jetUp               ? jetManIcons.jetUp :
			feetOnGround        ? jetManIcons.walking[newWalkState>>2] :
			jetForward          ? jetManIcons.jetForward :
			jetManIcons.fall0;
		if( facingLeft ) newIcon = JetManIcons.flipped(newIcon);
		
		return nt.withIcon(newIcon).withPositionAndVelocity(time, newX, newY, newVx, newVy).withBehavior(withState(newState));
	}
}
