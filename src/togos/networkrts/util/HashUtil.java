package togos.networkrts.util;

import java.util.regex.Matcher;

import org.bitpedia.util.Base32;

import togos.networkrts.cereal.CerealUtil;
import togos.networkrts.cereal.InvalidEncoding;

public class HashUtil
{

	public static final byte[] extractSha1FromUrn( String urn ) throws InvalidEncoding {
		Matcher m;
		if( (m = CerealUtil.SHA1_PATTERN.matcher(urn)).matches() ) {
			return Base32.decode( m.group(1) );
		}
		if( (m = CerealUtil.BITPRINT_PATTERN.matcher(urn)).matches() ) {
			return Base32.decode( m.group(1) );
		}
		throw new InvalidEncoding("Unrecognized SHA-1 URN: '"+urn+"'");
	}

	public static String sha1Urn( String urn ) throws InvalidEncoding {
		Matcher m;
		if( (m = CerealUtil.SHA1_PATTERN.matcher(urn)).matches() ) {
			return urn;
		}
		if( (m = CerealUtil.BITPRINT_PATTERN.matcher(urn)).matches() ) {
			return "urn:sha1:"+m.group(1);
		}
		throw new InvalidEncoding("Unrecognized SHA-1 URN: "+urn);
	}

	public static String sha1Urn( byte[] sha1 ) {
		assert sha1 != null;
		assert sha1.length == 20;
		return "urn:sha1:"+Base32.encode(sha1);
	}

}
