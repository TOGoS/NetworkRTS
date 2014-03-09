package togos.networkrts.experimental.game19.world;

//TODO: Delete, replace with messages to the simulator
public interface ActionContext
{
	public World getWorld();
	public void setWorld(World w);
	public void enqueueMessage(Message m);
}
