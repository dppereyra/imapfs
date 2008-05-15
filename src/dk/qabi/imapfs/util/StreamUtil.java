package dk.qabi.imapfs.util;

import java.io.*;

public class StreamUtil {
  /**
   * Pumps bytes from the given input stream into the given output stream
   * @param bufferSize the size of the buffer to use, given in bytes
   */
  public static void pump(InputStream in, OutputStream out, int bufferSize) throws IOException {
    byte buffer[] = new byte[bufferSize];

    int read;
    while ((read = in.read(buffer)) > 0) {
      out.write(buffer, 0, read);
    }
  }

  /**
   * Pumps bytes from the given input stream into the given output stream
   * @param bufferSize the size of the buffer to use, given in bytes
   */
  public static void pump(InputStream in, OutputStream out, int bufferSize, long start, long end) throws IOException {
    in.skip(start);

    long bytesToPump = end - start + 1;

    byte buffer[] = new byte[bufferSize];

    int read;
    while ( bytesToPump > 0 && (read = in.read(buffer)) > 0) {
      if (bytesToPump >= read) {
        out.write(buffer, 0, read);
        bytesToPump -= read;
      } else {
        out.write(buffer, 0, (int)bytesToPump);
        bytesToPump = 0;
      }
    }
  }

}
