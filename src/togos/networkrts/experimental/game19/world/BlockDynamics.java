package togos.networkrts.experimental.game19.world;

public class BlockDynamics
{
	public static final BlockDynamics NONE = new BlockDynamics(0,0,0,0,0,0,0,0,0,0);
	
	// X, Y offset within the cell 
	public final float subCellX, subCellY;
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
	
	public BlockDynamics(
		float scx, float scy, float vx, float vy,
		float drx, float dry, float mda, float mcv,
		float jx, float jy
	) {
		this.subCellX = scx; this.subCellY = scy;
		this.velX = vx; this.velY = vy;
		this.driveX = drx; this.driveY = dry;
		this.maxDriveAcceleration = mda;
		this.maxClimbVel = mcv;
		this.jumpX = jx;
		this.jumpY = jy;
	}
}
