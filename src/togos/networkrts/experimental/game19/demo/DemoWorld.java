package togos.networkrts.experimental.game19.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.Layer.LayerLink;
import togos.networkrts.experimental.game19.util.ResourceContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.QuadRSTNode;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.RSTNodeUpdater;
import togos.networkrts.experimental.game19.world.RSTUtil;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.beh.BoringBlockInternals;
import togos.networkrts.experimental.game19.world.gen.SolidNodeFiller;
import togos.networkrts.experimental.game19.world.thing.GenericPhysicalNonTileInternals;
import togos.networkrts.experimental.game19.world.thing.Substances;
import togos.networkrts.experimental.game19.world.thing.pickup.SubstanceContainerInternals;
import togos.networkrts.experimental.game19.world.thing.pickup.SubstanceContainerType;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.packet19.MalformedDataException;
import togos.networkrts.experimental.shape.TCircle;
import togos.networkrts.experimental.shape.TRectangle;
import togos.networkrts.util.SoftResourceHandle;

public class DemoWorld
{
	static String numRe = "([+-]?\\d+(?:.\\d+)?)"; 
	
	static Pattern iconPattern = Pattern.compile(
		numRe+","+numRe+","+numRe+","+numRe+","+numRe+"\\s+(\\S+)"
	);
	
	protected static Icon loadIcon( String name, ResourceContext rc ) throws IOException {
		// For now assume name is a filename of a bufferedimage
		Matcher m;
		float x, y, z, w, h;
		String filename;
		if( (m = iconPattern.matcher(name)).matches() ) {
			x = Float.parseFloat(m.group(1));
			y = Float.parseFloat(m.group(2));
			z = Float.parseFloat(m.group(3));
			w = Float.parseFloat(m.group(4));
			h = Float.parseFloat(m.group(5));
			filename = m.group(6);
		} else {
			x = y = -0.5f;
			z = 0.5f;
			w = h = 1;
			filename = name;
		}
		String imageUri = rc.storeFile(new File(filename));
		return new Icon(imageUri, x, y, z, w, h);
	}
	
	public static Block loadBlockDef( BufferedReader r, ResourceContext rc ) throws IOException {
		long flags = 0;
		Icon icon = null;
		String line;
		int colonIdx;
		boolean headerRead = false;
		while( (line = r.readLine()) != null ) {
			line = line.trim();
			if( line.equals("#TEXT-BLOCK") ) headerRead = true;
			if( line.startsWith("#") ) continue;
			
			if( !headerRead ) throw new MalformedDataException("Unrecognized block format; no '#TEXT-BLOCK' found");
			
			if( "opaque".equals(line) ) {
				flags |= Block.FLAG_OPAQUE;
			} else if( "solid".equals(line) ) {
				flags |= Block.FLAG_SOLID;
			} else if( "spikey".equals(line) ) {
				flags |= Block.FLAG_SPIKEY;
			} else if( (colonIdx = line.indexOf(":")) != -1 ) {
				String key = line.substring(0, colonIdx).trim();
				String val = line.substring(colonIdx+1).trim();
				if( "icon".equals(key) ) {
					icon = loadIcon(val, rc);
				}
			}
		}
		
		long bitAddress = BitAddresses.TYPE_BLOCK;
		if( (flags & Block.FLAG_SOLID) != 0 ) {
			bitAddress |= BitAddresses.PHYSINTERACT;
		}
		
		return new Block(bitAddress, flags, icon, BoringBlockInternals.INSTANCE);
	}
	
	public static Block loadBlock( File f, ResourceContext rc ) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			return loadBlockDef(br, rc);
		} finally {
			br.close();
		}
	}
	
	protected static Icon loadBlockIcon(ResourceContext rc, String filename, float frontZ) throws IOException {
		String urn = rc.storeFile(new File(filename));
		return new Icon(urn, -0.5f, -0.5f, frontZ, 1f, 1f);
	}
	
	protected static Icon loadIcon(ResourceContext rc, String filename, float size) throws IOException {
		String urn = rc.storeFile(new File(filename));
		return new Icon(urn, -size/2, -size/2, size/2, size, size);
	}
	
	static class DemoBlocks {
		public Block bricks, dirt, grass, tree, spikes;
		
		static DemoBlocks load( ResourceContext rc ) throws IOException {
			DemoBlocks db = new DemoBlocks();
			db.bricks = loadBlock( new File("things/blocks/gray-stone-bricks0.block"), rc );
			db.dirt = loadBlock( new File("things/blocks/dirt0.block"), rc );
			db.grass = loadBlock( new File("things/blocks/grass0.block"), rc );
			db.tree = loadBlock( new File("things/blocks/small-tree0.block"), rc );
			db.spikes = loadBlock( new File("things/blocks/big-gray-spikes0.block"), rc );
			return db;
		}
	}
	
	protected static GenericPhysicalNonTileInternals getCrateInternals( ResourceContext rc ) throws IOException {
		Icon crateIcon = loadIcon(rc, "tile-images/crate0.png", 1);
		return new GenericPhysicalNonTileInternals(crateIcon, new AABB(
			crateIcon.imageX, crateIcon.imageY, crateIcon.imageX,
			crateIcon.imageX + crateIcon.imageWidth,
			crateIcon.imageY + crateIcon.imageHeight,
			crateIcon.imageX + crateIcon.imageWidth
		));
	}
	
	protected static SubstanceContainerInternals getFuelCanInternals( ResourceContext rc ) throws IOException {
		Icon[] fuelCanIcons = new Icon[4];
		for( int i=0; i<fuelCanIcons.length; ++i ) {
			fuelCanIcons[i] = loadIcon(rc, "tile-images/FuelCan/FuelCan"+i+".png", 0.5f);
		}
		AABB fuelCanAabb = new AABB(-0.125, -0.125, -0.125, 0.25, 0.25, 0.25);
		SubstanceContainerType fuelCanType = new SubstanceContainerType(
			"Fuel can", fuelCanIcons, fuelCanAabb, 1, 0.01
		);
		return SubstanceContainerInternals.filled(fuelCanType, Substances.KEROSENE);
	}
	
	public static World initLittleWorld( ResourceContext rc ) throws IOException {
		final DemoBlocks blocks = DemoBlocks.load(rc);
		
		final SubstanceContainerInternals fuelCanInternals = getFuelCanInternals(rc);
		
		int worldSizePower = 24;
		int worldDataOrigin = -(1<<(worldSizePower-1));
		
		RSTNode n = QuadRSTNode.createHomogeneous(blocks.bricks.stack, worldSizePower);
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( 3, 0, 32, 32 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( 3, 31, 32, 1 ), new SolidNodeFiller( blocks.spikes.stack ));
		
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 0, 20, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 4, 20, 1 ), new SolidNodeFiller( blocks.dirt.stack ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 3, 20, 1 ), new RSTNodeUpdater() {
			final RSTNode treeAndGrass = BlockStackRSTNode.create(new Block[] { blocks.tree, blocks.grass } );
			final Random r = new Random();
			@Override public RSTNode update(RSTNode oldNode, int x, int y, int sizePower) {
				double v = r.nextDouble();
				return v < 0.1 ? blocks.bricks.stack : v < 0.25 ? treeAndGrass : blocks.grass.stack;
			}
		});
		
		EntitySpatialTreeIndex<NonTile> nonTiles = new EntitySpatialTreeIndex<NonTile>();
		
		nonTiles = nonTiles.with( new BlargNonTile(0, 0, -5, 0, 0, 0, fuelCanInternals) );
		
		return new World(0, n, worldSizePower, nonTiles,
			new LayerLink(true, new SoftResourceHandle<Layer>("urn:sha1:blah"), 0, 0, 0, 0xFF001122)
		);
	}
	
	public static World initWorld( ResourceContext rc ) throws IOException {
		final DemoBlocks blocks = DemoBlocks.load(rc);
		
		final GenericPhysicalNonTileInternals crateInternals = getCrateInternals(rc);
		final SubstanceContainerInternals fuelCanInternals = getFuelCanInternals(rc);
		
		int worldSizePower = 24;
		int worldDataOrigin = -(1<<(worldSizePower-1));
		
		RSTNode n = QuadRSTNode.createHomogeneous(blocks.bricks.stack, worldSizePower);
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( 3, 0, 32, 32 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( 3, 31, 32, 1 ), new SolidNodeFiller( blocks.spikes.stack ));
		
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 0, 20, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 4, 20, 1 ), new SolidNodeFiller( blocks.dirt.stack ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TRectangle( -24, 3, 20, 1 ), new RSTNodeUpdater() {
			final RSTNode treeAndGrass = BlockStackRSTNode.create(new Block[] { blocks.tree, blocks.grass } );
			final Random r = new Random();
			@Override public RSTNode update(RSTNode oldNode, int x, int y, int sizePower) {
				double v = r.nextDouble();
				return v < 0.1 ? blocks.bricks.stack : v < 0.25 ? treeAndGrass : blocks.grass.stack;
			}
		});
		
		EntitySpatialTreeIndex<NonTile> nonTiles = new EntitySpatialTreeIndex<NonTile>();
		
		Random r = new Random();
		for( int i=0; i<10; ++i ) {
			double sx = 0, sy = 0, dir = 0, rad = 8;
			while( rad > 1 ) {
				n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( sx, sy, rad ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
				sx += Math.cos(dir);
				sy += Math.sin(dir);
				dir += r.nextGaussian() * 0.1;
				rad *= (0.996 + r.nextGaussian()*0.02);
			}
			//nonTiles = nonTiles.with( new BlargNonTile(0, 0, sx, sy, 0, 0, fuelCanInternals) );
		}
		
		for( int i=0; i<20; ++i ) {
			double sx = r.nextGaussian() * 10;
			double sy = r.nextGaussian() * 10;
			//nonTiles = nonTiles.with( new BlargNonTile(0, 0, sx, sy, 0, 0, crateInternals) );
		}
		
		nonTiles = nonTiles.with( new BlargNonTile(0, 0, -5, 0, 0, 0, fuelCanInternals) );
		
		return new World(0, n, worldSizePower, nonTiles,
			new LayerLink(true, new SoftResourceHandle<Layer>("urn:sha1:blah"), 0, 0, 0, 0xFF001122)
		);
	}
}
