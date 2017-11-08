/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file node/libvirt_nag.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU
#include <string.h>                    /* strlen, strcpy */
#include <time.h>
#include <limits.h>                    /* INT_MAX */
#include <sys/types.h>                 /* fork */
#include <sys/wait.h>                  /* waitpid */
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <errno.h>
#include <sys/stat.h>
#include <pthread.h>
#include <sys/vfs.h>                   /* statfs */
#include <signal.h>                    /* SIGINT */
#include <linux/limits.h>
#include <pwd.h>                       /* getpwuid_r */
#include <libvirt/libvirt.h>
#include <ipc.h>
#include <getopt.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define THREADS                                  11
#define MAX_DOMS                                 100
#define LIBVIRT_TIMEOUT_SEC                      5
#define DUMMY_DISK_FILE                          "libvirt_tortura-dummy-disk"
#define DUMMY_DISK_SIZE_BYTES                    10000000

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
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

virConnectPtr global_conn = NULL;
char uri[] = "qemu:///system";
sem *hyp_sem = NULL;


/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

volatile sig_atomic_t stop;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

void check_hypervisor_conn(void);
virConnectPtr lock_hypervisor_conn(void);
virConnectPtr unlock_hypervisor_conn(void);
static void *tortura_thread(void *);
static void *startup_thread(void *);
static void *destroy_thread(void *);

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


//!
//!
//!
//! @param[in] ptr
//!
//! @return Always returns NULL
//!
static void *tortura_thread(void *ptr)
{
    int iter = 0;
    long long tid = (*(long long *)ptr);

    while (!stop) {
        sleep(1);
        if (stop) continue;

        int error = 0;
        int num_doms = 0;
        int *dom_ids = NULL;
        virDomainPtr *doms = NULL;
        virDomainInfo *infos = NULL;
        virConnectPtr conn = NULL;
        LOGTRACE("tortura_thread %lld starting iteration %d\n", tid, iter);

        conn = lock_hypervisor_conn();
        
        num_doms = virConnectNumOfDomains(conn);
        dom_ids = malloc(sizeof(int) * num_doms);
        doms = malloc(sizeof(virDomainPtr) * num_doms);
        infos = malloc(sizeof(virDomainInfo) * num_doms);

        num_doms = virConnectListDomains(conn, dom_ids, num_doms);

        LOGDEBUG("[%lld][%d] found %d domains active\n", tid, iter++, num_doms);

        for (int i = 0; i < num_doms; i++) {
            if(stop) break;
            
            doms[i] = virDomainLookupByID(conn, dom_ids[i]);

            if (!doms[i]) {
                LOGDEBUG("failed to look up domain %d\n", dom_ids[i]);
                conn = unlock_hypervisor_conn();
                continue;
            }

            error = virDomainGetInfo(doms[i], &infos[i]);

            if (error) {
                LOGDEBUG("failed to get info on domain %d\n", dom_ids[i]);
                conn = unlock_hypervisor_conn();
                continue;
            }

            if (strstr(virDomainGetName(doms[i]), "tortura-") != NULL) {
                LOGTRACE("domain %s state is %d after query\n", virDomainGetName(doms[i]), infos[i].state);
            }

            error = virDomainFree(doms[i]);
            
            if (error) {
                LOGDEBUG("failed to close domain %d\n", dom_ids[i]);
                conn = unlock_hypervisor_conn();
                continue;
            }
        }

        conn = unlock_hypervisor_conn();

        free(doms);
        free(infos);
        free(dom_ids);
    }

    return (NULL);
}

//!
//!
//!
//! @param[in] arg
//!
//! @return Always returns NULL
//!
static void *startup_thread(void *arg)
{
    int iter = 0;
    virConnectPtr conn = lock_hypervisor_conn();

    srand((int)time(NULL));

    for (int i = 0; i < MAX_DOMS; i ++)
    {
        if (stop) break;

        int fd = 0;
        int error = 0;
        virDomainPtr dom = NULL;
        virDomainInfo info;
        char file_name[1024] = { 0 };
        char xml[4096] = { 0 };
        char *xml_template =
            "<?xml version='1.0' encoding='UTF-8'?>"
            "<domain type='kvm'>"
            "  <name>tortura-%04d</name>"
            "  <description>Eucalyptus instance i-XXX</description>"
            "  <os>"
            "    <type>hvm</type>"
            "  </os>"
            "  <features>"
            "    <acpi/>"
            "  </features>"
            "  <clock offset='localtime'/>"
            "  <on_poweroff>destroy</on_poweroff>"
            "  <on_reboot>restart</on_reboot>"
            "  <on_crash>destroy</on_crash>"
            "  <vcpu>1</vcpu>"
            "  <memory>52428</memory>"
            "  <devices>" 
            "    <disk device='disk'>" 
            "      <source file='%s'/>" 
            "      <target bus='virtio' dev='vda'/>" 
            "    </disk>" 
            "  </devices>" 
            "</domain>";
        LOGTRACE("startup_thread starting iteration %d\n", iter++);
        snprintf(file_name, sizeof(file_name), "%s/%s-%d", get_current_dir_name(), DUMMY_DISK_FILE, rand() + 1);

        umask(0000);
        if ((fd = open(file_name, O_RDWR | O_CREAT | O_TRUNC, 0666)) < 0) {
            LOGDEBUG("failed to create %s\n", DUMMY_DISK_FILE);
            perror("libvirt_tortura");
            exit(1);
        }

        if (lseek(fd, DUMMY_DISK_SIZE_BYTES, SEEK_SET) == ((off_t) - 1)) {
            LOGDEBUG("failed to seek in %s\n", DUMMY_DISK_FILE);
            perror("libvirt_tortura");
            exit(1);
        }

        if (write(fd, "x", 1) != 1) {
            LOGDEBUG("failed to write to %s\n", DUMMY_DISK_FILE);
            perror("libvirt_tortura");
            exit(1);
        }

        close(fd);

        sync();
        snprintf(xml, sizeof(xml), xml_template, rand() + 1, file_name);
            
        dom = virDomainCreateLinux(conn, xml, 0);

        if (dom == NULL) {
            LOGDEBUG("ERROR: failed to start domain\n");
            conn = unlock_hypervisor_conn();
            continue;
        }

        error = virDomainGetInfo(dom, &info);

        if (error) {
            LOGDEBUG("failed to get info on domain %s\n", virDomainGetName(dom));
            conn = unlock_hypervisor_conn();
            continue;
        }

        LOGINFO("domain %s state is %d after creation\n", virDomainGetName(dom), info.state);

        virDomainFree(dom);
        dom = NULL;

        unlink(file_name);
    }

    conn = unlock_hypervisor_conn();

    return (NULL);
}

//!
//!
//!
//! @param[in] ptr
//!
//! @return Always returns NULL
//!
static void *destroy_thread(void *ptr)
{
    int error = 0;
    int num_doms = 0;
    int *dom_ids = NULL;
    virDomainPtr *doms = NULL;
    virDomainInfo *infos = NULL;
    virConnectPtr conn = NULL;
    LOGDEBUG("destroy_thread starting\n");

    conn = lock_hypervisor_conn();
    
    num_doms = virConnectNumOfDomains(conn);
    dom_ids = malloc(sizeof(int) * num_doms);
    doms = malloc(sizeof(virDomainPtr) * num_doms);
    infos = malloc(sizeof(virDomainInfo) * num_doms);

    num_doms = virConnectListDomains(conn, dom_ids, num_doms);

    for (int i = 0; i < num_doms; i++) {
        doms[i] = virDomainLookupByID(conn, dom_ids[i]);

        if (!doms[i]) {
            LOGDEBUG("failed to look up domain %d\n", dom_ids[i]);
            conn = unlock_hypervisor_conn();
            continue;
        }

        error = virDomainGetInfo(doms[i], &infos[i]);

        if (error) {
            LOGDEBUG("failed to get info on domain %d\n", dom_ids[i]);
            conn = unlock_hypervisor_conn();
            continue;
        }

        if (strstr(virDomainGetName(doms[i]), "tortura-") != NULL) {
            LOGDEBUG("domain %s state is %d before destroy\n", virDomainGetName(doms[i]), infos[i].state);
            virDomainDestroy(doms[i]);
        }

        error = virDomainFree(doms[i]);
        
        if (error) {
            LOGDEBUG("failed to close domain %d\n", dom_ids[i]);
            conn = unlock_hypervisor_conn();
            continue;
        }
    }

    conn = unlock_hypervisor_conn();

    free(doms);
    free(infos);
    free(dom_ids);

    return (NULL);
}

void sigint_handler(int signum)
{
    stop = 1;
}

//!
//!
//!
//! @param[in] ptr
//!
static void *libvirt_thread(void *ptr)
{
    int rc = 0;
    sigset_t mask = { {0} };
    long long tid = (*(long long *)ptr);

    // allow SIGUSR1 signal to be delivered to this thread and its children
    sigemptyset(&mask);
    sigaddset(&mask, SIGUSR1);
    sigprocmask(SIG_UNBLOCK, &mask, NULL);

    if (global_conn) {
        if ((rc = virConnectClose(global_conn)) != 0) {
            LOGDEBUG("[%lld] refcount on close was non-zero: %d\n", tid, rc);
        }
    }
    global_conn = virConnectOpen(uri);

    return (NULL);
}

//!
//! Checks and reset the hypervisor connection.
//!
//! @return a pointer to the hypervisor connection structure or NULL if we failed.
//!
virConnectPtr lock_hypervisor_conn(void)
{
    int rc = 0;
    int status = 0;
    pid_t cpid = 0;
    pthread_t thread = { 0 };
    long long thread_par = 0L;
    boolean bail = FALSE;
    //boolean try_again = FALSE;
    struct timespec ts = { 0 };
    virConnectPtr tmp_conn = NULL;

    // Acquire our hypervisor semaphore
    sem_p(hyp_sem);

    // Fork off a process just to open and immediately close a libvirt connection.
    // The purpose is to try to identify periods when open or close calls block indefinitely.
    // Success in the child process does not guarantee success in the parent process, but
    // hopefully it will flag certain bad conditions and will allow the parent to avoid them.

    if ((cpid = fork()) < 0) {         // fork error
        LOGDEBUG("failed to fork to check hypervisor connection\n");
        bail = TRUE;                   // we are in big trouble if we cannot fork
    } else if (cpid == 0) {            // child process - checks on the connection
        if ((tmp_conn = virConnectOpen(uri)) == NULL)
            exit(1);
        virConnectClose(tmp_conn);
        exit(0);
    } else {                           // parent process - waits for the child, kills it if necessary
        if ((rc = timewait(cpid, &status, LIBVIRT_TIMEOUT_SEC)) < 0) {
            LOGDEBUG("failed to wait for forked process: %s\n", strerror(errno));
            bail = TRUE;
        } else if (rc == 0) {
            LOGDEBUG("timed out waiting for hypervisor checker pid=%d\n", cpid);
            bail = TRUE;
        } else if (WEXITSTATUS(status) != 0) {
            LOGDEBUG("child process failed to connect to hypervisor\n");
            bail = TRUE;
        }
        // terminate the child, if any
        killwait(cpid);
    }

    if (bail) {
        sem_v(hyp_sem);
        return NULL;                   // better fail the operation than block the whole NC
    }

    LOGTRACE("process check for libvirt succeeded\n");

    // At this point, the check for libvirt done in a separate process was
    // successful, so we proceed to close and reopen the connection in a
    // separate thread, which we will try to wake up with SIGUSR1 if it
    // blocks for too long (as a last-resource effort). The reason we reset
    // the connection so often is because libvirt operations have a
    // tendency to block indefinitely if we do not do this.

    if (pthread_create(&thread, NULL, libvirt_thread, (void *)&thread_par) != 0) {
        LOGDEBUG("failed to create the libvirt refreshing thread\n");
        bail = TRUE;
    } else {
        for (;;) {
            if (clock_gettime(CLOCK_REALTIME, &ts) == -1) {
                LOGDEBUG("failed to obtain time\n");
                bail = TRUE;
                break;
            }

            ts.tv_sec += LIBVIRT_TIMEOUT_SEC;
            if ((rc = pthread_timedjoin_np(thread, NULL, &ts)) == 0)
                break;                 // all is well

            if (rc != ETIMEDOUT) {     // error other than timeout
                LOGDEBUG("failed to wait for libvirt refreshing thread (rc=%d)\n", rc);
                bail = TRUE;
                break;
            }

            LOGDEBUG("timed out on libvirt refreshing thread\n");
            pthread_kill(thread, SIGUSR1);
            sleep(1);
        }
    }

    if (bail) {
        sem_v(hyp_sem);
        return NULL;
    }
    LOGTRACE("thread check for libvirt succeeded\n");

    if (global_conn == NULL) {
        LOGDEBUG("failed to connect to %s\n", uri);
        sem_v(hyp_sem);
        return NULL;
    }
    return global_conn;
}

//!
//! Closes the connection with the hypervisor
//!
virConnectPtr unlock_hypervisor_conn(void)
{
    sem_v(hyp_sem);

    LOGTRACE("unlocking hypervisor semaphore\n");

    return NULL;
}


int run_forked(void)
{
    int j = 0;
    int thread_par_sum = 0;
    long long thread_par[THREADS] = { 0 };
    pthread_t threads[THREADS] = { 0 };

    LOGDEBUG("spawning %d competing threads\n", THREADS);

    for (j = 0; j < THREADS; j++) {
        thread_par[j] = j;
        if (j == 0) {
            pthread_create(&threads[j], NULL, startup_thread, (void *)&thread_par[j]);
        } else {
            pthread_create(&threads[j], NULL, tortura_thread, (void *)&thread_par[j]);
        }
    }

    for (j = 0; j < THREADS; j++) {
        pthread_join(threads[j], NULL);
        thread_par_sum += (int)thread_par[j];
    }

    LOGDEBUG("waited for all competing threads\n");

    pthread_t destroy_id = {0};
    pthread_create(&destroy_id, NULL, destroy_thread, NULL);

    LOGDEBUG("waiting for destroy thread\n");    

    pthread_join(destroy_id, NULL);

    return thread_par_sum;
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    int thread_par_sum = 0;
    hyp_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
    signal(SIGINT, &sigint_handler);

    log_file_set(NULL, NULL);
    log_params_set(EUCA_LOG_DEBUG, 0, 1);

    while(!stop){
        thread_par_sum  = run_forked();
    }

    return (0);
}
