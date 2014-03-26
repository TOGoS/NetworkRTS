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
import togos.networkrts.util.ResourceNotFound;
import togos.networkrts.util.Storer;

public class BlobRepository
{
	protected File repoDir;
	public BlobRepository( File repoDir ) {
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
	static final Pattern SHA1_PATTERN = Pattern.compile("sha1:([A-Z0-9]{32})");
	
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
	
	// TODO: replace with things that implement both getter and storer 
	
	public Getter<InputStreamable> toBlobGetter() {
		return new Getter<InputStreamable>() {
			@Override
			public InputStreamable get(String uri) throws ResourceNotFound {
				File f = BlobRepository.this.get(uri);
				if( f == null ) throw new ResourceNotFound(uri);
				return new FileInputStreamable(f);
			}
		};
	}
	
	public Storer<byte[]> toChunkStorer() {
		return new Storer<byte[]>() {
			@Override public String store(byte[] v) {
				try {
					return BlobRepository.this.store(v, 0, v.length);
				} catch( IOException e ) {
					// TODO: could emit a warning and return calculated ID anyway
					// System.err.println("Warning: failed to store a chunk due to "+e.getMessage());
					throw new RuntimeException(e);
				}
			}
		};
	}
}
