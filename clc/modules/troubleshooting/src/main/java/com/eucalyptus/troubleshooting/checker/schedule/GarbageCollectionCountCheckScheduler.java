package com.eucalyptus.troubleshooting.checker.schedule;

import java.util.concurrent.ScheduledFuture;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.checker.GarbageCollectionCountResourceCheck;
import com.eucalyptus.troubleshooting.checker.GarbageCollectionCountResourceCheck.GarbageCollectionChecker;
import com.eucalyptus.troubleshooting.checker.GarbageCollectionCountResourceCheck.GarbageCollectorInfo;

public class GarbageCollectionCountCheckScheduler {

	private static ScheduledFuture<?> garbageCollectionCountCheckerScheduledFuture = null;
	public static void garbageCollectionCountCheck() {
		String threshold = TroubleshootingBootstrapper.GC_COUNT_CHECK_THRESHOLD; 
		String pollTime = TroubleshootingBootstrapper.GC_COUNT_CHECK_POLL_TIME;
		String name = TroubleshootingBootstrapper.GC_COUNT_CHECK_NAME;
		if (garbageCollectionCountCheckerScheduledFuture != null) {
			garbageCollectionCountCheckerScheduledFuture.cancel(true);
		}
		GarbageCollectorInfo info = new GarbageCollectorInfo(name, Long.parseLong(threshold));
		GarbageCollectionChecker checker = new GarbageCollectionChecker(info, Eucalyptus.class, Long.parseLong(pollTime));
		garbageCollectionCountCheckerScheduledFuture = GarbageCollectionCountResourceCheck.start(checker);
	}

}
