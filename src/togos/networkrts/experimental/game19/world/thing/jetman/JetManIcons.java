package togos.networkrts.experimental.game19.world.thing.jetman;

import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.ImageHandle;

public class JetManIcons
{
	public Icon[] walking;
	public Icon fall0, flyUp, flyForward, jetUp, jetForward, jetUpAndForward;
	
	public Icon leg1, leg2, torso, jetpack, head;
	
	static final Pattern PIXSIZEPAT = Pattern.compile("-(\\d+)x(\\d+)");
	
	static Icon loadIcon2(ResourceContext rc, String name) throws IOException {
		Matcher m = PIXSIZEPAT.matcher(name);
		if( m.find() ) {
			int w = Integer.parseInt(m.group(1));
			int h = Integer.parseInt(m.group(2));
			ImageHandle ih = rc.storeImageHandle(new File("tile-images/JetMan/"+name));
			return new Icon(ih, -w/32f, -h/32f, Icon.DEFAULT_NONTILE_FRONT_Z, w/16f, h/16f ); 
		} else {
			throw new RuntimeException("Couldn't figure dimensions from filename '"+name+"'");
		}
	}

	static Icon loadIcon(ResourceContext rc, String name) throws IOException {
		ImageHandle ih = rc.storeImageHandle(new File("tile-images/JetMan/"+name));
		return new Icon(ih, -0.5f, -0.5f, Icon.DEFAULT_NONTILE_FRONT_Z, 1, 1 ); 
	}
	
	static final WeakHashMap<Icon,Icon> flipped = new WeakHashMap<Icon,Icon>();
	
	protected static final Icon flipIcon( Icon i ) {
		return new Icon(i.image, i.imageX, i.imageY, i.imageZ, -i.imageWidth, i.imageHeight);
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
		JetManIcons icons = new JetManIcons();
		icons.walking = new Icon[] {
			loadIcon(rc, "Walk0.png"),
			loadIcon(rc, "Walk1.png"),
			loadIcon(rc, "Walk2.png"),
			loadIcon(rc, "Walk3.png"),
			loadIcon(rc, "Walk4.png"),
			loadIcon(rc, "Walk5.png")
		};
		icons.fall0 = JetManIcons.loadIcon(rc, "Fall0.png");
		icons.flyUp = JetManIcons.loadIcon(rc, "FlyUp.png");
		icons.flyForward = JetManIcons.loadIcon(rc, "FlyForward.png");
		icons.jetUp = JetManIcons.loadIcon(rc, "JetUp.png");
		icons.jetForward = JetManIcons.loadIcon(rc, "JetForward.png");
		icons.jetUpAndForward = JetManIcons.loadIcon(rc, "JetUpAndForward.png");
		
		icons.leg1 = loadIcon2(rc, "Leg1-4x5.png");
		icons.leg2 = loadIcon2(rc, "Leg2-5x4.png");
		icons.head = loadIcon2(rc, "HelmetForward-6x5.png");
		icons.torso = loadIcon2(rc, "Torso-4x5.png");
		icons.jetpack = loadIcon2(rc, "JetPack-3x4.png");
		
		return icons;
	}
}
