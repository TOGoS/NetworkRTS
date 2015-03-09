package togos.networkrts.experimental.game19.extnet;

import togos.networkrts.experimental.game19.util.MessageSender;

public interface NetworkComponent extends MessageSender
{
	public void start();
	public void setDaemon(boolean d);
	public void halt();
}
