package togos.networkrts.experimental.entree2;

import java.util.Collections;


public class SetEntreeTest extends EntreeTest<SetEntree>
{
	public void setUp() {
		entree = new SetEntree<SimpleWorldObject>(Collections.EMPTY_SET);
	}
}
