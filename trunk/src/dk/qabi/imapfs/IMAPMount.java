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

import java.net.URL;
import java.net.MalformedURLException;

import fuse.*;
import javax.mail.MessagingException;

/**
 * This class has the main method and thereby implements the application for mounting an IMAP filesystem.
 */
public class IMAPMount {

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