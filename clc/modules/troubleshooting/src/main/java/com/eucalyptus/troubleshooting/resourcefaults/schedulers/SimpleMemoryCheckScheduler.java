package com.eucalyptus.troubleshooting.resourcefaults.schedulers;

import java.util.concurrent.ScheduledFuture;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.resourcefaults.SimpleMemoryResourceCheck;
import com.eucalyptus.troubleshooting.resourcefaults.SimpleMemoryResourceCheck.MemoryChecker;
import com.eucalyptus.troubleshooting.resourcefaults.SimpleMemoryResourceCheck.MemoryInfo;

public class SimpleMemoryCheckScheduler {
	private static ScheduledFuture<?> simpleMemoryCheckerScheduledFuture = null;
	public static void resetMemoryCheck() {
		String threshold = TroubleshootingBootstrapper.SIMPLE_MEMORY_CHECK_THRESHOLD; 
		String pollTime = TroubleshootingBootstrapper.SIMPLE_MEMORY_CHECK_POLL_TIME;
		if (simpleMemoryCheckerScheduledFuture != null) {
			simpleMemoryCheckerScheduledFuture.cancel(true);
		}
		MemoryInfo info = null;
		if (threshold.endsWith("%")) {
			info = new MemoryInfo(Double.parseDouble(threshold.substring(0,  threshold.length() - 1)));
		} else {
			info = new MemoryInfo(Long.parseLong(threshold));

		}
		MemoryChecker checker = new MemoryChecker(info, Eucalyptus.class, Long.parseLong(pollTime));
		simpleMemoryCheckerScheduledFuture = SimpleMemoryResourceCheck.start(checker);
	}
}
