package edu.ucsb.eucalyptus.cloud.ws;
/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

import com.eucalyptus.util.WalrusProperties;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Collection;

public class Tracker extends Thread {
	private static Logger LOG = Logger.getLogger( Tracker.class );
	private static Tracker tracker;

	private Process proc;

	public static void initialize() {
		tracker = new Tracker();
		if(tracker.exists())  {
			WalrusProperties.enableTorrents = true;
			tracker.start();
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				public void run() {
					tracker.bye();
					Collection<TorrentClient> torrentClients = Torrents.getClients();
					for(TorrentClient torrentClient : torrentClients) {
						torrentClient.bye();
					}
				}
			});
		}
	}

	public boolean exists() {
		return (new File(WalrusProperties.TRACKER_BINARY)).exists();
	}

	public void run() {
		track();
	}

	private void track() {
		new File(WalrusProperties.TRACKER_DIR).mkdirs();
		try {
			Runtime rt = Runtime.getRuntime();
			proc = rt.exec(new String[]{WalrusProperties.TRACKER_BINARY, "--port", WalrusProperties.TRACKER_PORT, "--dfile", WalrusProperties.TRACKER_DIR + "dstate", "--logfile", WalrusProperties.TRACKER_DIR + "tracker.log"});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			Thread.sleep(300);
			String errValue = error.getReturnValue();
			if(errValue.length() > 0) {
				if(!errValue.contains("already in use"))
					LOG.warn(errValue);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void bye() {
		if(proc != null)
			proc.destroy();
	}

}