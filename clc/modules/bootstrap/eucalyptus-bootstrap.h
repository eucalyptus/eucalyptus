/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 *
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 *
 *
 ******************************************************************************
 * Author: chris grzegorczyk grze@eucalyptus.com
 ******************************************************************************/
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
#define CAPSALL (1 << CAP_NET_BIND_SERVICE)+ \
                (1 << CAP_SETUID)+ \
                (1 << CAP_SETGID)+ \
                (1 << CAP_DAC_READ_SEARCH)+ \
                (1 << CAP_DAC_OVERRIDE)
#define CAPSMAX (1 << CAP_NET_BIND_SERVICE)+ \
                (1 << CAP_DAC_READ_SEARCH)+ \
                (1 << CAP_DAC_OVERRIDE)
#define CAPS    (1 << CAP_NET_BIND_SERVICE)+ \
                (1 << CAP_SETUID)+ \
                (1 << CAP_SETGID)
#define CAPSMIN (1 << CAP_NET_BIND_SERVICE)
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
#define __die(condition,format,...) do { if(condition) {fprintf(stderr,"[error:%04d] ", __LINE__);fprintf(stderr, format "\n", ##__VA_ARGS__ ); exit(1);} } while(0)
#define __fail(format,...) __die(1,format,##__VA_ARGS__)
#define __abort(r,condition,format,...) do { if(condition) {fprintf(stderr,"[error:%04d] ", __LINE__);fprintf(stderr, format "\n", ##__VA_ARGS__ ); return r;} } while(0)
#define __debug(format,...) do { if(debug){fprintf(stdout,"[debug:%04d] ", __LINE__);fprintf(stdout, format "\n", ##__VA_ARGS__ ); } } while(0)
#define __error(format,...) do { fprintf(stderr,"[error:%04d] ", __LINE__);fprintf(stderr, format "\n", ##__VA_ARGS__ ); } while(0)
#define EUCA_LIB_DIR "/usr/share/eucalyptus"
#define EUCA_ETC_DIR "/etc/eucalyptus/cloud.d"
#define EUCA_SCRIPT_DIR "/etc/eucalyptus/cloud.d/scripts"
#define EUCA_MAIN "com/eucalyptus/bootstrap/SystemBootstrapper"
#define EUCA_RET_RELOAD 123
#define java_load_bootstrapper euca_load_bootstrapper
void euca_load_bootstrapper(void);

typedef struct {
	char* method_name;
	char* method_signature;
} java_method_t;
static java_method_t euca_get_instance = { "getInstance", "()Lcom/eucalyptus/bootstrap/SystemBootstrapper;"};
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
	    "-Xbootclasspath/p:%1$s/usr/share/eucalyptus/openjdk-crypto.jar",
	    "-Xmx512m",
	    "-XX:MaxPermSize=128m",
	    "-XX:+UseConcMarkSweepGC",
	    "-Djava.net.preferIPv4Stack=true",
	    "-Djava.security.policy=%1$s/etc/eucalyptus/cloud.d/security.policy",
	    "-Djava.library.path=%1$s/usr/lib/eucalyptus",
	    "-Dsun.java.command=Eucalyptus",
	    "-Deuca.home=%1$s/",
	    "-Deuca.var.dir=%1$s/var/lib/eucalyptus",
	    "-Deuca.run.dir=%1$s/var/run/eucalyptus",
	    "-Deuca.lib.dir=%1$s/usr/share/eucalyptus",
	    "-Deuca.conf.dir=%1$s/etc/eucalyptus/cloud.d",
	    "-Deuca.log.dir=%1$s/var/log/eucalyptus",
	    "-Deuca.version=2.0.1",
	    NULL,
};
static char *libjvm_paths[ ] = {
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
static JavaVM *jvm=NULL;
static JNIEnv *env=NULL;
static bootstrapper_t bootstrap;
#define CHECK_ISDIR(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISDIR(home.st_mode) )
#define CHECK_ISREG(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISREG(home.st_mode) )
#define CHECK_ISLNK(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISLNK(home.st_mode) )
int main( int argc, char *argv[ ] );
void main_reload( void );
void main_shutdown( void );

#endif /* ifndef __EUCALYPTUS_BOOTSTRAP_H__ */
