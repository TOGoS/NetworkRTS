package togos.networkrts.experimental.dungeon.net;

import togos.networkrts.experimental.dungeon.DungeonGame.MessageReceiver;

/**
 * Anything that can accept ethernet frames! 
 */
public interface EthernetPort extends MessageReceiver<ObjectEthernetFrame<?>>
{
	public void messageReceived( ObjectEthernetFrame<?> f );
}
