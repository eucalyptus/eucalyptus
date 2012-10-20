package com.eucalyptus.troubleshooting.checker.schedule;

import java.util.concurrent.ScheduledFuture;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.checker.PermGenMemoryCheck;
import com.eucalyptus.troubleshooting.checker.PermGenMemoryCheck.PermGenMemoryChecker;

public class PermGenMemoryCheckScheduler {
	private static ScheduledFuture<?> permGenCheckerScheduledFuture = null;
	public static void resetMXBeanMemoryCheck() {
		String ratioStr = TroubleshootingBootstrapper.PERM_GEN_MEMORY_CHECK_RATIO; 
		String pollTime = TroubleshootingBootstrapper.PERM_GEN_MEMORY_CHECK_POLL_TIME;
		if (permGenCheckerScheduledFuture != null) {
			permGenCheckerScheduledFuture.cancel(true);
		}
		PermGenMemoryChecker checker = new PermGenMemoryChecker(Double.parseDouble(ratioStr), Eucalyptus.class, Long.parseLong(pollTime));
		permGenCheckerScheduledFuture = PermGenMemoryCheck.start(checker);
	}
}
