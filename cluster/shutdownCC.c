/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <time.h>
#include <misc.h>
#include <euca_axis.h>
#include <data.h>
#include <cc-client-marshal.h>

ncMetadata mymeta;

int main(int argc, char **argv) {
  axutil_env_t * env = NULL;
  axis2_char_t * client_home = NULL;
  axis2_char_t endpoint_uri[256], *tmpstr;
  axis2_stub_t * stub = NULL;
  int rc, i, port, use_wssec;
  char *euca_home, configFile[1024], policyFile[1024], logFile[1024];

  bzero(&mymeta, sizeof(ncMetadata));
  mymeta.userId = strdup("admin");
  mymeta.correlationId = strdup("shutdowncorrid");
  mymeta.epoch = 1;
  mymeta.servicesLen = 0;
  euca_home = getenv("EUCALYPTUS");
  if (!euca_home) {
    snprintf(configFile, 1024, "/etc/eucalyptus/eucalyptus.conf");
    snprintf(policyFile, 1024, "/var/lib/eucalyptus/keys/cc-client-policy.xml");
    snprintf(logFile, 1024, "/var/log/eucalyptus/shutdownCC_axis2c.log");
  } else {
    snprintf(configFile, 1024, "%s/etc/eucalyptus/eucalyptus.conf", euca_home);
    snprintf(policyFile, 1024, "%s/var/lib/eucalyptus/keys/cc-client-policy.xml", euca_home);
    snprintf(logFile, 1024, "%s/var/log/eucalyptus/shutdownCC_axis2c.log", euca_home);
  }

  if (argc != 2 || !argv[1]) {
    printf("must supply argument <host>:<port>\n");
  }
  snprintf(endpoint_uri, 256," http://%s/axis2/services/EucalyptusCC", argv[1]);

  env =  axutil_env_create_all(logFile, AXIS2_LOG_LEVEL_TRACE);
  
  client_home = AXIS2_GETENV("AXIS2C_HOME");
  if (!client_home) {
    printf("must have AXIS2C_HOME set\n");
    exit(1);
  }
  stub = axis2_stub_create_EucalyptusCC(env, client_home, endpoint_uri);
  
  rc = InitWSSEC(env, stub, policyFile);
  if (rc) {
    printf("cannot initialize WS-SEC policy (%s)\n",policyFile);
    exit(1);
  } 
  
  rc = cc_shutdownService(env, stub);
  if (rc != 0) {
    printf("cc_shutdownService() failed\n");
    exit(1);
  }

  sleep(10);
  exit(0);
}



