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

#ifndef __EUCALYPTUS_BOOTSTRAP_H__
#define __EUCALYPTUS_BOOTSTRAP_H__
#define __USE_GNU
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <linux/capability.h>
#include "eucalyptus-opts.h"

#define PRINT_NULL(x) ((x) == NULL ? "null" : (x))
#define LIMIT_FILENO 65535
#define LIMIT_NPROC RLIM_INFINITY
#define CAPS \
				(1 << CAP_SETUID)+ \
                (1 << CAP_SETGID)+ \
                (1 << CAP_NET_BIND_SERVICE)+ \
                (1 << CAP_NET_RAW)+ \
                (1 << CAP_SYS_RESOURCE)
#define CAPSMIN \
				(1 << CAP_NET_BIND_SERVICE)+ \
                (1 << CAP_NET_RAW)+ \
                (1 << CAP_SYS_RESOURCE)

typedef struct eucalyptus_opts euca_opts;
typedef struct {
    char *name;
    char *libjvm_path;
} jvm_info_t;
typedef struct {
    char *path;
    jvm_info_t **jvms;
    int jnum;
} java_home_t;
#define GETARG(a,x) (a->x##_arg)
static int debug = 0;
#define checkE \
  if (env->ExceptionOccurred() != 0) { \
  fprintf(stderr, "Unexpected exception "); \
  env->ExceptionDescribe(); env->ExceptionClear(); }

#define __die_jni(condition,format,...) do { \
	if(condition) {\
		fprintf(stderr,"[error:%04d] ", __LINE__);\
		fprintf(stderr, format "\n", ##__VA_ARGS__ ); \
		if((*env)->ExceptionCheck(env) != 0){ \
			(*env)->ExceptionDescribe(env); \
			(*env)->ExceptionClear(env); \
		}\
		exit(1);} \
	} while(0)
#define __die(condition,format,...) do { if(condition) {fprintf(stderr,"[error:%04d] ", __LINE__);fprintf(stderr, format "\n", ##__VA_ARGS__ ); exit(1);} } while(0)
#define __fail(format,...) __die(1,format,##__VA_ARGS__)
#define __abort(r,condition,format,...) do { if(condition) {fprintf(stderr,"[error:%04d] ", __LINE__);fprintf(stderr, format "\n", ##__VA_ARGS__ ); fflush(stderr); return r;} } while(0)
#define __debug(format,...) do { if(debug){fprintf(stdout,"[debug:%04d] ", __LINE__);fprintf(stdout, format "\n", ##__VA_ARGS__ );fflush(stdout); } } while(0)
#define __error(format,...) do { fprintf(stderr,"[error:%04d] ", __LINE__);fprintf(stderr, format "\n", ##__VA_ARGS__ );fflush(stderr); } while(0)
#define EUCA_MAIN "com/eucalyptus/bootstrap/SystemBootstrapper"
#define EUCA_RET_RELOAD 123

#ifndef LIBDIR
#define LIBDIR "/usr/lib"
#endif
#ifndef SYSCONFDIR
#define SYSCONFDIR "/etc"
#endif
#ifndef DATADIR
#define DATADIR	"/usr/share"
#endif
#ifndef LIBEXECDIR
#define LIBEXECDIR	"/usr/lib"
#endif
#ifndef SBINDIR
#define SBINDIR	"/usr/sbin"
#endif
#ifndef LOCALSTATEDIR
#define LOCALSTATEDIR	"/var"
#endif
#define EUCALYPTUS_DATA_DIR        "%s" DATADIR "/eucalyptus"
#define EUCALYPTUS_CONF_DIR        "%s" SYSCONFDIR "/eucalyptus"
#define EUCALYPTUS_LIB_DIR         "%s" LIBDIR "/eucalyptus"
#define EUCALYPTUS_LIBEXEC_DIR     "%s" LIBEXECDIR "/eucalyptus"
#define EUCALYPTUS_RUN_DIR         "%s" LOCALSTATEDIR "/run/eucalyptus"
#define EUCALYPTUS_STATE_DIR       "%s" LOCALSTATEDIR "/lib/eucalyptus"
#define EUCALYPTUS_LOG_DIR         "%s" LOCALSTATEDIR "/log/eucalyptus"
#define EUCALYPTUS_ETC_DIR         EUCALYPTUS_CONF_DIR "/cloud.d"
#define EUCALYPTUS_SCRIPT_DIR      EUCALYPTUS_ETC_DIR "/scripts"
#define EUCALYPTUS_JAVA_LIB_DIR    EUCALYPTUS_DATA_DIR
#define EUCALYPTUS_CLASSCACHE_DIR  EUCALYPTUS_RUN_DIR "/classcache"
#define java_load_bootstrapper euca_load_bootstrapper

void euca_load_bootstrapper(void);

typedef struct {
    char *method_name;
    char *method_signature;
} java_method_t;
static java_method_t euca_get_instance = { "getInstance", "()Lcom/eucalyptus/bootstrap/SystemBootstrapper;" };
static java_method_t euca_load = { "load", "()Z" };
static java_method_t euca_init = { "init", "()Z" };
static java_method_t euca_start = { "start", "()Z" };
static java_method_t euca_stop = { "stop", "()Z" };
static java_method_t euca_check = { "check", "()Z" };
static java_method_t euca_destroy = { "destroy", "()Z" };
static java_method_t euca_version = { "getVersion", "()Ljava/lang/String;" };

typedef struct {
    jclass clazz;
    jstring class_name;
    jobject class_ref;
    jobject instance;
    jmethodID constructor;
    jmethodID get_instance;
    jmethodID init;
    jmethodID load;
    jmethodID start;
    jmethodID stop;
    jmethodID check;
    jmethodID destroy;
    jmethodID version;
} bootstrapper_t;

#define JVM_ARG(jvm_opt,arg,...) do { \
    char temp[1024]; \
    snprintf(temp,1024,arg,##__VA_ARGS__); \
    jvm_opt.optionString=strdup(temp); \
} while(0)
static char *jvm_default_opts[] = {
    "-Xbootclasspath/p:%1$s" EUCALYPTUS_DATA_DIR "/openjdk-crypto.jar",
    "-Xmx1024m",
    "-XX:MaxPermSize=256m",
    "-XX:+UseConcMarkSweepGC",
    "-Djava.net.preferIPv4Stack=true",
    "-Djava.security.policy=" EUCALYPTUS_ETC_DIR "/security.policy",
    "-Djava.library.path=" EUCALYPTUS_LIB_DIR,
    "-Djava.awt.headless=true",
    "-Dsun.java.command=Eucalyptus",
    "-Deuca.home=%1$s",
    "-Deuca.db.home=",
    "-Deuca.extra_version=",
    "-Deuca.var.dir=" EUCALYPTUS_STATE_DIR,
    "-Deuca.state.dir=" EUCALYPTUS_STATE_DIR,
    "-Deuca.run.dir=" EUCALYPTUS_RUN_DIR,
    "-Deuca.lib.dir=" EUCALYPTUS_JAVA_LIB_DIR,
    "-Deuca.libexec.dir=" EUCALYPTUS_LIBEXEC_DIR,
    "-Deuca.conf.dir=" EUCALYPTUS_CONF_DIR "/cloud.d",
    "-Deuca.log.dir=" EUCALYPTUS_LOG_DIR,
    "-Deuca.jni.dir=" EUCALYPTUS_LIB_DIR,
    "-Djava.util.prefs.PreferencesFactory=com.eucalyptus.util.NoopPreferencesFactory",
    NULL,
};

static char *libjvm_paths[] = {
    "%1$s/jre/lib/amd64/server/libjvm.so",
    "%1$s/lib/amd64/server/libjvm.so",
    "%1$s/jre/lib/i386/server/libjvm.so",
    "%1$s/lib/i386/server/libjvm.so",
    NULL,
};

static struct stat home;
static int stopping = 0;
static int doreload = 0;
typedef void (*sig_handler_t) (int);
typedef struct {
    sig_handler_t *_int;
    sig_handler_t *_hup;
    sig_handler_t *_term;
} signal_handlers_t;
static signal_handlers_t handle;
static JavaVM *jvm = NULL;
static JNIEnv *env = NULL;
static bootstrapper_t bootstrap;
#define CHECK_ISDIR(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISDIR(home.st_mode) )
#define CHECK_ISREG(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISREG(home.st_mode) )
#define CHECK_ISLNK(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISLNK(home.st_mode) )
int main(int argc, char *argv[]);
void main_reload(void);
void main_shutdown(void);

#endif /* ifndef __EUCALYPTUS_BOOTSTRAP_H__ */
