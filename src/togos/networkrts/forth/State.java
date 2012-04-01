package togos.networkrts.forth;

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
	List data = new ArrayList();
}
