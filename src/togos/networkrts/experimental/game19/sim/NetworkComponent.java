package togos.networkrts.experimental.game19.sim;

public interface NetworkComponent extends MessageSender
{
	public void start();
	public void setDaemon(boolean d);
	public void halt();
}
