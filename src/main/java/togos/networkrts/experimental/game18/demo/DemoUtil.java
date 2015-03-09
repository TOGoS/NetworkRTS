package togos.networkrts.experimental.game18.demo;

import java.awt.Dimension;
import java.io.File;

import javax.swing.JFrame;

import togos.networkrts.experimental.game18.StorageContext;
import togos.networkrts.experimental.qt2drender.VizState;
import togos.networkrts.experimental.qt2drender.demo.NetRenderDemo.RenderContext;
import togos.networkrts.experimental.qt2drender.demo.NetRenderDemo.VizStateCanvas;
import togos.networkrts.repo.BitprintFileRepository;

public class DemoUtil
{
	public static final BitprintFileRepository DEFAULT_BLOB_REPO = new BitprintFileRepository(new File(".ccouch"));
	public static final StorageContext DEFAULT_STORAGE_CONTEXT = new StorageContext(DEFAULT_BLOB_REPO);
	
	public static void showVizStateWindow( VizState vs ) {
		final JFrame f = new JFrame("VizState view");
		final VizStateCanvas vsc = new VizStateCanvas(new RenderContext(DEFAULT_BLOB_REPO.toBlobGetter()));
		vsc.setPreferredSize(new Dimension(800,600));
		f.add(vsc);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		vsc.setState(vs);
	}
}
