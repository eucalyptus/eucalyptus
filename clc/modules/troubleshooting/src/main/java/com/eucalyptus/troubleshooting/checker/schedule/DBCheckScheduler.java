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
