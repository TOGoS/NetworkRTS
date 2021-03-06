package togos.networkrts.experimental.game19.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.WeakHashMap;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.CerealUtil;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;
import togos.networkrts.cereal.SHA1ObjectReference;
import togos.networkrts.cereal.StandardValueOps;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.experimental.game19.world.QuadRSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.packet19.MalformedDataException;
import togos.networkrts.experimental.packet19.PacketPayloadCodec;
import togos.networkrts.util.Getter;
import togos.networkrts.util.HasURI;
import togos.networkrts.util.HashUtil;
import togos.networkrts.util.Repository;
import togos.networkrts.util.ResourceNotFound;
import togos.networkrts.util.Storer;

public class CerealWorldIO implements WorldIO
{
	public static class ConstructWorldObject implements OpcodeDefinition {
		public static final ConstructWorldObject INSTANCE = new ConstructWorldObject();
		
		private ConstructWorldObject() { }
		
		@Override public int apply(
				byte[] data, int offset, DecodeState ds, CerealDecoder context
			) throws InvalidEncoding, ResourceNotFound {
				++offset;
				final long numberDecodeResult = NumberEncoding.readUnsignedBase128(data, offset);
				offset += NumberEncoding.base128Skip(numberDecodeResult);
				final long constructorNumber = NumberEncoding.base128Value(numberDecodeResult);
				if( constructorNumber < 0 || constructorNumber > decoders.length ) {
					throw new InvalidEncoding("No such constructor as "+constructorNumber);
				}
				WorldObjectCCCodec<?> decoder = decoders[(int)constructorNumber];
				if( decoder == null ) {
					throw new InvalidEncoding("No such constructor as "+constructorNumber);
				}
				return decoder.decode(data, offset, ds, context);
			}
			
			@Override public String getUrn() { return "urn:sha1:SE24V7IVXEPDO7XRVG7QDH7TJQZME3ML"; }
	}
	
	class CerealPacketPayloadCodec implements PacketPayloadCodec<Object>
	{
		@Override public Object decode( byte[] data, int offset, int length ) throws MalformedDataException {
			try {
				return cerealDecoder.decode(data, offset, length);
			} catch( InvalidEncoding e ) {
				throw new MalformedDataException("Invalid encoding", e);
			} catch( ResourceNotFound e ) {
				throw new MalformedDataException("Resource not found", e);
			}
		}
		
		@Override public void encode( Object obj, OutputStream os ) throws IOException {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException();
		}
	}
	
	static final Repository<byte[]> EMPTY_REPOSITORY = new Repository<byte[]>() {
		@Override public byte[] get( String uri ) throws ResourceNotFound {
			throw new ResourceNotFound(uri);
		}
		@Override public String store( byte[] v ) {
			throw new UnsupportedOperationException();
		}
	};
	/**
	 * A CerealWorldIO instance that is not connected to a repository
	 * and therefore can't load objects by reference. 
	 */
	public static final CerealWorldIO DISCONNECTED = new CerealWorldIO(EMPTY_REPOSITORY);
	
	public static final byte CONSTRUCTOR_OPCODE = 0x61;
	
	/** 24-bit-prefix opcodes */
	static final WorldObjectCCCodec<?>[] decoders = new WorldObjectCCCodec<?>[65536];
	/** Object constructor prefixes (opcode + base128-encoded constructor number), keyed by the codec that will interpret them */
	static final HashMap<WorldObjectCCCodec<?>,byte[]> encoderPrefixes = new HashMap<WorldObjectCCCodec<?>,byte[]>(); 
	static final HashMap<Class<?>,WorldObjectCCCodec<?>> classEncoders = new HashMap<Class<?>,WorldObjectCCCodec<?>>(); 
	
	static byte[] encodePrefix(int constructorNumber) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(CONSTRUCTOR_OPCODE);
		try {
			NumberEncoding.writeUnsignedBase128(constructorNumber, baos);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		return baos.toByteArray();
	}
	
	static void addConstructor( int num, WorldObjectCCCodec<?> woc ) {
		decoders[num] = woc;
		classEncoders.put(woc.getEncodableClass(), woc);
		encoderPrefixes.put(woc, encodePrefix(num));
	}
	
	static {
		addConstructor( 0x0001, QuadRSTNode.CCC );
		addConstructor( 0x0002, BlockStackRSTNode.CCC );
		addConstructor( 0x0003, Block.CCC1 );
		
		addConstructor( 0x1000, World.CCC1 );
		addConstructor( 0x1001, Icon.CCC );
	}
	
	static <T> WorldObjectCCCodec<? super T> getDefaultEncoder(T o) {
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>)o.getClass();
		@SuppressWarnings("unchecked")
		WorldObjectCCCodec<? super T> codec = (WorldObjectCCCodec<? super T>)classEncoders.get(c);
		return codec;
	}
	
	////
	
	protected final Getter<byte[]> chunkGetter;
	protected final Storer<byte[]> chunkStorer;
	protected final CerealDecoder cerealDecoder;
	
	public final CerealPacketPayloadCodec packetPayloadCodec = new CerealPacketPayloadCodec();
	
	protected CerealWorldIO( Getter<byte[]> getter, Storer<byte[]> storer ) {
		this.chunkGetter = getter;
		this.chunkStorer = storer;
		this.cerealDecoder = new CerealDecoder(getter);
	}
	public <R extends Getter<byte[]> & Storer<byte[]>> CerealWorldIO( R storage ) {
		this( storage, storage );
	}
	
	protected byte[] baseDecodeStateSha1;
	protected byte[] getBaseDecodeStateSha1() {
		if( baseDecodeStateSha1 == null ) synchronized(this) {
			if( baseDecodeStateSha1 == null ) {
				OpcodeDefinition[] opcodeImports = new OpcodeDefinition[256];
				for( int i=0; i<StandardValueOps.STANDARD_OPS.length; ++i ) {
					opcodeImports[i] = StandardValueOps.STANDARD_OPS[i];
				}
				opcodeImports[CONSTRUCTOR_OPCODE] = ConstructWorldObject.INSTANCE;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					CerealUtil.writeHeaderWithImports(opcodeImports, baos);
					baseDecodeStateSha1 = HashUtil.extractSha1FromUrn(chunkStorer.store(baos.toByteArray()));
				} catch( InvalidEncoding e ) {
					throw new RuntimeException(e);
				} catch( IOException e ) {
					throw new RuntimeException(e);
				}				
			}
		}
		return baseDecodeStateSha1;
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
	
	WeakHashMap<Object,SHA1ObjectReference> savedObjects = new WeakHashMap<Object,SHA1ObjectReference>();
	
	@Override public SHA1ObjectReference storeObject(Object o) {
		SHA1ObjectReference ref = savedObjects.get(o);
		if( ref != null ) return ref;
		
		// TODO: Look up in a WeakHashMap to see f it's already been saved
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			CerealUtil.writeTbbHeader(getBaseDecodeStateSha1(), baos);
			writeObjectInline(o, baos);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		ref = storeEncodedObject(baos.toByteArray());
		savedObjects.put(o, ref);
		return ref;
	}
	
	protected SHA1ObjectReference storeEncodedObject( byte[] chunk ) {
		try {
			return SHA1ObjectReference.parse( chunkStorer.store(chunk), true );
		} catch( InvalidEncoding e ) {
			throw new RuntimeException("Apparently storer did not return an SHA-1 URI?", e);
		}
	}
	
	public <T> void writeObjectInline(T o, WorldObjectCCCodec<? super T> codec, OutputStream os) throws IOException {
		codec.encode(o, encoderPrefixes.get(codec), os, this);
	}
	
	public <T> void writeObjectInline(T obj, OutputStream os) throws IOException {
		WorldObjectCCCodec<? super T> codec = getDefaultEncoder(obj);
		if( codec == null ) {
			throw new UnsupportedOperationException("No encoder registered for instances of "+obj.getClass());
		}
		writeObjectInline(obj, codec, os);
	}
	
	public void writeObjectReference(Object o, OutputStream os) throws IOException {
		StandardValueOps.writeSha1ObjectReference(storeObject(o), os);
	}
}
