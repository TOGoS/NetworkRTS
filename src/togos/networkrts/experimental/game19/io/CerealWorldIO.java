package togos.networkrts.experimental.game19.io;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.Opcodes;
import togos.networkrts.util.Getter;
import togos.networkrts.util.HasURI;
import togos.networkrts.util.ResourceNotFound;

public class CerealWorldIO implements WorldIO
{
	protected static final DecodeState INITIAL_DECODE_STATE;
	static {
		INITIAL_DECODE_STATE = new DecodeState(Opcodes.createDefaultOpTable());
	}
	
	protected final CerealDecoder cerealDecoder;
	protected CerealWorldIO( CerealDecoder cerealDecoder ) {
		this.cerealDecoder = cerealDecoder;
	}
	public CerealWorldIO( Getter<byte[]> chunkSource ) {
		this( new CerealDecoder(chunkSource, INITIAL_DECODE_STATE) );
	}
	
	@Override public Object getObject(HasURI ref) throws ResourceNotFound {
		// May eventually want to read objects of other encodings,
		// such as .properties files, in which case we'll need to get
		// the block and check the magic ourselves.
		return cerealDecoder.get(ref.getUri());
	}
	@Override public <T> T getObject(HasURI ref, Class<T> expectedClass) throws ResourceNotFound {
		return expectedClass.cast(getObject(ref));
	}
	@Override public HasURI saveObject(Object o) {
		throw new UnsupportedOperationException();
	}
}
