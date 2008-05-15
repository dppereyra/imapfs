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

import fuse.FuseException;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage;
import javax.activation.DataSource;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.util.Date;
import java.util.Properties;
import java.nio.ByteBuffer;
import java.io.*;

import dk.qabi.imapfs.util.*;

/**
 *
 * This class represents a file
 *
 */
public class IMAPFile extends IMAPEntry {

  private Message msg;
  private File file;

  /**
   * Constructor for creating a new file
   * @param name name of the created entry
   * @param parent IMapFileEntry for the parent
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   */
  public IMAPFile(String name, IMAPDirectory parent) throws MessagingException {
    this.parent = parent;
    this.absolutePath = makeAbsolutePath();
    this.name = name;

    if (parent.getChildFile(name) != null)
      throw new MessagingException("File '"+absolutePath+"' already exists");

    this.msg = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
    msg.setSubject(name);
    msg.setHeader("X-IMAPFS-Filesize", "0");
    msg.setSentDate(new Date());
    msg.setText("");
    this.msg.saveChanges();
    parent.getFolder().addMessages(new Message[]{this.msg});
    parent.clearChildren();
  }

  /**
   * Constructor for files
   * @param msg IMAP (JavaMail API) message
   * @param parent IMapFileEntry for the parent
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   */
  public IMAPFile(Message msg, IMAPDirectory parent) throws MessagingException {
    if (parent == null)
      throw new IllegalArgumentException("Parent must be a valid directory");

    this.parent = parent;
    this.name = PathUtil.extractName(msg.getSubject());
    this.msg = msg;
    this.absolutePath = makeAbsolutePath();
  }

  /**
   * Get the size of this entry
   *
   * @return the size in bytes
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   * @throws java.io.IOException if I/O errors occur
   */
  public long getSize() throws MessagingException, IOException {
    String[] headerValues = msg.getHeader("X-IMAPFS-Filesize");
    if (headerValues != null && headerValues.length > 0) {
      String header = headerValues[0];
      return Integer.parseInt(header);
    } else {
      InputStream in = msg.getInputStream();
      int length = 0;

      while (in.available() > 0) {
        length += in.skip(in.available());
      }

      return length;
    }
  }

  /**
   * The creation time of the entry.
   *
   * @return a long representation of the publication time
   * @throws javax.mail.MessagingException if IMAP communication goes wrong
   */
  public long getTime() throws MessagingException {
    return msg.getSentDate().getTime();
  }

  public boolean isDirectory() {
    return false;
  }

  public boolean isRoot() {
    return false;
  }

  public void readData(ByteBuffer buf, long offset) throws MessagingException, FuseException, IOException {

    if (this.file == null) {
      // Write to disk
      this.file = DiskStore.getInstance().getFile(absolutePath, getSize());

      FileOutputStream fos = new FileOutputStream(file);
      InputStream serverin = getInputStream();

      StreamUtil.pump(serverin, fos, 4096);
    }

    InputStream in = new FileInputStream(this.file);

    if (in.skip(offset) == offset) {
      byte[] bytes = new byte[(int) Math.min(buf.capacity(), file.length()-offset)];
      int nread;
      nread = in.read(bytes);

      if (nread == bytes.length) {
        buf.put(bytes, 0, nread);
      } else {
        log.info("read " + nread + " bytes but expected " + bytes.length);
      }

      log.info("read " + buf.position() + "/" + buf.capacity() + " requested bytes");
    } else {
      log.info("could not read beyond file length at requested offset " + offset);
    }
    
  }

  private InputStream getInputStream() throws IOException, MessagingException {
    Object content = msg.getContent();

    if (content instanceof Multipart) {
      Multipart m = (Multipart) content;
      BodyPart part = m.getBodyPart(0);
      return part.getInputStream();
    } else {
      // Not a multipart -> no attachemts -> no data
      return new ByteArrayInputStream(new byte[0]);
    }
  }

  public void writeData(ByteBuffer buf, long offset) throws MessagingException, IOException {
    if (this.file == null) {
      this.file = DiskStore.getInstance().getFile(absolutePath, offset + buf.capacity());
    }

    RandomAccessFile out = new RandomAccessFile(this.file, "rwd");

    out.seek(offset);
    out.write(buf.array());
  }

  public void flush() throws MessagingException, IOException {
    if (this.file == null) // nothing to flush
      return;

    MimeMessage newMsg = new MimeMessage((MimeMessage) msg);
    Object content = msg.getContent();

    BodyPart part;
    Multipart m;
    if (content instanceof Multipart) {
      m = (Multipart) content;
    } else {
      m = new MimeMultipart();
      newMsg.setContent(m);
    }

    if (m.getCount() < 1) {
      part = new MimeBodyPart();
      m.addBodyPart(part, 0);
    } else {
      part = m.getBodyPart(0);
    }

    DataSource ds = new FileDataSource(this.file);
    part.setDataHandler(new DataHandler(ds));
    part.setFileName("_imapfsdata.bin");

    newMsg.setHeader("X-IMAPFS-Filesize", String.valueOf(this.file.length()));

    String contentType = MIMETypes.get(PathUtil.extractExtension(name));
    if (contentType != null)
      part.setHeader("Content-Type", contentType);

    newMsg.setSentDate(new Date());
    replaceContainedMessage(newMsg);
  }

  public void setTime(long modifiedTime) throws MessagingException {
    MimeMessage newMsg = new MimeMessage((MimeMessage) msg);

    newMsg.setSentDate(new Date(modifiedTime));

    replaceContainedMessage(newMsg);
  }

  private void replaceContainedMessage(MimeMessage newMsg) throws MessagingException {
    newMsg.saveChanges();
    msg.setFlag(Flags.Flag.DELETED, true);
    parent.expunge();
    this.msg = newMsg;
    parent.getFolder().addMessages(new Message[]{newMsg});
  }

  public void rename(String newName) throws MessagingException {
    MimeMessage newMsg = new MimeMessage((MimeMessage) msg);
    newMsg.setSubject(newName);
    replaceContainedMessage(newMsg);
    parent.clearChildren();
  }

  public void truncate(long length) throws MessagingException, IOException {
    MimeMessage newMsg = new MimeMessage((MimeMessage) msg);
    Object content = msg.getContent();

    BodyPart part;
    Multipart m;
    if (content instanceof Multipart) {
      m = (Multipart) content;
    } else {
      m = new MimeMultipart();
      newMsg.setContent(m);
    }

    if (m.getCount() < 1) {
      part = new MimeBodyPart();
      m.addBodyPart(part, 0);
    } else {
      part = m.getBodyPart(0);
    }

    long size = getSize();
    ByteBuffer buf;
    if (size < length) {
      // add more bytes
      buf = ByteBuffer.allocate((int) (length - size));
    } else {
      // otherwise remove bytes or do nothing
      buf = ByteBuffer.allocate(0);
    }

    DataSource ds = new RewritingDataSource(part.getDataHandler(), buf, length);
    part.setDataHandler(new DataHandler(ds));
    part.setFileName("_imapfsdata.bin");

    newMsg.setHeader("X-IMAPFS-Filesize", String.valueOf(size));

    String contentType = MIMETypes.get(PathUtil.extractExtension(name));
    if (contentType != null)
      part.setHeader("Content-Type", contentType);

    newMsg.setSentDate(new Date());
    replaceContainedMessage(newMsg);
  }

  public Message getMessage() {
    return msg;
  }

  public void moveTo(IMAPDirectory dest) throws MessagingException {
    // Make a copy...
    parent.getFolder().copyMessages(new Message[]{msg}, dest.getFolder());
    dest.clearChildren();

    // ... and remove this
    msg.setFlag(Flags.Flag.DELETED, true);
    parent.expunge();
  }

  public void delete() throws MessagingException {
    msg.setFlag(Flags.Flag.DELETED, true);
    parent.expunge();
  }
}