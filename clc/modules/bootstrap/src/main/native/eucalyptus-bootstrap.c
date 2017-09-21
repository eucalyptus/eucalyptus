/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2001-2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *   or implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/

#include "eucalyptus-bootstrap.h"

#include <errno.h>
#include <time.h>
#include <string.h>
#include <dlfcn.h>
#include <signal.h>
#include <libgen.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/dir.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <stdio.h>
#include <pwd.h>
#include <grp.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <limits.h>
#include <stdlib.h>
#include <sys/time.h>
#include <sys/resource.h>

#define _LINUX_FS_H

// Helper function for logging and debugging. Helps track process tree
static pid_t wrapped_fork() {
  pid_t p = fork();
  if(p != 0) __debug("Forked child %d", p);
  return p;
}



extern char **environ;
pid_t child_pid = 0;
char *java_library(euca_opts *, java_home_t *);
int java_init(euca_opts *, java_home_t *);
int JVM_destroy(int);

static void java_fail(void)
{
    exit(1);
}

static java_home_t *get_java_home(char *path)
{
    java_home_t *data = NULL;
    char buf[1024];
    int x = -1, k = 0, i = 0;

    if (path == NULL)
        return NULL;
    __debug("Looking for libjvm in %s", path);
    __abort(NULL, (CHECK_ISDIR(path) == 0), "Path %s is not a directory", path);
    data = (java_home_t *) malloc(sizeof(java_home_t));
    data->path = strdup(path);
    data->jvms = NULL;
    data->jnum = 0;
    while (libjvm_paths[++x] != NULL) {
        if ((k = snprintf(buf, 1024, libjvm_paths[x], path)) <= 0) {
            __error("Error mangling jvm path");
            free(data->path);
            free(data);
            return NULL;
        }

        __debug("Attempting to locate VM library %s", buf);
        if (CHECK_ISREG(buf) == 1) {
            data->jvms = (jvm_info_t **) malloc(2 * sizeof(jvm_info_t *));
            data->jvms[i] = (jvm_info_t *) malloc(sizeof(jvm_info_t));
            data->jvms[i]->libjvm_path = strdup(buf);
            char *dir, *base;
            dir = dirname(buf);
            base = basename(dir);
            data->jvms[i]->name = base;
            data->jvms[++i] = NULL;
            data->jnum = i;
            return data;
        }
    }
    return data;
}

static void handler(int sig)
{
    switch (sig) {
    case SIGTERM:
        __debug("Caught SIGTERM: Scheduling a shutdown");
        if (stopping == 1)
            __error("Shutdown or reload already scheduled");
        else
            stopping = 1;
        break;
    case SIGINT:
        __debug("Caught SIGINT: Scheduling a shutdown");
        if (stopping == 1)
            __error("Shutdown or reload already scheduled");
        else
            stopping = 1;
        break;
    case SIGHUP:
        __debug("Caught SIGHUP: Scheduling a reload");
        if (stopping == 1)
            __error("Shutdown or reload already scheduled");
        else
            stopping = 1, doreload = 1;
        break;
    default:
        __debug("Caught unknown signal %d", sig);
        break;
    }
}

static int set_user_group(char *user, int uid, int gid)
{
    __abort(0, user == NULL, "No user to setuid to.");
    __abort(-1, setgid(gid) != 0, "Cannot set group id for user '%s'", user);
    if (initgroups(user, gid) != 0)
        __abort(-1, getuid() != uid, "Cannot set supplement group list for user '%s'", user);
    __abort(-1, setuid(uid) != 0, "Cannot set user id for user '%s'", user);
    return 0;
}

static int set_caps(int caps)
{
    struct __user_cap_header_struct caphead;
    struct __user_cap_data_struct cap;

    memset(&caphead, 0, sizeof caphead);
    caphead.version = _LINUX_CAPABILITY_VERSION;
    caphead.pid = 0;
    memset(&cap, 0, sizeof cap);
    cap.effective = caps;
    cap.permitted = caps;
    cap.inheritable = caps;
    if (syscall(__NR_capset, &caphead, &cap) < 0) {
        __error("syscall failed in set_caps");
        return -1;
    }
    return 0;
}

static int set_process_options( void )
{
    return (prctl( PR_SET_PDEATHSIG, SIGKILL ));
}

static int set_keys_ownership(char *home, int uid, int gid)
{
    char filename[2048];
    int rc;

    snprintf(filename, 2047, "%s/var/lib/eucalyptus/keys/euca.p12", home);
    rc = chown(filename, uid, gid);

    return (0);
}

static int linuxset_user_group(char *user, int uid, int gid)
{
    if (set_caps(CAPS) != 0)
        __abort(-1, getuid() != uid, "set_caps(CAPS) failed");
    __abort(-1, (prctl(PR_SET_KEEPCAPS, 1, 0, 0, 0) < 0), "prctl failed in linuxset_user_group");
    __abort(-1, (set_user_group(user, uid, gid) != 0), "set_user_group failed in linuxset_user_group");
    if (set_caps(CAPSMIN) != 0)
        __abort(-1, (getuid() != uid), "set_caps(CAPSMIN) failed");
    return 0;
}

static int checkuser(char *user, uid_t * uid, gid_t * gid)
{
    struct passwd *pwds = NULL;
    int status = 0;
    pid_t pid = 0;
    __abort(1, user == NULL, "");
    pwds = getpwnam(user);
    __abort(0, (pwds == NULL), "Invalid user name '%s' specified", user);
    *uid = pwds->pw_uid;
    *gid = pwds->pw_gid;
    pid = wrapped_fork();
    __abort(0, (pid == -1), "Cannot validate user name");
    if (pid == 0) {
        __die(set_user_group(user, *uid, *gid) != 0, "set_user_group failed.");
        exit(0);
    }
    while (waitpid(pid, &status, 0) != pid) ;
    if (WIFEXITED(status)) {
        status = WEXITSTATUS(status);
        __abort(0, (status != 0), "User '%s' validated", user);
    }
    return 1;
}

static void controller(int sig)
{
    switch (sig) {
    case SIGTERM:
    case SIGINT:
    case SIGHUP:
        __debug("Forwarding signal %d to process %d", sig, child_pid);
        kill(child_pid, sig);
        signal(sig, controller);
        break;
    default:
        __debug("Caught unknown signal %d", sig);
        break;
    }
}

static void *signal_set(int sig, void *newHandler)
{
    void *hand = signal(sig, newHandler);
    if (hand == SIG_ERR)
        hand = NULL;
    if (hand == handler || hand == controller)
        hand = NULL;
    return hand;
}

static int __get_pid(char *pidpath)
{
    FILE *pidfile;
    int pid = -1;

    if ((pidfile = fopen(pidpath, "r")) != NULL) {
        if (fscanf(pidfile, "%d", &pid) != 1) {
            __error("Failed to read pid file: %s", pidpath);
        }
        fclose(pidfile);
        if (pid > 0 && (kill(pid,0) != 0)) {
            __error("PID: %d is nonexistant",pid);
            pid = -1;
        }
    }
    __debug("pid return: %d", pid);
    return pid;
}

static int __write_pid(char *pidpath)
{
    FILE *pidfile;
    __abort(0, __get_pid(pidpath) > 0, "");
    __abort(-1, (pidfile = fopen(pidpath, "w")) == NULL, "");
    fprintf(pidfile, "%d\n", (int)getpid()), fflush(pidfile), fclose(pidfile);
    return 0;
}

static int wait_child(euca_opts * args, int pid)
{
    time_t timer = 0;
    int rc = 0, status;
    get_timestamp(ts_buff,sizeof(ts_buff));
    fprintf(stderr, "%s INFO Waiting for process to respond to signal.",ts_buff);
    while (rc <= 0 && timer < 5) {
        usleep(1000000), fprintf(stderr, "."), fflush(stderr);
        if ((rc = waitpid(pid, &status, WNOHANG)) == -1) {
            __debug("Error waiting for child: pid=%d waitpid=%d status=%d", pid, rc, status);
            return errno != ECHILD;
        } else if (rc == pid) {
            if (WIFEXITED(status)) {
                __debug("command terminated with exit status pid=%d waitpid=%d status=%d", pid, rc, WEXITSTATUS(status));
                return WEXITSTATUS(status);
            } else if (WIFSIGNALED(status)) {
                __debug("command terminated by signal pid=%d waitpid=%d status=%d", pid, rc, WTERMSIG(status));
                return WTERMSIG(status);
            } else {
                __debug("command terminated pid=%d waitpid=%d status=%d", pid, rc, status);
                return status;
            }
        } else {
            timer++;
            continue;
        }
    }
    if (rc == 0 && status == 0) {
        fprintf(stderr, "\n");
        return 0;
    } else {
        __error("Failed to signal child process pid=%d waitpid=%d status=%d", pid, rc, status);
        return 1;
    }
}

static int stop_child(euca_opts * args)
{
    int pid = __get_pid(GETARG(args, pidfile)), rc = 0, i;
    if (pid <= 0)
        return -1;
    get_timestamp(ts_buff,sizeof(ts_buff));
    fprintf(stderr, "%s INFO Waiting for process to terminate: pid=%d .",ts_buff, pid);
    for (i = 0; i < 60 && (rc = kill(pid, SIGTERM)) != -1; i++) {
        usleep(1000 * 1000), fprintf(stderr, "."), fflush(stderr);
    }
    if (i == 0) {
        i = errno;
        __die(rc == -1 && i == ESRCH, "No process with the specified pid=%d", pid);
        __die(rc == -1 && i == EPERM, "Do not have permission to kill pid=%d", pid);
    } else if (i == 60) {
        __debug("Forcefully terminating hung process.");
        kill(pid, SIGKILL);
    } else {
        get_timestamp(ts_buff,sizeof(ts_buff));
        fprintf(stderr, "\n%s INFO Terminated process with pid=%d\n",ts_buff, pid);
    }
    return 0;
}

static int get_timestamp(char *buff, int buff_len)
{
    time_t t = time(NULL);
    struct tm tm = { 0 };
    localtime_r(&t,&tm);
    return (strftime(buff, buff_len, "%F %T", &tm));
}

static int __update_limit(int resource, long long value)
{
    struct rlimit r;
    __abort(1, getrlimit(resource, &r) == -1, "Failed to get rlimit for resource=%d", resource);
    get_timestamp(ts_buff,sizeof(ts_buff));
    printf("%s INFO ulimit: resource=%d soft=%lld hard=%lld\n",ts_buff,resource, (long long)r.rlim_cur, (long long)r.rlim_max);
    fflush(stdout);
    if (r.rlim_cur == RLIM_INFINITY || r.rlim_cur < value) {
        r.rlim_cur = value;
        r.rlim_max = value;
        __abort(1, setrlimit(resource, &r) == -1, "Failed to set rlimit for resource=%d", resource);
        __abort(1, getrlimit(resource, &r) == -1, "Failed to set rlimit for resource=%d", resource);
        printf("%s INFO ulimit: resource=%d soft=%lld hard=%lld\n",ts_buff, resource, (long long)r.rlim_cur, (long long)r.rlim_max);
        fflush(stdout);
    }
    return 0;
}

static void __limits(int fileno_limit)
{
    __update_limit(RLIMIT_NOFILE, fileno_limit);
    __update_limit(RLIMIT_NPROC, LIMIT_NPROC);
}

static int child(euca_opts * args, java_home_t * data, uid_t uid, gid_t gid)
{
    int ret = 0;
    jboolean r = 0;
    __write_pid(GETARG(args, pidfile));
    setpgrp();
    __limits(GETARG(args,fdlimit));
    __die(set_process_options() != 0, "Setting process options failed.");
    __die(java_init(args, data) != 1, "Failed to initialize Eucalyptus.");
    __die_jni((r = (*env)->CallBooleanMethod(env, bootstrap.instance, bootstrap.init)) == 0, "Failed to init Eucalyptus.");
    __abort(4, set_keys_ownership(GETARG(args, home), uid, gid) != 0, "Setting ownership of keyfile failed.");
    __abort(4, linuxset_user_group(GETARG(args, user), uid, gid) != 0, "Setting the user failed.");
    __abort(4, set_process_options() != 0, "Setting process options failed.");
    __die_jni((r = (*env)->CallBooleanMethod(env, bootstrap.instance, bootstrap.load)) == 0, "Failed to load Eucalyptus.");
    __die_jni((r = (*env)->CallBooleanMethod(env, bootstrap.instance, bootstrap.start)) == 0, "Failed to start Eucalyptus.");
    handle._hup = signal_set(SIGHUP, handler);
    handle._term = signal_set(SIGTERM, handler);
    handle._int = signal_set(SIGINT, handler);
    child_pid = getpid();
    __debug("Waiting for a signal to be delivered");
    while (!stopping)
        sleep(60);
    __debug("Shutdown or reload requested: exiting");
    __die_jni((r = (*env)->CallBooleanMethod(env, bootstrap.instance, bootstrap.stop)) == 0, "Failed to stop Eucalyptus.");
    if (doreload == 1)
        ret = EUCA_RET_RELOAD;
    else
        ret = 0;
    __die_jni((r = (*env)->CallBooleanMethod(env, bootstrap.instance, bootstrap.destroy)) == 0, "Failed to destroy Eucalyptus.");
    __die((JVM_destroy(ret) != 1), "Failed trying to destroy JVM... bailing out seems like the right thing to do");
    return ret;
}

static FILE *loc_freopen(char *outfile, char *mode, FILE * stream)
{
    FILE *ftest;
    __abort(stream, (ftest = fopen(outfile, mode)) == NULL, "Unable to redirect to %s\n", outfile);
    fclose(ftest);
    return freopen(outfile, mode, stream);
}

static void set_output(char *outfile, char *errfile)
{
    if (freopen("/dev/null", "r", stdin)) ; //hack
    __debug("redirecting stdout to %s and stderr to %s", outfile, errfile);
    if (debug == 1 && strcmp(errfile, "/dev/null") == 0)
        return;
    if (strcmp(outfile, "&2") == 0 && strcmp(errfile, "&1") == 0)
        outfile = "/dev/null";
    if (strcmp(outfile, "&1") != 0)
        loc_freopen(outfile, "a", stdout);
    if (strcmp(errfile, "&2") != 0)
        loc_freopen(errfile, "a", stderr);
    else {
        close(2);
        if (dup(1)) ;           //hack
    }
    if (strcmp(outfile, "&2") == 0) {
        close(1);
        if (dup(2)) ;           //hack
    }
}

int main(int argc, char *argv[])
{
    euca_opts euca_args;
    euca_opts *args = &euca_args;
    java_home_t *data = NULL;
    int status = 0;
    pid_t pid = 0;
    uid_t uid = 0;
    gid_t gid = 0;
    int i;
    if (arguments(argc, argv, args) != 0)
        exit(1);
    if (args->extra_version_orig != NULL)
        args->extra_version_arg = args->extra_version_orig;
    debug = args->debug_flag;
    set_output(GETARG(args, out), GETARG(args, err));
    if (args->kill_flag == 1)
        return stop_child(args);
    if (checkuser(GETARG(args, user), &uid, &gid) == 0)
        return 1;
    for (i = 0; i < args->java_home_given; ++i) {
        __debug("Trying user supplied java home: %s", args->java_home_arg[i]);
        data = get_java_home(args->java_home_arg[i]);
        if (data != NULL) {
            break;
        }
    }
    char *java_home_env = getenv("JAVA_HOME");
    if (data == NULL && java_home_env != NULL) {
        __debug("Trying environment JAVA_HOME: %s", java_home_env);
        data = get_java_home(java_home_env);
    }
    if (data == NULL && !args->java_home_given && args->java_home_arg[0] != NULL && CHECK_ISDIR(args->java_home_arg[0])) {
        __debug("Trying built-in java home: %s", args->java_home_arg[0]);
        data = get_java_home(args->java_home_arg[0]);
    }
    if (data == NULL && CHECK_ISREG("/usr/bin/java")) {
        char *javapath = (char *)calloc(PATH_MAX, sizeof(char));
        javapath = realpath("/usr/bin/java", javapath);
        if (javapath != NULL) {
            javapath[strlen(javapath) - strlen("jre/bin/java")] = '\0';
            __debug("Trying system java home: %s", javapath);
            data = get_java_home(javapath);
        }
    }
    if (data == NULL) {
        __error("Cannot locate Java Home");
        return 1;
    }
    int x;
    if (debug == 1) {
        __debug("+-- DUMPING JAVA HOME STRUCTURE ------------------------");
        __debug("| Java Home:       \"%s\"", PRINT_NULL(data->path));
        __debug("| Found JVMs:      %d", data->jnum);
        for (x = 0; x < data->jnum; x++) {
            jvm_info_t *jvm = data->jvms[x];
            __debug("| JVM Name:        \"%s\"", PRINT_NULL(jvm->name));
            __debug("|                  \"%s\"", PRINT_NULL(jvm->libjvm_path));
        }
        __debug("+-------------------------------------------------------");
    }
    if (strcmp(argv[0], "eucalyptus-cloud") != 0) {
        char *oldpath = getenv("LD_LIBRARY_PATH"), *libf = java_library(args, data);
        char *old = argv[0], buf[32768], *tmp = NULL, *p1 = NULL, *p2 = NULL;
        p1 = strdup(libf);
        tmp = strrchr(p1, '/');
        if (tmp != NULL)
            tmp[0] = '\0';
        p2 = strdup(p1);
        tmp = strrchr(p2, '/');
        if (tmp != NULL)
            tmp[0] = '\0';
        if (oldpath == NULL)
            snprintf(buf, 32768, "%s:%s:%s/bin/linux-x64", p1, p2, GETARG(args, profiler_home));
        else
            snprintf(buf, 32768, "%s:%s:%s:%s/bin/linux-x64", oldpath, p1, p2, GETARG(args, profiler_home));
        tmp = strdup(buf);

        setenv("LD_LIBRARY_PATH", tmp, 1);
        __debug("Invoking w/ LD_LIBRARY_PATH=%s", getenv("LD_LIBRARY_PATH"));
        argv[0] = "eucalyptus-cloud";
        execve(old, argv, environ);
        __error("Cannot execute process");
        return 1;
    }
    __debug("Running w/ LD_LIBRARY_PATH=%s", getenv("LD_LIBRARY_PATH"));
    if (args->fork_flag) {
        pid = wrapped_fork();
        __die((pid == -1), "Cannot detach from parent process");
        if (pid != 0)
            return wait_child(args, pid);
        setsid();
    }
    while ((pid = wrapped_fork()) != -1) {
        if (pid == 0)
            exit(child(args, data, uid, gid));
        child_pid = pid;
        signal(SIGHUP, controller);
        signal(SIGTERM, controller);
        signal(SIGINT, controller);

        while (waitpid(-1, &status, 0) != pid) ;
        if (WIFEXITED(status)) {
            status = WEXITSTATUS(status);
            __debug("Eucalyptus exited with status: %d", status);
            unlink(GETARG(args, pidfile));
            if (status == EUCA_RET_RELOAD) {
                __info("Reloading service.");
                continue;
            } else if (status == 0) {
                __info("Service shut down cleanly.");
                return 0;
            }
            __error("Service exit with a return value of %d.", status);
            return 1;
        } else {
            perror("Service did not exit cleanly.");
            __error("Service did not exit cleanly: exit value=%d.", status);
            return 1;
        }
    }
    __error("Cannot decouple controller/child processes");
    return 1;
}

int ld_library_path_set = 0;
typedef void *dso_handle;

int dso_unlink(dso_handle libr)
{
    if (dlclose(libr) == 0)
        return 1;
    else
        return 0;
}

char *dso_error(void)
{
    return (dlerror());
}

static void hello(JNIEnv * env, jobject source)
{
    __error("uid=%d,euid=%d,gid=%d,egid=%d", getuid(), geteuid(), getgid(), getegid());
}

static void shutdown(JNIEnv * env, jobject source, jboolean reload)
{
    __debug("Shutdown requested (reload is %d)", reload);
    if (reload == 1)
        __debug("Killing self with HUP signal: %d", kill(child_pid, SIGHUP));
    else
        __debug("Killing self with TERM signal: %d", kill(child_pid, SIGTERM));
}

char *java_library(euca_opts * args, java_home_t * data)
{
    char *libjvm_path = NULL;
    int x;
    if (data->jnum == 0) {
        __error("Cannot find any VM in Java Home %s", data->path);
        exit(1);
    }
    if (args->jvm_name_given == 0) {
        libjvm_path = data->jvms[0]->libjvm_path;
    } else {
        for (x = 0; x < data->jnum; x++) {
            if (data->jvms[x]->name == NULL)
                continue;
            if (strcmp(GETARG(args, jvm_name), data->jvms[x]->name) == 0) {
                libjvm_path = data->jvms[x]->libjvm_path;
                break;
            }
        }
    }
    if (libjvm_path == NULL) {
        __error("Failed to locate usable JVM %s %s", GETARG(args, jvm_name), libjvm_path);
        exit(1);
    }
    __debug("Using libjvm.so in %s", libjvm_path);
    return libjvm_path;
}

void euca_load_bootstrapper(void)
{
    __die((bootstrap.class_name = ((*env)->NewStringUTF(env, EUCA_MAIN)))
          == NULL, "Cannot create string for class name.");
    __die((bootstrap.clazz = ((*env)->FindClass(env, EUCA_MAIN))) == NULL, "Cannot find Eucalyptus bootstrapper: %s.", EUCA_MAIN);
    __debug("Found Eucalyptus bootstrapper: %s", EUCA_MAIN);
    __die((bootstrap.class_ref = (*env)->NewGlobalRef(env, bootstrap.clazz))
          == NULL, "Cannot create global ref for %s.", EUCA_MAIN);

    JNINativeMethod shutdown_method = { "shutdown", "(Z)V", shutdown };
    __die((*env)->RegisterNatives(env, bootstrap.clazz, &shutdown_method, 1)
          != 0, "Cannot register native method: shutdown.");
    JNINativeMethod hello_method = { "hello", "()V", hello };
    __die((*env)->RegisterNatives(env, bootstrap.clazz, &hello_method, 1) != 0, "Cannot register native method: hello.");
    __debug("Native methods registered.");

    __die((bootstrap.constructor = (*env)->GetStaticMethodID(env,
                                                             bootstrap.clazz, euca_get_instance.method_name,
                                                             euca_get_instance.method_signature)) == NULL,
          "Failed to get reference to default constructor.");
    __die_jni((bootstrap.instance =
               (*env)->CallStaticObjectMethod(env, bootstrap.clazz, bootstrap.constructor)) == NULL, "Failed to create instance of bootstrapper.");
    __debug("Created bootstrapper instance.");  //TODO: fix all these error messages..
    __die((bootstrap.init =
           (*env)->GetMethodID(env, bootstrap.clazz, euca_init.method_name, euca_init.method_signature)) == NULL,
          "Failed to get method reference for load.");
    __debug("-> bound method: init");
    __die((bootstrap.load =
           (*env)->GetMethodID(env, bootstrap.clazz, euca_load.method_name, euca_load.method_signature)) == NULL,
          "Failed to get method reference for load.");
    __debug("-> bound method: load");
    __die((bootstrap.start =
           (*env)->GetMethodID(env, bootstrap.clazz, euca_start.method_name, euca_start.method_signature)) == NULL,
          "Failed to get method reference for start.");
    __debug("-> bound method: start");
    __die((bootstrap.stop =
           (*env)->GetMethodID(env, bootstrap.clazz, euca_stop.method_name, euca_stop.method_signature)) == NULL,
          "Failed to get method reference for stop.");
    __debug("-> bound method: stop");
    __die((bootstrap.destroy =
           (*env)->GetMethodID(env, bootstrap.clazz, euca_destroy.method_name, euca_destroy.method_signature)) == NULL,
          "Failed to get method reference for destroy.");
    __debug("-> bound method: destroy");
    __die((bootstrap.check =
           (*env)->GetMethodID(env, bootstrap.clazz, euca_check.method_name, euca_check.method_signature)) == NULL,
          "Failed to get method reference for check.");
    __debug("-> bound method: check");
    __die((bootstrap.version =
           (*env)->GetMethodID(env, bootstrap.clazz, euca_version.method_name, euca_version.method_signature)) == NULL,
          "Failed to get method reference for version.");
    __debug("-> bound method: version");
}

char *java_library_path(euca_opts * args)
{
#define JAVA_PATH_LEN 65536
    char lib_dir[256], etc_dir[256], script_dir[256], *jar_list = (char *)malloc(JAVA_PATH_LEN * sizeof(char));
    __die((strlen(GETARG(args, home)) + strlen(EUCALYPTUS_JAVA_LIB_DIR) >= 256), "Directory path too long: " EUCALYPTUS_JAVA_LIB_DIR,
          GETARG(args, home));
    snprintf(lib_dir, 255, EUCALYPTUS_JAVA_LIB_DIR, GETARG(args, home));
    snprintf(etc_dir, 255, EUCALYPTUS_ETC_DIR, GETARG(args, home));
    snprintf(script_dir, 255, EUCALYPTUS_SCRIPT_DIR, GETARG(args, home));
    if (!CHECK_ISDIR(lib_dir))
        __die(1, "Can't find library directory %s", lib_dir);
    int wb = 0;
    wb += snprintf(jar_list + wb, JAVA_PATH_LEN - wb, "-Djava.class.path=%s:", etc_dir);
    wb += snprintf(jar_list + wb, JAVA_PATH_LEN - wb, "%s", script_dir);
    DIR *lib_dir_p = opendir(lib_dir);
    if (!lib_dir_p)
        __die(1, "Can't open library directory %s", lib_dir);
    struct direct *dir_ent;
    while ((dir_ent = readdir(lib_dir_p)) != 0) {
        if (strcmp(dir_ent->d_name, ".") != 0 && strcmp(dir_ent->d_name, "..")
            != 0 && strcmp(dir_ent->d_name, "openjdk-crypto.jar") != 0
            && strstr(dir_ent->d_name, "disabled") == NULL && (strstr(dir_ent->d_name, "eucalyptus-") != NULL)) {
            char jar[256];
            snprintf(jar, 255, "%s/%s", lib_dir, dir_ent->d_name);
            if ((CHECK_ISREG(jar) || CHECK_ISLNK(jar)))
                wb += snprintf(jar_list + wb, JAVA_PATH_LEN - wb, ":%s", jar);
        }
    }
    closedir(lib_dir_p);
    lib_dir_p = opendir(lib_dir);
    if (!lib_dir_p)
        __die(1, "Can't open library directory %s", lib_dir);
    while ((dir_ent = readdir(lib_dir_p)) != 0) {
        if (strcmp(dir_ent->d_name, ".") != 0 && strcmp(dir_ent->d_name, "..")
            != 0 && strcmp(dir_ent->d_name, "openjdk-crypto.jar") != 0
            && strstr(dir_ent->d_name, "disabled") == NULL && (strstr(dir_ent->d_name, "eucalyptus-") == NULL)) {
            char jar[256];
            snprintf(jar, 255, "%s/%s", lib_dir, dir_ent->d_name);
            if ((CHECK_ISREG(jar) || CHECK_ISLNK(jar)))
                wb += snprintf(jar_list + wb, JAVA_PATH_LEN - wb, ":%s", jar);
        }
    }
    closedir(lib_dir_p);
    return jar_list;
}

static size_t getMemoryTotalSize( )
{
    #if defined(_SC_PHYS_PAGES) && defined(_SC_PAGESIZE)
        return (size_t)sysconf( _SC_PHYS_PAGES ) *
                (size_t)sysconf( _SC_PAGESIZE );
    #else
        return 0L;
    #endif
}

int java_init(euca_opts * args, java_home_t * data)
{
    jint(*hotspot_main) (JavaVM **, JNIEnv **, JavaVMInitArgs *);
    char *libjvm_path = NULL;
    if ((libjvm_path = java_library(args, data)) == NULL)
        __fail("Cannot locate JVM library file");
    dso_handle libjvm_handle = NULL;
    __die((libjvm_handle = dlopen(libjvm_path, RTLD_GLOBAL | RTLD_NOW))
          == NULL, "Cannot dynamically link to %s\n%s", libjvm_path, dso_error());
    __debug("JVM library %s loaded", libjvm_path);
    if ((hotspot_main = dlsym(libjvm_handle, "JNI_CreateJavaVM")) == NULL)
        __fail("Cannot find JVM library entry point");
    JavaVMInitArgs arg;
    arg.ignoreUnrecognized = 0;
#if defined(JNI_VERSION_1_4)
    arg.version = JNI_VERSION_1_4;
#else
    arg.version = JNI_VERSION_1_2;
#endif
    JavaVMOption *opt = NULL;
    char *java_class_path = java_library_path(args);
    __debug("Using classpath:\n%s", java_class_path);
#define JVM_MAX_OPTS 128
    int x = -1, i = 0, encoding_not_defined_flag = 1;
    opt = (JavaVMOption *) malloc(JVM_MAX_OPTS * sizeof(JavaVMOption));
    for (i = 0; i < JVM_MAX_OPTS; i++)
        opt[i].extraInfo = NULL;
    i = -1;
    while (jvm_default_opts[++i] != NULL)
        JVM_ARG(opt[++x], jvm_default_opts[i], GETARG(args, home));

    // setting heap size to half of total memory if it was not set by user
    char *s, str[16], size_i;
    long size;
    str[0] = 0;
    for (i = 0; i < args->jvm_args_given; i++) {
      if ((s = strstr(args->jvm_args_arg[i], "mx"))
             && sscanf(s, "mx%s",  str) == 1) {
        break;
      }
    }
    if (str[0] != 0) {
        sscanf (str,"%ld%c", &size, &size_i);
        switch(size_i) {
            case 'k' :
            case 'K' :
                  size = size * 1024L;
                  break;
            case 'm' :
            case 'M' :
                  size = size * 1024L * 1024L;
                  break;
            case 'g' :
            case 'G' :
                  size = size * GIG;
                  break;
            default:
                  size = size * 1L;
        }
        __debug("User defined heap size is %ld bytes", size);
        if (size < GIG) {
           __error("User defined heap size of %ld bytes is set too low and system might now work properly", size);
        }
    } else {
        size_t total_memory = getMemoryTotalSize();
        double heap_size_gb = 2;
        if (total_memory <= 32 * GIG && total_memory >= 4 * GIG)
           heap_size_gb = ceil(total_memory/GIG/2.0);
        else if (total_memory > 32 * GIG)
           heap_size_gb = 16.0;
        __info("User did not set heap size. Setting it automatically to %.0lfGB base on total memory of %zu bytes", heap_size_gb, total_memory);
        JVM_ARG(opt[++x], "-Xms%.0lfg", heap_size_gb);
        JVM_ARG(opt[++x], "-Xmx%.0lfg", heap_size_gb);
    }

    if (args->exhaustive_flag) {
        JVM_ARG(opt[++x], "-Deuca.log.exhaustive=TRACE");
        JVM_ARG(opt[++x], "-Deuca.log.exhaustive.db=TRACE");
        JVM_ARG(opt[++x], "-Deuca.log.exhaustive.user=TRACE");
        JVM_ARG(opt[++x], "-Deuca.log.exhaustive.cc=TRACE");
        JVM_ARG(opt[++x], "-Deuca.log.exhaustive.external=TRACE");
    } else {
        int exhaust = 0;
        if (args->exhaustive_db_flag) {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.db=TRACE");
            exhaust++;
        } else {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.db=FATAL");
        }
        if (args->exhaustive_cc_flag) {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.cc=TRACE");
            exhaust++;
        } else {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.cc=FATAL");
        }
        if (args->exhaustive_user_flag) {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.user=TRACE");
            exhaust++;
        } else {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.user=FATAL");
        }
        if (args->exhaustive_external_flag) {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.external=TRACE");
            exhaust++;
        } else {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive.external=FATAL");
        }
        if (exhaust) {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive=TRACE");
        } else {
            JVM_ARG(opt[++x], "-Deuca.log.exhaustive=FATAL");
        }
    }
    JVM_ARG(opt[++x], "-Deuca.version=%1$s", ARGUMENTS_VERSION);
    JVM_ARG(opt[++x], "-Deuca.log.level=%1$s", GETARG(args, log_level));
    JVM_ARG(opt[++x], "-Deuca.log.appender=%1$s", GETARG(args, log_appender));
    if (strlen(GETARG(args, extra_version)) > 1 && strncmp(GETARG(args, extra_version), "@", 1)) {
        JVM_ARG(opt[++x], "-Deuca.extra_version=%1$s", GETARG(args, extra_version));
    }
    if (args->initialize_flag) {
        JVM_ARG(opt[++x], "-Deuca.initialize=true");
        JVM_ARG(opt[++x], "-Deuca.remote.dns=true");
    } else if (args->upgrade_flag) {
        JVM_ARG(opt[++x], "-Deuca.upgrade=true");
        if (args->upgrade_force_flag) {
            JVM_ARG(opt[++x], "-Deuca.upgrade.force=true");
        }
        JVM_ARG(opt[++x], "-Deuca.upgrade.old.dir=%1$s", GETARG(args, upgrade_old_dir));
        JVM_ARG(opt[++x], "-Deuca.upgrade.old.version=%1$s", GETARG(args, upgrade_old_version));
    } else {
        if (args->remote_dns_flag) {
            JVM_ARG(opt[++x], "-Deuca.remote.dns=true");
        }
        if (args->disable_iscsi_flag) {
            JVM_ARG(opt[++x], "-Deuca.disable.iscsi=true");
        }
    }
    if (args->force_remote_bootstrap_flag || args->disable_cloud_flag) {
        JVM_ARG(opt[++x], "-Deuca.force.remote.bootstrap=true");
    }

    if (args->debug_noha_flag) {
        JVM_ARG(opt[++x], "-Deuca.noha.cloud");
    }

    if (args->db_home_given) {
        JVM_ARG(opt[++x], "-Deuca.db.home=%s", GETARG(args, db_home));
    }

    JVM_ARG(opt[++x], "-XX:+HeapDumpOnOutOfMemoryError");
    JVM_ARG(opt[++x], "-XX:HeapDumpPath=%s/var/log/eucalyptus/", GETARG(args, home));
    if (args->debug_flag) {
        JVM_ARG(opt[++x], "-agentlib:jdwp=transport=dt_socket,server=y,suspend=%2$s,address=%1$d",
                GETARG(args, debug_port),
                (args->debug_suspend_flag ? "y" : "n"));
    }
    if (args->jmx_flag) {
        JVM_ARG(opt[++x], "-Dcom.sun.management.jmxremote");    //TODO:GRZE:wrapup jmx stuff here.
        JVM_ARG(opt[++x], "-Dcom.sun.management.jmxremote.port=8772");
        JVM_ARG(opt[++x], "-Dcom.sun.management.jmxremote.authenticate=false"); //TODO:GRZE:RELEASE FIXME to use ssl
        JVM_ARG(opt[++x], "-Dcom.sun.management.jmxremote.ssl=false");
    }
    if (args->verbose_flag) {
        JVM_ARG(opt[++x], "-verbose:gc");
        JVM_ARG(opt[++x], "-XX:+PrintGCTimeStamps");
        JVM_ARG(opt[++x], "-XX:+PrintGCDetails");
    }
    if (args->profile_flag && args->agentlib_given) {
        JVM_ARG(opt[++x], "-agentlib:%s", GETARG(args, agentlib));
    } else if (args->profile_flag) {
        JVM_ARG(opt[++x], "-agentlib:jprofilerti=port=8849");
        JVM_ARG(opt[++x], "-Xbootclasspath/a:%1$s/bin/agent.jar", GETARG(args, profiler_home));
    }
    JVM_ARG(opt[++x], "-Deuca.user=%s", GETARG(args, user));
    for (i = 0; i < args->jvm_args_given; i++)
        JVM_ARG(opt[++x], "-X%s", args->jvm_args_arg[i]);
    for (i = 0; i < args->define_given; i++) {
        JVM_ARG(opt[++x], "-D%s", args->define_arg[i]);
        if (strncmp(args->define_arg[i], "file.encoding", 13) == 0)
            encoding_not_defined_flag = 0;
    }
    for (i = 0; i < args->bootstrap_host_given; i++)
        JVM_ARG(opt[++x], "-Deuca.bootstrap.host.%d=%s", i, args->bootstrap_host_arg[i]);
    for (i = 0; i < args->bind_addr_given; i++)
        JVM_ARG(opt[++x], "-Deuca.bind.addr.%d=%s", i, args->bind_addr_arg[i]);
    if (args->mcast_addr_given) {
        JVM_ARG(opt[++x], "-Deuca.mcast.addr=%s", GETARG(args, mcast_addr));
    }

    if (encoding_not_defined_flag)
        JVM_ARG(opt[++x], "-Dfile.encoding=UTF-8");

    opt[++x].optionString = java_class_path;
    opt[x].extraInfo = NULL;
    opt[++x].optionString = "abort";
    opt[x].extraInfo = java_fail;

    arg.nOptions = x + 1;
    arg.options = opt;
    if (debug) {
        __debug("+-------------------------------------------------------");
        if (strlen(GETARG(args, extra_version)) > 1 && strncmp(GETARG(args, extra_version), "@", 1))
            __debug("| Eucalyptus version:            %s-%s", ARGUMENTS_VERSION, GETARG(args, extra_version));
        else
            __debug("| Eucalyptus version:            %s", ARGUMENTS_VERSION);

        __debug("| JNI version:                   %x", arg.version);
        __debug("| Ignore Unrecognized Arguments: %d", arg.ignoreUnrecognized);
        __debug("| Extra options:                 %d", arg.nOptions);
        for (x = 0; x < arg.nOptions; x++)
            __debug("|   \"%-80.80s\" (0x%p)", opt[x].optionString, opt[x].extraInfo);
        __debug("+-------------------------------------------------------");
    }
    __debug("Starting JVM.");
    jint ret = 0;
    while ((ret = (*hotspot_main) (&jvm, &env, &arg) == EUCA_RET_RELOAD)) ;
    __die(ret < 0, "Failed to create JVM");
    java_load_bootstrapper();
    dlclose(libjvm_handle);
    return 1;
}

int JVM_destroy(int code)
{
    jclass system;
    jmethodID method;
    __die((system = (*env)->FindClass(env, "java/lang/System")) == NULL, "Cannot find class java/lang/System.");
    __die((method = (*env)->GetStaticMethodID(env, system, "exit", "(I)V"))
          == NULL, "Cannot find \"System.exit(int)\" entry point.");
    __debug("Calling System.exit(%d)", code);
    (*env)->CallStaticVoidMethod(env, system, method, (jint) code);
    if ((*jvm)->DestroyJavaVM(jvm) != 0)
        return 0;
    __debug("JVM destroyed.");
    return 1;
}

void java_sleep(int wait)
{
    jclass clsThread;
    jmethodID method;
    __die(((clsThread = (*env)->FindClass(env, "java/lang/Thread")) == NULL), "Cannot find java/lang/Thread class");
    __die(((method = (*env)->GetStaticMethodID(env, clsThread, "sleep", "(J)V")) == NULL), "Cannot found the sleep entry point");
    (*env)->CallStaticVoidMethod(env, clsThread, method, (jlong) wait * 1000);
}
