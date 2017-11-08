/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.bootstrap;

import com.eucalyptus.stats.StatsManager;
import org.apache.log4j.Logger;

import com.eucalyptus.component.Faults;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.troubleshooting.changelisteners.DBCheckPollTimeListener;
import com.eucalyptus.troubleshooting.changelisteners.DBCheckThresholdListener;
import com.eucalyptus.troubleshooting.changelisteners.MemoryCheckPollTimeListener;
import com.eucalyptus.troubleshooting.changelisteners.MemoryCheckRatioListener;
import com.eucalyptus.troubleshooting.changelisteners.LogFileDiskCheckPollTimeListener;
import com.eucalyptus.troubleshooting.changelisteners.LogFileDiskCheckThresholdListener;
import com.eucalyptus.troubleshooting.changelisteners.LogLevelListener;
import com.eucalyptus.troubleshooting.changelisteners.TriggerFaultListener;
import com.eucalyptus.troubleshooting.checker.schedule.DBCheckScheduler;
import com.eucalyptus.troubleshooting.checker.schedule.MemoryCheckScheduler;
import com.eucalyptus.troubleshooting.checker.schedule.LogFileDiskCheckScheduler;

@Provides(Empyrean.class)
@RunDuring(Bootstrap.Stage.CloudServiceInit)
@ConfigurableClass(root = "cloud", description = "Parameters controlling troubleshooting information.")
public class TroubleshootingBootstrapper extends Bootstrapper {

	private static final Logger LOG = Logger
			.getLogger(TroubleshootingBootstrapper.class);

	@Override
	public boolean load() throws Exception {
    LOG.info("Loading troubleshooting interface.");
    return true;
	}

	@Override
	public boolean start() throws Exception {
    LOG.info("Starting troubleshooting interface.");
		LogFileDiskCheckScheduler.resetLogFileDiskCheck();
		DBCheckScheduler.resetDBCheck();
		MemoryCheckScheduler.memoryCheck();
		Faults.init();
    LOG.info("Starting monitoring interface");

    try {
      StatsManager.init();
      StatsManager.start();
    } catch (Throwable f) {
      LOG.fatal("Could not initialize and start the monitoring interface. Failing bootstrap", f);
      throw f;
    }

    return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
	 */
	@Override
	public boolean enable() throws Exception {
    return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
	 */
	@Override
	public boolean stop() throws Exception {
        LOG.info("Stopping troubleshooting interface");
        StatsManager.stop();
        return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
	 */
	@Override
	public void destroy() throws Exception {
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
	 */
	@Override
	public boolean disable() throws Exception {
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#check()
	 */
	@Override
	public boolean check() throws Exception {
        try {
            StatsManager.check();
        } catch(Exception e) {
            LOG.error("Stat manager failed check. Failing Troubleshooting check call as well.",e);
            return false;
        }
        return true;
	}

	@ConfigurableField(description = "Poll time (ms) for log file disk check", initial = "5000", changeListener = LogFileDiskCheckPollTimeListener.class, displayName = "log.file.disk.check.poll.time")
	public static String LOG_FILE_DISK_CHECK_POLL_TIME = "5000";

	@ConfigurableField(description = "Threshold (bytes or %) for log file disk check", initial = "2.0%", changeListener = LogFileDiskCheckThresholdListener.class, displayName = "log.file.disk.check.threshold")
	public static String LOG_FILE_DISK_CHECK_THRESHOLD = "2.0%";

	@ConfigurableField(description = "Poll time (ms) for db connection check", initial = "60000", changeListener = DBCheckPollTimeListener.class, displayName = "db.check.poll.time")
	public static String DB_CHECK_POLL_TIME = "60000";

	@ConfigurableField(description = "Threshold (num connections or %) for db connection check", initial = "2.0%", changeListener = DBCheckThresholdListener.class, displayName = "db.check.threshold")
	public static String DB_CHECK_THRESHOLD = "2.0%";

	@ConfigurableField(description = "Poll time (ms) for memory check", initial = "5000", changeListener = MemoryCheckPollTimeListener.class, displayName = "memory.check.poll.time")
	public static String MEMORY_CHECK_POLL_TIME = "5000";

	@ConfigurableField(description = "Ratio (of post-garbage collected old-gen memory) for memory check", initial = "0.98", changeListener = MemoryCheckRatioListener.class, displayName = "memory.check.ratio")
	public static String MEMORY_CHECK_RATIO = "0.98";

	@ConfigurableField( description = "Fault id last used to trigger test", initial = "", changeListener = TriggerFaultListener.class, displayName = "trigger.fault" )
	public static String TRIGGER_FAULT = "";

	@ConfigurableField(description = "Log level for dynamic override.", initial = "INFO", changeListener = LogLevelListener.class, displayName = "euca.log.level")
	public static String EUCA_LOG_LEVEL = Logs.isExtrrreeeme() ? "EXTREME" : System.getProperty("euca.log.level", "");

}
