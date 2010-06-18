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
#include <unistd.h> /* getopt */
#include "data.h"
#include "client-marshal.h"
#include "misc.h"
#include "euca_axis.h"

#define NC_ENDPOINT "/axis2/services/EucalyptusNC"
#define WALRUS_ENDPOINT "/services/Walrus"
#define DEFAULT_WALRUS_HOSTPORT "localhost:8773"
#define DEFAULT_NC_HOSTPORT "localhost:8775"
#define DEFAULT_MAC_ADDR "aa:bb:cc:dd:ee:ff"
#define BUFSIZE 1024
char debug = 0;

void usage (void) 
{ 
    fprintf (stderr, "usage: NCclient [command] [options]\n"
        "\tcommands:\t\t\trequired options:\n"
             "\t\trunInstance\t\t[-m -k]\n"
             "\t\tterminateInstance\t[-i]\n"
             "\t\tdescribeInstances\n"
             "\t\tdescribeResource\n"
             "\t\tattachVolume\t\t[-i -V -R -L]\n"
             "\t\tdetachVolume\t\t[-i -V -R -L]\n"
        "\toptions:\n"
             "\t\t-d \t\t- print debug output\n"
             "\t\t-h \t\t- this help information\n"
             "\t\t-w [host:port] \t- Walrus endpoint\n"
             "\t\t-n [host:port] \t- NC endpoint\n"
             "\t\t-i [str] \t- instance ID\n"
             "\t\t-e [str] \t- reservation ID\n"
             "\t\t-m [id:path] \t- id and manifest path of disk image\n"
             "\t\t-k [id:path] \t- id and manifest path of kernel image\n"
             "\t\t-r [id:path] \t- id and manifest path of ramdisk image\n"
             "\t\t-a [address] \t- MAC address for instance to use\n"
             "\t\t-c [number] \t- number of instances to start\n"
             "\t\t-V [name] \t- name of the volume (for reference)\n"
             "\t\t-R [device] \t- remote/source device (e.g. /dev/etherd/e0.0)\n"
             "\t\t-L [device] \t- local/target device (e.g. hda)\n"
             "\t\t-F \t\t- force VolumeDetach\n"
             "\t\t-U [string] \t- user data to store with instance\n"
             "\t\t-I [string] \t- launch index to store with instance\n"
             "\t\t-G [str:str: ] \t- group names to store with instance\n"
        );

    exit (1);
}

#define CHECK_PARAM(par, name) if (par==NULL) { fprintf (stderr, "ERROR: no %s specified (try -h)\n", name); exit (1); } 

int main (int argc, char **argv) 
{
	char * nc_hostport = DEFAULT_NC_HOSTPORT;
    char * walrus_hostport = DEFAULT_WALRUS_HOSTPORT;
    char * instance_id = NULL;
    char * image_id = NULL;
    char * image_manifest = NULL;
    char * kernel_id = NULL;
    char * kernel_manifest = NULL;
    char * ramdisk_id = NULL;
    char * ramdisk_manifest = NULL;
    char * reservation_id = NULL;
    char * mac_addr = strdup (DEFAULT_MAC_ADDR);
    char * volume_id = NULL;
    char * remote_dev = NULL;
    char * local_dev = NULL;
    int force = 0;
    char * user_data = NULL;
    char * launch_index = NULL;
    char ** group_names = NULL;
    int group_names_size = 0;
	char * command = NULL;
    int count = 1;
	int ch;
    
	while ((ch = getopt(argc, argv, "hdn:w:i:m:k:r:e:a:c:h:V:R:L:FU:I:G:")) != -1) {
		switch (ch) {
        case 'c':
            count = atoi (optarg);
            break;
        case 'd':
            debug = 1;
            break;
        case 'n':
            nc_hostport = optarg; 
            break;
        case 'w':
            walrus_hostport = optarg; 
            break;
        case 'i':
            instance_id = optarg; 
            break;
        case 'm':
            image_id = strtok (optarg, ":");
            image_manifest = strtok (NULL, ":");
            if (image_id==NULL || image_manifest==NULL) {
                fprintf (stderr, "ERROR: could not parse image [id:manifest] paramters (try -h)\n");
                exit (1);
            }
            break;
        case 'k':
            kernel_id = strtok (optarg, ":");
            kernel_manifest = strtok (NULL, ":");
            if (kernel_id==NULL || kernel_manifest==NULL) {
                fprintf (stderr, "ERROR: could not parse kernel [id:manifest] paramters (try -h)\n");
                exit (1);
            }
            break;
        case 'r':
            ramdisk_id = strtok (optarg, ":");
            ramdisk_manifest = strtok (NULL, ":");
            if (ramdisk_id==NULL || ramdisk_manifest==NULL) {
                fprintf (stderr, "ERROR: could not parse ramdisk [id:manifest] paramters (try -h)\n");
                exit (1);
            }
            break;
        case 'e':
            reservation_id = optarg;
            break;
        case 'a':
            mac_addr = optarg;
            break;
        case 'V':
            volume_id = optarg;
            break;
        case 'R':
            remote_dev = optarg;
            break;
        case 'L':
            local_dev = optarg;
            break;
        case 'F':
            force = 1;
            break;
        case 'U':
            user_data = optarg;
            break;
        case 'I':
            launch_index = optarg;
            break;
        case 'G':
        {
            int i;
            group_names_size = 1;
            for (i=0; optarg[i]; i++) 
                if (optarg[i]==':')
                    group_names_size++;
            group_names = malloc (sizeof(char *) * group_names_size);
            if (group_names==NULL) {
                fprintf (stderr, "ERROR: out of memory for group_names[]\n");
                exit (1);
            }
            group_names [0] = strtok (optarg, ":");
            for (i=1; i<group_names_size; i++)
                group_names[i] = strtok (NULL, ":");
            break;
        }
        case 'h':
            usage (); // will exit
        case '?':
        default:
            fprintf (stderr, "ERROR: unknown parameter (try -h)\n");
            exit (1);
		}
	}
	argc -= optind;
	argv += optind;
    
	if (argc>0) {
		command = argv[0];
        if (argc>1) {
            fprintf (stderr, "WARNING: too many parameters, using first one as command\n");
        }
	} else {
        fprintf (stderr, "ERROR: command not specified (try -h)\n");
        exit (1);
    }
    
    ncMetadata meta = { "correlate-me-please", "eucalyptus" };
    virtualMachine params = { 64, 64, 1, "m1.small", 
			      { { "sda1", "root", 100, "none" }, 
				{ "sda2", "ephemeral1", 1000, "ext3" },
				{ "sda3", "swap", 50, "swap" } } };
    ncStub * stub;
    char configFile[1024], policyFile[1024];
    char *euca_home;
    int rc, use_wssec;
    char *tmpstr;
    
    euca_home = getenv("EUCALYPTUS");
    if (!euca_home) {
        snprintf(configFile, 1024, "/etc/eucalyptus/eucalyptus.conf");
        snprintf(policyFile, 1024, "/var/lib/eucalyptus/keys/nc-client-policy.xml");
    } else {
        snprintf(configFile, 1024, "%s/etc/eucalyptus/eucalyptus.conf", euca_home);
        snprintf(policyFile, 1024, "%s/var/lib/eucalyptus/keys/nc-client-policy.xml", euca_home);
    }
    rc = get_conf_var(configFile, "ENABLE_WS_SECURITY", &tmpstr);
    if (rc != 1) {
        logprintf("ERROR: parsing config file (%s) for ENABLE_WS_SECURITY\n",configFile);
        exit(1);
    } else {
        if (!strcmp(tmpstr, "Y")) {
            use_wssec = 1;
        } else {
            use_wssec = 0;
        }
    }
    
    char nc_url [BUFSIZE];
    snprintf (nc_url, BUFSIZE, "http://%s%s", nc_hostport, NC_ENDPOINT);
    if (debug) printf ("connecting to NC at %s\n", nc_url);
    stub = ncStubCreate (nc_url, "NCclient.log", NULL);
    if (!stub) {
        fprintf (stderr, "ERROR: failed to connect to Web service\n");
        exit (2);
    }
    
    if (use_wssec) {
        if (debug) printf ("using policy file %s\n", policyFile);
        rc = InitWSSEC(stub->env, stub->stub, policyFile);
        if (rc) {
            fprintf (stderr, "ERROR: cannot initialize WS-SEC policy from %s\n", policyFile);
            exit(1);
        } 
    }
    
    char * image_url = NULL;
    if (image_manifest) {
        char t [BUFSIZE];
        snprintf (t, BUFSIZE, "http://%s%s/%s", walrus_hostport, WALRUS_ENDPOINT, image_manifest);
        image_url = strdup (t);
    }

    char * kernel_url = NULL;
    if (kernel_manifest) {
        char t [BUFSIZE];
        snprintf (t, BUFSIZE, "http://%s%s/%s", walrus_hostport, WALRUS_ENDPOINT, kernel_manifest);
        kernel_url = strdup (t);
    }

    char * ramdisk_url = NULL;
    if (ramdisk_manifest) {
        char t [BUFSIZE];
        snprintf (t, BUFSIZE, "http://%s%s/%s", walrus_hostport, WALRUS_ENDPOINT, ramdisk_manifest);
        ramdisk_url = strdup (t);
    }

    /***********************************************************/
    if (!strcmp (command, "runInstance")) {
        CHECK_PARAM(image_id, "image ID and manifest path");
        CHECK_PARAM(kernel_id, "kernel ID and manifest path");

        char *privMac, *pubMac, *privIp;
        int vlan = 3;
        privMac = strdup (mac_addr);
        mac_addr [0] = 'b';
        mac_addr [1] = 'b';
	privIp = strdup("10.0.0.202");

        /* generate random IDs if they weren't specified*/
#define C rand()%26 + 97

        while (count--) {
            char * iid, * rid;

            char ibuf [8];
            if (instance_id==NULL || count>1) {
                snprintf (ibuf, 8, "i-%c%c%c%c%c", C, C, C, C, C);
                iid = ibuf;
            } else {
                iid = instance_id;
            }

            char rbuf [8];
            if (reservation_id==NULL || count>1) {
                snprintf (rbuf, 8, "r-%c%c%c%c%c", C, C, C, C, C);
                rid = rbuf;
            } else {
                rid = reservation_id;
            }
            
	    netConfig netparams;
            ncInstance * outInst;
	    netparams.vlan = vlan;
	    snprintf(netparams.privateIp, 24, "%s", privIp);
	    snprintf(netparams.privateMac, 24, "%s", privMac);

            int rc = ncRunInstanceStub(stub, &meta, 
                                       iid, rid,
                                       &params, 
                                       image_id, image_url, 
                                       kernel_id, kernel_url, 
                                       ramdisk_id, ramdisk_url, 
                                       "", /* key */
									   &netparams,
									   //                                       privMac, privIp, vlan, 
                                       user_data, launch_index, group_names, group_names_size, /* CC stuff */
                                       &outInst);
            if (rc != 0) {
                printf("ncRunInstance() failed: instanceId=%s error=%d\n", instance_id, rc);
                exit(1);
            }
	    // count device mappings
	    int i, count=0;
	    for (i=0; i<EUCA_MAX_DEVMAPS; i++) {
	      if (strlen(outInst->params.deviceMapping[i].deviceName)>0) count++;
	    }
            printf("instanceId=%s stateCode=%d stateName=%s deviceMappings=%d\n", outInst->instanceId, outInst->stateCode, outInst->stateName, count);
            free_instance(&outInst);
        }
    
        /***********************************************************/
    } else if (!strcmp(command, "powerDown")) {
      int rc = ncPowerDownStub(stub, &meta);
    } else if (!strcmp(command, "terminateInstance")) {
        CHECK_PARAM(instance_id, "instance ID");
        
        int shutdownState, previousState;
        int rc = ncTerminateInstanceStub (stub, &meta, instance_id, &shutdownState, &previousState);
        if (rc != 0) {
            printf("ncTerminateInstance() failed: error=%d\n", rc);
            exit(1);
        }
        printf("shutdownState=%d, previousState=%d\n", shutdownState, previousState);

        /***********************************************************/
    } else if (!strcmp(command, "describeInstances")) {
        /* TODO: pull out of argv[] requested instanceIDs */
        ncInstance ** outInsts;
        int outInstsLen, i;
        int rc = ncDescribeInstancesStub(stub, &meta, NULL, 0, &outInsts, &outInstsLen);
        if (rc != 0) {
            printf("ncDescribeInstances() failed: error=%d\n", rc);
            exit(1);
        }
        for (i=0; i<outInstsLen; i++) {
            ncInstance * inst = outInsts[i];
            printf("instanceId=%s state=%s time=%d\n", inst->instanceId, inst->stateName, inst->launchTime);
            if (debug) {
                printf ("              userData=%s launchIndex=%s groupNames=", inst->userData, inst->launchIndex);
                if (inst->groupNamesSize>0) {
                    int j;
                    for (j=0; j<inst->groupNamesSize; j++) {
                        if (j>0) 
                            printf (":");
                        printf ("%s", inst->groupNames[j]);
                    }
                } else {
                    printf ("(none)");
                }
                printf ("\n");
                
                printf ("              attached volumes: ");
                if (inst->volumesSize>0) {
                    int j;
                    for (j=0; j<inst->volumesSize; j++) {
                        if (j>0)
                            printf ("                                ");
                        printf ("%s %s %s\n", inst->volumes[j].volumeId, inst->volumes[j].remoteDev, inst->volumes[j].localDev);
                    }
                } else {
                    printf ("(none)\n");
                }

                free_instance(&(outInsts[i]));
            }
        }
        /* TODO: fix free(outInsts); */

    /***********************************************************/
    } else if (!strcmp(command, "describeResource")) {
        char * type = NULL;
        ncResource *outRes;
        
        int rc = ncDescribeResourceStub(stub, &meta, type, &outRes);
        if (rc != 0) {
            printf("ncDescribeResource() failed: error=%d\n", rc);
            exit(1);
        }
        printf ("node status=[%s] memory=%d/%d disk=%d/%d cores=%d/%d subnets=[%s]\n", 
                outRes->nodeStatus, 
                outRes->memorySizeMax, outRes->memorySizeAvailable,
                outRes->diskSizeMax, outRes->diskSizeAvailable,
                outRes->numberOfCoresMax, outRes->numberOfCoresAvailable,
                outRes->publicSubnets);

    /***********************************************************/
    } else if (!strcmp(command, "attachVolume")) {
        CHECK_PARAM(instance_id, "instance ID");
        CHECK_PARAM(volume_id, "volume ID");
        CHECK_PARAM(remote_dev, "remote dev");
        CHECK_PARAM(local_dev, "local dev");
        
        int rc = ncAttachVolumeStub (stub, &meta, instance_id, volume_id, remote_dev, local_dev);
        if (rc != 0) {
            printf("ncAttachVolume() failed: error=%d\n", rc);
            exit(1);
        }

    /***********************************************************/
    } else if (!strcmp(command, "detachVolume")) {
        CHECK_PARAM(instance_id, "instance ID");
        CHECK_PARAM(volume_id, "volume ID");
        CHECK_PARAM(remote_dev, "remote dev");
        CHECK_PARAM(local_dev, "local dev");
        
        int rc = ncDetachVolumeStub (stub, &meta, instance_id, volume_id, remote_dev, local_dev, force);
        if (rc != 0) {
            printf("ncDetachVolume() failed: error=%d\n", rc);
            exit(1);
        }
        
    /***********************************************************/
    } else {
        fprintf (stderr, "ERROR: command %s unknown (try -h)\n", command);
        exit (1);
    }
    
    exit(0);
}
