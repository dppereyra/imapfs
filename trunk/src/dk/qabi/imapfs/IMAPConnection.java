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

import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.*;
import java.util.Properties;
import java.net.URL;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

public class IMAPConnection {
  private Folder folder;
  private IMAPStore store;

  public IMAPConnection(URL url) throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

//    session.setDebug(true);
//    try {
//      session.setDebugOut(new PrintStream(new FileOutputStream("foobar.txt")));
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    }

    store = (IMAPStore) session.getStore(url.getProtocol());

    String username = null;
    String password = null;

    String userinfo = url.getUserInfo();
    if (userinfo != null) {
      String[] parts = userinfo.split(":");

      username = parts[0].replace('=', '@');
      
      if (parts.length > 1)
        password = parts[1];
    }

    if (username == null)
      username = System.getProperty("imapfs.username");

    if (password == null)
      password = System.getProperty("imapfs.password");

    store.connect(url.getHost(), username, password);

    String path = url.getPath();
    if (path.startsWith("/"))
      path = path.substring(1);
    String[] parts = path.split(";");

    if (parts != null && parts.length > 0)
      this.folder = store.getFolder(parts[0]);
    else
      this.folder = store.getDefaultFolder();

    if (!folder.exists())
      folder.create(Folder.HOLDS_MESSAGES);

    folder.open(Folder.READ_WRITE);
  }

  public Quota getQuota() throws MessagingException {
    Quota[] quotas = store.getQuota(folder.getFullName());

    if (quotas != null && quotas.length > 0)
      return quotas[0];
    else
      return null;
  }

  public IMAPFolder getRootFolder() {
    return (IMAPFolder) folder;
  }
}
