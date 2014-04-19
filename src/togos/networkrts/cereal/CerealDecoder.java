package togos.networkrts.cereal;

import static togos.networkrts.cereal.SHA1ObjectReference.SUBJECT_OF_PREFIX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import togos.networkrts.util.Getter;
import togos.networkrts.util.HasURI;
import togos.networkrts.util.HashUtil;
import togos.networkrts.util.ResourceHandlePool;
import togos.networkrts.util.ResourceNotFound;
import togos.networkrts.util.SoftResourceHandle;

public class CerealDecoder implements Getter<Object>
{
	public static class DecodeState {
		private final OpcodeBehavior[] opcodeBehaviors;
		private final ArrayList<Object> stack;
		private boolean frozen = false;
		
		public DecodeState( OpcodeBehavior[] opcodes, List<Object> stack ) {
			assert opcodes.length == 256;
			
			this.opcodeBehaviors = Arrays.copyOf(opcodes, opcodes.length);
			this.stack = new ArrayList<Object>(stack);
		}

		public DecodeState( OpcodeBehavior[] opcodes ) {
			this(opcodes, Collections.<Object>emptyList());
		}
		
		public Object getValue() throws InvalidEncoding {
			if( stack.size() == 0 ) throw new InvalidEncoding("Decode stack empty");
			return stack.get(stack.size()-1);
		}
		
		// Opcode registration
		
		public synchronized void setOpcodeBehavior( byte opcode, OpcodeBehavior behavior ) {
			if( frozen ) throw new RuntimeException("DecodeState is frozen; you cannot change its opcodes!");
			this.opcodeBehaviors[opcode&0xFF] = behavior;
		}
		
		// Stack mutation
		
		public Object[] getStackSnapshot() {
			return stack.toArray(new Object[stack.size()]);
		}
		
		public synchronized Object removeStackItem(int index) {
			if( frozen ) throw new RuntimeException("DecodeState is frozen; you cannah get its stack mutable!");
			if( index < 0 ) index = stack.size()+index;
			return stack.remove(index);
		}
		public synchronized Object getStackItem(int index) {
			if( frozen ) throw new RuntimeException("DecodeState is frozen; you cannah get its stack mutable!");
			if( index < 0 ) index = stack.size()+index;
			return stack.get(index);
		}
		public synchronized void pushStackItem(Object item) {
			if( frozen ) throw new RuntimeException("DecodeState is frozen; you cannah get its stack mutable!");
			stack.add(item);
		}
		public synchronized void insertStackItem(int index, Object item) {
			if( frozen ) throw new RuntimeException("DecodeState is frozen; you cannah get its stack mutable!");
			if( index < 0 ) index = stack.size()+index;
			stack.add(index, item);
		}
		
		public synchronized DecodeState freeze() {
			this.frozen = true;
			return this;
		}
		public synchronized DecodeState thaw() {
			return frozen ? new DecodeState( opcodeBehaviors, stack ) : this;
		}

		public DecodeState process( byte[] data, int offset, int length, CerealDecoder ctx ) throws InvalidEncoding, ResourceNotFound {
			assert offset+length <= data.length;
			int end = offset+length;
			if( offset == length ) return this;
			
			DecodeState ds = thaw();
			while( offset < end ) {
				OpcodeBehavior ob = ds.opcodeBehaviors[data[offset]&0xFF];
				offset = ob.apply(data, offset, ds, ctx);
			}
			return ds;
		}
	}
	
	public final Map<String, ? extends OpcodeBehavior> opLib = Opcodes.BY_URN;;
	
	/*
	protected static final IMPORT_LIBRARY_OP = new  
	
	protected static final List PREDEFINED_OPS = Collections.singletonList()
	
	protected final DecodeState INITIAL_DECODE_STATE = new DecodeState( PREDEFINED_OPS );
	 */
	
	// loading process
	// chunkRepo --(byte[])--> decodeToDecodeState --(DecodeState)--> decodeStateCache
	
	protected final Getter<byte[]> chunkSource;
	protected final ResourceHandlePool decodeStateCache = new ResourceHandlePool();
	protected final DecodeState initialDecodeState;
	
	protected CerealDecoder( Getter<byte[]> chunkSource, String initialDecodeStateUrn, DecodeState initialDecodeState ) {
		this.chunkSource = chunkSource;
		
		decodeStateCache.<DecodeState>get(initialDecodeStateUrn).setValue(initialDecodeState);
		this.initialDecodeState = initialDecodeState.freeze();
	}
	
	public CerealDecoder( Getter<byte[]> chunkSource ) {
		this( chunkSource, CerealUtil.CEREAL_SCHEMA_SHA1_URN, new DecodeState(Opcodes.createInitialOpcodeTable()) );
	}
	
	protected static boolean equals( byte[] a, int offsetA, byte[] b, int offsetB, int length ) {
		if( a.length < offsetA+length ) return false;
		if( b.length < offsetB+length ) return false;
		for( int i=length-1; i>=0; --i ) {
			if( a[offsetA+i] != b[offsetB+i] ) return false;
		}
		return true;
	}
	
	protected DecodeState decodeToDecodeState( byte[] data, int offset, int length ) throws InvalidEncoding, ResourceNotFound {
		if( !equals(data, 0, CerealUtil.TBB_HEADER, offset, 4) ) throw new InvalidEncoding("No TBB header found");
		
		byte[] initialStateSha1 = CerealUtil.extract(data, offset+4, 20);
		DecodeState ds = getDecodeState(initialStateSha1);
		return ds.process( data, offset+24, data.length-24, this ).freeze();
	}
	
	protected Getter<DecodeState> decodeStateGetter = new Getter<DecodeState>() {
		@Override public DecodeState get( String uri ) throws ResourceNotFound {
			try {
				// The subject-of: prefix is implied.  We'll ignore it.
				if( uri.startsWith(SUBJECT_OF_PREFIX) ) {
					uri = uri.substring(SUBJECT_OF_PREFIX.length());
				}
				byte[] data = chunkSource.get(uri);
				return decodeToDecodeState( data, 0, data.length );
			} catch( InvalidEncoding e ) {
				throw new ResourceNotFound( uri, e );
			}
		}
	};
	
	protected DecodeState getDecodeState( String urn ) throws InvalidEncoding, ResourceNotFound {
		SoftResourceHandle<DecodeState> decodeStateHandle = decodeStateCache.<DecodeState>get(urn);
		return decodeStateHandle.getValue(decodeStateGetter);
	}
	
	protected DecodeState getDecodeState( byte[] sha1 ) throws InvalidEncoding, ResourceNotFound {
		return getDecodeState(HashUtil.sha1Urn(sha1));
	}
	
	////
	
	public Object decode( byte[] data, int offset, int length ) throws InvalidEncoding, ResourceNotFound {
		return decodeToDecodeState(data, offset, length).getValue();
	}
	
	public Object get( byte[] sha1 ) throws ResourceNotFound {
		return get( HashUtil.sha1Urn(sha1) );
	}
	
	@Override public Object get( String urn ) throws ResourceNotFound {
		try {
			return getDecodeState(urn).getValue();
		} catch( InvalidEncoding e ) {
			throw new ResourceNotFound(urn, e);
		}
	}
	
	public <T> T removeStackItem( DecodeState ds, int index, Class<T> expectedClass )
		throws ResourceNotFound, InvalidEncoding
	{
		Object item = ds.removeStackItem(index);
		if( expectedClass.isAssignableFrom(item.getClass()) ) {
			return expectedClass.cast(item);
		} else if( item instanceof HasURI ) {
			final String uri = ((HasURI)item).getUri();
			item = get( uri );
			if( expectedClass.isAssignableFrom(item.getClass()) ) {
				return expectedClass.cast(item);
			} else {
				throw new InvalidEncoding(
					item.getClass().getSimpleName()+
					"-reference ("+uri+") found on the stack when expecting a "+
					expectedClass.getSimpleName()
				);
			}
		} else {
			throw new InvalidEncoding(
				item.getClass().getSimpleName()+
				" found on the stack when expecting a "+
				expectedClass.getSimpleName()
			);
		}
	}
}
