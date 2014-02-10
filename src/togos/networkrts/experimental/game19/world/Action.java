package togos.networkrts.experimental.game19.world;

public interface Action
{
	//public boolean appliesToWorldNode( WorldNode n, int x, int y, int size );
	//public WorldNode applyToWorldNode( WorldNode n, int x, int y, int size, List<Action> results );
	
	public void apply( ActionContext ctx );
}
