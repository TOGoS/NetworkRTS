package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;

// This whole thing might get deprecated
// to let tiles handle more things themselves,
// thereby making it easier to define new types of things,
// since they could then store all their behavior and state
// in one NonTile object.
public interface NonTileBehavior<NT extends NonTile>
{
	@SuppressWarnings("rawtypes")
	public static final NonTileBehavior<?> NONE = new NonTileBehavior() {
		public NonTile update( NonTile nt, long time, World w, MessageSet messages, NonTileUpdateContext updateContext ) {
			return nt;
		}
	};
	
	public NonTile update( NT nt, long time, World w, MessageSet messages, NonTileUpdateContext updateContext );
}
