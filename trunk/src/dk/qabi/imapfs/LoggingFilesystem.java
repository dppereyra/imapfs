package dk.qabi.imapfs;

import fuse.*;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;

/**
 * A Filesystem decorator that logs everything that happens
 */
public class LoggingFilesystem implements Filesystem {
  private Filesystem fs;
  private Log log;

  public LoggingFilesystem(Filesystem delegate, Log log) {
    this.fs = delegate;
    this.log = log;
  }

  public FuseStat getattr(String path) throws FuseException {
    log.debug("getattr(" + path + ")");

    try {
      return fs.getattr(path);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public String readlink(String path) throws FuseException {
    log.debug("readLink(" + path + ")");

    try {
      return fs.readlink(path);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public FuseDirEnt[] getdir(String path) throws FuseException {
    log.debug("getdir(" + path + ")");

    try {
      return fs.getdir(path);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void mknod(String path, int mode, int rdev) throws FuseException {
    log.info("mknod(" + path + ", " + mode + ")");
    try {
      fs.mknod(path, mode, rdev);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void mkdir(String path, int mode) throws FuseException {
    log.debug("mkdir(" + path + ", " + mode + ")");
    try {
      fs.mkdir(path, mode);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void unlink(String path) throws FuseException {
    log.debug("unlink(" + path + ")");
    try {
      fs.unlink(path);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void rmdir(String path) throws FuseException {
    log.debug("rmdir(" + path + ")");
    try {
      fs.rmdir(path);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void symlink(String from, String to) throws FuseException {
    log.debug("symlink(" + from + ", " + to + ")");
    try {
      fs.symlink(from ,to);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void rename(String from, String to) throws FuseException {
    log.debug("rename(" + from + ", " + to + ")");
    try {
      fs.rename(from, to);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void link(String from, String to) throws FuseException {
    log.debug("link(" + from + ", " + to + ")");
    try {
      fs.link(from, to);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void chmod(String path, int mode) throws FuseException {
    log.debug("chmod(" + path + ", " + mode + ")");
    try {
      fs.chmod(path, mode);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void chown(String path, int uid, int gid) throws FuseException {
    log.debug("chown(" + path + ", " + uid + ", " + gid + ")");
    try {
      fs.chown(path, uid, gid);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void truncate(String path, long size) throws FuseException {
    log.debug("truncate(" + path + ", " + size + ")");
    try {
      fs.truncate(path, size);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void utime(String path, int atime, int mtime) throws FuseException {
    log.debug("utime(" + path + ", " + atime + ", " + mtime + ")");
    try {
      fs.utime(path, atime, mtime);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public FuseStatfs statfs() throws FuseException {
    log.debug("statfs()");

    try {
      return fs.statfs();
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public long open(String path, int flags) throws FuseException {
    log.debug("open(" + path + ")");

    try {
      return fs.open(path, flags);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void read(String path, long fh, ByteBuffer buf, long offset) throws FuseException {
    log.debug("read(" + path + ", " + buf.capacity() + ", " + offset + ")");
    try {
      fs.read(path, fh, buf, offset);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void write(String path, long fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
    log.debug("write(" + path + ", " + fh + ", " + isWritepage + ", " + buf.capacity() + ", " + offset + ")");
    try {
      fs.write(path, fh, isWritepage, buf, offset);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void flush(String path, long fh) throws FuseException {
    log.debug("flush(" + path + ", " + fh + ")");
    try {
      fs.flush(path, fh);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void release(String path, long fh, int flags) throws FuseException {
    log.debug("release(" + path + ", " + fh + ", " + flags + ")");
    try {
      fs.release(path, fh, flags);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }

  public void fsync(String path, long fh, boolean isDatasync) throws FuseException {
    log.debug("fsync(" + path + ", " + fh + ")");
    try {
      fs.fsync(path, fh, isDatasync);
    } catch (FuseException e) {
      log.debug("FuseException thrown", e);
      throw e;
    } catch (RuntimeException e) {
      log.warn("Exception thrown", e);
      throw e;
    }
  }
}
