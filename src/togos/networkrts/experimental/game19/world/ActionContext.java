package togos.networkrts.experimental.game19.world;

public interface ActionContext extends NodeInstance
{
	public WorldNode getNode();
	public int getNodeX();
	public int getNodeY();
	public int getNodeSizePower();
	
	public void setNode(WorldNode n);
	public void enqueueMessage(Message m);
}
