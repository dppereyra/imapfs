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
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * This class represents a filesystem entry
 *
 */
public abstract class IMAPEntry {

  protected Log log = LogFactory.getLog(getClass());
  protected String absolutePath;
  protected String name;
  protected IMAPDirectory parent;

  protected String makeAbsolutePath() {
    if (parent.getAbsoluteName().endsWith("/"))
      return parent.getAbsoluteName() + this.name;
    else
      return parent.getAbsoluteName() + "/" + this.name;
  }

  public String getName() {
    return this.name;
  }

  /**
   * Get the absolute file entry name
   *
   * @return entry absolute name in the form "/filename"
   */
  public String getAbsoluteName(){
    return this.absolutePath;
  }

  /**
   * Get the size of this entry
   *
   * @return the size in bytes
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   * @throws java.io.IOException if I/O errors occur
   */
  public long getSize() throws MessagingException, IOException {
    return 0;
  }

  /**
   * The creation time of the entry.
   *
   * @return a long representation of the publication time
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   */
  public abstract long getTime() throws MessagingException;

  public abstract boolean isDirectory();

  public abstract boolean isRoot();

  public String toString() {
    return name + "(dir)";
  }

  public void printSubtree(int level) throws MessagingException {
    for (int i=0; i<level; i++)
      System.out.print("  ");

    System.out.println(this.toString());
  }

  
}