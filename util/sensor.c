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
