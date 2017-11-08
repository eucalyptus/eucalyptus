/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009 Ent. Services Development Corporation LP
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
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.blockstorage.san.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.san.common.SessionManager.TaskRunner;
import com.eucalyptus.blockstorage.san.common.entities.SANInfo;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ShellSessionManager implements SessionManager, TaskRunner {
  private BufferedWriter writer;
  private BufferedReader reader;
  private Channel channel;
  private static Logger LOG = Logger.getLogger(ShellSessionManager.class);
  private static long promptTimeout = Long.parseLong(System.getProperty("com.eucalyptus.blockstorage.shell.promptTimeout", "15000"));
  private static boolean logTiming = Boolean.valueOf(System.getProperty("com.eucalyptus.blockstorage.shell.logTiming"));

  public ShellSessionManager() {}

  private void readToPrompt(long timeout) throws IOException, InterruptedException {
    String text = "";
    char[] buffer = new char[512];
    long until = System.currentTimeMillis() + timeout;
    while (System.currentTimeMillis() < until) {
      while (reader.ready()) {
        int read = reader.read(buffer);
        text += new String(buffer, 0, read);
      }
      if (!text.isEmpty() && !text.endsWith("\r") && !text.endsWith("\n"))
        return;
      Thread.sleep(50);
    }
    LOG.warn("Timed out reading prompt");
  }

  public synchronized void connect() throws EucalyptusCloudException {
    try {
      JSch jsch = new JSch();
      Session session;
      SANInfo sanInfo = SANInfo.getStorageInfo();
      session = jsch.getSession(sanInfo.getSanUser(), sanInfo.getSanHost());
      session.setConfig("StrictHostKeyChecking", "no");
      session.setPassword(sanInfo.getSanPassword());
      session.connect();
      channel = session.openChannel("shell");
      PipedOutputStream outStream = new PipedOutputStream();
      channel.setInputStream(new PipedInputStream(outStream));
      PipedInputStream inStream = new PipedInputStream();
      channel.setOutputStream(new PipedOutputStream(inStream));
      channel.connect();
      writer = new BufferedWriter(new OutputStreamWriter(outStream, "utf-8"));
      reader = new BufferedReader(new InputStreamReader(inStream, "utf-8"));
      readToPrompt(promptTimeout);
    } catch (JSchException | IOException | InterruptedException e) {
      throw new EucalyptusCloudException(e);
    }
  }

  /**
   *
   * Caller must be synchronized:
   *
   * ShellSessionManager manager = ... synchronized( manager ) { try ( final TaskRunner runner = manager.getTaskRunner( ) ) { ... } }
   *
   */
  public TaskRunner getTaskRunner(String description) {
    return getTaskRunner(description, null);
  }

  /**
   *
   * Caller must be synchronized
   */
  private TaskRunner getTaskRunner(final String description, @Nullable final Map<String, Long> timings) {
    return new TaskRunner() {
      private boolean connected = false;
      private int taskNumber = 1;
      private Map<String, Long> timingMap = timings == null ? Maps.<String, Long>newLinkedHashMap() : timings;

      private void init() {
        try {
          connect();
          connected = true;
        } catch (EucalyptusCloudException e) {
          LOG.error(e);
        }
      }

      @Override
      public String runTask(final AbstractSANTask task) throws InterruptedException {
        String returnValue = "";
        if (!connected) {
          timingMap.put("begin", System.currentTimeMillis());
          init();
          timingMap.put("connect", System.currentTimeMillis());
        }
        if (!connected)
          return returnValue;
        timingMap.put("pre-task-" + taskNumber, System.currentTimeMillis());
        try {
          writer.write("" + task.getCommand() + task.getEOFCommand());
          writer.flush();
          for (String line = null; (line = reader.readLine()) != null;) {
            line = line + "\r";
            if (line.contains("" + task.getEOFCommand()))
              break;
            returnValue += line;
          }
        } catch (IOException e) {
          LOG.error(e, e);
        } finally {
          timingMap.put("task-" + taskNumber, System.currentTimeMillis());
          taskNumber++;
        }
        return returnValue;
      }

      @Override
      public void close() {
        timingMap.put("pre-close", System.currentTimeMillis());
        try {
          if (writer != null)
            try {
              writer.write("logout\r");
              writer.flush();
            } catch (Exception e) {
              LOG.warn("Error logging out of session", e);
            }

          if (reader != null) {
            Closeables.close(reader, true);
            reader = null;
          }

          if (writer != null) {
            Closeables.close(writer, true);
            writer = null;
          }

          // Tear it down. Do not persist session.
          // Doing so causes more issues than it is worth.
          // EQL serializes anyway and the overhead is
          // minor.
          if (channel != null) {
            channel.getSession().disconnect();
            channel.disconnect();
            channel = null;
          }

        } catch (JSchException | IOException e) {
          LOG.error(e, e);
        } finally {
          timingMap.put("close", System.currentTimeMillis());
          if (logTiming)
            dumpTiming(timingMap, description);
        }
      }
    };
  }

  public String runTask(final AbstractSANTask task) throws InterruptedException {
    String returnValue = "";
    final Map<String, Long> timingMap = Maps.newLinkedHashMap();
    timingMap.put("start", System.currentTimeMillis());
    synchronized (this) {
      try (final TaskRunner runner = getTaskRunner(String.valueOf(task.getCommand()))) {
        returnValue = runner.runTask(task);
      }
    }
    return returnValue;
  }

  private void dumpTiming(final Map<String, Long> timings, final String command) {
    final List<String> timingInfo = Lists.newArrayList();
    Long firstTime = 0l;
    Long lastTime = 0l;
    for (final Map.Entry<String, Long> timingEntry : timings.entrySet()) {
      if (lastTime != 0) {
        timingInfo.add(timingEntry.getKey() + ": " + (timingEntry.getValue() - lastTime) + "ms");
      } else {
        firstTime = timingEntry.getValue();
      }
      lastTime = timingEntry.getValue();
    }
    LOG.debug("Command '" + (command.replace('\r', ';')) + "', took " + (lastTime - firstTime) + "ms " + timingInfo);
  }

  public void stop() throws EucalyptusCloudException {
    // Do not disconnect the channel while operations are in flight
  }

  @Override
  public void close() {}
}
