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
