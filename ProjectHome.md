# imapfs #

Implements a filesystem which stores its data on an IMAP server.

# How to dive in #
  * Checkout source from svn
  * Set your JAVA\_HOME to a version 1.5 SDK
  * When in the root dir of the project Invoke 'bin/imapfsmnt' <imap url> 

&lt;mountpoint&gt;



```
bin/imapfsmnt imap://username:password@mailserver.com/INBOX /Volumes/imapfstest
```

  * Start hacking!

# Short tour #
The application is built using a slightly modified version of fuse-j version 2.2.3. The compiled 'libjavafs.jnilib' file is contained in subversion.

The application then simply invoke fuse-j to mount the filesystem, "parking" the thread there. Whenever the thread exists the application exits.

The key part is the 'IMAPFileSystem' class which implements the fuse-j callback API, with the actual functionality of accessing an IMAP server etc.

The intention is that folders on the IMAP server corresponds to directories in the filesystem, and messages to files. The subject of a message contains the files name and the actual data is stored in a MIME attachement on the message.

# Welcome contributions #
  * Unit tests
  * Implementing currently unsupported file operations
  * Porting to linux (different dynamic library and probably different mount script)
  * Nicer packaging. For instance mpkg for installing on a mac.
  * GUI for configuring settings.
  * Implementing "striping" of IMAP servers, so a single volume can somehow represent multiple IMAP servers
  * Good ideas for features and implementation


I can be contacted at dennis.thrysoe@gmail.com

_-dennis_