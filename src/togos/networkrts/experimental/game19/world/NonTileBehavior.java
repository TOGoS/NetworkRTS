package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;

public interface NonTileBehavior
{
	public static final NonTileBehavior NONE = new NonTileBehavior() {
		public NonTile update( NonTile nt, long time, World w, MessageSet messages, NonTileUpdateContext updateContext ) {
			return nt;
		}
	};
	
	public NonTile update( NonTile nt, long time, World w, MessageSet messages, NonTileUpdateContext updateContext );
}
