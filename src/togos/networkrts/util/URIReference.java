package togos.networkrts.util;

public class URIReference implements HasURI
{
	protected final String uri;
	
	public URIReference( String uri ) { this.uri = uri; }
	
	@Override public String getUri() { return uri; }
	
	@Override public int hashCode() {
		return uri.hashCode();
	}
	@Override public boolean equals(Object o) {
		return o instanceof HasURI && uri.equals(((HasURI)o).getUri());
	}
}
