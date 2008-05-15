package dk.qabi.imapfs.util;

import javax.mail.Message;
import javax.mail.search.StringTerm;

public class FilenameTerm extends StringTerm {

  public FilenameTerm(String s) {
    super(s);
  }

  public boolean match(Message message) {
    String s;
    try {
      s = message.getSubject();
    } catch(Exception exception) {
      return false;
    }

    return s != null && s.equals(super.pattern);
  }

  public boolean equals(Object obj) {
    return obj instanceof StringTerm && super.equals(obj);
  }
}
