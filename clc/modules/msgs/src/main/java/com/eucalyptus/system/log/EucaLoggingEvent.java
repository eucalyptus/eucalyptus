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

package com.eucalyptus.system.log;

import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class EucaLoggingEvent extends LoggingEvent {

	private static final long serialVersionUID = -2006882771771941367L;
	private Long threadId = null;
	
	public EucaLoggingEvent(String fqnOfCategoryClass,
			Category logger, Priority level, Object message, Throwable throwable) {
		super(fqnOfCategoryClass, logger, level, message, throwable);
	}

	public EucaLoggingEvent(String fqnOfCategoryClass,
			Category logger, long timeStamp, Priority level, Object message,
			Throwable throwable) {
		super(fqnOfCategoryClass, logger, timeStamp, level, message, throwable);
	}

	public EucaLoggingEvent(String fqnOfCategoryClass,
			Category logger, long timeStamp, Level level, Object message,
			String threadName, ThrowableInformation throwable, String ndc,
			LocationInfo info, Map properties) {
		super(fqnOfCategoryClass, logger, timeStamp, level, message,
				threadName, throwable, ndc, info, properties);
	}
	
	public long getThreadId() {
		if (threadId == null) {
			threadId = Thread.currentThread().getId();
		}
		return threadId;
	}
}
