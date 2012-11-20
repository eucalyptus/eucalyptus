/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.troubleshooting.checker.schedule;

import java.util.concurrent.ScheduledFuture;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck.Checker;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck.LocationInfo;

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
