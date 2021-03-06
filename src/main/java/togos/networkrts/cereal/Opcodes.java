package togos.networkrts.cereal;

import java.util.HashMap;

import togos.networkrts.cereal.op.LoadOpcode;
import togos.networkrts.cereal.op.PushDefault;
import togos.networkrts.cereal.op.PushFalse;
import togos.networkrts.cereal.op.PushFloat16;
import togos.networkrts.cereal.op.PushFloat32;
import togos.networkrts.cereal.op.PushFloat64;
import togos.networkrts.cereal.op.PushInt16;
import togos.networkrts.cereal.op.PushInt32;
import togos.networkrts.cereal.op.PushInt64;
import togos.networkrts.cereal.op.PushInt8;
import togos.networkrts.cereal.op.PushMediumString;
import togos.networkrts.cereal.op.PushOpcodeByteValue;
import togos.networkrts.cereal.op.PushSHA1BlobReference;
import togos.networkrts.cereal.op.PushSHA1ObjectReference;
import togos.networkrts.cereal.op.PushShortString;
import togos.networkrts.cereal.op.PushTrue;
import togos.networkrts.experimental.game19.io.CerealWorldIO;

public class Opcodes
{
	private static final OpcodeDefinition[] ALL = new OpcodeDefinition[] {
		LoadOpcode.INSTANCE,
		PushTrue.INSTANCE,
		PushFalse.INSTANCE,
		PushDefault.INSTANCE,
        PushFloat16.INSTANCE,
        PushFloat32.INSTANCE,
        PushFloat64.INSTANCE,
        PushInt8.INSTANCE,
        PushInt16.INSTANCE,
        PushInt32.INSTANCE,
        PushInt64.INSTANCE,
        PushShortString.INSTANCE,
        PushMediumString.INSTANCE,
        PushOpcodeByteValue.INSTANCE,
        PushSHA1BlobReference.INSTANCE,
        PushSHA1ObjectReference.INSTANCE,
        
        CerealWorldIO.ConstructWorldObject.INSTANCE
	};
	
	public static final HashMap<String, OpcodeDefinition> BY_URN = new HashMap<String,OpcodeDefinition>();
	static {
		for( OpcodeDefinition def : ALL ) {
			OpcodeDefinition oldDef = BY_URN.get(def.getUrn()); 
			if( oldDef != null ) {
				throw new RuntimeException(
					def.getClass().getName()+" redefines "+	def.getUrn()+
					", which was "+oldDef.getClass().getName()
				);
			}
			BY_URN.put( def.getUrn(), def );
		}
	}
	
	/**
	 * Get a copy of the opcode table that is provided by the
	 * initial CoolCereal decode state.
	 */
	public static final OpcodeBehavior[] createInitialOpcodeTable() {
		OpcodeDefinition[] defs = new OpcodeDefinition[256];
		for( int i=0; i<256; ++i ) defs[i] = PushOpcodeByteValue.INSTANCE;
		defs[0x41] = LoadOpcode.INSTANCE;
		return defs;
	}
}
