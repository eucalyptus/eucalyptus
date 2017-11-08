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

package com.eucalyptus.stats.sensors;

import com.eucalyptus.stats.StatsOutputValues;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ProxoolFacade;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Check for db pools
 */
public enum DbPoolSensor implements Callable<Map<String, Object>> {
    INSTANCE {
        public Map<String, Object> call() {
            Map<String, Object> results = Maps.newTreeMap();
            try {
                long max = 0;
                long active = 0;
                for (String alias : ProxoolFacade.getAliases()) {
                    max = ProxoolFacade.getConnectionPoolDefinition(alias).getMaximumConnectionCount();
                    active = ProxoolFacade.getSnapshot(alias, true).getActiveConnectionCount();
                    results.put(alias + ".MaxConnectionCount", max);
                    results.put(alias + ".ActiveConnectionCount", active);
                    results.put(alias + ".CapacityCheck", active < max ? StatsOutputValues.CHECK_OK : StatsOutputValues.CHECK_FAILED);
                }
            } catch (Throwable f) {
                LOG.warn("Error getting proxool db connection information. Continuing.", f);
            }
            return results;
        }
    };
    private static final Logger LOG = Logger.getLogger(DbPoolSensor.class);
}
