package togos.networkrts.cereal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceHandle;
import togos.networkrts.util.ResourceHandlePool;
import togos.networkrts.util.ResourceNotFound;

public class CerealDecoder implements Getter<Object>
{
	public static class DecodeState {
		private final ArrayList<OperationType> opTypes;
		private final ArrayList<Object> stack;
		private boolean frozen = false;
		
		public DecodeState( List<OperationType> opcodes, List<Object> stack ) {
			this.opTypes = new ArrayList<OperationType>(opcodes);
			this.stack = new ArrayList<Object>(stack);
		}

		public DecodeState( List<OperationType> opcodes ) {
			this(opcodes, Collections.<Object>emptyList());
		}
		
		public Object getValue() throws InvalidEncoding {
			if( stack.size() == 0 ) throw new InvalidEncoding("Decode stack empty");
			return stack.get(stack.size()-1);
		}
		
		// Opcode registration
		
		public synchronized void addOperationTypes( Collection<OperationType> opcodes ) {
			if( frozen ) throw new RuntimeException("DecodeState is frozen; you cannot get it's stack mutable!");
			this.opTypes.addAll(0, opcodes);
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
			return frozen ? new DecodeState( opTypes, stack ) : this;
		}

		public DecodeState process( byte[] data, int i, CerealDecoder coolCodec ) throws InvalidEncoding, ResourceNotFound {
			if( i == data.length ) return this;
			
			DecodeState ds = thaw();
			opstream: while( i < data.length ) {
				for( OperationType opType : ds.opTypes ) {
					int i2 = opType.apply( data, i, ds, coolCodec );
					if( i2 != i ) {
						i = i2;
						continue opstream;
					}
				}
				throw new InvalidEncoding(String.format("Opcode 0x%02x is not defined", data[i]));
			}
			return ds;
		}
	}
	
	protected static final Map<String, OperationLibrary> OP_LIBS = new HashMap<String,OperationLibrary>();
	
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
	
	public CerealDecoder( Getter<byte[]> chunkSource, DecodeState initialDecodeState ) {
		this.chunkSource = chunkSource;
		
		decodeStateCache.<DecodeState>get(CerealUtil.CEREAL_SCHEMA_SHA1_URN).setValue(initialDecodeState);
		this.initialDecodeState = initialDecodeState.freeze();
	}
	
	protected static boolean equals( byte[] a, int offsetA, byte[] b, int offsetB, int length ) {
		if( a.length < offsetA+length ) return false;
		if( b.length < offsetB+length ) return false;
		for( int i=length-1; i>=0; --i ) {
			if( a[offsetA+i] != b[offsetB+i] ) return false;
		}
		return true;
	}
	
	protected DecodeState decodeToDecodeState( byte[] data ) throws InvalidEncoding, ResourceNotFound {
		if( !equals(data, 0, CerealUtil.TBB_HEADER, 0, 4) ) throw new InvalidEncoding("No TBB header found");
		
		byte[] initialStateSha1 = CerealUtil.extract(data, 4, 20);
		DecodeState ds = getDecodeState(initialStateSha1);
		return ds.process( data, 24, this );
	}
	
	protected Getter<DecodeState> decodeStateGetter = new Getter<DecodeState>() {
		@Override public DecodeState get( String uri ) throws ResourceNotFound {
			try {
				return decodeToDecodeState( chunkSource.get(uri) );
			} catch( InvalidEncoding e ) {
				throw new ResourceNotFound( uri, e );
			}
		}
	};
	
	protected DecodeState getDecodeState( String urn ) throws InvalidEncoding, ResourceNotFound {
		ResourceHandle<DecodeState> decodeStateHandle = decodeStateCache.<DecodeState>get(urn);
		return decodeStateHandle.getValue(decodeStateGetter);
	}
	
	protected DecodeState getDecodeState( byte[] sha1 ) throws InvalidEncoding, ResourceNotFound {
		return getDecodeState(CerealUtil.sha1Urn(sha1));
	}
	
	////
	
	public Object decode( byte[] data ) throws InvalidEncoding, ResourceNotFound {
		return decodeToDecodeState(data).getValue();
	}
	
	public Object get( byte[] sha1 ) throws ResourceNotFound {
		return get( CerealUtil.sha1Urn(sha1) );
	}
	
	@Override public Object get( String urn ) throws ResourceNotFound {
		try {
			return getDecodeState(urn).getValue();
		} catch( InvalidEncoding e ) {
			throw new ResourceNotFound(urn, e);
		}
	}
}
