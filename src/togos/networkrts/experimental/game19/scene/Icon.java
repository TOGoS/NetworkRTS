package togos.networkrts.experimental.game19.scene;

import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.SHA1ObjectReference;
import togos.networkrts.cereal.StandardValueOps;
import togos.networkrts.experimental.game19.io.CerealWorldIO;
import togos.networkrts.experimental.game19.io.WorldObjectCCCodec;
import togos.networkrts.util.ResourceNotFound;

public class Icon
{
	public static final WorldObjectCCCodec<Icon> CCC = new WorldObjectCCCodec<Icon>() {
		@Override public Class<Icon> getEncodableClass() { return Icon.class; }
		
		// image blob reference, top left X, top left Y, front Z, width, height -> SimpleIcon
		
		@Override public void encode(
			Icon icon, byte[] constructorPrefix, OutputStream os, CerealWorldIO cwio
		) throws IOException {
			try {
				StandardValueOps.writeSha1ObjectReference(SHA1ObjectReference.parse(icon.imageUri), os);
			} catch( InvalidEncoding e ) {
				throw new UnsupportedOperationException(
					"Can't represent image URI as SHA-1 ref: "+icon.imageUri, e);
			}
			StandardValueOps.writeNumberCompact(icon.imageX, os);
			StandardValueOps.writeNumberCompact(icon.imageY, os);
			StandardValueOps.writeNumberCompact(icon.imageZ, os);
			StandardValueOps.writeNumberCompact(icon.imageWidth, os);
			StandardValueOps.writeNumberCompact(icon.imageHeight, os);
			os.write(constructorPrefix);
		}

		@Override public int decode(
			byte[] data, int offset, DecodeState ds, CerealDecoder context
		) throws InvalidEncoding, ResourceNotFound {
			/*
			float h = context.removeStackItem(ds, -1, Number.class).floatValue();
			float w = context.removeStackItem(ds, -1, Number.class).floatValue();
			float z = context.removeStackItem(ds, -1, Number.class).floatValue();
			float y = context.removeStackItem(ds, -1, Number.class).floatValue();
			float x = context.removeStackItem(ds, -1, Number.class).floatValue();
			HasURI imageRef = context.removeStackItem(ds, -1, HasURI.class);
			*/
			throw new UnsupportedOperationException(
				"Can't instantiate icon because of the way the image handle class is architected.");
		}
	};

	
	public static final Float DEFAULT_NONTILE_FRONT_Z = 0.1f;
	public static final Float DEFAULT_BLOCK_FRONT_Z = 0.5f;
	
	public final String imageUri;
	public final float imageX, imageY, imageZ, imageWidth, imageHeight;
	public Icon( String imageUri, float x, float y, float z, float w, float h ) {
		this.imageUri = imageUri;
		this.imageZ = z;
		this.imageX = x; this.imageWidth  = w;
		this.imageY = y; this.imageHeight = h;
	}
}
