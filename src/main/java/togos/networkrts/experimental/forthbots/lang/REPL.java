package togos.networkrts.experimental.forthbots.lang;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.lang.ScriptError;
import togos.networkrts.experimental.forthbots.ForthBotsWorld.ForthVM;

public class REPL implements Handler<Token,ScriptError>
{
	static interface Word {
		public void invoke(ForthVM vm); 
	}
	static class VMWord implements Word {
		final short location, length;
		public VMWord(short location, short length) {
			this.location = location;
			this.length = length;
		}
		@Override public void invoke(ForthVM vm) {
			vm.pushPs((short)0);
			for( ; vm.fetch(ForthVM.PC_REG) != 0; vm.step() );
		}
	}
	static class InstructionWord implements Word {
		final short instruction;
		public InstructionWord( short instruction ) {
			this.instruction = instruction;
		}
		@Override public void invoke(ForthVM vm) {
			//vm.doInstruction(instruction);
		}
	}
	
	protected final HashMap<String,Word> words = new HashMap<>();
	protected final ForthVM vm;
	protected boolean quitting = false;
	
	public REPL() {
		short[] program = new short[65536];
		program[ForthVM.PC_REG] = program[ForthVM.PC_RESET_REG] = 1024;
		program[ForthVM.DS_START_REG] = ForthVM.DS_START_DEFAULT;
		program[ForthVM.DS_END_REG]   = ForthVM.DS_END_DEFAULT;
		program[ForthVM.DS_REG]       = ForthVM.DS_END_DEFAULT;
		program[ForthVM.PS_START_REG] = ForthVM.PS_START_DEFAULT;
		program[ForthVM.PS_END_REG]   = ForthVM.PS_END_DEFAULT;
		program[ForthVM.PS_REG]       = ForthVM.PS_END_DEFAULT;
		
		vm = new ForthVM(program);
		
		words.put("ps", new Word() {
			// Print stack
			@Override public void invoke(ForthVM vm) {
				int ds = vm.fetchUint(ForthVM.DS_REG);
				System.out.println(String.format("# Stack start=%04x end=%04x top=%04x",
					vm.fetch(ForthVM.DS_START_REG),
					vm.fetch(ForthVM.DS_END_REG),
					vm.fetch(ForthVM.DS_REG)
				));
				for( int i = vm.fetchUint(ForthVM.DS_END_REG)-1; i >= ds; --i ) {
					short v = vm.fetch(i);
					System.out.print(String.format("0x%04x ", v));
				}
				System.out.println();
			}
		});
		Word quit = new Word() {
			@Override public void invoke(ForthVM vm) {
				System.err.println("Goodbye.");
				quitting = true;
			}
		};
		words.put("q", quit);
		words.put("quit", quit);
		words.put("exit", quit);
		words.put("bye", quit);
	}
	
	static final Pattern DECIMAL = Pattern.compile("^[0-9]+$"); 
	static final Pattern HEX = Pattern.compile("^0x([0-9da-f]+)$", Pattern.CASE_INSENSITIVE);
	
	@Override public void handle(Token v) throws ScriptError {
		switch( v.type ) {
		case BAREWORD: case SINGLE_QUOTED_STRING:
			Word w = words.get(v.text);
			if( w == null ) {
				Matcher m;
				if( (m = DECIMAL.matcher(v.text)).matches() ) {
					vm.push(Short.parseShort(v.text));
				} else if( (m = HEX.matcher(v.text)).matches() ) {
					vm.push(Short.parseShort(m.group(1), 16));
				} else {
					throw new ScriptError("Unrecognized word: '"+v.text+"'", v);
				}
			} else {
				w.invoke(vm);
			}
			return;
		case DOUBLE_QUOTED_STRING:
			throw new ScriptError("Literal strings not supported!", v);
		}
	}
	
	public static void main( String[] args ) {
		REPL repl = new REPL();
		Tokenizer tokenizer = new Tokenizer("stdin", 1, 1, 8, repl);
		InputStreamReader r = new InputStreamReader(System.in);
		int c;
		try {
			while( !repl.quitting && (c = r.read()) != -1 ) {
				tokenizer.handle((char)c);
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
