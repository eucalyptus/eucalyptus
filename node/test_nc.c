#include <stdio.h>
#include <stdlib.h>
#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>
#include "misc.h"
#include "eucalyptus.h"

#define MAXDOMS 1024

static void print_libvirt_error (void)
{
    virError * verr = virGetLastError();
    if ( verr!=NULL ) {
        fprintf (stderr, "libvirt error: %s (code=%d)\n", verr->message, verr->code);
        virResetLastError();
    }
}

int main (int argc, char * argv[] )
{
  virConnectPtr conn = NULL;
  int dom_ids [MAXDOMS];
  int num_doms = 0;
  char *hypervisor, hypervisorURL[32], cmd[1024];
  char *eucahome=NULL;
  
  logfile (NULL, EUCAFATAL); /* suppress all messages */
 
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
  
  return 0;
}


