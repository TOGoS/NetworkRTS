package togos.networkrts.awt;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import togos.service.InterruptableSingleThreadedService;
import togos.service.Service;
import togos.service.ServiceManager;

public class Apallit extends Applet
{
	private static final long serialVersionUID = 1L;
	
	String title;
	ServiceManager sman = new ServiceManager();
	
	public Apallit( String title ) {
		setTitle( title );
		setPreferredSize( 640, 480 );
	}
	
	public Apallit() {
		this("Some Apallit");
	}
	
	/**
	 * Creates this apallit with a single space-filling component. 
	 */
	public Apallit( String title, Component c ) {
		this( title );
		fillWith( c );
	}
	
	public void setTitle( String title ) {
		this.title = title;
	}
	
	/**
	 * Adds the given child component in such a way that it will
	 * fill the entire applet no matter how it is resized.
	 */
	public void fillWith( Component c ) {
		removeAll();
		add( c );
		// The default layout manager for applets seems to be something else.
		setLayout(new GridLayout());
		validate();
	}
	
	public void fillWith( final TimestampedPaintable paintable, final long repaintInterval ) {
		final DoubleBufferedCanvas dbc = new DoubleBufferedCanvas() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void _paint(Graphics g) {
				paintable.paint(System.currentTimeMillis(), getWidth(), getHeight(), (Graphics2D)g);
			}
		};
		
		if( repaintInterval != -1 ) {
			addService( new InterruptableSingleThreadedService() {
				@Override
				protected void _run() throws InterruptedException {
					while( !Thread.interrupted() ) {
						Thread.sleep( repaintInterval );
						dbc.repaint();
					}
				}
			});
		}
		
		fillWith(dbc);
	}
	
	public void setPreferredSize( int w, int h ) {
		setPreferredSize( new Dimension(w,h) );
	}
	
	public void addService( Service s ) {
		sman.add( s );
	}
	
	/**
	 * Call after adding components to put the given key listener
	 * on the applet *and* on any current sub-components.
	 */
	public void addKeyListenerEverywhere( KeyListener l ) {
		addKeyListener(l);
		for( Component c : getComponents() ) {
			c.addKeyListener(l);
		}
	}
	
	@Override
	public void init() {
		sman.start();
	}
	
	@Override
	public void destroy() {
		sman.halt();
	}
	
	public void runWindowed() {
		final Frame f = new Frame(title);
		f.add(this);
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				f.dispose();
				destroy();
			}
		});
		f.setVisible(true);
		init();
	}
}
