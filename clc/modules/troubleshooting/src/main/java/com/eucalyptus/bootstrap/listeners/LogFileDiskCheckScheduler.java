package com.eucalyptus.bootstrap.listeners;

import java.util.concurrent.ScheduledFuture;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.troubleshooting.resourcefaults.DiskResourceCheck;
import com.eucalyptus.troubleshooting.resourcefaults.DiskResourceCheck.Checker;
import com.eucalyptus.troubleshooting.resourcefaults.DiskResourceCheck.LocationInfo;

public class LogFileDiskCheckScheduler {
	private static ScheduledFuture<?> logFileDiskCheckerScheduledFuture = null;
	public static void resetLogFileDiskCheck() {
		String threshold = TroubleshootingBootstrapper.LOG_FILE_DISK_CHECK_THRESHOLD; 
		String pollTime = TroubleshootingBootstrapper.LOG_FILE_DISK_CHECK_POLL_TIME;
		if (logFileDiskCheckerScheduledFuture != null) {
			logFileDiskCheckerScheduledFuture.cancel(true);
		}
		LocationInfo info = null;
		if (threshold.endsWith("%")) {
			info = new LocationInfo(BaseDirectory.LOG.getFile(), 
					Double.parseDouble(threshold.substring(0,  threshold.length() - 1)));
		} else {
			info = new LocationInfo(BaseDirectory.LOG.getFile(), 
					Long.parseLong(threshold));

		}
		Checker checker = new Checker(info, Eucalyptus.class, Long.parseLong(pollTime));
		logFileDiskCheckerScheduledFuture = DiskResourceCheck.start(checker);
	}
}
