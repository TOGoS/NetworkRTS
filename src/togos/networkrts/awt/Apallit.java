package togos.networkrts.awt;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import togos.service.Service;
import togos.service.ServiceManager;

public class Apallit extends Applet
{
	private static final long serialVersionUID = 1L;
	
	String title;
	ServiceManager sman = new ServiceManager();
	
	public Apallit( String title ) {
		this.title = title;
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
	
	/**
	 * Adds the given child component in such a way that it will
	 * fill the entire applet no matter how it is resized.
	 */
	public void fillWith( Component c ) {
		removeAll();
		add( c );
		// The default layout manager for applets seems to be something else.
		setLayout(new GridLayout());
	}
	
	public void addService( Service s ) {
		sman.add( s );
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
		init();
		f.add(this);
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				f.dispose();
				destroy();
			}
		});
		f.setVisible(true);
	}
}
