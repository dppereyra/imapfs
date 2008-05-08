package dk.qabi.imapfs;

import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Iterator;

public class PathUtil {

  public static String extractExtension(String name) {
    if (name == null) return null;
    int dot = name.lastIndexOf('.');

    if (dot > -1 && dot < name.length()-1)
      return name.substring(dot+1, name.length());
    else
      return null;
  }

  public static String extractName(String uri) {
    if (uri == null) return null;
    return uri.substring(uri.lastIndexOf('/', uri.length()-2)+1);
  }

  public static String extractParent(String uri) {
    if (uri == null) return null;
    if ("/".equals(uri))
      return null;
    else {
      if (uri.endsWith("/"))
        uri = uri.substring(0, uri.length()-1);

      int lastSlash = uri.lastIndexOf('/', uri.length()-1);

      if (lastSlash > 0)
        return uri.substring(0, lastSlash);
      else
        return "/";
    }
  }

  /**
   * Returns a concatenation of the two arguments
   * @param absolute First part
   * @param relative Second part
   * @return a concatenation of the two arguments and ensures that there is
   *         excactly one slash between them.
   */
  public static String concatAbsRel(String absolute, String relative) {

    if (absolute == null || absolute.length() == 0)
      return relative;

    absolute = absolute.replace('\\', '/');

    if (relative == null || relative.length() == 0)
      return absolute;


    if (absolute.endsWith("/")) {
      if (relative.startsWith("/"))
        return absolute + relative.substring(1);
      else
        return absolute + relative;
    }
    else {
      if (relative.startsWith("/"))
        return absolute + relative;
      else
        return absolute + "/" + relative;
    }
  }

  /**
   * Makes an absolute URI to the location which the given relative URI identifies, seen as relative to the given
   * absolute URI
   * @param absolute Absolute part
   * @param relative Relative part
   * @return absolute concatenated path
   */
  public static String makeAbsolute(String absolute, String relative) {
    boolean trailingSlash = relative.endsWith("/");
    Stack<String> stack = new Stack<String>();

    StringTokenizer st = new StringTokenizer(absolute, "/", false);
    while (st.hasMoreTokens())
      stack.push(st.nextToken());

    st = new StringTokenizer(relative, "/", false);
    while (st.hasMoreTokens()) {
      String part = st.nextToken();
      if ("..".equals(part)) {
        if (stack.isEmpty())
          throw new IllegalArgumentException("Relative path " + relative + " refers to a folder above root of the absolute path " + absolute);
        else
          stack.pop();
      } else {
        if (!".".equals(part))
          stack.push(part);
      }
    }
    StringBuffer sb = new StringBuffer(absolute.length()+relative.length());
    sb.append('/');
    Iterator<String> iterator = stack.iterator();
    while (iterator.hasNext()) {
      String part = iterator.next();
      sb.append(part);
      if (trailingSlash || iterator.hasNext())
        sb.append('/');
    }
    return sb.toString();
  }

  /**
   * Returns the relative path that expresses the location of <code>path</code> seen as relative to <code>origin</code>
   * @param origin Where to make relative to
   * @param path The full path
   * @return The relative path from <code>origin</code> to <code>path</code>
   */
  public static String makeRelative(String origin, String path) {
    boolean trailingSlash = path.endsWith("/");
    StringBuffer sb = new StringBuffer(40);
    StringTokenizer originTokenizer = new StringTokenizer(origin, "/", false);
    StringTokenizer pathTokenizer = new StringTokenizer(path, "/", false);

    // Walk through the parts that are in common
    String firstTokenNotInCommon = null;
    while (originTokenizer.hasMoreTokens() && pathTokenizer.hasMoreTokens() && firstTokenNotInCommon == null) {
      firstTokenNotInCommon = pathTokenizer.nextToken();
      if (originTokenizer.nextToken().equals(firstTokenNotInCommon))
        firstTokenNotInCommon = null;
    }

    // Find number of elements that aren't in common, and print a '..' for each
    int stepsBack = originTokenizer.countTokens();
    if (firstTokenNotInCommon != null)
      stepsBack++;
    for (int i=0; i<stepsBack; i++) {
      sb.append("..");
      if (trailingSlash || pathTokenizer.hasMoreTokens() || firstTokenNotInCommon != null || i<stepsBack-1)
        sb.append('/');
    }

    // Print the rest of path
    if (firstTokenNotInCommon != null) {
      sb.append(firstTokenNotInCommon);
      if (trailingSlash || pathTokenizer.hasMoreTokens())
        sb.append('/');
    }
    while (pathTokenizer.hasMoreTokens()) {
      String part = pathTokenizer.nextToken();
      sb.append(part);
      if (trailingSlash || pathTokenizer.hasMoreTokens())
        sb.append('/');
    }
    if (sb.length() == 0)
      return "";
    else
      return sb.toString();
  }

}
