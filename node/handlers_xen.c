#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU /* strnlen */
#include <string.h> /* strlen, strcpy */
#include <time.h>
#include <limits.h> /* INT_MAX */
#include <sys/types.h> /* fork */
#include <sys/wait.h> /* waitpid */
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <errno.h>
#include <sys/stat.h>
#include <pthread.h>
#include <sys/vfs.h> /* statfs */
#include <signal.h> /* SIGINT */

#include "ipc.h"
#include "misc.h"
#include <handlers.h>
#include <storage.h>
#include <eucalyptus.h>
#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>
#include <vnetwork.h>
#include <euca_auth.h>

#define BUFSIZE 512 /* random buffer size used all over the place */

/* resource limits, in MB, will be determined by hardware if set to 0 */
static int config_max_disk  = 0;
static int config_max_mem   = 0;
static int config_max_cores = 0;

/* actual resource limits of the system, as determined at NC startup */
static long long mem_max  = 0; 
static long long disk_max = 0;
static int cores_max      = 0;

/* network configuration defaults (may be overriden from config file) */
static char config_network_path [BUFSIZE];
static int  config_network_port = NC_NET_PORT_DEFAULT;

/* debuging parameter, can be set in config file */
static int save_instance_files = 0; 

static char * admin_user_id = EUCALYPTUS_ADMIN;
static char gen_libvirt_xml_command_path [BUFSIZE] = "";
static char get_xen_info_command_path [BUFSIZE] = "";
static char virsh_command_path [BUFSIZE] = "";
static char xm_command_path [BUFSIZE] = "";


#define BYTES_PER_DISK_UNIT 1048576 /* disk stats are in Gigs */
#define SWAP_SIZE 512 /* for now, the only possible swap size, in MBs */
#define MAXDOMS 1024 /* max number of running domains on node */
#define LIBVIRT_QUERY_RETRIES 5

vnetConfig *vnetconfig = NULL;
sem * xen_sem; /* semaphore for serializing domain creation */
sem * inst_sem; /* semaphore for guarding access to global instance structs */
bunchOfInstances * global_instances = NULL; /* will be initiated upon first call */
static virConnectPtr conn = NULL; /* global hypervisor connection used by all calls */

const int unbooted_cleanup_threshold = 60 * 60 * 2; /* after this many seconds any unbooted and SHUTOFF domains will be cleaned up */
const int teardown_state_duration = 60; /* after this many seconds in TEARDOWN state (no resources), we'll forget about the instance */
const int monitoring_period_duration = 5; /* how frequently we check on instances */

static void libvirt_error_handler (void * userData, virErrorPtr error)
{
    if ( error==NULL) {
        logprintfl (EUCAERROR, "libvirt error handler was given a NULL pointer\n");
    } else {
        /* these are common, they appear for evey non-existing domain,
         * such as BOOTING/CRASHED/SHUTOFF, which we catch elsewhere,
         * so we won't print them */
        if (error->code==10) { 
            return;
        }
        logprintfl (EUCAERROR, "libvirt: %s (code=%d)\n", error->message, error->code);
    }
}

static void change_state (ncInstance * instance, instance_states state)
{
    instance->state = (int) state;
    switch (state) { /* mapping from NC's internal states into external ones */
    case BOOTING:
        instance->stateCode = PENDING;
        break;
    case RUNNING:
    case BLOCKED:
    case PAUSED:
    case SHUTDOWN:
    case SHUTOFF:
    case CRASHED:
        instance->stateCode = EXTANT;
	instance->retries = LIBVIRT_QUERY_RETRIES;
        break;
    case TEARDOWN:
        instance->stateCode = TEARDOWN;
        break;
    default:
        logprintfl (EUCAERROR, "error: change_sate(): unexpected state (%d) for instance %s\n", instance->state, instance->instanceId);        
        return;
    }

    strncpy(instance->stateName, instance_state_names[instance->stateCode], CHAR_BUFFER_SIZE);
}

/* verify the connection to hypervisor, try to reopen it
 * if it is closed, and, failing that, return 1 */
static int check_hypervisor_conn ()
{
    char * uri;

    if ( conn == NULL ||
         ( uri = virConnectGetURI (conn) ) == NULL ) {
        conn = virConnectOpen ("xen:///");
        if ( conn == NULL) {
            logprintfl (EUCAFATAL, "Failed to connect to hypervisor\n");
            return ERROR;
        }
    }
    
    return OK;
}

static void refresh_instance_info (ncInstance * instance)
{
    int now = instance->state;
    
    /* no need to bug Xen for domains without state */
    if (now==TEARDOWN)
        return;
    
    /* try to get domain state from Xen */
    if (check_hypervisor_conn ()) {
        return;
    }
    virDomainPtr dom = virDomainLookupByName (conn, instance->instanceId);
    if (dom == NULL) { /* Xen doesn't know about it */
        if (now==RUNNING ||
            now==BLOCKED ||
            now==PAUSED ||
            now==SHUTDOWN) {
            /* Most likely the user has shut it down from the inside */
            if (instance->retries) {
		instance->retries--;
		logprintfl (EUCAWARN, "warning: hypervisor failed to find domain %s, will retry %d more times\n", instance->instanceId, instance->retries);	
            } else {
            	logprintfl (EUCAWARN, "warning: hypervisor failed to find domain %s, assuming it was shut off\n", instance->instanceId);
            	change_state (instance, SHUTOFF);
            }
        }
        /* else 'now' stays in SHUTFOFF, BOOTING or CRASHED */
        return;
    }
    virDomainInfo info;
    int error = virDomainGetInfo(dom, &info);
    if (error < 0 || info.state == VIR_DOMAIN_NOSTATE) {
        logprintfl (EUCAWARN, "warning: failed to get informations for domain %s\n", instance->instanceId);
        /* what to do? hopefully we'll find out more later */
        virDomainFree (dom);
        return;
    } 
    int xen = info.state;

    switch (now) {
    case BOOTING:
    case RUNNING:
    case BLOCKED:
    case PAUSED:
        /* change to Xen's state, whatever it happens to be */
        change_state (instance, xen);
        break;
    case SHUTDOWN:
    case SHUTOFF:
    case CRASHED:
        if (xen==RUNNING ||
            xen==BLOCKED ||
            xen==PAUSED) {
            /* cannot go back! */
            logprintfl (EUCAWARN, "warning: detected prodigal domain %s, terminating it\n", instance->instanceId);
            sem_p (xen_sem);
            virDomainDestroy (dom);
            sem_v (xen_sem);
        } else {
            change_state (instance, xen);
        }
        break;
    default:
        logprintfl (EUCAERROR, "error: refresh...(): unexpected state (%d) for instance %s\n", now, instance->instanceId);
        return;
    }
    virDomainFree(dom);

    /* if instance is running, try to find out its IP address */
    if (instance->state==RUNNING ||
        instance->state==BLOCKED ||
        instance->state==PAUSED) {
        char *ip=NULL;
        int rc;

        if (!strncmp(instance->ncnet.publicIp, "0.0.0.0", 32)) {
	  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
            rc = discover_mac(vnetconfig, instance->ncnet.publicMac, &ip);
            if (!rc) {
	      logprintfl (EUCAINFO, "discovered public IP %s for instance %s\n", ip, instance->instanceId);
	      strncpy(instance->ncnet.publicIp, ip, 32);
            }
	  }
        }
        if (!strncmp(instance->ncnet.privateIp, "0.0.0.0", 32)) {
            rc = discover_mac(vnetconfig, instance->ncnet.privateMac, &ip);
            if (!rc) {
                logprintfl (EUCAINFO, "discovered private IP %s for instance %s\n", ip, instance->instanceId);
                strncpy(instance->ncnet.privateIp, ip, 32);
            }
        }
    }
}

static void print_running_domains (void)
{
    bunchOfInstances * head;
    char buf [BUFSIZE] = "";
    sem_p (inst_sem);
    for ( head=global_instances; head; head=head->next ) {
        ncInstance * instance = head->instance;
        if (instance->state==BOOTING 
            || instance->state==RUNNING 
            || instance->state==BLOCKED
            || instance->state==PAUSED) {
            strcat (buf, " ");
            strcat (buf, instance->instanceId);
        }
    }
    sem_v (inst_sem);
    logprintfl (EUCAINFO, "currently running/booting: %s\n", buf);
}

static void * monitoring_thread (void *arg)
{
    int i;

    for (;;) {
        bunchOfInstances * head;
        time_t now = time(NULL);
        sem_p (inst_sem);

    restart: 
        for ( head = global_instances; head; head = head->next ) {
            ncInstance * instance = head->instance;

            /* query Xen for current state, if any */
            refresh_instance_info (instance);
            /* don't touch running threads */
            if (instance->state!=BOOTING && 
                instance->state!=SHUTOFF &&
                instance->state!=SHUTDOWN &&
                instance->state!=TEARDOWN) continue;

            if (instance->state==TEARDOWN) {
                /* it's been long enugh, we can fugetaboutit */
                if ((now - instance->terminationTime)>teardown_state_duration) {
                    remove_instance (&global_instances, instance);
                    logprintfl (EUCAINFO, "forgetting about instance %s\n", instance->instanceId);
                    free_instance (&instance);
                    goto restart; /* reset the head since we modified the list */
                }
                continue;
            }

            if (instance->state==BOOTING &&
                (now - instance->launchTime)<unbooted_cleanup_threshold) /* hasn't been long enough */
                continue; /* let it be */
            
            /* ok, it's been condemned => destroy the files */
            if (!save_instance_files) {
                if (scCleanupInstanceImage(instance->userId, instance->instanceId)) {
                    logprintfl (EUCAWARN, "warning: failed to cleanup instance image %d\n", instance->instanceId);
                }
            }
            
            /* check to see if this is the last instance running on vlan */
            int left = 0;
            bunchOfInstances * vnhead;
            for (vnhead = global_instances; vnhead; vnhead = vnhead->next ) {
                ncInstance * vninstance = vnhead->instance;
                if (vninstance->ncnet.vlan == (instance->ncnet).vlan 
                    && strcmp(instance->instanceId, vninstance->instanceId)) {
                    left++;
                }
            }
            if (left==0) {
                logprintfl (EUCAINFO, "stopping the network (vlan=%d)\n", (instance->ncnet).vlan);
                vnetStopNetwork (vnetconfig, (instance->ncnet).vlan, NULL, NULL);
            }
            change_state (instance, TEARDOWN); /* TEARDOWN = no more resources */
            instance->terminationTime = time (NULL);
        }
        sem_v (inst_sem);
        
        sleep (monitoring_period_duration);
    }
    
    return NULL;
}

static int get_value (char * s, const char * name, long long * valp) 
{
    char buf [BUFSIZE];
    if (s==NULL || name==NULL || valp==NULL) return ERROR;
    snprintf (buf, BUFSIZE, "%s=%%lld", name);
    return (sscanf_lines (s, buf, valp)==1 ? OK : ERROR);
}

static int doInitialize (void) 
{
    struct stat mystat;
    char config [BUFSIZE], logpath[BUFSIZE];
    char *brname, *s, *home, *pubInterface, *bridge, *mode, *loglevelstr;
    int error, rc, loglevel;

    /* read in configuration - this should be first! */
    int do_warn = 0;
    home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) {
        home = strdup (""); /* root by default */
        do_warn = 1;
    } else {
        home = strdup (home);
    }

    snprintf(logpath, BUFSIZE, "%s/var/log/eucalyptus/nc.log", home);
    logfile(logpath, EUCADEBUG); // TODO: right level?
    if (do_warn) 
        logprintfl (EUCAWARN, "env variable %s not set, using /\n", EUCALYPTUS_ENV_VAR_NAME);

    snprintf(config, BUFSIZE, EUCALYPTUS_CONF_LOCATION, home);
    if (stat(config, &mystat)==0) {
      logprintfl (EUCAINFO, "NC is looking for configuration in %s\n", config);

      // reset loglevel to that set in config file (if any)
      loglevelstr = getConfString(config, "LOGLEVEL");
      if (loglevelstr) {
	if (!strcmp(loglevelstr,"DEBUG")) {loglevel=EUCADEBUG;}
	else if (!strcmp(loglevelstr,"INFO")) {loglevel=EUCAINFO;}
	else if (!strcmp(loglevelstr,"WARN")) {loglevel=EUCAWARN;}
	else if (!strcmp(loglevelstr,"ERROR")) {loglevel=EUCAERROR;}
	else if (!strcmp(loglevelstr,"FATAL")) {loglevel=EUCAFATAL;}
	else {loglevel=EUCADEBUG;}
	logfile(logpath, loglevel);
	free(loglevelstr);
      }
	

#define GET_VAR_INT(var,name) \
            if (get_conf_var(config, name, &s)>0){\
                var = atoi(s);\
                free (s);\
            }
        
        GET_VAR_INT(config_max_mem,      CONFIG_MAX_MEM);
        GET_VAR_INT(config_max_disk,     CONFIG_MAX_DISK);
        GET_VAR_INT(config_max_cores,    CONFIG_MAX_CORES);
        GET_VAR_INT(save_instance_files, CONFIG_SAVE_INSTANCES);
    }

    if ((xen_sem = sem_alloc (1, "eucalyptus-nc-xen-semaphore")) == NULL
        || (inst_sem = sem_alloc (1, "eucalyptus-nc-inst-semaphore")) == NULL) {
        logprintfl (EUCAFATAL, "failed to create and initialize a semaphore\n");
        return ERROR_FATAL;
    }
    
    /* prompt the SC to read the configuration, too */
    rc = scInitConfig(); 
    if (rc) {
      logprintfl (EUCAFATAL, "ERROR: scInitConfig() failed\n");
      return ERROR_FATAL;
    }

    /* set up paths of Eucalyptus commands NC relies on */
    snprintf (gen_libvirt_xml_command_path, BUFSIZE, EUCALYPTUS_GEN_LIBVIRT_XML, home, home);
    snprintf (get_xen_info_command_path,    BUFSIZE, EUCALYPTUS_GET_XEN_INFO,    home, home);
    snprintf (virsh_command_path, BUFSIZE, EUCALYPTUS_VIRSH, home);
    snprintf (xm_command_path, BUFSIZE, EUCALYPTUS_XM);
    
    /* open the connection to hypervisor */
    if (check_hypervisor_conn () == ERROR) {
        free(home);
        return ERROR_FATAL;
    }

    /* "adopt" currently running Xen instances */
    {
        int dom_ids[MAXDOMS];
        int num_doms = 0;
        
        logprintfl (EUCAINFO, "looking for existing Xen domains\n");
        virSetErrorFunc (NULL, libvirt_error_handler);
        
        num_doms = virConnectListDomains(conn, dom_ids, MAXDOMS);
        if (num_doms > 0) {
            virDomainPtr dom = NULL;
            int i;
            
            for ( i=0; i<num_doms; i++) {
                dom = virDomainLookupByID(conn, dom_ids[i]);
                
                if (dom) {
                    int error;
                    virDomainInfo info;
                    const char * dom_name;
                    ncInstance * instance;
                    
                    error = virDomainGetInfo(dom, &info);
                    if (error < 0 || info.state == VIR_DOMAIN_NOSTATE) {
                        logprintfl (EUCAWARN, "WARNING: failed to get info on running Xen domain #%d, ignoring it\n", dom_ids[i]);
                        continue;
                    }

                    if (info.state == VIR_DOMAIN_SHUTDOWN ||
                        info.state == VIR_DOMAIN_SHUTOFF ||
                        info.state == VIR_DOMAIN_CRASHED ) {
                        logprintfl (EUCADEBUG, "ignoring non-running Xen domain #%d\n", dom_ids[i]);
                        continue;
                    }
                    
                    if ((dom_name = virDomainGetName(dom))==NULL) {
                        logprintfl (EUCAWARN, "WARNING: failed to get name of running Xen domain #%d, ignoring it\n", dom_ids[i]);
                        continue;
                    }

                    if (!strcmp(dom_name, "Domain-0")) {
                        continue;
                    }

                    if ((instance = scRecoverInstanceInfo (dom_name))==NULL) {
                        logprintfl (EUCAWARN, "WARNING: failed to recover Eucalyptus metadata of running Xen domain %s, ignoring it\n", dom_name);
                        continue;
                    }
                    
                    change_state (instance, info.state);                    
                    sem_p (inst_sem);
                    int err = add_instance (&global_instances, instance);
                    sem_v (inst_sem);
                    if (err) {
                        free_instance (&instance);
                        continue;
                    }
                    
                    logprintfl (EUCAINFO, "- adopted running Xen domain %s from user %s\n", instance->instanceId, instance->userId);
                    /* TODO: try to look up IPs? */

                    virDomainFree (dom);
                } else {
                    logprintfl (EUCAWARN, "WARNING: failed to lookup running Xen domain #%d, ignoring it\n", dom_ids[i]);
                }
            }
        } else if (num_doms==0) {
            logprintfl (EUCAINFO, "no currently running Xen domains to adopt\n");
        } else {
            logprintfl (EUCAWARN, "WARNING: failed to find out about running domains\n");
        }
    }

    /* network startup */
    vnetconfig = malloc(sizeof(vnetConfig));
    snprintf (config_network_path, BUFSIZE, NC_NET_PATH_DEFAULT, home);
    pubInterface = getConfString(config, "VNET_INTERFACE");
    bridge = getConfString(config, "VNET_BRIDGE");
    mode = getConfString(config, "VNET_MODE");

    vnetInit(vnetconfig, mode, home, config_network_path, NC, pubInterface, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, bridge);
    
    /* cleanup from previous runs and verify integrity of instances
     * directory */
    sem_p (inst_sem);
    long long instances_bytes = scFSCK (&global_instances);
    sem_v (inst_sem);
    if (instances_bytes<0) {
        logprintfl (EUCAFATAL, "instances store failed integrity check (error=%lld)\n", instances_bytes);
        return ERROR_FATAL;
    }

    /* discover resource capacity */
    { 
        const char * ipath = scGetInstancePath();
        char buf [BUFSIZE];
        char * s = NULL;
        int len;
        
        /* calculate disk_max */
        { 
            long long fs_free_blocks = 0;
            long long fs_block_size  = 0;

            struct statfs fs;
            if (statfs(ipath, &fs) == -1) {
                printf ("error: failed to stat %s\n", ipath);
            }
            
            disk_max = fs.f_bsize * fs.f_bavail + instances_bytes; /* max for Euca, not total */
            disk_max /= BYTES_PER_DISK_UNIT; /* convert bytes to the right units */
            if (config_max_disk
                && disk_max>config_max_disk) 
                disk_max=config_max_disk; /* reduce if the number exceeds config limits */

            logprintfl (EUCAINFO, "Maximum disk available = %lld (under %s)\n", disk_max, ipath);
        }

        /* get memory and cores from Xen */
        virNodeInfo ni;
        if (virNodeGetInfo(conn, &ni)) {
            logprintfl (EUCAFATAL, "error: failed to discover memory and cores\n");
            return ERROR_FATAL;
        }
        
        /* calculate mem_max */
        {
            long long total_memory = ni.memory/1024;
            long long dom0_min_mem;

            /* dom0-min-mem has to come from xend config file */
            s = system_output (get_xen_info_command_path);
            if (get_value (s, "dom0-min-mem", &dom0_min_mem)) {
                logprintfl (EUCAFATAL, "error: did not find dom0-min-mem in output from %s\n", get_xen_info_command_path);
                free (s);
                return ERROR_FATAL;
            }
            free (s);

            mem_max = total_memory - 32 - dom0_min_mem;
            if (config_max_mem 
                && mem_max>config_max_mem) 
                mem_max = config_max_mem; /* reduce if the number exceeds config limits */
            logprintfl (EUCAINFO, "Maximum memory available = %lld\n", mem_max);
        }

        /* calculate cores_max: unlike with disk or memory limits, use the
         * limit as the number of cores, regardless of whether the actual
         * number of cores is bigger or smaller */
        cores_max = ni.cpus;
        if (config_max_cores) 
            cores_max = config_max_cores; 
        logprintfl (EUCAINFO, "Maximum cores available = %d\n", cores_max);
    }

    pthread_t tcb;
    if ( pthread_create (&tcb, NULL, monitoring_thread, NULL) ) {
        logprintfl (EUCAFATAL, "failed to spawn a monitoring thread\n");
        return ERROR_FATAL;
    }

    return OK;
}

static int get_instance_xml (char *userId, char *instanceId, int ramdisk, char *disk_path, ncInstParams *params, char *privMac, char *pubMac, char *brname, char **xml)
{
    char buf [BUFSIZE];

    if (ramdisk) {
        snprintf (buf, BUFSIZE, "%s --ramdisk", gen_libvirt_xml_command_path);
    } else {
        snprintf (buf, BUFSIZE, "%s", gen_libvirt_xml_command_path);
    }
    if (params->diskSize > 0) { /* TODO: get this info from scMakeImage */
        strncat (buf, " --ephemeral", BUFSIZE);
    }
    * xml = system_output (buf);
    if ( ( * xml ) == NULL ) {
        logprintfl (EUCAFATAL, "%s: %s\n", gen_libvirt_xml_command_path, strerror (errno));
        return ERROR;
    }
    
    /* the tags better be not substring of other tags: BA will substitute
     * ABABABAB */
    replace_string (xml, "BASEPATH", disk_path);
    replace_string (xml, "SWAPPATH", disk_path);
    replace_string (xml, "NAME", instanceId);
    replace_string (xml, "PRIVMACADDR", privMac);
    replace_string (xml, "PUBMACADDR", pubMac);
    replace_string (xml, "BRIDGEDEV", brname);
    snprintf(buf, BUFSIZE, "%d", params->memorySize * 1024); /* because libvirt wants memory in Kb, while we use Mb */
    replace_string (xml, "MEMORY", buf);
    snprintf(buf, BUFSIZE, "%d", params->numberOfCores);
    replace_string (xml, "VCPUS", buf);
    
    return 0;
}

void * startup_thread (void * arg)
{
    ncInstance * instance = (ncInstance *)arg;
    virDomainPtr dom = NULL;
    char * disk_path, * xml;
    char *brname=NULL;
    int error;
    
    if (check_hypervisor_conn () == ERROR) {
        logprintfl (EUCAFATAL, "could not start instance %s, abandoning it\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    
    error = vnetStartNetwork (vnetconfig, instance->ncnet.vlan, NULL, NULL, &brname);
    if ( error ) {
        logprintfl (EUCAFATAL, "start network failed for instance %s, terminating it\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    logprintfl (EUCAINFO, "network started for instance %s\n", instance->instanceId);
    
    error = scMakeInstanceImage (instance->userId, 
                                 instance->imageId, instance->imageURL, 
                                 instance->kernelId, instance->kernelURL, 
                                 instance->ramdiskId, instance->ramdiskURL, 
                                 instance->instanceId, instance->keyName, 
                                 &disk_path, xen_sem, 0, instance->params.diskSize*1024);
    if (error) {
        logprintfl (EUCAFATAL, "Failed to prepare images for instance %s (error=%d)\n", instance->instanceId, error);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    if (instance->state!=BOOTING) {
        logprintfl (EUCAFATAL, "Startup of instance %s was cancelled\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    
    error = get_instance_xml (instance->userId, instance->instanceId, 
                              strnlen (instance->ramdiskId, CHAR_BUFFER_SIZE), /* 0 if no ramdisk */
                              disk_path, 
                              &(instance->params), 
                              instance->ncnet.privateMac, instance->ncnet.publicMac, 
                              brname, &xml);
    if (xml) logprintfl (EUCADEBUG2, "libvirt XML config:\n%s\n", xml);
    if (error) {
        logprintfl (EUCAFATAL, "Failed to create libvirt XML config for instance %s\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    
    scStoreStringToInstanceFile (instance->userId, instance->instanceId, "libvirt.xml", xml); /* for debugging */
    scSaveInstanceInfo(instance); /* to enable NC recovery */

    /* we serialize domain creation as Xen can get confused with
     * too many simultaneous create requests */
    logprintfl (EUCADEBUG2, "about to start domain %s\n", instance->instanceId);
    print_running_domains ();
    sem_p (xen_sem); 
    dom = virDomainCreateLinux (conn, xml, 0);
    sem_v (xen_sem);
    if (dom == NULL) {
        logprintfl (EUCAFATAL, "hypervisor failed to start domain\n");
        change_state (instance, SHUTOFF);
        return NULL;
    }
    eventlog("NC", instance->userId, "", "instanceBoot", "begin"); /* TODO: bring back correlationId */
    
    virDomainFree(dom);
    logprintfl (EUCAINFO, "started VM instance %s\n", instance->instanceId);
    
    return NULL;
}

static int doRunInstance (ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, 
                   char *imageId, char *imageURL, 
                   char *kernelId, char *kernelURL, 
                   char *ramdiskId, char *ramdiskURL, 
                   char *keyName, 
                   char *privMac, char *pubMac, int vlan, 
                   char *userData, char *launchIndex, char **groupNames, int groupNamesSize,
                   ncInstance **outInst)
{
    ncInstance * instance = NULL;
    * outInst = NULL;
    pid_t pid;
    ncNetConf ncnet;
    int error;

    logprintfl (EUCAINFO, "doRunInstance() invoked (id=%s cores=%d disk=%d memory=%d\n", 
                instanceId, params->numberOfCores, params->diskSize, params->memorySize);
    logprintfl (EUCAINFO, "                         image=%s at %s\n", imageId, imageURL);
    logprintfl (EUCAINFO, "                         krnel=%s at %s\n", kernelId, kernelURL);
    if (ramdiskId) {
        logprintfl (EUCAINFO, "                         rmdsk=%s at %s\n", ramdiskId, ramdiskURL);
    }
    logprintfl (EUCAINFO, "                         vlan=%d priMAC=%s pubMAC=%s\n",
                vlan, privMac, pubMac);
    
    strcpy(ncnet.privateMac, privMac);
    strcpy(ncnet.publicMac, pubMac);
    ncnet.vlan = vlan;

    /* check as much as possible before forking off and returning */
    sem_p (inst_sem);
    instance = find_instance (&global_instances, instanceId);
    sem_v (inst_sem);
    if (instance) {
        logprintfl (EUCAFATAL, "Error: instance %s already running\n", instanceId);
        return 1; /* TODO: return meaningful error codes? */
    }
    if (!(instance = allocate_instance (instanceId, 
                                        reservationId,
                                        params, 
                                        imageId, imageURL,
                                        kernelId, kernelURL,
                                        ramdiskId, ramdiskURL,
                                        instance_state_names[PENDING], 
                                        PENDING, 
                                        meta->userId, 
                                        &ncnet, keyName,
                                        userData, launchIndex, groupNames, groupNamesSize))) {
        logprintfl (EUCAFATAL, "Error: could not allocate instance struct\n");
        return 2;
    }
    instance->state = BOOTING; /* TODO: do this in allocate_instance()? */

    sem_p (inst_sem); 
    error = add_instance (&global_instances, instance);
    sem_v (inst_sem);
    if ( error ) {
        free_instance (&instance);
        logprintfl (EUCAFATAL, "Error: could not save instance struct\n");
        return error;
    }

    instance->launchTime = time (NULL);
    instance->params.memorySize = params->memorySize;
    instance->params.numberOfCores = params->numberOfCores;
    instance->params.diskSize = params->diskSize;
    strcpy (instance->ncnet.privateIp, "0.0.0.0");
    strcpy (instance->ncnet.publicIp, "0.0.0.0");

    /* do the potentially long tasks in a thread */
    pthread_attr_t* attr = (pthread_attr_t*) malloc(sizeof(pthread_attr_t));
    pthread_attr_init(attr);
    pthread_attr_setdetachstate(attr, PTHREAD_CREATE_DETACHED);
    
    if ( pthread_create (&(instance->tcb), attr, startup_thread, (void *)instance) ) {
        pthread_attr_destroy(attr);
        logprintfl (EUCAFATAL, "failed to spawn a VM startup thread\n");
        sem_p (inst_sem);
        remove_instance (&global_instances, instance);
        sem_v (inst_sem);
        free_instance (&instance);
        return 1;
    }
    pthread_attr_destroy(attr);

    * outInst = instance;
    return 0;

}

static int doRebootInstance(ncMetadata *meta, char *instanceId) 
{
    ncInstance *instance;

    logprintfl (EUCAINFO, "doRebootInstance() invoked (id=%s)\n", instanceId);
    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) return NOT_FOUND;
    
    /* reboot the Xen domain */
    if (check_hypervisor_conn () == OK) {
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom) {
            /* also protect 'reboot', just in case */
            sem_p (xen_sem);
            int err=virDomainReboot (dom, 0);
            sem_v (xen_sem);
            if (err==0) {
                logprintfl (EUCAINFO, "rebooting Xen domain for instance %s\n", instanceId);
            }
            virDomainFree(dom); /* necessary? */
        } else {
            if (instance->state != BOOTING) {
                logprintfl (EUCAWARN, "warning: domain %s to be rebooted not running on hypervisor\n", instanceId);
            }
        }
    }

    return 0;
}

static int doGetConsoleOutput(ncMetadata *meta, char *instanceId, char **consoleOutput) {
  char *output;
  char cmd[256];
  int pid, status, rc, bufsize, fd;
  char filename[1024];  

  fprintf(stderr, "getconsoleoutput called\n");

  bufsize = sizeof(char) * 1024 * 64;
  output = malloc(bufsize);
  bzero(output, bufsize);

  snprintf(filename, 1024, "/tmp/consoleOutput.%s", instanceId);
  
  pid = fork();
  if (pid == 0) {
    int fd;
    fd = open(filename, O_WRONLY | O_TRUNC | O_CREAT, 0644);
    if (fd < 0) {
      // error
    } else {
      dup2(fd, 2);
      dup2(2, 1);
      close(0);
      rc = execl("/usr/sbin/xm", "/usr/sbin/xm", "console", instanceId, NULL);
      fprintf(stderr, "execl() failed\n");
      close(fd);
    }
    exit(0);
  } else {
    int count;
    fd_set rfds;
    struct timeval tv;
    struct stat statbuf;
    
    count=0;
    while(count < 10000 && stat(filename, &statbuf) < 0) {count++;}
    fd = open(filename, O_RDONLY);
    if (fd < 0) {
      logprintfl (EUCAERROR, "ERROR: could not open consoleOutput file %s for reading\n", filename);
    } else {
      FD_ZERO(&rfds);
      FD_SET(fd, &rfds);
      tv.tv_sec = 0;
      tv.tv_usec = 500000;
      rc = select(1, &rfds, NULL, NULL, &tv);
      bzero(output, bufsize);
      
      count = 0;
      rc = 1;
      while(rc && count < 1000) {
	rc = read(fd, output, bufsize-1);
	count++;
      }
      close(fd);
    }
    kill(pid, 9);
    wait(&status);
  }
  
  unlink(filename);
  
  if (output[0] == '\0') {
    snprintf(output, bufsize, "EMPTY");
  }
  
  *consoleOutput = base64_enc((unsigned char *)output, strlen(output));
  free(output);
  
  return(0);
}

static int doTerminateInstance (ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState)
{
    ncInstance *instance, *vninstance;
    int left;

    logprintfl (EUCAINFO, "doTerminateInstance() invoked (id=%s)\n", instanceId);
    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) return NOT_FOUND;

    /* try stopping the Xen domain */
    if (check_hypervisor_conn () == OK) {
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom) {
            /* also protect 'destroy' commands, just in case */
            sem_p (xen_sem);
            int err=virDomainDestroy (dom);
            sem_v (xen_sem);
            if (err==0) {
                logprintfl (EUCAINFO, "destroyed Xen domain for instance %s\n", instanceId);
            }
            virDomainFree(dom); /* necessary? */
        } else {
            if (instance->state != BOOTING) {
                logprintfl (EUCAWARN, "warning: domain %s to be terminated not running on hypervisor\n", instanceId);
            }
        }
    }

    /* change the state and let the monitoring_thread clean up state */
    change_state (instance, SHUTOFF);
    * previousState = instance->stateCode;
    * shutdownState = instance->stateCode;

    return 0;
}

static int doDescribeInstances (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    logprintfl (EUCAINFO, "doDescribeInstances() invoked\n");
    * outInstsLen = 0;
    * outInsts = NULL;

    sem_p (inst_sem);
    if (instIdsLen == 0) { /* describe all instances */
        int total = total_instances (&global_instances);
        if ( total ) {
            ncInstance * instance;
            int i, k = 0;
            
            * outInsts = malloc (sizeof(ncInstance *)*total);
            if ( (* outInsts) == NULL ) goto out_oom;
            
            for (i=0; (instance = get_instance (&global_instances))!=NULL; i++) {
                /* only pick ones the user is allowed to see */
                if (!strcmp(meta->userId, admin_user_id) || /* admin will see all */
                    !strcmp(meta->userId, instance->userId)) { /* owner */
                    (* outInsts)[k++] = instance;
                }
            }
            * outInstsLen = k;
        }
        
    } else { /* describe specific instances */
        ncInstance * instance;
        int i, j, k = 0;

        * outInsts = malloc (sizeof(ncInstance *)*(instIdsLen));
        if ( (* outInsts) == NULL ) goto out_oom;
        
        for (i=0; (instance = get_instance (&global_instances))!=NULL; i++) {        
            for (j=0; j<instIdsLen; j++) {
                if ( !strcmp(instance->instanceId, instIds[j]) ) {
                    /* only pick ones the user is allowed to see */
                    if (!strcmp(meta->userId, admin_user_id) || /* admin will see all */
                        !strcmp(meta->userId, instance->userId)) { /* owner */
                        (* outInsts)[k++] = instance;
                    }
                }
                /* TODO: do we complain about instIds[j] that weren't found? */
            }
        }
        * outInstsLen = k;
    }
    sem_v (inst_sem);
    return 0;

 out_oom:
    sem_v (inst_sem);
    return OUT_OF_MEMORY;
}

static int doDescribeResource (ncMetadata *meta, char *resourceType, ncResource **outRes)
{
    ncResource * res;
    ncInstance * inst;

    /* stats to re-calculate now */
    long long mem_free;
    long long disk_free;
    int cores_free;

    /* intermediate sums */
    long long sum_mem = 0;  /* for known domains: sum of requested memory */
    long long sum_disk = 0; /* for known domains: sum of requested disk sizes */
    int sum_cores = 0;      /* for known domains: sum of requested cores */

    logprintfl (EUCAINFO, "doDescribeResource() invoked\n");

    * outRes = NULL;
    sem_p (inst_sem); 
    while ((inst=get_instance(&global_instances))!=NULL) {
        if (inst->state == TEARDOWN) continue; /* they don't take up resources */
        sum_mem += inst->params.memorySize;
        sum_disk += (inst->params.diskSize + SWAP_SIZE);
        sum_cores += inst->params.numberOfCores;
    }
    sem_v (inst_sem);
    
    disk_free = disk_max - sum_disk;
    if ( disk_free < 0 ) disk_free = 0; /* should not happen */
    
    mem_free = mem_max - sum_mem;
    if ( mem_free < 0 ) mem_free = 0; /* should not happen */

    cores_free = cores_max - sum_cores; /* TODO: should we -1 for dom0? */
    if ( cores_free < 0 ) cores_free = 0; /* due to timesharing */

    /* check for potential overflow - should not happen */
    if (mem_max > INT_MAX ||
        mem_free > INT_MAX ||
        disk_max > INT_MAX ||
        disk_free > INT_MAX) {
        logprintfl (EUCAERROR, "stats integer overflow error (bump up the units?)\n");
        logprintfl (EUCAERROR, "   memory: max=%-10lld free=%-10lld\n", mem_max, mem_free);
        logprintfl (EUCAERROR, "     disk: max=%-10lld free=%-10lld\n", disk_max, disk_free);
        logprintfl (EUCAERROR, "    cores: max=%-10d free=%-10d\n", cores_max, cores_free);
        logprintfl (EUCAERROR, "       INT_MAX=%-10d\n", INT_MAX);
        return 10;
    }
    
    res = allocate_resource ("OK", mem_max, mem_free, disk_max, disk_free, cores_max, cores_free, "none");
    if (res == NULL) {
        logprintfl (EUCAERROR, "Out of memory\n");
        return 1;
    }
    * outRes = res;
    return 0;
}

static int doStartNetwork(ncMetadata *ccMeta, char **remoteHosts, int remoteHostsLen, int port, int vlan) {
  int rc, ret, i, status;
  char *brname;

  logprintfl (EUCAINFO, "StartNetwork(): called\n");

  rc = vnetStartNetwork(vnetconfig, vlan, NULL, NULL, &brname);
  if (rc) {
    ret = 1;
    logprintfl (EUCAERROR, "StartNetwork(): ERROR return from vnetStartNetwork %d\n", rc);
  } else {
    logprintfl (EUCAINFO, "StartNetwork(): SUCCESS return from vnetStartNetwork %d\n", rc);
    ret = 0;
  }
  logprintfl (EUCAINFO, "StartNetwork(): done\n");
  return(ret);
}

static int convert_dev_names (char *localDev, char *localDevReal, char *localDevTag) 
{
    char *strptr;

    bzero(localDevReal, 32);
    if ((strptr = strchr(localDev, '/')) != NULL) {
        sscanf(localDev, "/dev/%s", localDevReal);
    } else {
        snprintf(localDevReal, 32, "%s", localDev);
    }
    if (localDevReal[0] == 0) {
        logprintfl(EUCAERROR, "bad input parameter for localDev (should be /dev/XXX): '%s'\n", localDev);
        return(ERROR);
    }
    if (localDevTag) {
        bzero(localDevTag, 256);
        snprintf(localDevTag, 256, "unknown,requested:%s", localDev);
    }

    return 0;
}

static int doAttachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    int ret = OK;
    ncInstance * instance;
    char localDevReal[32];

    logprintfl (EUCAINFO, "doAttachVolume() invoked (id=%s vol=%s remote=%s local=%s)\n", instanceId, volumeId, remoteDev, localDev);

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, NULL);
    if (ret)
        return ret;

    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) 
        return NOT_FOUND;

    /* try attaching to the Xen domain */
    if (check_hypervisor_conn () == ERROR) {
        ret = ERROR;

    } else {
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom) {

            int err = 0;
            char xml [1024];
            snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", remoteDev, localDevReal);

            /* protect Xen calls, just in case */
            sem_p (xen_sem);
            err = virDomainAttachDevice (dom, xml);
            sem_v (xen_sem);
            if (err) {
                logprintfl (EUCAERROR, "AttachVolume() failed (err=%d) XML=%s\n", err, xml);
                ret = ERROR;
            } else {
                logprintfl (EUCAINFO, "attached %s to %s in domain %s\n", remoteDev, localDevReal, instanceId);
            }
            virDomainFree(dom);
        } else {
            if (instance->state != BOOTING) {
                logprintfl (EUCAWARN, "warning: domain %s not running on hypervisor, cannot attach device\n", instanceId);
            }
            ret = ERROR;
        }
    }

    if (ret==OK) {
        ncVolume * volume;

        sem_p (inst_sem);
        volume = add_volume (instance, volumeId, remoteDev, localDevReal);
        sem_v (inst_sem);
        if ( volume == NULL ) {
            logprintfl (EUCAFATAL, "ERROR: Failed to save the volume record, aborting volume attachment\n");
            return ERROR;
        }
    }

    return ret;
}

static int doDetachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    int ret = OK;
    ncInstance * instance;
    char localDevReal[32];

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, NULL);
    if (ret)
        return ret;

    logprintfl (EUCAINFO, "doDetachVolume() invoked (id=%s vol=%s remote=%s local=%s force=%d)\n", instanceId, volumeId, remoteDev, localDev, force);
    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) 
        return NOT_FOUND;

    /* try attaching to the Xen domain */
    if (check_hypervisor_conn () == ERROR) {
        ret = ERROR;

    } else {
        virDomainPtr dom = virDomainLookupByName(conn, instanceId);
        if (dom) {
            int err = 0, fd;
            char xml [1024], tmpfile[32], cmd[1024];
	    FILE *FH;
	    
            snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", remoteDev, localDevReal);

            /* protect Xen calls, just in case */
            sem_p (xen_sem);
	    if (!getuid()) {
	      err = virDomainDetachDevice (dom, xml);
	    } else {
	      /* virsh detach function does not work as non-root user on xen (bug). workaround is to shellout to virsh */
	      snprintf(tmpfile, 32, "/tmp/detachxml.XXXXXX");
	      fd = mkstemp(tmpfile);
	      if (fd > 0) {
		write(fd, xml, strlen(xml));
		close(fd);
		snprintf(cmd, 1024, "%s detach-device %s %s",virsh_command_path, instanceId, tmpfile);
		logprintfl(EUCADEBUG, "Running command: %s\n", cmd);
		err = WEXITSTATUS(system(cmd));
		unlink(tmpfile);
		if (err) {
		  logprintfl(EUCADEBUG, "first workaround command failed (%d), trying second workaround...\n", err);
		  snprintf(cmd, 1024, "%s block-detach %s %s", xm_command_path, instanceId, localDevReal);
		  logprintfl(EUCADEBUG, "Running command: %s\n", cmd);
		  err = WEXITSTATUS(system(cmd));
		}
	      } else {
		err = 1;
	      }
	    }
            sem_v (xen_sem);

            if (err) {
                logprintfl (EUCAERROR, "DetachVolume() failed (err=%d) XML=%s\n", err, xml);
                ret = ERROR;
            } else {
                logprintfl (EUCAINFO, "detached %s as %s in domain %s\n", remoteDev, localDevReal, instanceId);
            }
            virDomainFree(dom);
        } else {
            if (instance->state != BOOTING) {
                logprintfl (EUCAWARN, "warning: domain %s not running on hypervisor, cannot detach device\n", instanceId);
            }
            ret = ERROR;
        }
    }

    if (ret==OK) {
        ncVolume * volume;

        sem_p (inst_sem);
        volume = free_volume (instance, volumeId, remoteDev, localDevReal);
        sem_v (inst_sem);
        if ( volume == NULL ) {
            logprintfl (EUCAFATAL, "ERROR: Failed to find and remove volume record, aborting volume detachment\n");
            return ERROR;
        }
    }

    return ret;
}

struct handlers xen_libvirt_handlers = {
    .name = "xen",
    .doInitialize        = doInitialize,
    .doDescribeInstances = doDescribeInstances,
    .doRunInstance       = doRunInstance,
    .doTerminateInstance = doTerminateInstance,
    .doRebootInstance    = doRebootInstance,
    .doGetConsoleOutput  = doGetConsoleOutput,
    .doDescribeResource  = doDescribeResource,
    .doStartNetwork      = doStartNetwork,
    .doAttachVolume      = doAttachVolume,
    .doDetachVolume      = doDetachVolume
};

