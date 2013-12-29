package togos.blob;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileInputStreamable implements InputStreamable
{
	protected final File file;
	public FileInputStreamable( File f ) {
		this.file = f;
	}
	
	@Override public InputStream openInputStream() throws IOException {
		return new FileInputStream(file);
	}
}
