package togos.networkrts.experimental.forth;

import java.util.ArrayList;
import java.util.List;

public class State
{
	static class ReturnPosition {
		byte[] program;
		int programPosition;
		ReturnPosition returnPosition;
	}
	
	byte[] program;
	int programPosition;
	ReturnPosition returnPosition;
	List stack = new ArrayList();
}
