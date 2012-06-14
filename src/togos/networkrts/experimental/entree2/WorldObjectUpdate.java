package togos.networkrts.experimental.entree2;

public final class WorldObjectUpdate<WorldObjectClass extends WorldObject> {
	public final WorldObjectClass worldObject;
	public final boolean isAddition; // Otherwise it is a removal
	
	protected WorldObjectUpdate( WorldObjectClass worldObject, boolean add ) {
		this.worldObject = worldObject;
		this.isAddition = add;
	}
	
	public static <WorldObjectClass extends WorldObject> WorldObjectUpdate<WorldObjectClass> addition( WorldObjectClass worldObject ) {
		return new WorldObjectUpdate( worldObject, true );
	}

	public static <WorldObjectClass extends WorldObject> WorldObjectUpdate<WorldObjectClass> removal( WorldObjectClass worldObject ) {
		return new WorldObjectUpdate( worldObject, false );
	}
}
