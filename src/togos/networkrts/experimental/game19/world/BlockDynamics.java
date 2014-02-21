package togos.networkrts.experimental.game19.world;

public class BlockDynamics implements HasAutoUpdateTime
{
	public static final BlockDynamics NONE = new BlockDynamics(0,0,0,0,0,0,0,0,0,0,0);
	
	/** Time at which the following values are accurate */
	public final long referenceTime;
	public final long nextAutoUpdateTime;
	
	// X, Y offset within the cell 
	public final float posX, posY;
	// Current actual velocity
	public final float velX, velY;
	
	// Target velocity X and Y to attain by driving or climbing
	public final float driveX, driveY;
	// Maximum acceleration on frictive surfaces (vel / tick)
	// May be cancelled out by other forces such as gravity
	public final float maxDriveAcceleration;
	// Maximum velocity via climbing
	public final float maxClimbVel;
	// Target velocity instantly attained if launching from a hard surface
	public final float jumpX, jumpY;
	
	protected static long boundaryTraversalTime( long refTime, float pos, float vel ) {
		if( vel > 0 ) {
			return refTime + (long)Math.ceil((1 - pos) / vel);
		} else if( vel < 0 ) {
			return refTime + (long)Math.ceil(pos / (-vel));
		} else {
			return Long.MAX_VALUE;
		}
	}
	
	protected long calcNextAutoUpdateTime() {
		if( driveX != velX || driveY != velY || jumpX != velX || jumpY != velY ) {
			return referenceTime+1;
		}
		// Otherwise speed is constant, so calculate when the thing would leave the current cell
		return Math.min( Long.MAX_VALUE, Math.min(
			boundaryTraversalTime(referenceTime, posX, velX),
			boundaryTraversalTime(referenceTime, posY, velY)
		));
	}
	
	public BlockDynamics(
		long referenceTime,
		float scx, float scy, float vx, float vy,
		float drx, float dry, float mda, float mcv,
		float jx, float jy
	) {
		this.referenceTime = referenceTime;
		this.posX = scx; this.posY = scy;
		this.velX = vx; this.velY = vy;
		this.driveX = drx; this.driveY = dry;
		this.maxDriveAcceleration = mda;
		this.maxClimbVel = mcv;
		this.jumpX = jx;
		this.jumpY = jy;
		this.nextAutoUpdateTime = calcNextAutoUpdateTime();
	}
	
	@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }
	
	protected static float sign( float v ) {
		return v < 0 ? -1 : 1;
	}
	
	protected static float adjustVelocity( float v0, float target, float maxAccel ) {
		float delta = target - v0;
		if( Math.abs(delta) < maxAccel ) return target;
		return v0 + sign(delta) * maxAccel; 
	}
	
	protected static boolean canAccellerateTo( float v0, float target ) {
		if( sign(v0) != sign(target) ) return true;
		if( Math.abs(target) > Math.abs(v0) ) return true;
		return false;
	}
	
	protected static float clampSigned( float v0, float max ) {
		return v0 < -max ? -max : v0 > max ? max : v0;
	}
	
	protected static boolean gtAbs( float v0, float v1 ) {
		return Math.abs(v1) > Math.abs(v0);
	}
	
	public BlockDynamics update( long time, float traction, boolean onClimbable, float jumpability ) {
		float dt = (float)(time-referenceTime);
		
		float posX = this.posX, posY = this.posY;
		float velX = this.velX, velY = this.velY;
		
		velX = adjustVelocity( velX, driveX, maxDriveAcceleration*traction*dt );
		velY = adjustVelocity( velY, driveY, maxDriveAcceleration*traction*dt );
		
		/*
		if( onClimbable && maxClimbVel > 0 ) {
			if( canAccellerateTo(velX, clampSigned(driveX, maxClimbVel)) ) {
				velX += clampSigned(driveX, maxClimbVel);
			}
			if( canAccellerateTo(velY, clampSigned(driveY, maxClimbVel)) ) {
				velY += clampSigned(driveY, maxClimbVel);
			}
		}
		*/
		
		//if( gtAbs(jumpX,velX) ) velX += (jumpX-velX)*jumpability;
		//if( gtAbs(jumpY,velY) ) velY += (jumpY-velY)*jumpability;
		
		posX += velX;
		posY += velY;
		
		return new BlockDynamics(
			time, posX, posY, velX, velY, driveX, driveY, maxDriveAcceleration, maxClimbVel, jumpX, jumpY
		);
	}
	
	public BlockDynamics withDriveJump( float driveX, float driveY, float jumpX, float jumpY ) {
		return new BlockDynamics(
			referenceTime, posX, posY, velX, velY, driveX, driveY, maxDriveAcceleration, maxClimbVel, jumpX, jumpY
		);
	}
	
	protected static float repositioned( float v ) {
		while( v < -0.5f ) ++v;
		while( v > +0.5f ) --v;
		return v;
	}
	
	public BlockDynamics repositioned() {
		return new BlockDynamics(
			referenceTime, repositioned(posX), repositioned(velY), velX, velY, driveX, driveY, maxDriveAcceleration, maxClimbVel, jumpX, jumpY
		);
	}
}
