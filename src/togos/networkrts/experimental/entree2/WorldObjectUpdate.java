package togos.networkrts.experimental.entree2;

public final class WorldObjectUpdate {
	public final WorldObject worldObject;
	public final boolean isAddition; // Otherwise it is a removal
	
	protected WorldObjectUpdate( WorldObject worldObject, boolean add ) {
		this.worldObject = worldObject;
		this.isAddition = add;
	}
	
	public static WorldObjectUpdate addition( WorldObject worldObject ) {
		return new WorldObjectUpdate( worldObject, true );
	}

	public static WorldObjectUpdate removal( WorldObject worldObject ) {
		return new WorldObjectUpdate( worldObject, false );
	}
}