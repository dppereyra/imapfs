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
import java.util.Map;
import java.util.HashMap;
import com.sun.mail.imap.IMAPFolder;

/**
 *
 * This class represents a directory
 *
 */
public class IMAPDirectory extends IMAPEntry {

  private Map<String, IMAPEntry> children = new HashMap<String, IMAPEntry>();
  private IMAPFolder folder;

  /**
   * Constructor for creating a new directory
   * @param name name of the created entry
   * @param parent IMapFileEntry for the parent
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   */
  public IMAPDirectory(String name, IMAPDirectory parent) throws MessagingException {
    this.parent = parent;
    this.absolutePath = makeAbsolutePath();
    this.name = name;

    folder = parent.getChildFolder(name);
    if (!folder.exists()) {
      if (!folder.create(Folder.HOLDS_MESSAGES))
        throw new MessagingException("Folder '" + name + "' not created");
    } else {
      throw new MessagingException("Directory '"+absolutePath+"' already exists");
    }
  }

  /**
   * Constructor for directories
   * @param folder The JavaMail Folder representing the IMAP folder
   * @param parent IMapFileEntry for the parent
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   */
  public IMAPDirectory(IMAPFolder folder, IMAPDirectory parent) throws MessagingException {
    this.parent = parent;
    this.name = folder.getName();
    this.folder = folder;

    if (parent != null) {
      this.absolutePath = makeAbsolutePath();
    } else {
      this.absolutePath = "/";
    }

    Folder[] folders = folder.list();
    for (Folder f : folders) {
      IMAPEntry child = new IMAPDirectory((IMAPFolder) f, this);
      children.put(child.getName(), child);
    }

    Message[] messages = folder.getMessages();
    for (Message m : messages) {
      IMAPEntry child = new IMAPFile(m, this);
      children.put(child.getName(), child);
    }
  }

  /**
   * The creation time of the entry.
   *
   * @return a long representation of the publication time
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   */
  public long getTime() throws MessagingException {
    return 0;
  }

  public boolean isDirectory() {
    return true;
  }

  public boolean isRoot() {
    return this.parent == null;
  }

  public IMAPEntry[] getChildren() {
    if (children != null)
      return children.values().toArray(new IMAPEntry[children.size()]);
    else
      return new IMAPEntry[0];
  }

  public IMAPEntry get(String relPath) {
    int pos = relPath.indexOf('/');
    IMAPEntry result;

    if (pos > -1) {
      String firstPart = relPath.substring(0, pos);
      String rest = relPath.substring(pos+1);
      result = ((IMAPDirectory)children.get(firstPart)).get(rest);
    } else {
      result = children.get(relPath);
    }

    return result;
  }

  public void printSubtree(int level) {
    super.printSubtree(level);
    for (IMAPEntry e : children.values()) {
      e.printSubtree(level+1);
    }
  }

  void expunge() throws MessagingException {
    if (folder == null)
      throw new IllegalStateException("Entry not associated with folder");
    folder.expunge();
  }

  public IMAPFolder getFolder() {
    return folder;
  }

  public IMAPFolder getChildFolder(String name) throws MessagingException {
    if (folder == null)
      throw new IllegalStateException("Entry not associated with folder");
    return (IMAPFolder) folder.getFolder(name);
  }

  public Object getChildFile(String name) throws MessagingException {
    if (folder == null)
      throw new IllegalStateException("Entry not associated with folder");

    Message[] msgs = folder.getMessages();
    for (Message msg : msgs) {
      if (msg.getSubject().equals(name))
        return msg;
    }

    return null;
  }
}