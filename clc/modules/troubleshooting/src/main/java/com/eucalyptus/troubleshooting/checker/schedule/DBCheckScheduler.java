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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.logicalcobwebs.proxool.ProxoolFacade;

import com.eucalyptus.bootstrap.TroubleshootingBootstrapper;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.checker.DBResourceCheck;
import com.eucalyptus.troubleshooting.checker.DBResourceCheck.DBChecker;
import com.eucalyptus.troubleshooting.checker.DBResourceCheck.DBPoolInfo;

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
				dbPools.add(info); 
			}
		}
		DBChecker dbChecker = new DBChecker(dbPools, Eucalyptus.class, Long.parseLong(pollTime));
		dbCheckerScheduledFuture = DBResourceCheck.start(dbChecker);
	}


}
