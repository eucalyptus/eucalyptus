/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package edu.ucsb.eucalyptus.cloud.ws;
/*
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
