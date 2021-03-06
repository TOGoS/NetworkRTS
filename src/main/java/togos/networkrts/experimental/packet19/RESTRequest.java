package togos.networkrts.experimental.packet19;

public interface RESTRequest extends HasWackPayload
{
	public static final String GET = "GET";
	public static final String PUT = "PUT";
	public static final String POST = "POST";
	public static final String DELETE = "DELETE";
	
	public String getMethod();
	public String getPath();
}
