package dk.qabi.imapfs;

import java.util.Map;
import java.util.HashMap;

public class MIMETypes {
  private static Map<String, String> mimeTypes = new HashMap<String, String>();

  static {
    mimeTypes.put("gif", "image/gif");
    mimeTypes.put("jpg", "image/jpeg");
  }

  public static String get(String extension) {
    return mimeTypes.get(extension);
  }
}
