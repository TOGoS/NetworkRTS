package togos.networkrts.cereal;

import togos.networkrts.util.HasURI;
import togos.networkrts.util.HashUtil;

public class SHA1ObjectReference implements HasURI
{
	protected static final String SUBJECT_OF_PREFIX = "subject-of:";
	
	private final byte[] sha1;
	private final boolean sha1IdentifiesSerialization;
	private String uri = null;
	
	public SHA1ObjectReference( byte[] sha1, boolean sha1IdentifiesSerialization ) {
		assert sha1 != null;
		this.sha1 = sha1;
		this.sha1IdentifiesSerialization = sha1IdentifiesSerialization;
	}
	
	public byte[] getSha1() { return sha1; }
	public boolean sha1IdentifiesSerialization() { return sha1IdentifiesSerialization; }
	
	public synchronized String getUri() {
		if( uri == null ) {
			uri = (sha1IdentifiesSerialization ? SUBJECT_OF_PREFIX : "") + HashUtil.sha1Urn(sha1); 
		}
		return uri;
	}
	
	@Override public String toString() {
		return "<"+getUri()+">";
	}
	
	@Override public int hashCode() {
		return getUri().hashCode();
	}
	@Override public boolean equals(Object o) {
		return o instanceof HasURI && getUri().equals(((HasURI)o).getUri());
	}
	
	public static SHA1ObjectReference parse( String uri, boolean identifiesSerialization ) throws InvalidEncoding {
		return new SHA1ObjectReference(HashUtil.extractSha1FromUrn(uri), identifiesSerialization);
	}
	
	public static SHA1ObjectReference parse( String uri ) throws InvalidEncoding {
		boolean postfixIdentifiesSerialization;
		if( uri.startsWith(SUBJECT_OF_PREFIX) ) {
			postfixIdentifiesSerialization = true;
			uri = uri.substring(SUBJECT_OF_PREFIX.length());
		} else {
			postfixIdentifiesSerialization = false;
		}
		return parse( uri, postfixIdentifiesSerialization );
	}
}
