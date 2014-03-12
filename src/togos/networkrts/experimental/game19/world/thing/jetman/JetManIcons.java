package togos.networkrts.experimental.game19.world.thing.jetman;

import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.world.NonTile.Icon;

public class JetManIcons
{
	public Icon[] walking;
	public Icon fall0, flyUp, flyForward, jetUp, jetForward, jetUpAndForward;
	
	static Icon loadIcon(ResourceContext rc, String name) throws IOException {
		ImageHandle ih = rc.storeImageHandle(new File("tile-images/JetMan/"+name));
		return new Icon(ih, -0.5f, -0.5f, 1, 1 ); 
	}
	
	static final WeakHashMap<Icon,Icon> flipped = new WeakHashMap<Icon,Icon>();
	
	protected static final Icon flipIcon( Icon i ) {
		return new Icon(i.image, i.imageX, i.imageY, -i.imageWidth, i.imageHeight);
	}
	
	public static final Icon flipped( Icon i ) {
		Icon f = flipped.get(i);
		if( f == null ) {
			f = flipIcon(i);
			flipped.put(i, f);
		}
		return f;
	}

	public static JetManIcons load(ResourceContext rc) throws IOException {
		JetManIcons jetManImages = new JetManIcons();
		jetManImages.walking = new Icon[] {
			loadIcon(rc, "Walk0.png"),
			loadIcon(rc, "Walk1.png"),
			loadIcon(rc, "Walk2.png"),
			loadIcon(rc, "Walk3.png"),
			loadIcon(rc, "Walk4.png"),
			loadIcon(rc, "Walk5.png")
		};
		jetManImages.fall0 = JetManIcons.loadIcon(rc, "Fall0.png");
		jetManImages.flyUp = JetManIcons.loadIcon(rc, "FlyUp.png");
		jetManImages.flyForward = JetManIcons.loadIcon(rc, "FlyForward.png");
		jetManImages.jetUp = JetManIcons.loadIcon(rc, "JetUp.png");
		jetManImages.jetForward = JetManIcons.loadIcon(rc, "JetForward.png");
		jetManImages.jetUpAndForward = JetManIcons.loadIcon(rc, "JetUpAndForward.png");
		return jetManImages;
	}
}
