package togos.networkrts.experimental.game19.world;

public interface ActionContext
{
	public WorldNode getRootNode();
	public int getRootX();
	public int getRootY();
	public int getRootSizePower();
	
	public void setRootNode(WorldNode n);
	public void enqueueMessage(Message m);
}
