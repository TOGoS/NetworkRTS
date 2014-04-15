package togos.networkrts.repo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.blob.FileInputStreamable;
import togos.blob.InputStreamable;
import togos.networkrts.util.Getter;
import togos.networkrts.util.Repository;
import togos.networkrts.util.ResourceNotFound;

public class BitprintFileRepository
{
	protected File repoDir;
	public BitprintFileRepository( File repoDir ) {
		this.repoDir = repoDir;
	}
	
	public String store( InputStream is ) throws IOException {
		BitprintDigest dig = new BitprintDigest();
		File tempFile = tempFile();
		FileOutputStream fos = new FileOutputStream(tempFile);
		try {
			byte[] buffer = new byte[65536];
			int r;
			while( (r = is.read(buffer)) > 0 ) {
				dig.update(buffer, 0, r);
				fos.write(buffer, 0, r);
			}
		} finally {
			is.close();
			fos.close();
		}
		
		String bpBase32 = BitprintDigest.format(dig.digest());
		
		File dest = new File(repoDir, "data/auto/"+bpBase32.substring(0,2)+"/"+bpBase32.substring(0,32));
		if( dest.exists() ) {
			tempFile.delete();
		} else {
			mkParentDirs(dest);
			tempFile.renameTo(dest);
		}
		
		return "urn:bitprint:"+bpBase32;
	}
	
	public String store( File f, boolean removeSource ) throws IOException {
		BitprintDigest dig = new BitprintDigest();
		FileInputStream fis = new FileInputStream(f);
		try {
			byte[] buffer = new byte[65536];
			int r;
			while( (r = fis.read(buffer)) > 0 ) {
				dig.update(buffer, 0, r);
			}
		} finally {
			fis.close();
		}
		
		String bpBase32 = BitprintDigest.format(dig.digest());
		
		File dest = new File(repoDir, "data/auto/"+bpBase32.substring(0,2)+"/"+bpBase32.substring(0,32));
		if( dest.exists() ) {
			if( removeSource ) f.delete();
		} else {
			mkParentDirs(dest);
			if( removeSource && f.renameTo(dest) ) {
			} else {
				// Then we shall have to do some things again...
				try {
					fis = new FileInputStream(f);
					return store( fis );
				} finally {
					fis.close();
				}
			}
		}
		
		return "urn:bitprint:"+bpBase32;
	}
	
	public File tempFile() throws IOException {
		if( !repoDir.isDirectory() ) repoDir.mkdirs();
		File temp = File.createTempFile(".temp", "", repoDir);
		return temp;
	}
	
	protected void mkParentDirs( File f ) {
		File parent = f.getParentFile();
		if( parent == null || parent.isDirectory() ) return;
		parent.mkdirs();
	}
	
	public String store(byte[] data, int offset, int length) throws IOException {
		File temp = tempFile();
		mkParentDirs(temp);
		FileOutputStream fos = new FileOutputStream(temp);
		try {
			fos.write(data, offset, length);
		} finally {
			fos.close();
		}
		return store(temp, true);
	}
	
	static final Pattern BITPRINT_PATTERN = Pattern.compile("urn:bitprint:([A-Z0-9]{32})\\.([A-Z0-9]{39})");
	static final Pattern SHA1_PATTERN = Pattern.compile("urn:sha1:([A-Z0-9]{32})");
	
	public File get(String urn) {
		Matcher m;
		String sha1Base32;
		if( (m = BITPRINT_PATTERN.matcher(urn)).matches() || (m = SHA1_PATTERN.matcher(urn)).matches() ) {
			sha1Base32 = m.group(1);
		} else {
			return null;
		}
		
		String psp = sha1Base32.substring(0,2)+"/"+sha1Base32;
		
		File[] sectors = new File(repoDir, "data").listFiles();
		if( sectors == null ) return null;
		for( File sector : sectors ) {
			File f = new File(sector, psp);
			if( f.exists() ) return f;
		}
		
		return null;
	}
	
	class ByteArrayRepository implements Repository<byte[]> {
		@Override public String store(byte[] v) {
			try {
				return BitprintFileRepository.this.store(v, 0, v.length);
			} catch( IOException e ) {
				// TODO: could emit a warning and return calculated ID anyway
				// System.err.println("Warning: failed to store a chunk due to "+e.getMessage());
				throw new RuntimeException(e);
			}
		}
		
		@Override public byte[] get( String uri ) throws ResourceNotFound {
			File f = BitprintFileRepository.this.get(uri);
			if( f == null ) throw new ResourceNotFound(uri);
			long fileLen = f.length();
			if( fileLen > (2<<24) ) {
				// It was found; we just can't load it!
				throw new RuntimeException(f+" cannot be read into a byte array because it is way too big!");
			}
			byte[] buffer = new byte[(int)fileLen];
			try {
				FileInputStream fis = new FileInputStream(f);
				int i = 0;
				while( i < buffer.length ) {
					int r = fis.read(buffer, i, buffer.length-i);
					if( r == -1 ) {
						throw new ResourceNotFound(
							"Failed to read as many bytes as expected from "+f+" while loading "+uri+".\n"+
							"maybe it changed size while we were reading?");
					}
					i += r;
				}
			} catch( IOException e ) {
				throw new ResourceNotFound("Could not load resource '"+uri+"' due to I/O error while reading "+f, e);
			}
			return buffer;
		}
	};
	
	protected final ByteArrayRepository byteArrayRepo = new ByteArrayRepository();
	
	// TODO: replace with things that implement both getter and storer 
	
	public Repository<byte[]> toByteArrayRepository() { return byteArrayRepo; }
	
	public Getter<InputStreamable> toBlobGetter() {
		return new Getter<InputStreamable>() {
			@Override
			public InputStreamable get(String uri) throws ResourceNotFound {
				File f = BitprintFileRepository.this.get(uri);
				if( f == null ) throw new ResourceNotFound(uri);
				return new FileInputStreamable(f);
			}
		};
	}
}
