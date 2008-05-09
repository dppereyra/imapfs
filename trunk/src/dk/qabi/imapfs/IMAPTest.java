/*
 * Copyright (c) 2008 Dennis Thrysøe
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package dk.qabi.imapfs;

import javax.mail.*;
import java.net.URL;
import java.io.IOException;

public class IMAPTest {
  public static void main(String[] args) throws MessagingException, IOException {
    IMAPConnection con = new IMAPConnection(new URL(null, "imap://qabi01=gmail.com:foobar123@imap.gmail.com/INBOX", new IMAPStreamHandler()));

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
