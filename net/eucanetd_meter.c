// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

//!
//! @file net/eucanetd_meter.c
//! Implementation of eucanetd_meter using libpcap
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>  
#include <unistd.h>
#include <signal.h>
#include <asm/types.h>
#include <sys/types.h>
#include <pwd.h>

#include <pthread.h>

#include <arpa/inet.h>
#include <net/if.h>
#include <net/if_arp.h>

#include <sys/ioctl.h>
#include <sys/socket.h>

#include <linux/filter.h>
#include <linux/if_packet.h>
#include <linux/if_ether.h>

#include <pcap/pcap.h>

#include "eucanetd_meter.h"
#include "config.h"
#include "log.h"
#include "misc.h"
#include <atomic_file.h>
#include <ctype.h>
#include "euca_gni.h"
#include "eucalyptus.h"
#include "eucalyptus-config.h"
#include "eucanetd_util.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! List of configuration keys that are handled when the application starts
configEntry configKeysRestartEnm[] = {
    {"EUCALYPTUS", "/"}
    ,
    {"EUCA_USER", "eucalyptus"}
    ,
    {NULL, NULL}
    ,
};

//! List of configuration keys that are periodically monitored for changes
configEntry configKeysNoRestartEnm[] = {
    {"LOGLEVEL", "INFO"}
    ,
    {"LOGROLLNUMBER", "10"}
    ,
    {"LOGMAXSIZE", "104857600"}
    ,
    {"LOGPREFIX", ""}
    ,
    {"LOGFACILITY", ""}
    ,
    {NULL, NULL}
    ,
};

int enm_sig_rcvd = 0;
pthread_t monitorThreadId = 0;
pcap_t *ph = NULL;
enmConfig *config = NULL;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! USR1 and USR2 signals
static volatile boolean usr1Caught = FALSE;
static volatile boolean usr2Caught = FALSE;
static volatile boolean hupCaught = FALSE;
static volatile boolean termCaught = FALSE;
static volatile boolean intCaught = FALSE;

static volatile boolean pcapThreadRunning = FALSE;
static volatile boolean monitorThreadRunning = FALSE;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static pthread_t enm_create_thread(void *(*start_routine)(void *), void *arg);
static void *enm_monitor_thread(void *config);

static void *enm_pcap_thread(void *config);
static int enm_pcap_open_retry(enmConfig *config, int tries);
static int enm_pcap_loop(enmConfig *config);
static void enm_handle_ippkt(u_char *args, const struct pcap_pkthdr *header, const u_char *packet);

static int enm_read_config(enmConfig *config);
static int enm_read_gni(enmConfig *config, globalNetworkInfo *gni, char *eucahome);
static int enm_initialize_logs(enmConfig *config, log_level_e llevel);

static int enm_read_config_nc(char *dev, enmConfig *config, globalNetworkInfo *gni);
static int enm_read_config_dev(char *dev, enmConfig *config);

static int enm_config_free(enmConfig *config);

static void enm_sigterm_handler(int signal);
static void enm_sigint_handler(int signal);
static void enm_sighup_handler(int signal);
static void enm_sigusr1_handler(int signal);
static void enm_sigusr2_handler(int signal);
static void enm_install_signal_handlers(void);

static int enm_drop_priv(enmConfig *config);
static int enm_daemonize(enmConfig *config);

static int enm_trim(char *str);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/**
 * Outputs eucanetd_meter usage message
 * @param argv0 [in] command line string
 */
static void print_usage(char *argv0) {
    printf("Eucalyptus version %s - eucanetd_meter\n"
            "USAGE: %s OPTIONS\n"
            "\t%-12s| capture up to count packets\n"
            "\t%-12s| interface name to meter\n"
            "\t%-12s| operation mode: device (default) | nc\n"
            "\t%-12s| comma separated list of local IPs\n"
            "\t%-12s| local subnet CIDR block\n"
            "\t%-12s| run in background\n"
            "\t%-12s| enable interface in promiscuous mode (note: inverse of tcpdump)\n"
            "\t%-12s| read snaplen bytes of each captured packet (default 64 bytes)\n"
            , EUCA_VERSION, argv0, "-c count", "-i dev", "-m mode", "-l l_ips",
            "-n l_sn", "-d", "-p", "-s slen");
    exit (1);
}

/**
 * Logs a message and exits.
 * @param code [in] exit code
 * @param msg [in] optional message to log
 */
static void enm_exit(int code, char *msg) {
    if (msg) {
        LOGINFO("%s\n", msg);
    } else {
        LOGINFO("eucanetd_meter exiting.\n");
    }
    exit (code);
}

/**
 * eucanetd_meter application main entry point
 *
 * @param argc [in] the number of arguments passed on the command line
 * @param argv [in] the list of arguments passed on the command line
 *
 * @return 0 on success or 1 on failure
 */
int main(int argc, char *argv[]) {
    int opt = 0;
    char *dev = NULL;
    char *mode = NULL;
    char *lips = NULL;
    char *lsn = NULL;
    globalNetworkInfo *gni;

    config = EUCA_ZALLOC_C(1, sizeof (enmConfig));
    config->snaplen = 64;

    while ((opt = getopt(argc, argv, "hHc:i:m:l:n:dps:")) != -1) {
        switch (opt) {
            case 'c':
                config->count = atoi(optarg);
                break;
            case 'i':
                dev = strdup(optarg);
                break;
            case 'm':
                mode = strdup(optarg);
                break;
            case 'l':
                lips = strdup(optarg);
                break;
            case 'n':
                lsn = strdup(optarg);
                break;
            case 'd':
                config->daemonize = 1;
                break;
            case 'p':
                config->promiscuous_mode = 1;
                break;
            case 's':
                config->snaplen = atoi(optarg);
                if ((config->snaplen > 65535) || ((config->snaplen > 0) && (config->snaplen < 56))) {
                    LOGWARN("Invalid length %s. Will default to 64\n", optarg);
                    config->snaplen = 64;
                }
                break;
            case 'H':
            case 'h':
            default:
                print_usage(argv[0]);
                break;
        }
    }
    if (dev == NULL) {
        fprintf(stderr, "\tPlease specify interface to meter.\n");
        enm_exit(1, NULL);
    }
    if (mode == NULL) {
        mode = strdup("device");
    }

    if (config->daemonize) {
        enm_initialize_logs(config, EUCA_LOG_WARN);
    } else {
        enm_initialize_logs(config, EUCA_LOG_INFO);
    }
    
    enm_read_config(config);

    // Open pcap device
    char pcapdev[IF_NAME_LEN];
    if (!strcmp(mode, "device")) {
        snprintf(pcapdev, IF_NAME_LEN, "%s", dev);
    } else if (!strcmp(mode, "nc")) {
        snprintf(pcapdev, IF_NAME_LEN, "vn_%s", dev);
    }
    config->device = strdup(pcapdev);
    if (enm_pcap_open_retry(config, 100)) {
        enm_exit(1, NULL);
    }
    
    LOGINFO("switching to user %s\n", config->eucauser);
    enm_drop_priv(config);

    if (config->daemonize) {
        enm_initialize_logs(config, EUCA_LOG_ALL);
        enm_initialize_logs(config, EUCA_LOG_INFO);
        enm_daemonize(config);
    }

    LOGINFO("main thread %ld\n", pthread_self());
    enm_install_signal_handlers();

    if (!strcmp(mode, "device")) {
        LOGINFO("device mode selected\n");
        if (lips) {
            config->lips = strdup(lips);
        }
        if (lsn) {
            config->lsn = strdup(lsn);
        }
        enm_read_config_dev(dev, config);
    } else if (!strcmp(mode, "nc")) {
        LOGINFO("NC mode selected\n");
        gni = EUCA_ZALLOC_C(1, sizeof (globalNetworkInfo));

        enm_read_config_nc(dev, config, gni);
        
        GNI_FREE(gni);
        EUCA_FREE(gni);
    }

    monitorThreadId = enm_create_thread(enm_monitor_thread, config);
    if (monitorThreadId > 0) {
        LOGINFO("created thread %ld\n", monitorThreadId);
    }

    enm_pcap_thread(config);
    
    if (monitorThreadId > 0) {
        if (monitorThreadRunning) {
            monitorThreadRunning = FALSE;
            pthread_kill(monitorThreadId, SIGINT);
        }
        LOGINFO("waiting for %ld\n", monitorThreadId);
        pthread_join(monitorThreadId, NULL);
    }

    if (ph) {
        pcap_close(ph);
    }

    char *stats = enmPrintCounters(config);
    LOGINFO("\n%s\n", stats);
    EUCA_FREE(stats);

    EUCA_FREE(dev);
    EUCA_FREE(mode);
    EUCA_FREE(lips);
    EUCA_FREE(lsn);
    enm_config_free(config);
    EUCA_FREE(config);
    return (0);
}

/**
 * Creates a joinable POSIX thread that executes start_routine
 * @param start_routine [in] function pointer of interest
 * @param arg [in] optional argument to start_routine
 * @return newly created joinable POSIX thread ID on success. -1 on failure.
 */
static pthread_t enm_create_thread(void *(*start_routine)(void *), void *arg) {
    pthread_t ptid;
    pthread_attr_t ptattr;
    pthread_attr_init(&ptattr);
    pthread_attr_setdetachstate(&ptattr, PTHREAD_CREATE_JOINABLE);
    if (pthread_create(&ptid, &ptattr, start_routine, arg)) {
        LOGERROR("failed to create POSIX thread.\n");
        return (-1);
    } else {
        return (ptid);
    }
}

/**
 * eucanetd meter monitor thread - responsible to publish metered data
 * @param config [in] eucanetd_meter configuration parameters
 * @return NULL
 */
static void *enm_monitor_thread(void *config) {
    //enmConfig *c = (enmConfig *) config;
    monitorThreadRunning = TRUE;
    while (monitorThreadRunning) {
        sleep (10);
        if (monitorThreadRunning) {
            char *stats = enmPrintCounters(config);
            LOGINFO("\n%s\n", stats);
            EUCA_FREE(stats);
        }
    }
    monitorThreadRunning = FALSE;
    pthread_exit(NULL);
}

/**
 * eucanetd meter thread - responsible for network metering.
 * @param config [in] eucanetd_meter configuration parameters
 * @return NULL
 */
static void *enm_pcap_thread(void *config) {
    enmConfig *c = (enmConfig *) config;
    enm_pcap_loop(c);
    return (NULL);
}

/**
 * Try tries times (once a second) to open a raw/cooked socket through pcap from parameters in config.
 * Useful when waiting for VM interface to be created.
 * @param config [in] eucanetd_meter configuration parameters
 * @param tries [in] number of open attempts
 * @return 0 on success. Positive integer on failure.
 */
static int enm_pcap_open_retry(enmConfig *config, int tries) {
    if (!config || !config->device) {
        LOGWARN("cannot open NULL device.\n");
        return (1);
    }
    char errbuf[PCAP_ERRBUF_SIZE];
    LOGINFO("opening raw/cooked socket on %s\n", config->device);
    while (tries) {
        ph = pcap_open_live(config->device, config->snaplen, config->promiscuous_mode, 1000, errbuf);
        if (ph) {
            return (0);
        }
        tries--;
        sleep(1);
    }
    LOGERROR("Failed to open %s: %s\n", config->device, errbuf);
    return (1);
}

/**
 * Prepares for and invokes pcap_loop() from parameters in config.
 * @param config [in] eucanetd_meter configuration parameters
 * @return 0 on success. Positive integer on any failure.
 */
static int enm_pcap_loop(enmConfig *config) {
    if (!config || !ph) {
        LOGWARN("cannot proceed with pcap_loop() with null config\n");
        return (1);
    }

    if (pcap_datalink(ph) != DLT_EN10MB) {
        LOGERROR("Unknown datalink type %d detected for %s\n", pcap_datalink(ph), config->device);
        return (1);
    }

    struct bpf_program bpfprogram = { 0 };
    if (pcap_compile(ph, &bpfprogram, "ip", 0, PCAP_NETMASK_UNKNOWN) == 0) {
        LOGINFO("successfully compiled bpf program\n");
        if (pcap_setfilter(ph, &bpfprogram) == 0) {
            LOGINFO("successfully applied bpf program to %s\n", config->device);
        }
        pcap_freecode(&bpfprogram);
    }

    pcapThreadRunning = TRUE;
    while (pcapThreadRunning) {
        pcap_loop(ph, config->count, enm_handle_ippkt, (u_char *) config);
        pcapThreadRunning = FALSE;
    }
    
    return (0);
}

/**
 * Call back function to be used with pcap_loop() or pcap_dispatch().
 * @param args [in] user data
 * @param header [out] captured packet's pcap header
 * @param packet [out] captured packet's data
 */
static void enm_handle_ippkt(u_char *args, const struct pcap_pkthdr *header, const u_char *packet) {
    struct ip *ip_hdr = NULL;
       
    if (!header || !packet || !args) {
        LOGWARN("cannot handle null packet\n");
        return;
    }
    
    enmConfig *config = (enmConfig *) args;
    enmInterface *eni = &(config->eni);
    
    ip_hdr = (struct ip *) (packet + ETH_HLEN);
    int ip_hdr_len = ip_hdr->ip_hl * 4;
    if (ip_hdr_len < 20) {
        LOGWARN("invalid IP header length %d\n", ip_hdr_len);
        return;
    }

    if (header->len < (ip_hdr_len + ETH_HLEN)) {
        LOGWARN("invalid IP packet captured\n");
        return;
    }
    
    boolean pktin = FALSE;
    boolean pktout = FALSE;
    for (int i = 0; !pktin && !pktout && (i < eni->max_local_ips); i++) {
        if (ip_hdr->ip_src.s_addr == eni->local_ips[i]->s_addr) {
            pktout = TRUE;
        }
        if (ip_hdr->ip_dst.s_addr == eni->local_ips[i]->s_addr) {
            pktin = TRUE;
        }
    }

    for (int i = 0; i < eni->max_counters; i++) {
        enmCounter *counter = eni->counters[i];
        if (pktin) {
            for (int j = 0; j < counter->max_match; j++) {
                if ((ip_hdr->ip_src.s_addr & counter->match_netmask[j]->s_addr) ==
                        counter->match_netaddr[j]->s_addr) {
                    (counter->pkts_in)++;
                    counter->bytes_in += header->len;
                }
            }
            for (int j = 0; j < counter->max_inv_match; j++) {
                if ((ip_hdr->ip_src.s_addr & counter->inv_match_netmask[j]->s_addr) !=
                        counter->inv_match_netaddr[j]->s_addr) {
                    (counter->pkts_in)++;
                    counter->bytes_in += header->len;
                }
            }
        }
        if (pktout) {
            for (int j = 0; j < counter->max_match; j++) {
                if ((ip_hdr->ip_dst.s_addr & counter->match_netmask[j]->s_addr) ==
                        counter->match_netaddr[j]->s_addr) {
                    (counter->pkts_out)++;
                    counter->bytes_out += header->len;
                }
            }
            for (int j = 0; j < counter->max_inv_match; j++) {
                if ((ip_hdr->ip_dst.s_addr & counter->inv_match_netmask[j]->s_addr) !=
                        counter->inv_match_netaddr[j]->s_addr) {
                    (counter->pkts_out)++;
                    counter->bytes_out += header->len;
                }
            }
        }
    }

#ifdef EUCANETD_DEBUG
    struct ethhdr *eth_hdr = (struct ethhdr *) packet;
    u16 type = ntohs(eth_hdr->h_proto);
    char *src = EUCA_INETA2DOT(&(ip_hdr->ip_src));
    char *dst = EUCA_INETA2DOT(&(ip_hdr->ip_dst));
    LOGINFO("%d bytes, type %x, %s -> %s %s\n", header->len, type,
            src, dst, pktin ? "in" : "out");
    EUCA_FREE(src);
    EUCA_FREE(dst);
#endif //EUCANETD_DEBUG
}

/**
 * Reads the eucalyptus.conf configuration file and pull the important fields.
 * @param config [in] pointer to data structure that hold eucanetd_meter information
 * @return 0 on success or 1 on failure. If any FATAL error occurs, the
 *         process will return with an exit code of 1.
 */
static int enm_read_config(enmConfig *config) {
    int i = 0;
    char *tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
    char home[EUCA_MAX_PATH] = "";
    char netPath[EUCA_MAX_PATH] = "";
    char eucadir[EUCA_MAX_PATH] = "";
    char *cvals[ENM_CVAL_LAST] = {NULL};

    LOGINFO("reading configuration\n");

    memset(cvals, 0, sizeof (char *) * ENM_CVAL_LAST);

    // set 'home' based on environment
    if (!tmpstr) {
        snprintf(home, EUCA_MAX_PATH, "/");
    } else {
        snprintf(home, EUCA_MAX_PATH, "%s", tmpstr);
    }
    if (strlen(home) && (home[strlen(home) - 1] == '/')) {
        home[strlen(home) - 1] = '\0';
    }

    snprintf(eucadir, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR, home);
    if (check_directory(eucadir)) {
        LOGFATAL("cannot locate eucalyptus installation: make sure EUCALYPTUS env is set\n");
        return (1);
    }

    snprintf(netPath, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT, home);

    // setup and read local NC eucalyptus.conf file
    snprintf(config->configFiles[0], EUCA_MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
    configInitValues(configKeysRestartEnm, configKeysNoRestartEnm); // initialize config subsystem
    readConfigFile(config->configFiles, 1);

    cvals[ENM_CVAL_EUCAHOME] = configFileValue("EUCALYPTUS");
    cvals[ENM_CVAL_MODE] = configFileValue("VNET_MODE");
    cvals[ENM_CVAL_EUCA_USER] = configFileValue("EUCA_USER");

    EUCA_FREE(config->eucahome);
    config->eucahome = strdup(cvals[ENM_CVAL_EUCAHOME]);
    if (strlen(config->eucahome)) {
        if (config->eucahome[strlen(config->eucahome) - 1] == '/') {
            config->eucahome[strlen(config->eucahome) - 1] = '\0';
        }
    }

    EUCA_FREE(config->eucauser);
    config->eucauser = strdup(cvals[ENM_CVAL_EUCA_USER]);

    snprintf(config->cmdprefix, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, config->eucahome);

    for (i = 0; i < ENM_CVAL_LAST; i++) {
        EUCA_FREE(cvals[i]);
    }

    return (0);
}

/**
 * Reads the global network information XML.
 * @param config [in] pointer to data structure that hold eucanetd_meter information
 * @param gni [in] pointer to data structure to hold GNI
 * @param eucahome [in] eucalyptus home dir
 * @return 0 on success or 1 on failure.
 */
static int enm_read_gni(enmConfig *config, globalNetworkInfo *gni, char *eucahome) {
    int rc = 0;
    char home[EUCA_MAX_PATH] = "";
    char destfile[EUCA_MAX_PATH] = "";
    char sourceuri[EUCA_MAX_PATH] = "";
    boolean gni_updated;

    if (!eucahome) {
        snprintf(home, EUCA_MAX_PATH, "/");
    } else {
        snprintf(home, EUCA_MAX_PATH, "%s", eucahome);
    }
    if (strlen(home) && (home[strlen(home) - 1] == '/')) {
        home[strlen(home) - 1] = '\0';
    }

    LOGINFO("reading gni\n");

    if (gni) {
        // search for the global state file from eucalyptus
        snprintf(sourceuri, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/global_network_info.xml", home);
        if (check_file(sourceuri)) {
            LOGFATAL("cannot find global_network_info.xml state file in $EUCALYPTUS/var/run/eucalyptus\n");
            return (1);
        } else {
            snprintf(sourceuri, EUCA_MAX_PATH, "file://" EUCALYPTUS_RUN_DIR "/global_network_info.xml", home);
            snprintf(destfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/enm_global_network_info.xml", home);
            LOGTRACE("found global_network_info.xml state file: setting source URI to '%s'\n", sourceuri);
        }

        // initialize and populate data from global_network_info.xml file
        atomic_file_init(&(config->gni_atomic_file), sourceuri, destfile, 0);

        rc = atomic_file_get(&(config->gni_atomic_file), &gni_updated);
        if (rc) {
            LOGWARN("cannot get latest global network info file (%s)\n", config->gni_atomic_file.dest);
            return (1);
        }

        rc = gni_populate_v(GNI_POPULATE_ALL, gni, NULL, config->gni_atomic_file.dest);
        if (rc) {
            LOGFATAL("could not initialize global network info data structures from XML input\n");
            return (1);
        }

        if (strlen(gni->sMode)) {
            snprintf(config->netMode, NETMODE_LEN, "%s", gni->sMode);
        } else {
            snprintf(config->netMode, NETMODE_LEN, "%s", NETMODE_INVALID);
        }
        config->nmCode = euca_netmode_atoi(config->netMode);
    }

    return (0);
}

/**
 * Initialize the logging services
 * @param config [in] pointer to data structure that hold eucanetd_meter information
 * @param llevel [in] log level to be set. Specifying EUCA_LOG_ALL will configure
 * using parameters in eucalyptus.conf
 * @return Always returns 0
 *
 * @see log_file_set(), configReadLogParams(), log_params_set(), log_prefix_set()
 */
static int enm_initialize_logs(enmConfig *config, log_level_e llevel) {
    int log_level = 0;
    int log_roll_number = 0;
    long log_max_size_bytes = 0;
    char *log_prefix = NULL;
    char logfile[EUCA_MAX_PATH] = "";

    switch (llevel) {
        case EUCA_LOG_ALL:
            if (!config) {
               fprintf(stderr, "unable to initialize logs with NULL config\n"); 
            }
            snprintf(logfile, EUCA_MAX_PATH,  EUCALYPTUS_LOG_DIR "/eucanetd_meter.log", config->eucahome);
            log_file_set(logfile, NULL);

            configReadLogParams(&log_level, &log_roll_number, &log_max_size_bytes, &log_prefix);

            log_params_set(log_level, log_roll_number, log_max_size_bytes);
            log_prefix_set(log_prefix);
            EUCA_FREE(log_prefix);
            break;
        case EUCA_LOG_TRACE:
            log_params_set(EUCA_LOG_TRACE, 0, 100000);
            break;
        case EUCA_LOG_DEBUG:
            log_params_set(EUCA_LOG_DEBUG, 0, 100000);
            break;
        case EUCA_LOG_INFO:
            log_params_set(EUCA_LOG_INFO, 0, 100000);
            break;
        case EUCA_LOG_WARN:
            log_params_set(EUCA_LOG_WARN, 0, 100000);
            break;
        case EUCA_LOG_ERROR:
            log_params_set(EUCA_LOG_ERROR, 0, 100000);
            break;
        case EUCA_LOG_FATAL:
            log_params_set(EUCA_LOG_FATAL, 0, 100000);
            break;
        default:
            log_params_set(EUCA_LOG_INFO, 0, 100000);
            break;
    }

    return (0);
}

/**
 * Retrieves the metering configuration for nc mode.
 * @param dev [in] interface name to meter
 * @param config [in] pointer to data structure that hold eucanetd_meter information
 * @param gni [in] pointer to data structure to hold GNI
 * @return 0 on success or 1 on failure. If any FATAL error occurs, the
 *         process will return with an exit code of 1.
 */
static int enm_read_config_nc(char *dev, enmConfig *config, globalNetworkInfo *gni) {
    if (!config || !gni) {
        LOGWARN("cannot read config (nc) from NULL\n");
        return (1);
    }
    if (enm_read_gni(config, gni, config->eucahome)) {
        return (1);
    }

    char *strptra = NULL;
    enmCounter *counter = NULL;

    gni_node *gni_nc = NULL;
    gni_find_self_node(gni, &gni_nc);
    if (gni_nc) {
        LOGINFO("found NC %s in GNI\n", gni_nc->name);
    } else {
        LOGWARN("this NC does not seem to be registered.\n");
        return (1);
    }

    char eni_dev[INTERFACE_ID_LEN] = { 0 };
    gni_instance *gnidev = NULL;
    gni_find_interface(gni, dev, &gnidev);
    if (gnidev) {
        snprintf(eni_dev, INTERFACE_ID_LEN, "vn_%s", gnidev->name);
        if (config->device) {
            EUCA_FREE(config->device);
        }
        config->device = strdup(eni_dev);
        snprintf(config->eni.name, SMALL_CHAR_BUFFER_SIZE, "%s", eni_dev);
        config->eni.id = euca_id_strtol(eni_dev);

        // VPC
        gni_vpc *gnivpc = gni_get_vpc(gni, gnidev->vpc, NULL);
        if (gnivpc) {
            gni_vpcsubnet *gnisubnet = gni_get_vpcsubnet(gnivpc, gnidev->subnet, NULL);
            if (gnisubnet) {
                LOGINFO("metering %s (%s), %s %s\n", gnidev->name, gnidev->ifname, gnivpc->name, gnisubnet->name);
                boolean update_lip = TRUE;
                if (config->eni.max_local_ips) {
                    if (config->eni.local_ips[0]->s_addr != htonl(gnidev->privateIp)) {
                        for (int i = 0; i < config->eni.max_local_ips; i++) {
                            EUCA_FREE(config->eni.local_ips[i]);
                        }
                        EUCA_FREE(config->eni.local_ips);
                    } else {
                        update_lip = FALSE;
                    }
                }
                if (update_lip) {
                    struct in_addr *lip = EUCA_ZALLOC_C(1, sizeof (struct in_addr));
                    config->eni.local_ips = EUCA_APPEND_PTRARR(config->eni.local_ips, &(config->eni.max_local_ips), lip);
                    lip->s_addr = htonl(gnidev->privateIp);
                    strptra = EUCA_INETA2DOT(config->eni.local_ips[0]);
                    LOGINFO("\tlocal IP %s\n", strptra);
                    EUCA_FREE(strptra);
                }

                // VPC CIDR
                if (!config->eni.vpc) {
                    counter = newCounter(gnivpc->name, gnivpc->cidr);
                    config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                            &(config->eni.max_counters), counter);
                    config->eni.vpc = counter;
                }

                // VPC subnet CIDR
                if (!config->eni.vpcsubnet) {
                    counter = newCounter(gnisubnet->name, gnisubnet->cidr);
                    config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                            &(config->eni.max_counters), counter);
                    config->eni.vpcsubnet = counter;
                }

                // external net
                if (!config->eni.external) {
                    counter = newCounterEmpty("external");
                    counterAddMatch(counter, TRUE, gnivpc->cidr);
                    config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                            &(config->eni.max_counters), counter);
                    config->eni.external = counter;
                }

                // VPC other subnets
                for (int i = 0; i < gnivpc->max_subnets; i++) {
                    gni_vpcsubnet *gvs = &(gnivpc->subnets[i]);
                    if (strcmp(gvs->name, gnisubnet->name)) {
                        counter = findCounter(gvs->name, config->eni.counters, config->eni.max_counters);
                        if (!counter) {
                            counter = newCounter(gvs->name, gvs->cidr);
                            config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                                    &(config->eni.max_counters), counter);
                        }
                    }
                }

                // Meta data
                if (!config->eni.metadata) {
                    counter = newCounterEmpty("metadata");
                    counterAddMatch(counter, FALSE, "169.254.169.254/32");
                    config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                            &(config->eni.max_counters), counter);
                    config->eni.metadata = counter;
                }

                // CLC
                if (!config->eni.clc) {
                    struct in_addr clcip;
                    clcip.s_addr = htonl(gni->enabledCLCIp);
                    strptra = EUCA_INETA2DOT(&clcip);
                    counter = newCounterEmpty("clc");
                    counterAddMatch(counter, FALSE, strptra);
                    EUCA_FREE(strptra);
                    config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                            &(config->eni.max_counters), counter);
                    config->eni.clc = counter;
                }

            } else {
                LOGWARN("cannot find %s of %s\n", gnidev->subnet, gnivpc->name);
            }
        } else {
            LOGWARN("cannot find %s\n", gnidev->vpc);
        }
    } else {
        LOGWARN("%s not found in gni\n", dev);
    }
    return (0);
}

/**
 * Retrieves the metering configuration for dev mode.
 * @param dev [in] interface name to meter
 * @param config [in] pointer to data structure that hold eucanetd_meter information
 * @return 0 on success or 1 on failure. If any FATAL error occurs, the
 *         process will return with an exit code of 1.
 */
static int enm_read_config_dev(char *dev, enmConfig *config) {
    char *strptra = NULL;
    char **straptr = NULL;
    int max_straptr = 0;
    enmCounter *counter = NULL;

    snprintf(config->eni.name, SMALL_CHAR_BUFFER_SIZE, "%s", dev);
    config->eni.id = euca_id_strtol(dev);

    if (config->lips) {
        euca_split_string(config->lips, &straptr, &max_straptr, ',');
        for (int i = 0; i < max_straptr; i++) {
            if (euca_ip_is_dot(straptr[i])) {
                struct in_addr *lip = EUCA_ZALLOC_C(1, sizeof (struct in_addr));
                config->eni.local_ips = EUCA_APPEND_PTRARR(config->eni.local_ips, &(config->eni.max_local_ips), lip);
                enm_trim(straptr[i]);
                lip->s_addr = euca_inet_aton(straptr[i]);
                strptra = EUCA_INETA2DOT(config->eni.local_ips[config->eni.max_local_ips - 1]);
                LOGINFO("\tlocal IP %s\n", strptra);
                EUCA_FREE(strptra);
            }
        }
        free_ptrarr(straptr, max_straptr);
        straptr = NULL;
        max_straptr = 0;
    }

    if (config->lsn) {
        char neta[INET_ADDR_LEN] = {0};
        int netm = 32;
        if (sscanf(config->lsn, "%15[^/]/%d", neta, &netm) != 2) {
            netm = 32;
        }

        if (euca_ip_is_dot(neta)) {
            LOGINFO("\tlocal subnet %s\n", config->lsn);
            // external net
            counter = newCounterEmpty("external");
            counterAddMatch(counter, TRUE, config->lsn);
            config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                    &(config->eni.max_counters), counter);
            // internal net
            counter = newCounterEmpty("internal");
            counterAddMatch(counter, FALSE, config->lsn);
            config->eni.counters = EUCA_APPEND_PTRARR(config->eni.counters,
                    &(config->eni.max_counters), counter);
        }
    }

    return (0);
}

/**
 * Releases memory allocated to the given config
 * @param config [in] enmConfig data structure of interest
 * @return always 0
 */
static int enm_config_free(enmConfig *config) {
    if (config) {
        EUCA_FREE(config->eucahome);
        EUCA_FREE(config->eucauser);
        EUCA_FREE(config->euca_version_str);
        EUCA_FREE(config->device);
        EUCA_FREE(config->lips);
        EUCA_FREE(config->lsn);
        atomic_file_free(&(config->gni_atomic_file));
        memset(config, 0, sizeof (enmConfig));
    }
    return (0);
}

/**
 * Handles the SIGTERM signal
 * @param signal [in] received signal number (SIGTERM is expected)
 */
static void enm_sigterm_handler(int signal) {
    LOGINFO("enm caught SIGTERM signal.\n");
    termCaught = TRUE;
    enm_sig_rcvd = signal;
    if (ph && pcapThreadRunning) {
        pcap_breakloop(ph);
    }
}

/**
 * Handles the SIGINT signal
 * @param signal [in] received signal number (SIGINT is expected)
 */
static void enm_sigint_handler(int signal) {
    LOGINFO("enm caught SIGINT signal.\n");
    intCaught = TRUE;
    enm_sig_rcvd = signal;
    if (ph && pcapThreadRunning) {
        pcap_breakloop(ph);
    }
}

/**
 * Handles the SIGHUP signal
 * @param signal [in] received signal number (SIGHUP is expected)
 */
static void enm_sighup_handler(int signal) {
    LOGINFO("enm caught a SIGHUP signal.\n");
    hupCaught = TRUE;
    enm_sig_rcvd = signal;
}

/**
 * Handles SIGUSR1 signal.
 * @param signal [in] received signal number.
 */
static void enm_sigusr1_handler(int signal) {
    LOGINFO("enm caught a SIGUSR1 (%d) signal.\n", signal);
    usr1Caught = TRUE;
    enm_sig_rcvd = signal;
    if (monitorThreadId && pthread_equal(monitorThreadId, pthread_self())) {
        char *stats = enmPrintCounters(config);
        LOGINFO("\n%s\n", stats);
        EUCA_FREE(stats);
        usr1Caught = FALSE;
    } else {
        if (monitorThreadId > 0) {
            pthread_kill(monitorThreadId, signal);
        }
    }
}

/**
 * Handles SIGUSR2 signal.
 * @param signal [in] received signal number.
 */
static void enm_sigusr2_handler(int signal) {
    LOGINFO("enm caught a SIGUSR2 (%d) signal.\n", signal);
    usr2Caught = TRUE;
    enm_sig_rcvd = signal;
}

/**
 * Installs signal handlers for this application
 */
static void enm_install_signal_handlers(void) {
    struct sigaction act = { {0} };

    // Install the SIGTERM signal handler
    memset(&act, 0, sizeof(struct sigaction));
    act.sa_handler = &enm_sigterm_handler;
    if (sigaction(SIGTERM, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGTERM handler");
        enm_exit(1, NULL);
    }
    // Install the SIGINT signal handler
    memset(&act, 0, sizeof(struct sigaction));
    act.sa_handler = &enm_sigint_handler;
    if (sigaction(SIGINT, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGINT handler");
        enm_exit(1, NULL);
    }
    // Install the SIGHUP signal handler
    memset(&act, 0, sizeof(struct sigaction));
    act.sa_handler = &enm_sighup_handler;
    if (sigaction(SIGHUP, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGHUP handler");
        enm_exit(1, NULL);
    }
    // Install the SIGUSR1 signal handler
    memset(&act, 0, sizeof(struct sigaction));
    act.sa_handler = &enm_sigusr1_handler;
    if (sigaction(SIGUSR1, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGUSR1 handler");
        enm_exit(1, NULL);
    }
    // Install the SIGUSR1 signal handler
    memset(&act, 0, sizeof(struct sigaction));
    act.sa_handler = &enm_sigusr2_handler;
    if (sigaction(SIGUSR2, &act, NULL) < 0) {
        LOGFATAL("Failed to install SIGUSR2 handler");
        enm_exit(1, NULL);
    }
}

/**
 * Converts the hexadecimal portion of IDs (e.g., i-12345678, eni-abcdef01, vpc-1234abcd)
 * to integer representation. Only the first 8 characters after the first hyphen are
 * processed.
 * @param id [in] id of interest
 * @return the converted integer. Errors are ignored - if the hexadecimal portion cannot
 * be identified, 0 is returned.
 */
u32 euca_id_strtol(char *id) {
    if (!id) {
        return (0);
    }
    u32 sid = 0;
    sscanf(id, "%*[^-]-%8x", &sid);
    return (sid);
}

/**
 * Releases memory allocated to the given eni enmInterface structure.
 * @param eni [in] enmInterface structure of interest
 * @return 0 on success. 1 on any failure.
 */
int enmInterface_free(enmInterface *eni) {
    if (!eni) {
        return (0);
    }
    for (int i = 0; i < eni->max_local_ips; i++) {
        EUCA_FREE(eni->local_ips[i]);
    }
    EUCA_FREE(eni->local_ips);
    for (int i = 0; i < eni->max_counters; i++) {
        enmCounter_free(eni->counters[i]);
        EUCA_FREE(eni->counters[i]);
    }
    EUCA_FREE(eni->counters);
    memset(eni, 0, sizeof(enmInterface));
    return (0);
}

/**
 * Releases memory allocated to the given counter enmCounter structure
 * @param counter [in] enmCounter structure of interest
 * @return 0 on success. 1 on any failure.
 */
int enmCounter_free(enmCounter *counter) {
    if (!counter) {
        return (0);
    }
    for (int i = 0; i < counter->max_match; i++) {
        EUCA_FREE(counter->match_netaddr[i]);
        EUCA_FREE(counter->match_netmask[i]);
    }
    EUCA_FREE(counter->match_netaddr);
    EUCA_FREE(counter->match_netmask);
    for (int i = 0; i < counter->max_inv_match; i++) {
        EUCA_FREE(counter->inv_match_netaddr[i]);
        EUCA_FREE(counter->inv_match_netmask[i]);
    }
    EUCA_FREE(counter->inv_match_netaddr);
    EUCA_FREE(counter->inv_match_netmask);
    memset(counter, 0, sizeof(enmCounter));
    return (0);
}

/**
 * Creates a new empty counter structure
 * @return a newly allocated enmCounter structure
 */
enmCounter *newCounterEmpty(char *name) {
    enmCounter *counter = EUCA_ZALLOC_C(1, sizeof (enmCounter));
    snprintf(counter->name, SMALL_CHAR_BUFFER_SIZE, "%s", name);
    counter->id = euca_id_strtol(name);
    if (!counter->id) {
        LOGTRACE("failed to identify %s\n", name);
    }
    
    return (counter);
}

/**
 * Creates a new counter structure with parameters pre-populated.
 * @param name [in] name of the counter of interest.
 * @param cidr [in] CIDR block of the counter of interest.
 * @return a newly allocated enmCounter structure with pre-populated parameters.
 */
enmCounter *newCounter(char *name, char *cidr) {
    enmCounter *counter = EUCA_ZALLOC_C(1, sizeof (enmCounter));
    snprintf(counter->name, SMALL_CHAR_BUFFER_SIZE, "%s", name);
    counter->id = euca_id_strtol(name);
    if (!counter->id) {
        LOGWARN("failed to identify %s\n", name);
    }

    counterAddMatch(counter, FALSE, cidr);
    
    return (counter);
}

/**
 * Searches for counter named name in counters array with max_counters entries.
 * @param name [in] name of the counter of interest
 * @param counters [in] pointer to an array of counters
 * @param max_counters [in] number of entries in counters array
 * @return pointer to the counter of interest if found. NULL otherwise.
 */
enmCounter *findCounter(char *name, enmCounter **counters, int max_counters) {
    if (!name || !counters) {
        LOGWARN("Cannot search for NULL counter\n");
        return (NULL);
    }
    for (int i = 0; i < max_counters; i++) {
        if (counters[i] == NULL) {
            continue;
        }
        if (!strcmp(name, counters[i]->name)) {
            return (counters[i]);
        }
    }
    return (NULL);
}

/**
 * Add the the cidr to the given enmCounter counter match list. If inv is TRUE,
 * the cidr is added to the inverse match list.
 * @param inv [in] boolean that indicates whether the match is an inverse match or not
 * @param cidr [in] CIDR block of interest
 * @return 0 on success, 1 otherwise.
 */
int counterAddMatch(enmCounter *counter, boolean inv, char *cidr) {
    if (!counter || !cidr) {
        return (1);
    }

    struct in_addr *ina_net = NULL;
    struct in_addr *ina_mask = NULL;
    char *strptra = NULL;
    char *strptrb = NULL;

    ina_net = EUCA_ZALLOC_C(1, sizeof (struct in_addr));
    ina_mask = EUCA_ZALLOC_C(1, sizeof (struct in_addr));
    euca_cidr_aton(cidr, ina_net, ina_mask);

    strptra = EUCA_INETA2DOT(ina_net);
    strptrb = EUCA_INETA2DOT(ina_mask);
    LOGINFO("\tcounter %s %s %s/%s\n", counter->name, inv ? "inv  " : "match", strptra, strptrb);
    EUCA_FREE(strptra);
    EUCA_FREE(strptrb);

    int tmpmax = 0;
    if (inv) {
        tmpmax = counter->max_inv_match;
        counter->inv_match_netaddr = EUCA_APPEND_PTRARR(counter->inv_match_netaddr,
                &(counter->max_inv_match), ina_net);
        counter->inv_match_netmask = EUCA_APPEND_PTRARR(counter->inv_match_netmask,
                &tmpmax, ina_mask);
    } else {
        tmpmax = counter->max_match;
        counter->match_netaddr = EUCA_APPEND_PTRARR(counter->match_netaddr,
                &(counter->max_match), ina_net);
        counter->match_netmask = EUCA_APPEND_PTRARR(counter->match_netmask,
                &tmpmax, ina_mask);
    }
    
    return (0);
}

/**
 * Generates a string with current counter statistics.
 * @param config [in] pointer to data structure that hold eucanetd_meter information
 * @return pointer to a newly allocated buffer, where current counter statistics is
 * stored in string format. Caller responsible to release the allocated memory.
 */
char *enmPrintCounters(enmConfig *config) {
    if (!config) {
        return (NULL);
    }
    
    enmInterface *eni = &(config->eni);
    char *buf = EUCA_ALLOC(2048, sizeof (char));
    buf[0] = '\0';
    char *pbuf = buf;
    int pbuf_len = 2047;
    
    for (int i = 0; i < eni->max_counters; i++) {
        enmCounter *counter = eni->counters[i];
        euca_buffer_snprintf(&pbuf, &pbuf_len, "%s\n", counter->name);
        euca_buffer_snprintf(&pbuf, &pbuf_len, "\tpkts_in : %ld, bytes_in  %ld\n",
                counter->pkts_in, counter->bytes_in);
        euca_buffer_snprintf(&pbuf, &pbuf_len, "\tpkts_out: %ld, bytes_out %ld\n",
                counter->pkts_out, counter->bytes_out);
    }
    
    return (buf);
}

/**
 * Switches user (drop priv)
 *
 * @return 0 or exits
 *
 */
static int enm_drop_priv(enmConfig *config) {
    struct passwd *pwent = NULL;

    if (!config) {
        fprintf(stderr, "unable to drop enm privileges with NULL config\n");
        exit (1);
    }
    pwent = getpwnam(config->eucauser);
    if (!pwent) {
        fprintf(stderr, "could not find user %s in password database\n", SP(config->eucauser));
        exit (1);
    }

    if (setgid(pwent->pw_gid) || setuid(pwent->pw_uid)) {
        fprintf(stderr, "could not switch process to UID/GID '%d/%d'\n", pwent->pw_uid, pwent->pw_gid);
        exit (1);
    }

    return (0);
}

/**
 * back-grounds eucanetd_meter
 *
 * @return 0 or exits
 */
static int enm_daemonize(enmConfig *config) {
    int pid, sid;
    char pidfile[EUCA_MAX_PATH];
    FILE *FH = NULL;

    if (!config) {
        LOGFATAL("enm_daemonize() invalid argument: NULL config\n");
        enm_exit(1, NULL);
    }
    pid = fork();
    if (pid) {
        exit(0);
    }

    pid = getpid();
    sid = setsid();
    if (sid < 0) {
        LOGFATAL("%d failed to setsid()\n", pid);
        enm_exit(1, NULL);
    }

    char eucadir[EUCA_MAX_PATH] = "";
    snprintf(eucadir, EUCA_MAX_PATH, EUCALYPTUS_LOG_DIR, config->eucahome);
    if (check_directory(eucadir)) {
        LOGFATAL("cannot locate eucalyptus installation (log dir): make sure EUCALYPTUS env is set\n");
        enm_exit(1, NULL);
    }

    snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/eucanetd_meter_%s.pid", config->eucahome, config->device);
    FH = fopen(pidfile, "w");
    if (FH) {
        fprintf(FH, "%d\n", pid);
        fclose(FH);
    } else {
        fprintf(stderr, "could not open pidfile for write (%s)\n", pidfile);
        enm_exit(1, NULL);
    }

    close(STDIN_FILENO);
    close(STDOUT_FILENO);
    close(STDERR_FILENO);

    return (0);
}

/**
 * Removes the leading and trailing spaces of the string str. Input string str is
 * modified by this function.
 * @param str [in] string of interest.
 * @return 0 on success. 1 on failure.
 */
static int enm_trim(char *str) {
    if (!str || !strlen(str)) {
        return (1);
    }
    char *tmp = str;
    for (int i = 0; i < strlen(str); i++) {
        if (isspace(str[i])) {
            tmp++;
        } else {
            break;
        }
    }
    if (strlen(tmp)) {
        tmp = strdup(tmp);
        for (int i = (strlen(tmp) - 1); i > 0; i--) {
            if (isspace(tmp[i])) {
                tmp[i] = '\0';
            } else {
                break;
            }
        }
        euca_strncpy(str, tmp, strlen(tmp) + 1);
        EUCA_FREE(tmp);
    } else {
        str[0] = '\0';
    }
    return (0);
}