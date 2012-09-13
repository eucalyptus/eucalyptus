// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h> // strlen, strcpy
#include <ctype.h> // isspace
#include <assert.h>
#include <stdarg.h>

#include "misc.h"
#include "sensor.h"

int sensor_str2type (const char * counterType)
{
    for (int i=0; i<(sizeof (sensorCounterTypeName) / sizeof (char *)); i++) {
        if (strcmp (sensorCounterTypeName[i], counterType) == 0)
            return i;
    }
    logprintfl (EUCAERROR, "internal error (sensor counter type out of range)\n");
    return -1;
}

const char * sensor_type2str (int type)
{
    if (type>=0 && type<(sizeof (sensorCounterTypeName) / sizeof (char *)))
        return sensorCounterTypeName[type];
    else
        return "[invalid]";
}

int sensor_res2str (char * buf, int bufLen, const sensorResource **res, int resLen)
{
    char * s = buf;
    int left = bufLen-1;
    int printed;

    for (int r=0; r<resLen; r++) {
        const sensorResource * sr = res [r];
        printed = snprintf (s, left, "resource: %s type: %s metrics: %d\n", sr->resourceName, sr->resourceType, sr->metricsLen);
#define MAYBE_BAIL s = s + printed; left = left - printed; if (left < 1) return (bufLen - left);
        MAYBE_BAIL
        for (int m=0; m<sr->metricsLen; m++) {
            const sensorMetric * sm = sr->metrics + m;
            printed = snprintf (s, left, "\tmetric: %s counters: %d\n", sm->metricName, sm->countersLen); 
            MAYBE_BAIL
            for (int c=0; c<sm->countersLen; c++) {
                const sensorCounter * sc = sm->counters + c;
                printed = snprintf (s, left, "\t\tcounter: %s interval: %lld seq: %lld dimensions: %d\n", 
                                    sensor_type2str(sc->type), sc->collectionIntervalMs, sc->sequenceNum, sc->dimensionsLen);
                MAYBE_BAIL
                for (int d=0; d<sc->dimensionsLen; d++) {
                    const sensorDimension * sd = sc->dimensions + d;
                    printed = snprintf (s, left, "\t\t\tdimension: %s values: %d\n", sd->dimensionName, sd->valuesLen);
                    MAYBE_BAIL
                    for (int v=0; v<sd->valuesLen; v++) {
                        const sensorValue * sv = sd->values + v;
                        printed = snprintf (s, left, "\t\t\t\t%lld %s %f\n", sv->timestampMs, sv->available?"YES":" NO", sv->available?sv->value:-1);
                        MAYBE_BAIL
                    }
                }
            }
        }
    }
    * s = '\0';

    return 0;
}

int sensor_set_instance_data (const char * instanceId, const char ** sensorIds, int sensorIdsLen, sensorResource * sr) // TODO3.2: actually implement the function
{
    sensorResource example = { 
        .resourceName = "i-23456",
        .resourceType = "instance",
        .metricsLen = 2,
        .metrics = { 
            {
                .metricName = "CPUUtilization",
                .countersLen = 1,
                .counters = {
                    {
                        .type = SENSOR_AVERAGE,
                        .collectionIntervalMs = 20000,
                        .sequenceNum = 0,
                        .dimensionsLen = 1,
                        .dimensions = {
                            {
                                .dimensionName = "default",
                                .valuesLen = 5,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 33.3, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 34.7, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 31.1, .available = 1 },
                                    { .timestampMs = 1344056970424, .value = 666, .available = 1 },
                                    { .timestampMs = 1344056990424, .value = 39.9, .available = 1 },
                                }
                            }
                        }
                    }
                }
            },
            {
                .metricName = "DiskReadOps",
                .countersLen = 1,
                .counters = {
                    {
                        .type = SENSOR_SUMMATION,
                        .collectionIntervalMs = 20000,
                        .sequenceNum = 0,
                        .dimensionsLen = 3,
                        .dimensions = {
                            {
                                .dimensionName = "root",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 111.0, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 2222222.0, .available = 1 },
                                }
                            },
                            {
                                .dimensionName = "ephemeral0",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 3333333.0, .available = 1 },
                                }
                            },
                            {
                                .dimensionName = "vol-34567",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 44444.0, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 55555555.0, .available = 1 },
                                }
                            }
                        }
                    }
                }
            } 
        }
    };
    memcpy (sr, &example, sizeof(sensorResource));
    strncpy (sr->resourceName, instanceId, sizeof(sr->resourceName));

    return 0;
}
