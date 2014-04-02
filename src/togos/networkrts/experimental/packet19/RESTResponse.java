package togos.networkrts.experimental.packet19;

public interface RESTResponse<P>
{
	public int getStatus();
	public P getPayload();
}
