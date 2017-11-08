/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.bittorrent;

import java.io.File;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class Tracker extends Thread {
  private static Logger LOG = Logger.getLogger(Tracker.class);
  private static Tracker tracker;

  private Process proc;

  public Tracker( ) {
    super( Threads.threadUniqueName( "osg-torrent-tracker" ) );
  }

  public static void initialize() {
    tracker = new Tracker();
    if (tracker.exists()) {
      ObjectStorageProperties.enableTorrents = true;
      tracker.start();
      Runtime.getRuntime().addShutdownHook(new Thread( "osg-torrent-shutdown-hook" ) {
        public void run() {
          tracker.bye();
          Collection<TorrentClient> torrentClients = Torrents.getClients();
          for (TorrentClient torrentClient : torrentClients) {
            torrentClient.bye();
          }
        }
      });
    } else {
      LOG.warn("bttrack not found (bittorrent installed?). Torrent support disabled (non critical).");
    }
  }

  public boolean exists() {
    try {
      return (findTracker().length() > 0);
    } catch (EucalyptusCloudException e) {
      return false;
    }
  }

  private String findTracker() throws EucalyptusCloudException {
    return SystemUtil.run(new String[] {ObjectStorageProperties.TRACKER_BINARY});
  }

  public void run() {
    track();
  }

  private void track() {
    new File(ObjectStorageProperties.TRACKER_DIR).mkdirs();
    try {
      Runtime rt = Runtime.getRuntime();
      proc =
          rt.exec(new String[] {ObjectStorageProperties.TRACKER_BINARY, "--port", ObjectStorageProperties.TRACKER_PORT, "--dfile",
              ObjectStorageProperties.TRACKER_DIR + "dstate", "--logfile", ObjectStorageProperties.TRACKER_DIR + "tracker.log"});
      StreamConsumer error = new StreamConsumer(proc.getErrorStream());
      StreamConsumer output = new StreamConsumer(proc.getInputStream());
      error.start();
      output.start();
      Thread.sleep(300);
      String errValue = error.getReturnValue();
      if (errValue.length() > 0) {
        if (!errValue.contains("already in use"))
          LOG.warn(errValue);
      }
    } catch (Exception t) {
      t.printStackTrace();
    }
  }

  public void bye() {
    if (proc != null)
      proc.destroy();
  }

  public static void die() {
    if (tracker != null) {
      tracker.bye();
    }
  }
}
