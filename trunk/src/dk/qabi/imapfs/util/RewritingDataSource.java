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
package dk.qabi.imapfs.util;

import javax.activation.DataSource;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

/**
 * This activation framework DataSource implementation is capable of rewriting all or part of existing data. 
 */
public class RewritingDataSource implements DataSource {
  private ByteBuffer buf;
  private long offset;
  private DataHandler original;

  public RewritingDataSource(DataHandler original, ByteBuffer buf, long offset) throws MessagingException, IOException {
    this.original = original;
    this.buf = buf;
    this.offset = offset;
  }

  public InputStream getInputStream() throws IOException {
    if (offset > 0) {
      // Read first N bytes from original input
      return new ConcatInputStream(original.getInputStream(), offset, buf.array());
    } else {
      // Read all data directly from buffer
      return new ByteArrayInputStream(buf.array());
    }
  }

  public OutputStream getOutputStream() throws IOException {
    throw new IOException("Direct writing not supportd");
  }

  public String getContentType() {
    return original.getContentType();
  }

  public String getName() {
    return original.getName();
  }
}
