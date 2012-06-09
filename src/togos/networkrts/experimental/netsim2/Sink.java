package togos.networkrts.experimental.netsim2;

public interface Sink<ItemClass>
{
	public void give( ItemClass p ) throws Exception;
}
