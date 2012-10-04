package com.eucalyptus.troubleshooting.resourcefaults.schedulers;

import java.util.concurrent.ScheduledFuture;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.resourcefaults.MXBeanMemoryResourceCheck;
import com.eucalyptus.troubleshooting.resourcefaults.MXBeanMemoryResourceCheck.MXBeanInfo;
import com.eucalyptus.troubleshooting.resourcefaults.MXBeanMemoryResourceCheck.MXBeanMemoryChecker;

public class MXBeanMemoryCheckScheduler {
	private static ScheduledFuture<?> mxBeanMemoryCheckerScheduledFuture = null;
	public static void resetMXBeanMemoryCheck() {
		String threshold = TroubleshootingBootstrapper.SIMPLE_MEMORY_CHECK_THRESHOLD; 
		String pollTime = TroubleshootingBootstrapper.SIMPLE_MEMORY_CHECK_POLL_TIME;
		if (mxBeanMemoryCheckerScheduledFuture != null) {
			mxBeanMemoryCheckerScheduledFuture.cancel(true);
		}
		MXBeanInfo info = null;
		if (threshold.endsWith("%")) {
			info = new MXBeanInfo(Double.parseDouble(threshold.substring(0,  threshold.length() - 1)));
		} else {
			info = new MXBeanInfo(Long.parseLong(threshold));

		}
		MXBeanMemoryChecker checker = new MXBeanMemoryChecker(info, Eucalyptus.class, Long.parseLong(pollTime));
		mxBeanMemoryCheckerScheduledFuture = MXBeanMemoryResourceCheck.start(checker);
	}
}
