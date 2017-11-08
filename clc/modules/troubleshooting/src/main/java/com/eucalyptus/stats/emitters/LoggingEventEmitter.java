/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.stats.emitters;

import com.eucalyptus.stats.SystemMetric;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Created by zhill on 8/8/14.
 * <p/>
 * An event emitter backed by local-host filesystem. Events are written to a filesystem with
 * a specific layout/format.
 * <p/>
 * Events are written in json with service names forming a directory tree.
 */
public class LoggingEventEmitter implements EventEmitter {
    private static final Logger logger = Logger.getLogger(LoggingEventEmitter.class);
    private Level logLevel;

    public LoggingEventEmitter() {
        this(Level.INFO);
    }

    public LoggingEventEmitter(Level p) {
        this.logLevel = p;
    }

    @Override
    public boolean emit(SystemMetric event) {
        try {
            if (event == null) {
                return false;
            }

            logger.log(logLevel, event.toString());
            return true;
        } catch (Exception e) {
            logger.error("Failed to emit event!",e);
        }
        return false;
    }

    @Override
    public boolean doesBatching() {
        return false;
    }

    @Override
    public void check() throws Exception {
        return;
    }

}
