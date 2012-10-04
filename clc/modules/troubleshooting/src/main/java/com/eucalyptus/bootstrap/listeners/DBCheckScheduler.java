package com.eucalyptus.bootstrap.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.logicalcobwebs.proxool.ProxoolFacade;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.resourcefaults.DBResourceCheck;
import com.eucalyptus.troubleshooting.resourcefaults.DBResourceCheck.DBChecker;
import com.eucalyptus.troubleshooting.resourcefaults.DBResourceCheck.DBPoolInfo;

public class DBCheckScheduler {
	private static ScheduledFuture<?> dbCheckerScheduledFuture = null;
	public static void resetDBCheck() {
		String threshold = TroubleshootingBootstrapper.DB_CHECK_THRESHOLD; 
		String pollTime = TroubleshootingBootstrapper.DB_CHECK_POLL_TIME;
		if (dbCheckerScheduledFuture != null) {
			dbCheckerScheduledFuture.cancel(true);
		}
		List<DBPoolInfo> dbPools = new ArrayList<DBPoolInfo>();
		String[] aliases = ProxoolFacade.getAliases();
		if (aliases != null) {
			for (String alias : aliases) {
				DBPoolInfo info = null;
				if (threshold.endsWith("%")) {
					info = new DBPoolInfo(alias, 
							Double.parseDouble(threshold.substring(0,  threshold.length() - 1)));
				} else {
					info = new DBPoolInfo(alias, 
							Integer.parseInt(threshold));

				}
				dbPools.add(info); // Do 2%
			}
		}
		DBChecker dbChecker = new DBChecker(dbPools, Eucalyptus.class, Long.parseLong(pollTime));
		dbCheckerScheduledFuture = DBResourceCheck.start(dbChecker);
	}


}
