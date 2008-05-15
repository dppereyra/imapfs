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
    File parent = new File(Configuration.getDiskstoreFolder(), PathUtil.extractParent(path));
    parent.mkdirs();

    return new File(parent, PathUtil.extractName(path));
  }
}
