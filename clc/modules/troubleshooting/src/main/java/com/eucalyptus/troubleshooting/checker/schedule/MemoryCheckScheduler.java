package com.eucalyptus.troubleshooting.checker.schedule;

import java.util.concurrent.ScheduledFuture;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.checker.MemoryCheck;
import com.eucalyptus.troubleshooting.checker.MemoryCheck.GarbageCollectionChecker;

public class MemoryCheckScheduler {

	private static ScheduledFuture<?> memoryCheckerScheduledFuture = null;
	public static void memoryCheck() {
		String ratioStr = TroubleshootingBootstrapper.MEMORY_CHECK_RATIO; 
		String pollTime = TroubleshootingBootstrapper.MEMORY_CHECK_POLL_TIME;
		if (memoryCheckerScheduledFuture != null) {
			memoryCheckerScheduledFuture.cancel(true);
		}
		GarbageCollectionChecker checker = new GarbageCollectionChecker(Double.parseDouble(ratioStr), Eucalyptus.class, Long.parseLong(pollTime));
		memoryCheckerScheduledFuture = MemoryCheck.start(checker);
	}

}
