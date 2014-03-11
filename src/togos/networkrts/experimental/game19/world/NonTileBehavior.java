package togos.networkrts.experimental.game19.world;

import java.util.Collection;

public interface NonTileBehavior
{
	public static final NonTileBehavior NONE = new NonTileBehavior() {
		public NonTile update( NonTile nt, long time, World w, Collection<Message> messages, Collection<Message> outgoingMessages ) {
			return nt;
		}
	};
	
	public NonTile update( NonTile nt, long time, World w, Collection<Message> messages, Collection<Message> outgoingMessages );
}
