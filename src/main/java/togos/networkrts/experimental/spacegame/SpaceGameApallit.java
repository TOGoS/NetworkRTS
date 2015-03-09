package togos.networkrts.experimental.spacegame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collections;

import togos.networkrts.awt.Apallit;
import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;
import togos.networkrts.experimental.spacegame.SpaceObject.SpaceObjectPosition;
import togos.service.InterruptableSingleThreadedService;

public class SpaceGameApallit extends Apallit {
	private static final long serialVersionUID = 1602149203058121266L;

	static class SpaceWorldPaintable implements TimestampedPaintable {
		SpaceWorld w;
		double cx, cy;
		double zoom = 1;
		
		SpaceWorldPaintable( SpaceWorld w ) {
			this.w = w;
		}
		
		protected void drawObject(long time, SpaceObject o, double x, double y, double a, Graphics2D g2d, SpaceObjectPosition pos) {
			double sRad = zoom*o.solidRadius;
			g2d.setColor(o.solidColor);
			g2d.fillOval((int)(x-sRad), (int)(y-sRad), (int)(sRad*2), (int)(sRad*2));
			
			double sin = Math.sin(a);
			double cos = Math.cos(a);
			
			for( SpaceObject subObj : o.subObjects ) {
				subObj.position.getPosition(time, pos);
				
				double subX = x + zoom*pos.x * cos - zoom*pos.y * sin;
				double subY = y + zoom*pos.x * sin + zoom*pos.y * cos;
				double subAng = a + pos.angle;
				
				drawObject(time, subObj, subX, subY, subAng, g2d, pos );
			}
		}
		
		@Override public void paint(long time, int width, int height, Graphics2D g2d) {
			SpaceObjectPosition pos = new SpaceObjectPosition();
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, width, height);
			for( SpaceObject o : w.objects ) {
				o.position.getPosition(time, pos);
				drawObject(time, o, pos.x + width/2 - cx*zoom, pos.y + height/2 - cy*zoom, pos.angle, g2d, pos);
			}
		}
	}
	
	protected QueuelessRealTimeEventSource<SpaceWorld.SpaceWorldEvent> es =
		new QueuelessRealTimeEventSource<SpaceWorld.SpaceWorldEvent>();
	protected SpaceWorld w = new SpaceWorld();
	
	public void init() {
		w.objects = new SpaceObject[] {
			new SpaceObject(
				1,
				new SpaceObject.AccellerativePositionFunction(0, 0, 0, 0, 0, 0, 0, 0, 2, 0),
				new SpaceObject[] {
					new SpaceObject(
						2,
						new SpaceObject.AccellerativePositionFunction(0, 20, 0, 0, 20, 0, 0, 0, 0.1, 0),
						new SpaceObject[] {},
						Color.GREEN, 4,
						Long.MAX_VALUE, Collections.EMPTY_MAP,
						SpaceObject.NoBehavior.INSTANCE
					)
				},
				Color.BLUE, 10,
				Long.MAX_VALUE, Collections.EMPTY_MAP,
				SpaceObject.NoBehavior.INSTANCE
			)
		};
		SpaceWorldPaintable p = new SpaceWorldPaintable(w);
		this.fillWith(p, 30);
		addService( new InterruptableSingleThreadedService() {
			@Override
			protected void _run() throws InterruptedException {
				try {
					EventLoop.run(es, w);
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
				} catch( Exception e ) {
					throw new RuntimeException(e);
				}
			}
		});
		super.init();
	}
}
