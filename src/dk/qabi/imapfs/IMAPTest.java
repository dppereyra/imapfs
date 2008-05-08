package dk.qabi.imapfs;

import javax.mail.*;
import java.net.URL;
import java.io.IOException;

public class IMAPTest {
  public static void main(String[] args) throws MessagingException, IOException {
    IMAPConnection con = new IMAPConnection(new URL(null, "imap://imap.gmail.com/INBOX", new IMAPStreamHandler()));

    /* Create a tree structure to represent the file system */
    IMAPDirectory rootEntry = new IMAPDirectory(con.getRootFolder(), null);

//    IMAPFile entry = new IMAPFile("geysir-logo.gif", rootEntry);
    new IMAPDirectory("testfolder", rootEntry);

//    File file = new File("/Users/dth/Pictures/geysir-logo.gif");
//
//    byte[] bytes = new byte[(int) file.length()];
//
//    new FileInputStream(file).read(bytes);
//
//    ByteBuffer buf = ByteBuffer.allocate((int) file.length());
//    buf.put(bytes);
//    buf.clear();
//
//    entry.writeData(buf, 0);

    
  }

}
