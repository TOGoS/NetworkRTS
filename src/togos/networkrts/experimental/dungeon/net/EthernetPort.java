package togos.networkrts.experimental.dungeon.net;

/**
 * Anything that can accept ethernet frames! 
 */
public interface EthernetPort
{
	public void put( ObjectEthernetFrame<?> f );
}
