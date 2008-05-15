/*
 * Copyright (c) 2008 Dennis Thrysøe
 *
 * Based on bloggerfs which is copyright (c) 2007 Networked Systems Lab - http://www.ece.ubc.ca
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

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.URL;
import fuse.*;
import javax.mail.MessagingException;
import javax.mail.Quota;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.qabi.imapfs.util.PathUtil;

/**
 * This class implements a user-level Filesystem based on FUSE and FUSE-J interacting with an IMAP server.
 *
 * todo cannot write because "too long filename" /  "._" files?
 * todo overwriting file seems to delete it instead?
 * 
 * todo custom icon: volicon=PATH, where PATH is path to an icon (.icns) file as well as fssubtype=N
 * todo implement splitting in multiple messages at configurable file lengths
 * todo limit on diskusage - LRU?
 * todo multiple IMAP stores?
 */
public class IMAPFileSystem implements Filesystem {

  private Log log = LogFactory.getLog(getClass());

  /* Attributes */
  private FuseStatfs statfs;

  private static final int BLOCK_SIZE = 512;
  private IMAPDirectory rootEntry;
  private long nextFileHandle;
  private IMAPConnection con;

  /**
   * @param url URL to connect to
   * @throws javax.mail.MessagingException when an IMAP communication occurs
   */
  public IMAPFileSystem(URL url) throws MessagingException {
    this.con = new IMAPConnection(url);

    /* Create a tree structure to represent the file system */
    this.rootEntry = new IMAPDirectory(con.getRootFolder(), null);

    log.info("IMAPFS Initialized ("+ url.getHost() + ")");

  }

  public FuseStat getattr(String absolutePath) throws FuseException {
    log.debug("getattr(" + absolutePath + ")");
    IMAPEntry entry = findEntry(absolutePath);

    FuseStat stat = new FuseStat();

    if (entry != null) {
      stat.mode = entry.isDirectory() ? FuseFtype.TYPE_DIR | 0777 : FuseFtype.TYPE_FILE | 0777;
      stat.nlink = 1;
      stat.uid = 1000;
      stat.gid = 1000;
      try {
        stat.size = entry.getSize();
        stat.atime = stat.mtime = stat.ctime = (int) (entry.getTime() / 1000L);
      } catch (Exception e) {
        log.error("IMAP error determining size", e);
        throw new FuseException("IMAP error determining file attributes").initErrno(FuseException.ECOMM);
      }
      stat.blocks = (int) stat.size / BLOCK_SIZE;
    } else {
      log.info("Path '" + absolutePath + "' not found");
      throw new FuseException("Path '" + absolutePath + "' not found").initErrno(FuseException.ENOENT);
    }

    return stat;
  }

  /**
   * Return an array with entries to the content the directory passed as a parameter
   */
  public FuseDirEnt[] getdir(String absolutePath) throws FuseException {
    log.debug("getdir(" + absolutePath + ")");
    FuseDirEnt[] dirEntries;
    IMAPDirectory dir = (IMAPDirectory)findEntry(absolutePath);

    if (dir.isDirectory()) {
      IMAPEntry[] children;
      try {
        children = dir.getChildren(true);
      } catch (MessagingException e) {
        log.error("Error getting children", e);
        throw new FuseException("Error getting children: " + e.getMessage()).initErrno(FuseException.ENOENT);
      }
      dirEntries= new FuseDirEnt[children.length];

      for (int i = 0; i < children.length; i++) {
        IMAPEntry child = children[i];
        dirEntries[i] = new FuseDirEnt();
        dirEntries[i].name = child.getName();
        dirEntries[i].mode = child.isDirectory() ? FuseFtype.TYPE_DIR : FuseFtype.TYPE_FILE;
      }
    } else {
      dirEntries = new FuseDirEnt[0];
    }

    return dirEntries;
  }

  private IMAPEntry findEntry(String path) throws FuseException {
    IMAPEntry entry;
    if (path == null) {
      entry = null;
    } else {
      if ("/".equals(path))
        entry = rootEntry;
      else
        try {
          entry = rootEntry.get(path.substring(1));
        } catch (MessagingException e) {
          log.error("Error finding entry", e);
          throw new FuseException("Error finding entry: " + e.getMessage()).initErrno(FuseException.ENOENT);
        }
    }

    if (entry == null) {
      log.debug("Path '" + path + "' not found");
      throw new FuseException("Path '" + path + "' not found").initErrno(FuseException.ENOENT);
    }

    return entry;
  }

  public long open(String path, int flags) throws FuseException {
    log.debug("open(" + path + ")");
    IMAPEntry entry = findEntry(path);

    if (entry.isDirectory()) {
      log.info("Cannot open directory entry");
      throw new FuseException("Cannot open directory entry").initErrno(FuseException.EACCES);
    }

    synchronized (this) {
      return ++nextFileHandle;
    }
  }

  public String readlink(String path) throws FuseException {
    log.debug("readLink(" + path + ")");
    return path;
  }

  public FuseStatfs statfs() throws FuseException {
    log.debug("statfs()");

    if (this.statfs == null) {

      this.statfs = new FuseStatfs();
      statfs.blockSize = BLOCK_SIZE;
      statfs.files = 3; // not really known up-front
      statfs.filesFree = 1000000;
      statfs.namelen = 2048;

      Quota quota;
      try {
        quota = con.getQuota();
      } catch (MessagingException e) {
        log.error("Cannot get quota", e);
        throw new FuseException("Cannot get quota").initErrno(FuseException.EACCES);
      }

      Quota.Resource res = null;
      if (quota != null) {
        for (Quota.Resource r : quota.resources) {
          if ("STORAGE".equals(r.name))
            res = r;
        }
      }

      if (res != null) {
        statfs.blocks = (int) (res.limit * 1024 / BLOCK_SIZE);
        statfs.blocksFree = (int) (statfs.blocks - (res.usage * 1024 / BLOCK_SIZE));
      } else {
        statfs.blocks = 1000000000;
        statfs.blocksFree = statfs.blocks;
      }
    }

    return this.statfs;
  }

  public void chmod(String path, int mode) throws FuseException {
    log.debug("chmod(" + path + ", " + mode + ")");
    throw new FuseException("chmod not supported").initErrno(FuseException.EACCES);
  }

  public void chown(String path, int uid, int gid) throws FuseException {
    log.debug("chown(" + path + ", " + uid + ", " + gid + ")");
    throw new FuseException("chown not supported").initErrno(FuseException.EACCES);
  }

  public void link(String from, String to) throws FuseException {
    log.debug("link(" + from + ", " + to + ")");
    throw new FuseException("link not supported").initErrno(FuseException.EACCES);
  }

  public void mkdir(String path, int mode) throws FuseException {
    log.debug("mkdir(" + path + ", " + mode + ")");
    IMAPEntry parent = findEntry(PathUtil.extractParent(path));

    if (!(parent instanceof IMAPDirectory)) {
      log.warn("Parent parent is not a directory");
      throw new FuseException("Parent parent is not a directory").initErrno(FuseException.EACCES);
    }

    IMAPDirectory dir = (IMAPDirectory) parent;
    try {
      new IMAPDirectory(PathUtil.extractName(path), dir);
    } catch (MessagingException e) {
      log.warn("Error creating directory '"+path+"'");
      throw new FuseException("Error creating directory '"+path+"'").initErrno(FuseException.EACCES);
    }
  }

  public void mknod(String path, int mode, int rdev) throws FuseException {
    log.info("mknod(" + path + ", " + mode + ")");
    IMAPEntry parent = findEntry(PathUtil.extractParent(path));

    if (!(parent instanceof IMAPDirectory)) {
      log.warn("Parent entry is not a directory");
      throw new FuseException("Parent entry is not a directory").initErrno(FuseException.EACCES);
    }

    IMAPDirectory dir = (IMAPDirectory) parent;
    try {
      new IMAPFile(PathUtil.extractName(path), dir);
    } catch (MessagingException e) {
      log.warn("Error creating file '"+path+"'");
      throw new FuseException("Error creating file '"+path+"'").initErrno(FuseException.EACCES);
    }
  }

  public void rename(String from, String to) throws FuseException {
    log.debug("rename(" + from + ", " + to + ")");
    IMAPEntry src = findEntry(from);
    IMAPEntry srcdir = findEntry(PathUtil.extractParent(from));
    IMAPEntry destdir = findEntry(PathUtil.extractParent(to));

    if (src == null) {
      log.warn("Source does not exist");
      throw new FuseException("Source does not exist").initErrno(FuseException.EACCES);
    }

    if (!(srcdir instanceof IMAPDirectory)) {
      log.warn("Source is not an existing directory");
      throw new FuseException("Source is not an existing directory").initErrno(FuseException.EACCES);
    }

    if (!(destdir instanceof IMAPDirectory)) {
      log.warn("Destination is not an existing directory");
      throw new FuseException("Destination is not an existing directory").initErrno(FuseException.EACCES);
    }

    try {

      if (src instanceof IMAPDirectory) {
        ((IMAPDirectory)src).renameTo(to);
      } else {
        final IMAPFile file = (IMAPFile)src;

        if (!srcdir.equals(destdir)) {
          file.moveTo((IMAPDirectory)destdir);
        }
  
        final String destName = PathUtil.extractName(to);
        if (!destName.equals(file.getName())) {
          file.rename(destName);
        }
      }

    } catch (MessagingException e) {
      log.error("Error renaming", e);
      throw new FuseException("Error renaming: " + e.getMessage()).initErrno(FuseException.EACCES);
    }
  }

  public void rmdir(String path) throws FuseException {
    log.debug("rmdir(" + path + ")");
    IMAPDirectory dir = (IMAPDirectory)findEntry(path);

    if (dir.isDirectory()) {
      try {
        dir.delete();
      } catch (MessagingException e) {
        log.warn("Error deleting directory '"+path+"'");
        throw new FuseException("Error deleting directory '"+path+"'").initErrno(FuseException.EACCES);
      }
    }
  }

  public void truncate(String path, long size) throws FuseException {
    log.debug("truncate(" + path + ", " + size + ")");
    IMAPEntry entry = findEntry(path);

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot truncate directory entry");
      throw new FuseException("Cannot truncate directory entry").initErrno(FuseException.EACCES);
    }

    try {
      ((IMAPFile)entry).truncate(size);
      this.statfs = null;
    } catch (Exception e) {
      log.error("Error updating file", e);
      throw new FuseException("Error updating file").initErrno(FuseException.EIO); // Map to better error code?
    }
  }

  public void utime(String path, int atime, int mtime) throws FuseException {
    log.debug("utime(" + path + ", " + atime + ", " + mtime + ")");
    IMAPEntry entry = findEntry(path);

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot update time on directory entry");
      throw new FuseException("Cannot update time on directory entry").initErrno(FuseException.EACCES);
    }

    try {
      ((IMAPFile)entry).setTime(mtime*1000);
    } catch (MessagingException e) {
      log.error("Error updating file", e);
      throw new FuseException("Error updating file").initErrno(FuseException.EIO); // Map to better error code?
    }
  }

  public void flush(String path, long fh) throws FuseException {
    log.debug("flush(" + path + ", " + fh + ")");
    // Ignore for now
  }

  public void fsync(String path, long fh, boolean isDatasync) throws FuseException {
    log.debug("fsync(" + path + ", " + fh + ")");
    IMAPEntry entry = findEntry(path);

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot fsync directory entry");
      throw new FuseException("Cannot fsync directory entry").initErrno(FuseException.EACCES);
    }

    IMAPFile file = (IMAPFile) entry;

    try {
      file.flush();
      this.statfs = null;
    } catch (Exception e) {
      log.error("I/O error syncing data", e);
      throw new FuseException("I/O error syncing data").initErrno(FuseException.EIO); // Map to better error code?
    }

  }

  public void symlink(String from, String to) throws FuseException {
    log.debug("symlink(" + from + ", " + to + ")");
    throw new FuseException("symlink not supported").initErrno(FuseException.EACCES);
  }

  public void unlink(String path) throws FuseException {
    log.debug("unlink(" + path + ")");

    IMAPEntry entry = findEntry(path);

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot read data from directory entry");
      throw new FuseException("Cannot read data from directory entry").initErrno(FuseException.EACCES);
    }

    IMAPFile file = (IMAPFile) entry;

    try {
      file.delete();
    } catch (MessagingException e) {
      log.error("IMAP error deleting message of '"+path+"'", e);
      throw new FuseException("IMAP error deleting message of '"+path+"'").initErrno(FuseException.ECOMM); // Map to better error code?
    }
  }

  public void release(String path, long fh, int flags) throws FuseException {
    log.debug("release(" + path + ", " + fh + ", " + flags + ")");
  }

  public void read(String path, long fh, ByteBuffer buf, long offset) throws FuseException {
    log.debug("read(" + path + ", " + buf.capacity() + ", " + offset + ")");
    IMAPEntry entry = findEntry(path);

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot read data from directory entry");
      throw new FuseException("Cannot read data from directory entry").initErrno(FuseException.EACCES);
    }

    IMAPFile file = (IMAPFile) entry;

    try {
      file.readData(buf, offset);
    } catch (MessagingException e) {
      log.error("IMAP error reading data of '"+path+"'", e);
      throw new FuseException("IMAP error reading data of '"+path+"'").initErrno(FuseException.ECOMM); // Map to better error code?
    } catch (IOException e) {
      log.error("I/O error reading data", e);
      throw new FuseException("I/O error reading data").initErrno(FuseException.EIO); // Map to better error code?
    }
  }

  public void write(String path, long fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
    log.debug("write(" + path + ", " + fh + ", " + isWritepage + ", " + buf.capacity() + ", " + offset + ")");
    IMAPEntry entry = findEntry(path);

    if (isWritepage) {
      log.error("writepage not supported");
      throw new FuseException("writepage not supported").initErrno(FuseException.EACCES);
    }

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot write data to directory entry");
      throw new FuseException("Cannot write data to directory entry").initErrno(FuseException.EACCES);
    }

    IMAPFile file = (IMAPFile) entry;

    try {
      file.writeData(buf, offset);
    } catch (MessagingException e) {
      log.error("IMAP error reading data", e);
      throw new FuseException("IMAP error reading data").initErrno(FuseException.ECOMM); // Map to better error code?
    } catch (IOException e) {
      log.error("I/O error reading data", e);
      throw new FuseException("I/O error reading data").initErrno(FuseException.EIO); // Map to better error code?
    }
  }
}