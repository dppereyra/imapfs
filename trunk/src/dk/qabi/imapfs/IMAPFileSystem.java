/*
 * Copyright (c) 2007 Networked Systems Lab - http://www.ece.ubc.ca
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
import java.net.MalformedURLException;

import fuse.*;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a user-level Filesystem based on FUSE and FUSE-J interacting with an IMAP server.
 *
 * todo custom icon: volicon=PATH, where PATH is path to an icon (.icns) file as well as fssubtype=N
 * todo various metainfo like filesystem type etc.
 * todo implement lazy loading, and some kind of statelessness
 * todo copy data into and out of messages
 * todo How to handle directory hierachy in relation to gmail's way of doing it?
 * todo implement rest of fuse API methods
 * todo implement splitting in multiple messages at configurable file lengths
 * todo implement filehandle table to keep track of open files, and release them when no longer used
 * todo implement buffering on local disk and only write on flush()
 */
public class IMAPFileSystem implements Filesystem {

  private Log log = LogFactory.getLog(getClass());

  /* Attributes */
  private FuseStatfs statfs;

  private static final int blockSize = 1;
  private IMAPDirectory rootEntry;
  private long nextFileHandle;

  /**
   * @param url URL to connect to
   * @throws javax.mail.MessagingException when an IMAP communication occurs
   */
  public IMAPFileSystem(URL url) throws MessagingException {
                          
    IMAPConnection con = new IMAPConnection(url);

    /* Create a tree structure to represent the file system */
    this.rootEntry = new IMAPDirectory(con.getRootFolder(), null);

    statfs = new FuseStatfs();
    statfs.blocks = 0;
    statfs.blockSize = blockSize;
    statfs.blocksFree = 0;
    statfs.files = 3; // not really known up-front
    statfs.filesFree = 0;
    statfs.namelen = 2048;

    log.info("IMAPFS Initialized ("+ url.getHost() + ")");

  }

  public FuseStat getattr(String absolutePath) throws FuseException {
    IMAPEntry entry = findEntry(absolutePath);

    FuseStat stat = new FuseStat();

    if (entry != null) {
      stat.mode = entry.isDirectory() ? FuseFtype.TYPE_DIR | 0x1ed : FuseFtype.TYPE_FILE | 0x1ff;
      stat.nlink = 1;
      stat.uid = 1000;
      stat.gid = 1000;
      try {
        stat.size = entry.getSize();
        stat.atime = stat.mtime = stat.ctime = (int) (entry.getTime() / 1000L);
      } catch (Exception e) {
        log.error("IMAP error determining size");
        throw new FuseException("IMAP error determining file attributes").initErrno(FuseException.ECOMM);
      }
      stat.blocks = (int) stat.size;
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
    FuseDirEnt[] dirEntries;
    IMAPDirectory dir = (IMAPDirectory)findEntry(absolutePath);

    if (dir == null) {
      log.info("Directory '" + absolutePath + "' not found");
      throw new FuseException("Directory '" + absolutePath + "' not found").initErrno(FuseException.ENOENT);
    }

    if (dir.isDirectory()) {
      IMAPEntry[] children = dir.getChildren();
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

  private IMAPEntry findEntry(String path) {
    if (path == null)
      return null;
    else {
      if ("/".equals(path))
        return rootEntry;
      else
        return rootEntry.get(path.substring(1));
    }
  }

  public long open(String path, int flags) throws FuseException {
    IMAPEntry entry = findEntry(path);

    if (entry == null) {
      log.info("Path '" + path + "' not found");
      throw new FuseException("Path '" + path + "' not found").initErrno(FuseException.ENOENT);
    }

    if (entry.isDirectory()) {
      log.warn("Cannot open directory entry");
      throw new FuseException("Cannot open directory entry").initErrno(FuseException.EACCES);
    }

    synchronized (this) {
      return ++nextFileHandle;
    }
  }

  public String readlink(String path) throws FuseException {
    return path;
  }

  public FuseStatfs statfs() throws FuseException {
    return this.statfs;
  }

  public void chmod(String arg0, int arg1) throws FuseException {
    throw new FuseException("Read Only").initErrno(FuseException.EACCES);
  }

  public void chown(String arg0, int arg1, int arg2) throws FuseException {
    throw new FuseException("Read Only").initErrno(FuseException.EACCES);
  }

  public void link(String arg0, String arg1) throws FuseException {
    throw new FuseException("link not supported").initErrno(FuseException.EACCES);
  }

  public void mkdir(String arg0, int arg1) throws FuseException {
    throw new FuseException("Read Only").initErrno(FuseException.EACCES);
  }

  public void mknod(String arg0, int arg1, int arg2) throws FuseException {
    throw new FuseException("Read Only").initErrno(FuseException.EACCES);
  }

  public void rename(String arg0, String arg1) throws FuseException {
    throw new FuseException("Read Only").initErrno(FuseException.EACCES);
  }

  public void rmdir(String arg0) throws FuseException {
    throw new FuseException("Read Only").initErrno(FuseException.EACCES);
  }

  public void symlink(String arg0, String arg1) throws FuseException {
    throw new FuseException("symlink not supported").initErrno(FuseException.EACCES);
  }

  public void truncate(String arg0, long arg1) throws FuseException {
    throw new FuseException("Read Only").initErrno(FuseException.EACCES);
  }

  public void unlink(String arg0) throws FuseException {
    throw new FuseException("unlink not supported").initErrno(FuseException.EACCES);
  }

  public void utime(String arg0, int arg1, int arg2) throws FuseException {
  }

  public void read(String path, long fh, ByteBuffer buf, long offset) throws FuseException {
    IMAPEntry entry = findEntry(path);

    if (entry == null) {
      log.warn("Path '" + path + "' not found");
      throw new FuseException("Path '" + path + "' not found").initErrno(FuseException.ENOENT);
    }

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot read data from directory entry");
      throw new FuseException("Cannot read data from directory entry").initErrno(FuseException.EACCES);
    }

    IMAPFile file = (IMAPFile) entry;

    try {
      file.readData(buf, offset);
    } catch (MessagingException e) {
      log.error("IMAP error reading data");
      throw new FuseException("IMAP error reading data").initErrno(FuseException.ECOMM); // Map to better error code?
    } catch (IOException e) {
      log.error("I/O error reading data");
      throw new FuseException("I/O error reading data").initErrno(FuseException.EIO); // Map to better error code?
    }
  }

  public void write(String path, long fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
    IMAPEntry entry = findEntry(path);

    if (isWritepage) {
      log.error("writepage not supported");
      throw new FuseException("writepage not supported").initErrno(FuseException.EACCES);
    }

    if (entry == null) {
      log.warn("Path '" + path + "' not found");
      throw new FuseException("Path '" + path + "' not found").initErrno(FuseException.ENOENT);
    }

    if (!(entry instanceof IMAPFile)) {
      log.warn("Cannot write data to directory entry");
      throw new FuseException("Cannot write data to directory entry").initErrno(FuseException.EACCES);
    }

    IMAPFile file = (IMAPFile) entry;

    try {
      file.writeData(buf, offset);
    } catch (MessagingException e) {
      log.error("IMAP error reading data");
      throw new FuseException("IMAP error reading data").initErrno(FuseException.ECOMM); // Map to better error code?
    } catch (IOException e) {
      log.error("I/O error reading data");
      throw new FuseException("I/O error reading data").initErrno(FuseException.EIO); // Map to better error code?
    }
  }

  public void flush(String path, long fh) throws FuseException {
  }

  public void release(String path, long fh, int flags) throws FuseException {

  }

  public void fsync(String path, long fh, boolean isDatasync) throws FuseException {
  }

  public static void main(String[] args) throws MessagingException, MalformedURLException {

    if (args.length < 2) {
      System.out.println("[Error]: Must specify a mounting point");
      System.out.println();
      System.out.println("[Usage]: imapfsmnt <mounting point>");
      System.exit(-1);
    }

    final String urlSpec = args[0];
    final URL url = new URL(null, urlSpec, new IMAPStreamHandler());
    final String mountpoint = args[1];

    String[] fs_args = new String[4];
    fs_args[0] = "-f";
    fs_args[1] = "-s";
    fs_args[2] = mountpoint;
    fs_args[3] = "-ovolname="+ url.getHost() + ",fssubtype=7";

    Filesystem imapfs = new IMAPFileSystem(url);

    try {
      FuseMount.mount(fs_args, imapfs);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}