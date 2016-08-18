/*
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
 */

import com.eucalyptus.stats.sensors.Sensors;

//5 sec buffer for ttl over polling interval by default
default_ext = 1

//Memory sensor polling interval
memoryUsagePollingIntervalSeconds = 60
memoryUsageTtlSeconds = memoryUsagePollingIntervalSeconds + default_ext

//Component check interval
//Seconds between service check() calls
componentsPollingIntervalSeconds = 60
componentsCheckTtlSeconds = componentsPollingIntervalSeconds + default_ext

//Long polling for threads since it is expensive
threadPollingIntervalSeconds = 60
threadTtlSeconds = threadPollingIntervalSeconds + default_ext

//Long polling for db connections since it is expensive
dbPollingIntervalSeconds = 60
dbTtlSeconds = dbPollingIntervalSeconds + default_ext

contextsIntervalSeconds = 60
contextsEventTtlSeconds = contextsIntervalSeconds + default_ext

return [ ] +
        Sensors.JvmMemorySensors(memoryUsagePollingIntervalSeconds, memoryUsageTtlSeconds) +
        Sensors.JvmThreadSensors(threadPollingIntervalSeconds, threadTtlSeconds) +
        Sensors.DbConnectionPoolSensors(dbPollingIntervalSeconds, dbTtlSeconds) +
        Sensors.ComponentsSensor(componentsPollingIntervalSeconds, componentsCheckTtlSeconds) +
        Sensors.ContextSensor(contextsIntervalSeconds, contextsEventTtlSeconds)

