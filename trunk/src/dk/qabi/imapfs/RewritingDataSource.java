package dk.qabi.imapfs;

import javax.activation.DataSource;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

/**
 * This activation framework DataSource implementation is capable of rewriting all or part of existing data. 
 */
public class RewritingDataSource implements DataSource {
  private ByteBuffer buf;
  private long offset;
  private DataHandler original;

  public RewritingDataSource(DataHandler original, ByteBuffer buf, long offset) throws MessagingException, IOException {
    this.original = original;
    this.buf = buf;
    this.offset = offset;
  }

  public InputStream getInputStream() throws IOException {
    if (offset > 0) {
      // Read first N bytes from original input
      return new ConcatInputStream(original.getInputStream(), offset, buf.array());
    } else {
      // Read all data directly from buffer
      return new ByteArrayInputStream(buf.array());
    }
  }

  public OutputStream getOutputStream() throws IOException {
    throw new IOException("Direct writing not supportd");
  }

  public String getContentType() {
    return original.getContentType();
  }

  public String getName() {
    return original.getName();
  }

  private class ConcatInputStream extends InputStream {
    private InputStream in;
    private int offset;
    private ByteArrayInputStream bytes;
    private int currentPos;
    private int bytesLength;

    public ConcatInputStream(InputStream in, long offset, byte[] bytes) {
      this.in = in;
      this.offset = (int) offset;
      this.bytes = new ByteArrayInputStream(bytes);
      this.bytesLength = bytes.length;
    }

    public int read() throws IOException {
      if (currentPos++ < offset) {
        return in.read();
      } else {
        return bytes.read();
      }
    }

    public long skip(long n) throws IOException {
      if (currentPos < offset) {
        long skipped = in.skip(n);
        long rest = n - skipped;

        if (rest > 0)
          skipped += bytes.skip(rest);

        return skipped;
      } else {
        return bytes.skip(n);
      }
    }

    public int read(byte b[], int off, int len) throws IOException {

      // Read from original input stream until offset is reached, or len is read
      int readCount = 0;
      do {
        readCount += in.read(b, off, offset-off);
        off += readCount;
        currentPos += readCount;
      } while (currentPos < offset && readCount < len);

      // Then read the rest from the new bytes stream
      if (readCount < len) {
        do {
          readCount += bytes.read(b, off, len-readCount);
          off += readCount;
          currentPos += readCount;
        } while (currentPos < offset && readCount < len);
      }

      return readCount;
    }

    public boolean markSupported() {
      return false;
    }

    public int available() throws IOException {
      return (offset-currentPos) + bytesLength;
    }

  }
}
