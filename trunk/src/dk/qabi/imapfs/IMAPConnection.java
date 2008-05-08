package dk.qabi.imapfs;

import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.*;
import java.util.Properties;
import java.net.URL;

public class IMAPConnection {
  private Folder folder;

  public IMAPConnection(URL url) throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    IMAPStore store = (IMAPStore) session.getStore("imaps");
                 
    // todo Figure out authentication...
    store.connect(url.getHost(), System.getProperty("username"), System.getProperty("password"));

    String path = url.getPath();
    if (path.startsWith("/"))
      path = path.substring(1);
    String[] parts = path.split(";");

    if (parts != null && parts.length > 0)
      this.folder = store.getFolder(parts[0]);
    else
      this.folder = store.getDefaultFolder();

    folder.open(Folder.READ_WRITE);
  }

  public IMAPFolder getRootFolder() {
    return (IMAPFolder) folder;
  }
}
