package togos.networkrts.experimental.game19.world;

public interface ActionContext extends NodeInstance
{
	public RSTNode getNode();
	public int getNodeX();
	public int getNodeY();
	public int getNodeSizePower();
	
	public void setNode(RSTNode n);
	public void enqueueMessage(Message m);
}
