package togos.networkrts.cereal;

import togos.networkrts.util.ResourceNotFound;

public class CerealDecoderTest extends BaseCerealDecoderTest
{
	public void testEmptyStackIsParseError() throws ResourceNotFound {
		byte[] data = encodeStuff();
		try {
			decoder.decode(data, 0, data.length);
			fail("It should've thrown an InvalidEncoding!");
		} catch( InvalidEncoding ie ) {
			// Yay
		}
	}
}
