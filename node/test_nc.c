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
#include <stdlib.h>
#include <string.h>
#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>
#include "misc.h"
#include "eucalyptus.h"
#include "diskutil.h"

#define MAXDOMS 1024

static void print_libvirt_error (void)
{
    virError * verr = virGetLastError();
    if ( verr!=NULL ) {
        fprintf (stderr, "libvirt error: %s (code=%d)\n", verr->message, verr->code);
        virResetLastError();
    }
}

// find value of the given param in the eucalyptus.conf (e.g., /usr/bin/euca-bundle-upload for NC_BUNDLE_UPLOAD_PATH)
// return NULL if the param is commented out
static char* find_conf_value(const char* eucahome, const char* param)
{
     char conf_path[1024];
     char line[1024];
     char *value = NULL;
     FILE *f_conf = NULL;
     int i =0; 
     if(!eucahome || !param)
	return NULL;

     snprintf(conf_path, 1024, "%s/etc/eucalyptus/eucalyptus.conf", eucahome); 
     f_conf=fopen(conf_path, "r");
     if(!f_conf){ 
    	return NULL;
     }
      
     while(fgets(line, 1024, f_conf)!=NULL){
        if(strstr(line, param)!=NULL){ // found the param in the line
           if(strchr(line, '#')!= NULL) // the line is commented out (assume # can't appear in the middle)
	      break;   
           else{
		char* pch = strtok(line, "="); // again assume '=' can't appear in the middle of value
	        pch = strtok(NULL, "=");
		if(pch && strlen(pch)>0) {
	           value = calloc(strlen(pch)+1, 1);
	           snprintf(value, strlen(pch)+1, "%s", pch);
	        }             
	        break;
	   }
	}
	bzero(line, 1024);
     } 

     // remove "" from the value 	
     if(value){
	int quote=0;
        for(int i=0; i<strlen(value); i++)	
 	{	
	   if(value[i]=='\"')	
	      quote++;
	   else
	      value[i-quote] = value[i];
	}
	value[strlen(value)-quote] = 0x00;

       // remove spaces
       i=0;
       while(value[i]==' ' || value[i]=='\t')
          i++;
       for(int j=i; j<strlen(value); j++)
          value[j-i] = value[j];
       value[strlen(value)-i] = 0x00;
    
       if(value[strlen(value)-1] == '\n')
	  value[strlen(value)-1] = 0x00;
     }
    
     fclose(f_conf); 
     return value;
}

int main (int argc, char * argv[] )
{
  virConnectPtr conn = NULL;
  int dom_ids [MAXDOMS];
  int num_doms = 0;
  char *hypervisor, hypervisorURL[32], cmd[1024];
  char *eucahome=NULL;
  
  //  logfile (NULL, EUCAFATAL); // suppress all messages
 
  if (argc != 2) {
      fprintf (stderr, "error: test_nc expects one parameter (name of hypervisor)\n");
      exit (1);
  }
 
  hypervisor = argv[1];
  if (!strcmp(hypervisor, "kvm")) {
      snprintf(hypervisorURL, 32, "qemu:///system");
  } else if (!strcmp(hypervisor, "xen")) {
      snprintf(hypervisorURL, 32, "xen:///");      
  } else if (!strcmp(hypervisor, "not_configured")) {
      fprintf (stderr, "error: HYPERVISOR variable is not set in eucalyptus.conf\n");
      exit (1);
  } else {
      fprintf (stderr, "error: hypervisor type (%s) is not recognized\n", hypervisor);
      exit (1);
  }
  
  /* check that commands that NC needs are there */
  
  if ( system("perl --version") ) {
    fprintf (stderr, "error: could not run perl\n");
    exit (1);
  }
  
  eucahome = getenv (EUCALYPTUS_ENV_VAR_NAME);
  if (!eucahome) {
    eucahome = strdup (""); /* root by default */
  } else {
    eucahome = strdup(eucahome);
  }

  add_euca_to_path (eucahome);
  if (diskutil_init())
      exit (1);

  if (!strcmp(hypervisor, "kvm")) {
    snprintf(cmd, 1024, "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/get_sys_info", eucahome, eucahome);
  } else {
    snprintf(cmd, 1024, "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/get_xen_info", eucahome, eucahome);
  }
  
  if ( system(cmd) ) {
    fprintf (stderr, "error: could not run '%s'\n", cmd);
    exit (1);
  }
  
  /* check that libvirt can query the hypervisor */
  conn = virConnectOpen (hypervisorURL); /* NULL means local hypervisor */
  if (conn == NULL) {
    print_libvirt_error ();
    fprintf (stderr, "error: failed to connect to hypervisor\n");
    exit (1);
  }
  
  num_doms = virConnectListDomains (conn, dom_ids, MAXDOMS);
  if (num_doms < 0) {
    print_libvirt_error ();
    fprintf (stderr, "error: failed to query running domains\n");
    exit (1);
  }

  /* check if euca2ools command lines for bundle-instance is in the PATH */
  fprintf(stderr, "euca-bundle-upload: ");
  char* bundle_upload = find_conf_value(eucahome, "NC_BUNDLE_UPLOAD_PATH");
  if(bundle_upload){
     snprintf(cmd, 1024,"%s --version", bundle_upload);
     if (system(cmd)){ 
	fprintf(stderr, "error: could not run '%s'\n", cmd);
	exit(1);
     }
     free(bundle_upload);
  }else if(system("euca-bundle-upload --version")){
	fprintf(stderr, "error: could not find euca-bundle-upload in the PATH\n");
	exit(1);
  } 
  
  fprintf(stderr, "euca-check-bucket: ");
  char* chk_bucket = find_conf_value(eucahome, "NC_CHECK_BUCKET_PATH");
  if(chk_bucket){ 
      snprintf(cmd, 1024, "%s --version", chk_bucket);
      if(system(cmd)){
          fprintf(stderr, "error: could not run '%s'\n", cmd);
          exit(1);
      }
      free(chk_bucket);
  }else if(system("euca-check-bucket --version")){
      fprintf(stderr, "error: could not find euca-check-bucket in the PATH\n");
      exit(1);
  }

  fprintf(stderr, "euca-delete-bundle: ");
  char* delete_bundle = find_conf_value(eucahome, "NC_DELETE_BUNDLE_PATH");
  if(delete_bundle){
      snprintf(cmd, 1024, "%s --version", delete_bundle);
      if(system(cmd)){
          fprintf(stderr, "error: could not run '%s'\n", cmd);
          exit(1);
      } 
      free(delete_bundle);
  }else if(system("euca-delete-bundle --version")){
     fprintf(stderr, "error: could not find euca-delete-bundle in the PATH\n");
     exit(1);
  }
  
  return 0;
}


