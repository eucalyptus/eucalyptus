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

