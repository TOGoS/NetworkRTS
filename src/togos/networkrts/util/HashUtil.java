package togos.networkrts.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
	
	public static final byte[] sha1( byte[] data, int offset, int length ) {
		MessageDigest sha1Digester;
		try {
			sha1Digester = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Apparently SHA-1 isn't available", e);
		}
		sha1Digester.update(data, offset, length);
		return sha1Digester.digest();
	}
}
