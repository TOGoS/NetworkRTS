package togos.networkrts.experimental.game19.world.thing.jetman;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.demo.ServerClientDemo.Scene;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.Layer.VisibilityClip;
import togos.networkrts.experimental.game19.scene.QuadTreeLayerData;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.game19.scene.VisibilityChecker;
import togos.networkrts.experimental.game19.sim.AsyncTask;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTile.Icon;
import togos.networkrts.experimental.game19.world.NonTileBehavior;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.encoding.WorldConverter;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.Visitor;
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
	
	class Collision {
		final int direction;
		final double overlap;
		public Collision( int dir, double overlap ) {
			this.direction = dir;
			this.overlap = overlap;
		}
	}
	
	protected Collision findCollisionWithRst(AABB a, RSTNode rst, int rstX, int rstY, int rstSizePower, long tileFlags) {
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
			// already know there's a collision!
			// just determine specifics
			// For now assume down!
			return new Collision(2, a.maxY - rstY);
		default:
			throw new UnsupportedOperationException("Unrecognized RST node type "+rst.getNodeType());
		}
	}
	
	protected Collision findCollisionWithRst(NonTile nt, World w, long tileFlag) {
		int rad = 1<<(w.rstSizePower-1);
		return findCollisionWithRst(nt.physicalAabb, w.rst, -rad, -rad, w.rstSizePower, tileFlag);
	}
	
	@Override public NonTile update(final NonTile nt, long time, final World world,
		MessageSet messages, NonTileUpdateContext updateContext
	) {
		updateContext.startAsyncTask( new AsyncTask() {
			@Override public void run( UpdateContext ctx ) {
				int worldRadius = 1<<(world.rstSizePower-1);
				double centerX = nt.x, centerY = nt.y;
				
				int intCenterX = (int)Math.floor(centerX);
				int intCenterY = (int)Math.floor(centerY);
				
				int ldWidth = 41;
				int ldHeight = 31;
				// center of layer data
				int ldCenterX = ldWidth/2;
				int ldCenterY = ldHeight/2;
				
				// TODO: Only collect the ones actually visible
				final List<NonTile> visibleNonTiles = new ArrayList<NonTile>();
				
				world.nonTiles.forEachEntity( EntityRanges.BOUNDLESS, new Visitor<NonTile>() {
					@Override public void visit( NonTile v ) {
						visibleNonTiles.add(v);
					}
				});
				
				// There are various ways to go about this:
				// - do visibility checks, send only visible area
				// - send nearby quadtree nodes
				// - send entire world
				
				boolean sendTiles = true;
				Layer l;
				VisibilityClip visibilityClip = new Layer.VisibilityClip(intCenterX-ldCenterX, intCenterY-ldCenterY, intCenterX-ldCenterX+ldWidth, intCenterY-ldCenterY+ldHeight);
				if( sendTiles ) {
					TileLayerData layerData = new TileLayerData( ldWidth, ldHeight, 1 );
					WorldConverter.nodeToLayerData( world.rst, -worldRadius, -worldRadius, 0, 1<<world.rstSizePower, layerData, intCenterX-ldCenterX, intCenterY-ldCenterY, ldWidth, ldHeight );
					VisibilityChecker.calculateAndApplyVisibility(layerData, ldCenterX, ldCenterY, 0, 16);
					l = new Layer( layerData, intCenterX-ldCenterX, intCenterY-ldCenterY, visibilityClip, false, null, 0, 0, 0 );
				} else {
					int size = 1<<world.rstSizePower;
					l = new Layer( new QuadTreeLayerData(world.rst, size), -size/2.0, -size/2.0, null, false, null, 0, 0, 0 );
				}
				Scene scene = new Scene( l, visibleNonTiles, centerX, centerY );
				ctx.sendMessage(new Message(clientBitAddress, clientBitAddress, MessageType.INCOMING_PACKET, scene));
			}
		});
		
		double newX = nt.x, newY = nt.y;
		double newVx = nt.vx, newVy = nt.vy;
		boolean feetOnGround = false;
		
		// TODO: Collision detection!
		Collision c = findCollisionWithRst(nt, world, BitAddresses.BLOCK_SOLID);
		if( c != null ) {
			// TODO: Don't assume direction = down (2)
			newY -= c.overlap;
			newVy = 0;
			feetOnGround = true;
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
		boolean goUp = false, goDown = false;
		boolean goLeft = false, goRight = false; 
		// Change our direction!
		switch( newThrustDir ) {
		case -1: break;
		case  0:
			goRight = true;
			break;
		 case 1:
			goRight = true;
			goDown = true;
			break;
		case  2:
			goDown = true;
			break;
		case  3:
			goLeft = true;
			goDown = true;
			break;
		case  4:
			goLeft = true;
			break;
		case  5:
			goLeft = true;
			goUp = true;
			break;
		case  6:
			goUp = true;
			break;
		case  7:
			goRight = true;
			goUp = true;
			break;
		}
		if( goRight ) facingLeft = false;
		
		newVy = newVy + (goUp ? -0.001 : +0.001);
		newVx = newVx + (goLeft ? -0.002 : goRight ? +0.002 : 0); 
		
		int newWalkState = state.walkState + 1;
		if( (newWalkState>>3) >= jetManIcons.walking.length ) {
			newWalkState = 0;
		}
		
		JetManState newState = new JetManState(newWalkState, newThrustDir, facingLeft);
		
		Icon newImage = goUp ?
			(goRight ? jetManIcons.jetUpAndForward : jetManIcons.jetUp) :
			(goRight ? jetManIcons.jetForward : feetOnGround ? jetManIcons.walking[newWalkState>>3] : jetManIcons.fall0);
		
		return nt.withIcon(newImage).withPositionAndVelocity(time, newX, newY, newVx, newVy).withBehavior(withState(newState));
	}
}
