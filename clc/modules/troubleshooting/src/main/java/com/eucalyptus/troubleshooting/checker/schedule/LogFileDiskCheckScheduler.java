/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
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
