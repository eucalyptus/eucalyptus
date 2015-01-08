/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
