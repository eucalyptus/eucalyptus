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
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
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

#ifndef MODE
#define MODE 0
#endif

int main(int argc, char **argv) {
  axutil_env_t * env = NULL;
  axis2_char_t * client_home = NULL;
  axis2_char_t endpoint_uri[256], *tmpstr;
  axis2_stub_t * stub = NULL;
  int rc, i, port, use_wssec;
  char *euca_home, configFile[1024], policyFile[1024];
  
  if (MODE == 0) {
    if (argc != 2 || strcmp(argv[1], "-9")) {
      printf("only runnable from inside euca\n");
      exit(1);
    }
  } else {
    if (argc < 3) {
      printf("USAGE: CCclient <host:port> <command> <opts>\n");
      exit(1);
    }
  }
  
  euca_home = getenv("EUCALYPTUS");
  if (!euca_home) {
    snprintf(configFile, 1024, "/etc/eucalyptus/eucalyptus.conf");
    snprintf(policyFile, 1024, "/var/lib/eucalyptus/keys/cc-client-policy.xml");
  } else {
    snprintf(configFile, 1024, "%s/etc/eucalyptus/eucalyptus.conf", euca_home);
    snprintf(policyFile, 1024, "%s/var/lib/eucalyptus/keys/cc-client-policy.xml", euca_home);
  }

  rc = get_conf_var(configFile, "CC_PORT", &tmpstr);
  if (rc != 1) {
    // error
    logprintf("ERROR: parsing config file (%s) for CC_PORT\n",configFile);
    exit(1);
  } else {
    port = atoi(tmpstr);
  }

  rc = get_conf_var(configFile, "ENABLE_WS_SECURITY", &tmpstr);
  if (rc != 1) {
    // error
    logprintf("ERROR: parsing config file (%s) for ENABLE_WS_SECURITY\n",configFile);
    exit(1);
  } else {
    if (!strcmp(tmpstr, "Y")) {
      use_wssec = 1;
    } else {
      use_wssec = 0;
    }
  }
  
  if (MODE == 0) {
    snprintf(endpoint_uri, 256," http://localhost:%d/axis2/services/EucalyptusCC", port);
  } else {
    snprintf(endpoint_uri, 256," http://%s/axis2/services/EucalyptusCC", argv[1]);
  }
  //env =  axutil_env_create_all(NULL, 0);
  env =  axutil_env_create_all("/tmp/fofo", AXIS2_LOG_LEVEL_TRACE);
  
  client_home = AXIS2_GETENV("AXIS2C_HOME");
  if (!client_home) {
    printf("must have AXIS2C_HOME set\n");
    exit(1);
  }
  stub = axis2_stub_create_EucalyptusCC(env, client_home, endpoint_uri);
  
  if (use_wssec) {
    rc = InitWSSEC(env, stub, policyFile);
    if (rc) {
      printf("cannot initialize WS-SEC policy (%s)\n",policyFile);
      exit(1);
    } 
  }
  
  if (MODE == 0) {
    rc = cc_killallInstances(env, stub);
    if (rc != 0) {
      printf("cc_killallInstances() failed\n");
      exit(1);
    }
  } else {
    /*
    if (!strcmp(argv[2], "registerImage")) {
      rc = cc_registerImage(argv[3], env, stub);
      if (rc != 0) {
	printf("cc_registerImage() failed: in:%s out:%d\n", argv[3], rc);
	exit(1);
      }
    */
    if (!strcmp(argv[2], "runInstances")) {
      char *amiId=NULL, *amiURL=NULL, *kernelId=NULL, *kernelURL=NULL, *ramdiskId=NULL, *ramdiskURL=NULL;
      if (argv[3]) amiId = argv[3];
      if (argv[4]) amiURL = argv[4];
      if (argv[5]) kernelId = argv[5];
      if (argv[6]) kernelURL = argv[6];
      if (argv[10]) ramdiskId = argv[10];
      if (argv[11]) ramdiskURL = argv[11];

      virtualMachine params = { 64, 1, 64, "m1.small", 
				{ { "sda1", "root", 100, "none" }, 
				  { "sda2", "ephemeral1", 1000, "ext3" },
				  { "sda3", "swap", 50, "swap" } } };

      rc = cc_runInstances(amiId, amiURL, kernelId, kernelURL, ramdiskId, ramdiskURL, atoi(argv[7]), atoi(argv[8]), argv[9], &params, env, stub);
      if (rc != 0) {
	printf("cc_runInstances() failed: in:%s out:%d\n", argv[4], rc);
	exit(1);
      }
    } else if (!strcmp(argv[2], "describeInstances")) {
      rc = cc_describeInstances(NULL, 0, env, stub);
      if (rc != 0) {
	printf("cc_describeInstances() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "getConsoleOutput")) {
      rc = cc_getConsoleOutput(argv[3], env, stub);
      if (rc != 0) {
	printf("cc_getConsoleOutput() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "rebootInstances")) {
      char *instIds[256];
      if (argv[3] != NULL) {
	instIds[0] = strdup(argv[3]);
      }
      rc = cc_rebootInstances(instIds, 1, env, stub);
      if (rc != 0) {
	printf("cc_rebootInstances() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "terminateInstances")) {
      char *instIds[256];
      i=3;
      while (argv[i] != NULL) {
	instIds[i-3] = strdup(argv[i]);
	i++;
      }
      if ( (i-3) > 0) {
	rc = cc_terminateInstances(instIds, i-3, env, stub);
	if (rc != 0) {
	  printf("cc_terminateInstances() failed\n");
	  exit(1);
	}
      }
    } else if (!strcmp(argv[2], "describeResources")) {
      rc = cc_describeResources(env, stub);
      if (rc != 0) {
	printf("cc_describeResources() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "startNetwork")) {
      char **ccs;
      int ccsLen=0, i;
      ccs = malloc(sizeof(char *) * 32);
      for (i=0; i<32; i++) {
	if (argv[i+5]) {
	  ccs[i] = strdup(argv[i+5]);
	  ccsLen++;
	} else {
	  i=33;
	}
      }
      rc = cc_startNetwork(atoi(argv[3]), argv[4], ccs, ccsLen, env, stub);
      if (rc != 0) {
	printf("cc_startNetwork() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "describeNetworks")) {
      char **ccs, *nameserver;
      int ccsLen=0, i;
      ccs = malloc(sizeof(char *) * 32);
      for (i=0; i<32; i++) {
	if (argv[i+3]) {
	  ccs[i] = strdup(argv[i+3]);
	  ccsLen++;
	} else {
	  i=33;
	}
      }
      nameserver = strdup("1.2.3.4");

      rc = cc_describeNetworks(nameserver, ccs, ccsLen, env, stub);
      if (rc != 0) {
	printf("cc_describeNetworks() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "configureNetwork")) {
      rc = cc_configureNetwork(argv[3], argv[4], argv[5], atoi(argv[6]), atoi(argv[7]), argv[8], env, stub);
      if (rc != 0) {
	printf("cc_configureNetwork() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "stopNetwork")) {
      rc = cc_stopNetwork(atoi(argv[3]), argv[4], env, stub);
      if (rc != 0) {
	printf("cc_stopNetwork() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "assignAddress")) {
      rc = cc_assignAddress(argv[3], argv[4], env, stub);
      if (rc != 0) {
	printf("cc_assignNetwork() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "unassignAddress")) {
      rc = cc_unassignAddress(argv[3], argv[4], env, stub);
      if (rc != 0) {
	printf("cc_unassignNetwork() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "attachVolume")) {
      rc = cc_attachVolume(argv[3], argv[4], argv[5], argv[6], env, stub);
      if (rc != 0) {
	printf("cc_attachVolume() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "detachVolume")) {
      rc = cc_detachVolume(argv[3], argv[4], argv[5], argv[6], atoi(argv[7]), env, stub);
      if (rc != 0) {
	printf("cc_unassignNetwork() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "describePublicAddresses")) {
      rc = cc_describePublicAddresses(env, stub);
      if (rc != 0) {
	printf("cc_describePublicAddresses() failed\n");
	exit(1);
      }
    } else if (!strcmp(argv[2], "killallInstances")) {
      rc = cc_killallInstances(env, stub);
      if (rc != 0) {
	printf("cc_killallInstances() failed\n");
	exit(1);
      }
    }
  }
  
  exit(0);
}



