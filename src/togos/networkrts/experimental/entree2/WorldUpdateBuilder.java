package togos.networkrts.experimental.entree2;

import java.util.ArrayList;

public class WorldUpdateBuilder {
	ArrayList<WorldObjectUpdate> updates = new ArrayList();
	
	public void add( WorldObject o ) {
		updates.add( WorldObjectUpdate.addition(o) );
	}
	
	public void remove( WorldObject o ) {
		updates.add( WorldObjectUpdate.removal(o) );
	}
	
	public WorldObjectUpdate[] getUpdateArray() {
		return updates.toArray(new WorldObjectUpdate[updates.size()]);
	}
	
	public void clear() {
		updates.clear();
	}
	
	public <EntreeClass extends Entree> EntreeClass applyAndClear( EntreeClass entree ) {
		WorldObjectUpdate[] updates = getUpdateArray();
		clear();
		return (EntreeClass)entree.update(updates, 0, updates.length);
	}
}
