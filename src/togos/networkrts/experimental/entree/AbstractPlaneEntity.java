package togos.networkrts.experimental.entree;

import togos.networkrts.tfunc.PositionFunction;
import togos.networkrts.tfunc.ScalarFunction;

public abstract class AbstractPlaneEntity implements PlaneEntity, WorldPositioned
{
	public final Object entityId;
	public final Object planeId;
	public final double[] referencePosition = new double[3];
	public final PositionFunction position;
	public final ScalarFunction rotation;
	public final int flags;
	
	public AbstractPlaneEntity( Object entityId, Object planeId, long referenceTimestamp, PositionFunction position, ScalarFunction rotation, int flags ) {
		assert (flags & PlaneEntity.FLAG_EXISTS) != 0;
		this.entityId = entityId;
		this.planeId = planeId;
		this.flags = flags;
		this.position = position;
		this.rotation = rotation;
		position.getPosition(referenceTimestamp, referencePosition);
	}
	
	public AbstractPlaneEntity( Object planeId, long referenceTimestamp, PositionFunction position, ScalarFunction rotation, int flags ) {
		assert (flags & PlaneEntity.FLAG_EXISTS) != 0;
		this.entityId = this;
		this.planeId = planeId;
		this.flags = flags;
		this.position = position;
		this.rotation = rotation;
		position.getPosition(referenceTimestamp, referencePosition);
	}
	
	@Override public Object getEntityId() {  return entityId;  }
	@Override public Object getPlaneId() {  return planeId;  }
	@Override public double getX() {  return referencePosition[0];  }
	@Override public double getY() {  return referencePosition[1];  }
	@Override public int getFlags() {  return flags;  }
	
	@Override public void getPosition( long timestamp, double[] dest ) {
		position.getPosition(timestamp, dest);
	}

	@Override public double getRotation( long timestamp ) {
		return rotation.getValue(timestamp);
	}
}