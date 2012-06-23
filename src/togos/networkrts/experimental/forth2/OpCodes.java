package togos.networkrts.experimental.forth2;

public class OpCodes
{
	public static final int NOP  = 0;
	public static final int PUSH_STRING    = 0x01; // followed by 8-bit length, then data
	public static final int PUSH_STRING2   = 0x02; // followed by 16-bit length, then data
	public static final int DEFINE_SYMBOL  = 0x03; // ( <symbol> <value> -- )
	public static final int PUSH_PRIMITIVE = 0x80; // bits 4-6 are type, 0-4 are count
	
	public static final int PP_INT8    = 0x10;
	public static final int PP_INT16   = 0x20;
	public static final int PP_INT32   = 0x30;
	public static final int PP_FLOAT32 = 0x40;
	public static final int PP_FLOAT64 = 0x50;
	public static final int PP_FIXED16 = 0x60;
}
