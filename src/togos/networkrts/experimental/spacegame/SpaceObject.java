package togos.networkrts.experimental.spacegame;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class SpaceObject {
	static class SpaceObjectPosition {
		public double x, y, angle;
	}
	interface SpaceObjectPositionFunction {
		public void getPosition( long time, SpaceObjectPosition dest );
	}
	interface SpaceObjectBehavior {
		public void receivedSignal( long time, SpaceObject self, int code, byte[] data, int off, int len, List<SpaceObject> replacementDest, List<SpaceWorld.SpaceWorldEvent>receivedSignal );
	}
	static class NoBehavior implements SpaceObjectBehavior {
		public static NoBehavior INSTANCE = new NoBehavior();
		@Override public void receivedSignal( long time, SpaceObject self, int code, byte[] data, int off, int len, List<SpaceObject> replacementDest, List<SpaceWorld.SpaceWorldEvent>receivedSignal ) {
			replacementDest.add(self);
		}
	}
	static class AccellerativePositionFunction implements SpaceObjectPositionFunction {
		final long referenceTime;
		final double x, dx, ddx;
		final double y, dy, ddy;
		final double a, da, dda;
		
		public AccellerativePositionFunction(
			long referenceTime,
			double x, double dx, double ddx,
			double y, double dy, double ddy,
			double a, double da, double dda
		) {
			this.referenceTime = referenceTime;
			this.x = x; this.dx = dx; this.ddx = ddx;
			this.y = y; this.dy = dy; this.ddy = ddy;
			this.a = a; this.da = da; this.dda = dda;
		}
		
		public AccellerativePositionFunction( double x, double y, double a ) {
			this( 0,x, 0, 0, y, 0, 0, a, 0, 0 );
		}
		
		@Override public void getPosition(long time, SpaceObjectPosition dest) {
			double dt = (time - referenceTime)/(1000.0);
			dest.x     = x + dx*dt + ddx*dt*dt/2;
			dest.y     = y + dy*dt + ddy*dt*dt/2;
			dest.angle = a + da*dt + dda*dt*dt/2;
		}
	}
	
		
	protected static final SpaceObject[] EMPTY_OBJECT_LIST = new SpaceObject[0]; 
	
	public final long entityId;
	public final SpaceObjectPositionFunction position;
	public final SpaceObject[] subObjects;
	public final Color solidColor;
	public final double solidRadius;
	public final double systemRadius;
	public final long autoUpdateTime;
	public final Map<String,Object> properties;
	public final SpaceObjectBehavior behavior;
	
	public SpaceObject(
		long entityId,
		SpaceObjectPositionFunction position, SpaceObject[] subObjects,
		Color solidColor, double solidRadius,
		long autoUpdateTime, Map properties, SpaceObjectBehavior behavior
	) {
		this.entityId       = entityId;
		this.position       = position;
		this.subObjects     = subObjects;
		this.solidColor     = solidColor;
		this.solidRadius    = solidRadius;
		this.systemRadius   = 0; // TODO: calculate
		this.autoUpdateTime = autoUpdateTime;
		this.properties     = properties;
		this.behavior       = behavior;
	}
}
