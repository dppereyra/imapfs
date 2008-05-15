package dk.qabi.imapfs.util;

import java.io.File;

public class Configuration {

  public static long getMaxDiskUsage() {
    return 1000000000;
  }

  public static File getDiskstoreFolder() {
    File f = new File("/tmp/imapfs");
    f.mkdirs();
    return f;
  }

}
