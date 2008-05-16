package dk.qabi.imapfs;

import dk.qabi.imapfs.util.PathUtil;
import dk.qabi.imapfs.util.Configuration;

import java.io.File;

public class DiskStore {
  private static DiskStore instance = new DiskStore();

  public static DiskStore getInstance() {
    return instance;
  }


  public File getFile(String path, long size) {
    final String parentPath = PathUtil.extractParent(path);

    File parent;
    if (!"/".equals(parentPath))
      parent = new File(Configuration.getDiskstoreFolder(), parentPath);
    else
      parent = Configuration.getDiskstoreFolder();

    parent.mkdirs();
    
    return new File(parent, PathUtil.extractName(path));
  }
}
