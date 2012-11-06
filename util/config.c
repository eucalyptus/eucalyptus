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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <assert.h>
#include "string.h"
#include "misc.h"
#include "config.h"

static int configRestartLen=0, configNoRestartLen=0;
static char *configValuesRestart[256], *configValuesNoRestart[256];
static configEntry * configKeysRestart, * configKeysNoRestart;
static time_t lastConfigMtime = 0;

void configInitValues (configEntry newConfigKeysRestart[], configEntry newConfigKeysNoRestart[])
{
  configKeysRestart = newConfigKeysRestart;
  configKeysNoRestart = newConfigKeysNoRestart;
}

int isConfigModified (char configFiles[][MAX_PATH], int numFiles)
{    
    struct stat statbuf;
    time_t configMtime = 0;
    
    for (int i=0; i<numFiles; i++) {
        // stat the config file, update modification time
        if (stat(configFiles[i], &statbuf) == 0) {
            if (statbuf.st_mtime > 0 || statbuf.st_ctime > 0) {
                if (statbuf.st_ctime > statbuf.st_mtime) {
                    configMtime = statbuf.st_ctime;
                } else {
                    configMtime = statbuf.st_mtime;
                }
            }
        }
    }
    if (configMtime == 0) {
        logprintfl(EUCAERROR, "could not stat config files (%s,%s)\n", configFiles[0], configFiles[1]);
        return -1;
    }
    
    if (lastConfigMtime!=configMtime) {
        logprintfl(EUCADEBUG, "current mtime=%d, stored mtime=%d\n", configMtime, lastConfigMtime);
        lastConfigMtime=configMtime;
        return 1;
    }
    return 0;
}

char *configFileValue(const char *key) 
{
    int i;
    for (i=0; i<configRestartLen; i++) {
        if (configKeysRestart[i].key) {
            if (!strcmp (configKeysRestart[i].key, key)) { // have a match among values that restart
                if (configValuesRestart[i])
                    return strdup(configValuesRestart[i]);
                else if (configKeysRestart[i].defaultValue)
                    return strdup(configKeysRestart[i].defaultValue);
                else
                    return NULL;
            }
        }
    }
    for (i=0; i<configNoRestartLen; i++) {
        if (configKeysNoRestart[i].key) {
            if (!strcmp(configKeysNoRestart[i].key, key)) {
                if (configValuesNoRestart[i])
                    return strdup(configValuesNoRestart[i]);
                else if (configKeysNoRestart[i].defaultValue)
                    return strdup(configKeysNoRestart[i].defaultValue);
                else
                    return NULL;
            }
        }
    }
    
    return NULL;
}

int configFileValueLong(const char *key, long *val)
{
    int found = 0;
    char * tmpstr = configFileValue(key);
    if (tmpstr != NULL) {
        char * endptr;
        errno = 0;
        long v = strtoll (tmpstr, &endptr, 10);
        if (errno == 0 && *endptr == '\0') { // successful complete conversion
            * val = v;
            found = 1;
        }
        free (tmpstr);
    }
    return found;
}

int readConfigFile(char configFiles[][MAX_PATH], int numFiles) 
{
  int i, ret=0;
  char *old=NULL, *new=NULL;

  for (i=0; configKeysRestart[i].key; i++) {
    old = configValuesRestart[i];
    new = getConfString(configFiles, numFiles, configKeysRestart[i].key);
    if (configRestartLen) {
      if ( (!old && new) || (old && !new) || ( (old && new) && strcmp(old, new) ) ) {
	logprintfl(EUCAWARN, "configuration file changed (KEY=%s, ORIGVALUE=%s, NEWVALUE=%s): clean restart is required before this change will take effect!\n", configKeysRestart[i].key, SP(old), SP(new));
      }
      if (new) free(new);
    } else {
      logprintfl(EUCAINFO, "read (%s=%s, default=%s)\n", configKeysRestart[i].key, SP(new), SP(configKeysRestart[i].defaultValue));
      if (configValuesRestart[i]) free(configValuesRestart[i]);
      configValuesRestart[i] = new;
      ret++;
    }
  }
  configRestartLen = i;
  
  for (i=0; configKeysNoRestart[i].key; i++) {
    old = configValuesNoRestart[i];
    new = getConfString(configFiles, numFiles, configKeysNoRestart[i].key);
    
    if (configNoRestartLen) {
      if ( (!old && new) || (old && !new) || ( (old && new) && strcmp(old, new) ) ) {
	logprintfl(EUCAINFO, "configuration file changed (KEY=%s, ORIGVALUE=%s, NEWVALUE=%s): change will take effect immediately.\n", configKeysNoRestart[i].key, SP(old), SP(new));
	ret++;
	if (configValuesNoRestart[i]) free(configValuesNoRestart[i]);
	configValuesNoRestart[i] = new;
      } else {
	if (new) free(new);
      }
    } else {
      logprintfl(EUCAINFO, "read (%s=%s, default=%s)\n", configKeysNoRestart[i].key, SP(new), SP(configKeysNoRestart[i].defaultValue));
      if (configValuesNoRestart[i]) free(configValuesNoRestart[i]);
      configValuesNoRestart[i] = new;
      ret++;
    }
  }
  configNoRestartLen = i;

  return(ret);
}

// helper for reading log-related params from eucalyptus.conf
void configReadLogParams(int *log_level_out, int *log_roll_number_out, long *log_max_size_bytes_out, char **log_prefix)
{
    char * s = configFileValue ("LOGLEVEL");
    assert (s!=NULL); // configFileValue should return default
    * log_level_out = log_level_int (s);
    free(s);

    long l;
    configFileValueLong ("LOGROLLNUMBER", &l);
    * log_roll_number_out = (int)l;
    
    configFileValueLong ("LOGMAXSIZE", log_max_size_bytes_out);

    * log_prefix = configFileValue ("LOGPREFIX");
}
