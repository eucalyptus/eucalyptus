/*
Bits of this code are based on based on Apache commons-daemon's jsvc.
Please see: http://commons.apache.org/daemon/
   Copyright 2001-2004 The Apache Software Foundation.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

#include "eucalyptus-bootstrap.h"

#include <jni.h>
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
#define _LINUX_FS_H 
#include <linux/capability.h>

extern char **environ;

pid_t controlled = 0;
static int stopping = 0;
static int doreload = 0;
static void (*handler_int)( int ) = NULL;
static void (*handler_hup)( int ) = NULL;
static void (*handler_trm)( int ) = NULL;

//static char *common_java_homes[ ] = {
//		"/usr/java",
//		"/usr/local/java",
//		"/usr/lib/jvm/java-6-openjdk",
//		NULL, };

static char *libjvm_paths[ ] = {
	"$JAVA_HOME/jre/lib/" CPU "/server/libjvm.so",
	"$JAVA_HOME/lib/" CPU "/server/libjvm.so",
	NULL,
};


static java_home_t *get_java_home( char *path ) {
	java_home_t *data = NULL;
	char buf[ 1024 ];
	int x = -1, k = 0, i = 0;

	if( path == NULL ) return NULL;
	log_debug( "Looking for libjvm in %s", path );
	if( CHECK_ISDIR( path ) == 0 ) {
		return NULL;
		log_debug( "Path %s is not a directory", path );
	}
	data = (java_home_t *) malloc( sizeof(java_home_t) );
	data->path = strdup( path );
	data->jvms = NULL;
	data->jnum = 0;
	while( libjvm_paths[ ++x ] != NULL ) {
		if( ( k = replace( buf, 1024, libjvm_paths[ x ], "$JAVA_HOME", path ) ) != 0 ) {
			log_error( "Error mangling jvm path" );
			return NULL;
		}
		log_debug( "Attempting to locate VM library %s", buf );
		if( CHECK_ISREG( buf ) == 1 ) {
			data->jvms = (jvm_info_t **) malloc( 2 * sizeof(jvm_info_t *) );
			data->jvms[ i ] = (jvm_info_t *) malloc( sizeof(jvm_info_t) );
			data->jvms[ i ]->libjvm_path = strdup( buf );
			char *dir, *base;
			dir = dirname(buf);
			base = basename(dir);
			data->jvms[ i ]->name = base;
			data->jvms[ ++i ] = NULL;
			data->jnum = i;
			return data;
		}
	}
	return data;
}


static void handler( int sig ) {
	switch( sig ) {
		case SIGTERM:
			log_debug( "Caught SIGTERM: Scheduling a shutdown" );
			if( stopping == 1 ) log_error( "Shutdown or reload already scheduled" );
			else stopping = 1;
			break;
		case SIGINT:
			log_debug( "Caught SIGINT: Scheduling a shutdown" );
			if( stopping == 1 ) log_error( "Shutdown or reload already scheduled" );
			else stopping = 1;
			break;
		case SIGHUP:
			log_debug( "Caught SIGHUP: Scheduling a reload" );
			if( stopping == 1 ) log_error( "Shutdown or reload already scheduled" );
			else stopping = 1,doreload = 1;
			break;
		default:
			log_debug( "Caught unknown signal %d", sig );
			break;
	}
}

static int set_user_group( char *user, int uid, int gid ) {
	if( user != NULL ) {
		if( setgid( gid ) != 0 ) {
			log_error( "Cannot set group id for user '%s'", user );
			return -1;
		}
		if( initgroups( user, gid ) != 0 ) {
			if( getuid( ) != uid ) {
				log_error( "Cannot set supplement group list for user '%s'", user );
				return -1;
			} else
				log_debug( "Cannot set supplement group list for user '%s'", user );
		}
		if( setuid( uid ) != 0 ) {
			log_error( "Cannot set user id for user '%s'", user );
			return -1;
		}
		log_debug( "user changed to '%s'", user );
	}
	return 0;
}
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
		log_error("syscall failed in set_caps");
		return -1;
	}
	return 0;
}
static int linuxset_user_group(char *user, int uid, int gid){
	if (set_caps(CAPS)!=0) {
		if (getuid()!= uid) {
			log_error("set_caps(CAPS) failed");
			return -1;
		}
		log_debug("set_caps(CAPS) failed");
	}
	if (prctl(PR_SET_KEEPCAPS,1,0,0,0) < 0) {
		log_error("prctl failed in linuxset_user_group");
		return -1;
	}
	if (set_user_group(user,uid,gid)!=0) {
		log_error("set_user_group failed in linuxset_user_group");
		return -1;
	}
	if (set_caps(CAPSMIN)!=0) {
		if (getuid()!= uid) {
			log_error("set_caps(CAPSMIN) failed");
			return -1;
		}
		log_debug("set_caps(CAPSMIN) failed");
	}
	return 0;
}

static int checkuser( char *user, uid_t *uid, gid_t *gid ) {
	struct passwd *pwds = NULL;
	int status = 0;
	pid_t pid = 0;
	if( user == NULL )
		return 1;
	pwds = getpwnam( user );
	if( pwds == NULL ) {
		log_error( "Invalid user name '%s' specified", user );
		return 0;
	}
	*uid = pwds->pw_uid;
	*gid = pwds->pw_gid;
	pid = fork( );
	if( pid == -1 ) {
		log_error( "Cannot validate user name" );
		return 0;
	}
	if( pid == 0 ) {
		if( set_user_group( user, *uid, *gid ) != 0 ) exit( 1 );
		else exit( 0 );
	}
	while( waitpid( pid, &status, 0 ) != pid );
	if( WIFEXITED( status ) ) {
		status = WEXITSTATUS( status );
		if( status == 0 ) {
			log_debug( "User '%s' validated", user );
			return 1;
		}
	}
	log_error( "Error validating user '%s'", user );
	return 0;
}

static void controller( int sig ) {
	switch( sig ) {
	case SIGTERM:
	case SIGINT:
	case SIGHUP:
		log_debug( "Forwarding signal %d to process %d", sig, controlled );
		kill( controlled, sig );
		signal( sig, controller );
		break;
	default:
		log_debug( "Caught unknown signal %d", sig );
		break;
	}
}

static void * signal_set( int sig, void * newHandler ) {
	void *hand;
	hand = signal( sig, newHandler );
#ifdef SIG_ERR
	if (hand==SIG_ERR)
	hand=NULL;
#endif
	if( hand == handler || hand == controller )
		hand = NULL;
	return hand;
}

static int check_pid( euca_opts *args ) {
	int fd,i, pid;
	FILE *pidf;
	char buff[ 80 ];
	pid_t pidn = getpid( );
	fd = open( GETARG( args, pidfile ), O_RDWR | O_CREAT, S_IRUSR | S_IWUSR );
	if( fd < 0 ) {
		log_error( "Cannot open PID file %s, PID is %d",GETARG( args, pidfile ), pidn );
		return -1;
	} else {
		if( lockf( fd, F_LOCK, 0 ) );//hack
		i = read( fd, buff, sizeof( buff ) );
		if( i > 0 ) {
			buff[ i ] = '\0';
			pid = atoi( buff );
			if( kill( pid, 0 ) == 0 ) {
				log_error( "Still running according to PID file %s, PID is %d",GETARG( args, pidfile ), pid );
				if( lockf( fd, F_ULOCK, 0 ) );//hack
				close( fd );
				return 122;
			}
		}
		lseek( fd, SEEK_SET, 0 );
		pidf = fdopen( fd, "r+" );
		fprintf( pidf, "%d\n", (int) getpid( ) );
		fflush( pidf );
		if( lockf( fd, F_ULOCK, 0 ) );//hack
		fclose( pidf );
		close( fd );
	}
	return 0;
}

static int get_pidf( euca_opts *args ) {
	int fd,i;
	char buff[ 80 ];

	fd = open( GETARG( args, pidfile ), O_RDONLY, 0 );
	log_debug( "get_pidf: %d in %s", fd, GETARG( args, pidfile ) );
	if( fd < 0 )
		return -1;
	if( lockf( fd, F_LOCK, 0 ) );//hack
	i = read( fd, buff, sizeof( buff ) );
	if( lockf( fd, F_ULOCK, 0 ) );//hack
	close( fd );
	if( i > 0 ) {
		buff[ i ] = '\0';
		i = atoi( buff );
		log_debug( "get_pidf: pid %d", i );
		if( kill( i, 0 ) == 0 ) return i;
	}
	return -1;
}

static int check_tmp_file( euca_opts *args ) {
	int pid=get_pidf( args ),fd;
	char buff[ 80 ];
	if( pid < 0 ) return -1;
	sprintf( buff, "/tmp/%d.euca_up", pid );
	log_debug( "check_tmp_file: %s", buff );
	fd = open( buff, O_RDONLY );
	if( fd < 0 ) return -1;
	close( fd );
	return 0;
}
static void create_tmp_file( euca_opts *args ) {
	char buff[ 80 ];
	int fd;
	sprintf( buff, "/tmp/%d.euca_up", (int) getpid( ) );
	log_debug( "create_tmp_file: %s", buff );
	fd = open( buff, O_RDWR | O_CREAT, S_IRUSR | S_IWUSR );
	if( fd < 0 ) return;
	close( fd );
}
static void remove_tmp_file( euca_opts *args ) {
	char buff[ 80 ];
	sprintf( buff, "/tmp/%d.euca_up", (int) getpid( ) );
	log_debug( "remove_tmp_file: %s", buff );
	unlink( buff );
}

static int wait_child( euca_opts *args, int pid ) {
	int count = 10;
	int havejvm = 0;
	int fd;
	char buff[ 80 ];
	int i, status, waittime;
	log_debug( "wait_child %d", pid );
	waittime = 200 / 10;
	if( waittime > 10 ) {
		count = waittime;
		waittime = 10;
	}
	while( count > 0 ) {
		sleep( 1 );
		/* check if the controler is still running */
		if( waitpid( pid, &status, WNOHANG ) == pid ) {
			if( WIFEXITED( status ) ) return WEXITSTATUS( status );
			else return 1;
		}

		/* check if the pid file process exists */
		fd = open( GETARG( args, pidfile ), O_RDONLY );
		if( fd < 0 && havejvm )
			return 1; /* something has gone wrong the JVM has stopped */
		if( lockf( fd, F_LOCK, 0 ) );//hack
		i = read( fd, buff, sizeof( buff ) );
		if( lockf( fd, F_ULOCK, 0 ) );//hack
		close( fd );
		if( i > 0 ) {
			buff[ i ] = '\0';
			i = atoi( buff );
			if( kill( i, 0 ) == 0 ) {
				/* the JVM process has started */
				havejvm = 1;
				if( check_tmp_file( args ) == 0 ) {
					/* the JVM is started */
					if( waitpid( pid, &status, WNOHANG ) == pid ) {
						if( WIFEXITED( status ) ) return WEXITSTATUS( status );
						else return 1;
					}
					return 0; /* ready JVM started */
				}
			}
		}
		sleep( waittime );
		count--;
	}
	return 1; /* It takes more than the wait time to start, something must be wrong */
}

static int stop_child( euca_opts *args ) {
	int pid = get_pidf( args );
	int count = 10;
	if( pid > 0 ) {
		/* kill the process and wait until the pidfile has been removed by the controler */
		kill( pid, SIGTERM );
		while( count > 0 ) {
			sleep( 6 );
			pid = get_pidf( args );
			if( pid <= 0 ) return 0; /* JVM has stopped */
			count--;
		}
	}
	return -1;
}

static int child( euca_opts *args, java_home_t *data, uid_t uid, gid_t gid ) {
	int ret = 0;
	ret = check_pid( args );
	setpgrp( );
	log_debug("Calling java_init.");
	if( java_init( args, data ) != 1 )
		return 1;
	log_debug("Calling java_load.");
	if( java_load( args ) != 1 )
		return 3;
	log_debug("Dropping privileges.");
	if( linuxset_user_group( GETARG( args, user ), uid, gid ) != 0 )
		return 4;
	if (set_caps(0)!=0) {
		log_debug("set_caps (0) failed");
		return 4;
	}
	log_debug("Calling java_start.");
	if( java_start( ) != 1 )
		return 5;
	handler_hup = signal_set( SIGHUP, handler );
	handler_trm = signal_set( SIGTERM, handler );
	handler_int = signal_set( SIGINT, handler );
	controlled = getpid( );
	log_debug( "Waiting for a signal to be delivered" );
	create_tmp_file(args);
	while( !stopping ) sleep( 60 );
	remove_tmp_file(args);
	log_debug( "Shutdown or reload requested: exiting" );
	if( java_stop( ) != 1 ) return 6;
	if( doreload == 1 ) ret = 123;
	else ret = 0;
	java_destroy( );
	if( JVM_destroy( ret ) != 1 ) return 7;
	return ret;
}

static FILE *loc_freopen( char *outfile, char *mode, FILE *stream ) {
	FILE *ftest;
	ftest = fopen( outfile, mode );
	if( ftest == NULL ) {
		fprintf( stderr, "Unable to redirect to %s\n", outfile );
		return stream;
	}
	fclose( ftest );
	return freopen( outfile, mode, stream );
}

static void set_output( char *outfile, char *errfile ) {
	if( freopen( "/dev/null", "r", stdin ) );//hack
	log_debug( "redirecting stdout to %s and stderr to %s", outfile, errfile );
	if( debug == 1 && strcmp( errfile, "/dev/null" ) == 0 ) return;
	if( strcmp( outfile, "&2" ) == 0 && strcmp( errfile, "&1" ) == 0 ) outfile = "/dev/null";
	if( strcmp( outfile, "&1" ) != 0 ) loc_freopen( outfile, "a", stdout );
	if( strcmp( errfile, "&2" ) != 0 ) loc_freopen( errfile, "a", stderr );
	else {
		close( 2 );
		if( dup( 1 ) );//hack
	}
	if( strcmp( outfile, "&2" ) == 0 ) {
		close( 1 );
		if( dup( 2 ) );//hack
	}
}

int main( int argc, char *argv[ ] ) {
	euca_opts euca_args;
	euca_opts *args = &euca_args;
	java_home_t *data = NULL;
	int status = 0;
	pid_t pid = 0;
	uid_t uid = 0;
	gid_t gid = 0;
	if( arguments( argc, argv, args ) != 0 ) exit( 1 );
	debug = args->verbose_flag;
	if( args->stop_flag == 1 ) return stop_child( args );
	if( checkuser( GETARG( args, user ), &uid, &gid ) == 0 ) return 1;
	char* java_home_user = GETARG(args,java_home);
	char* java_home_env = getenv( "JAVA_HOME" );
	if( java_home_user != NULL ) {
		log_debug("Trying user supplied java home: %s", java_home_user);
		data = get_java_home(java_home_user);
	}
	if( data == NULL && java_home_env != NULL ) {
		log_debug("Trying environment JAVA_HOME: %s", java_home_env);
		data = get_java_home(java_home_env);
	}
	log_debug("TODO: loop through common locations for JVMs here.");
	if( data == NULL ) {
		log_error( "Cannot locate Java Home" );
		return 1;
	}
	int x;
	if( debug == 1 ) {
		log_debug( "+-- DUMPING JAVA HOME STRUCTURE ------------------------" );
		log_debug( "| Java Home:       \"%s\"", PRINT_NULL( data->path ) );
		log_debug( "| Found JVMs:      %d", data->jnum );
		for( x = 0; x < data->jnum; x++ ) {
			jvm_info_t *jvm = data->jvms[ x ];
			log_debug( "| JVM Name:        \"%s\"", PRINT_NULL( jvm->name ) );
			log_debug( "|                  \"%s\"", PRINT_NULL( jvm->libjvm_path ) );
		}
		log_debug( "+-------------------------------------------------------" );
	}
	if( strcmp( argv[ 0 ], "eucalyptus-cloud" ) != 0 ) {
		char *oldpath = getenv( "LD_LIBRARY_PATH" ),*libf = java_library( args, data );
		char *old = argv[ 0 ],buf[ 2048 ],*tmp = NULL,*p1 = NULL,*p2 = NULL;
		p1 = strdup( libf );
		tmp = strrchr( p1, '/' );
		if( tmp != NULL ) tmp[ 0 ] = '\0';
		p2 = strdup( p1 );
		tmp = strrchr( p2, '/' );
		if( tmp != NULL ) tmp[ 0 ] = '\0';
		if( oldpath == NULL ) snprintf( buf, 2048, "%s:%s", p1, p2 );
		else snprintf( buf, 2048, "%s:%s:%s", oldpath, p1, p2 );
		tmp = strdup( buf );
		setenv( "LD_LIBRARY_PATH", tmp, 1 );
		log_debug( "Invoking w/ LD_LIBRARY_PATH=%s", getenv( "LD_LIBRARY_PATH" ) );
		argv[ 0 ] = "eucalyptus-cloud";
		execve( old, argv, environ );
		log_error( "Cannot execute process" );
		return 1;
	}
	log_debug( "Running w/ LD_LIBRARY_PATH=%s", getenv( "LD_LIBRARY_PATH" ) );
	if( args->detach_flag == 1 ) {
		pid = fork( );
		if( pid == -1 ) {
			log_error( "Cannot detach from parent process" );
			return 1;
		}
		if( pid != 0 ) {
			if( 200 >= 10 )//HACK: args->wait
				return wait_child( args, pid );
			else return 0;
		}
#ifndef NO_SETSID
		setsid( );
#endif
	}
	//    set_output(GETARG(args,out), GETARG(args,err));
	set_output( "&1", "&2" );
	while( ( pid = fork( ) ) != -1 ) {
		if( pid == 0 ) exit( child( args, data, uid, gid ) );
		controlled = pid;
		signal( SIGHUP, controller );
		signal( SIGTERM, controller );
		signal( SIGINT, controller );
		while( waitpid( pid, &status, 0 ) != pid );
		if( WIFEXITED( status ) ) {
			status = WEXITSTATUS( status );
			if( args->jvm_version_flag != 1 && status != 122 ) unlink( GETARG( args, pidfile ) );
			if( status == 123 ) {
				log_debug( "Reloading service" );
				continue;
			}
			if( status == 0 ) {
				log_debug( "Service shut down" );
				return 0;
			}
			log_error( "Service exit with a return value of %d", status );
			return 1;
		} else {
			log_error( "Service did not exit cleanly exit value %d", status );
			return 1;
		}
	}
	log_error( "Cannot decouple controller/child processes" );
	return 1;
}

void main_reload( void ) {
	log_debug( "Killing self with HUP signal" );
	kill( controlled, SIGHUP );
}

void main_shutdown( void ) {
	log_debug( "Killing self with TERM signal" );
	kill( controlled, SIGTERM );
}

static JavaVM *jvm=NULL;
static JNIEnv *env=NULL;
static jclass cls=NULL;

int ld_library_path_set=0;
typedef void *dso_handle;

int dso_init(void) {
    return 1;
}

dso_handle dso_link(const char *path) {
    log_debug("Attemtping to load library %s",path);
    return((void *)dlopen(path,RTLD_GLOBAL|RTLD_NOW));
}
int dso_unlink(dso_handle libr) {
    if (dlclose(libr)==0) return 1;
    else return 0;
}
void *dso_symbol(dso_handle hdl, const char *nam) {
    return(dlsym(hdl,nam));
}
char *dso_error(void) {
    return(dlerror());
}

static void hello(JNIEnv *env, jobject source) {
    log_error("uid=%d,euid=%d,gid=%d,egid=%d",getuid(),geteuid(),getgid(),getegid());
}

static void shutdown(JNIEnv *env, jobject source, jboolean reload) {
    log_debug("Shutdown requested (reload is %d)",reload);
    if (reload==1) main_reload();
    else main_shutdown();
}

static void java_abort123(void) {
    exit(123);
}

char *java_library(euca_opts *args, java_home_t *data) {
    char *libjvm_path=NULL;
    int x;
    if (data->jnum==0) {
        log_error("Cannot find any VM in Java Home %s",data->path);
		exit(1);
    }
    if (args->jvm_name_given==0) {
    	libjvm_path=data->jvms[0]->libjvm_path;
    } else {
        for (x=0; x<data->jnum; x++) {
            if (data->jvms[x]->name==NULL) continue;
            if (strcmp(GETARG(args,jvm_name),data->jvms[x]->name)==0) {
            	libjvm_path=data->jvms[x]->libjvm_path;
                break;
            }
        }
    }
    if (libjvm_path==NULL) {
        log_error("Failed to locate usable JVM %s %s",GETARG(args,jvm_name),libjvm_path);
        exit(1);
    }
    log_debug("Using libjvm.so in %s",libjvm_path);
    return libjvm_path;
}

int java_init(euca_opts *args, java_home_t *data) {
    jint (*symb)(JavaVM **, JNIEnv **, JavaVMInitArgs *);
    JNINativeMethod nativemethod,hello_method;
    JavaVMOption *opt=NULL;
    dso_handle libh=NULL;
    JavaVMInitArgs arg;
    char *libf=NULL;
    jint ret;
    int x;
    char euca_boot_class[]=EUCA_MAIN;
    char hello_class[]="hello";
    char hello_params[]="()V";
    char shutdownclass[]="shutdown";
    char shutdownparams[]="(Z)V";
    libf=java_library(args,data);
    if (libf==NULL) {
        log_error("Cannot locate JVM library file");
        return 0;
    }
    if (dso_init()!=1) {
        log_error("Cannot initialize the dynamic library loader");
        return 0;
    }
    libh=dso_link(libf);
    if (libh==NULL) {
        log_error("Cannot dynamically link to %s",libf);
        log_error("%s",dso_error());
        return 0;
    }
    log_debug("JVM library %s loaded",libf);
    symb=dso_symbol(libh,"JNI_CreateJavaVM");
    if (symb==NULL) {
            log_error("Cannot find JVM library entry point");
            return 0;
    }
    log_debug("JVM library entry point found (0x%p)",symb);
#if defined(JNI_VERSION_1_4)
	arg.version=JNI_VERSION_1_4;
#else
	arg.version=JNI_VERSION_1_2;
#endif
    char lib_dir[256];
    char etc_dir[256];
    char jar_list[16384],*jar_list_p=jar_list;
    if( strlen(GETARG(args,home))+strlen(EUCA_LIB_DIR)>=254) {
    	log_error("Directory path too long: %s/%s", GETARG(args,home), EUCA_LIB_DIR);
    	exit(1);
    }
    snprintf(lib_dir,255,"%s%s",GETARG(args,home),EUCA_LIB_DIR);
    snprintf(etc_dir,255,"%s%s",GETARG(args,home),EUCA_ETC_DIR);
    if( !CHECK_ISDIR(lib_dir) ) {
    	log_error("Can't find library directory %s", lib_dir );
    	exit(1);
    }
    int jar_len =16383, wb = 0;
    wb = snprintf(jar_list_p,jar_len,"-Djava.class.path=%s",etc_dir);
    jar_list_p+=wb; jar_len-=wb;
    DIR* lib_dir_p = opendir(lib_dir);
    struct direct *dir_ent;
    while ((dir_ent = readdir(lib_dir_p))!=0)  {
	  if (strcmp(dir_ent->d_name,".") != 0 && strcmp(dir_ent->d_name,"..") != 0)  {
		 char jar[256];
		 wb = snprintf(jar,255,"%s/%s",lib_dir,dir_ent->d_name);
		 if( CHECK_ISREG(jar) ){
			wb = snprintf(jar_list_p,jar_len-wb,":%s",jar);
			jar_list_p+=wb;
		 }
	  }
	}
    arg.ignoreUnrecognized=0;
    arg.nOptions=args->jvm_args_given+args->define_given;
    arg.nOptions+=13; /* Add abort code, and classpath */
    opt=(JavaVMOption *)malloc(arg.nOptions*sizeof(JavaVMOption));
    for( x=0;x<11;x++) opt[x].extraInfo = NULL;
    x=0;
    char temp[1024];
    snprintf(temp,1023,"-Xbootclasspath/p:%s/usr/share/eucalyptus/eucalyptus-crypto.jar:%s/usr/share/eucalyptus/eucalyptus-workarounds.jar",GETARG(args,home),GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    opt[x++].optionString="-Xmx256m";
    opt[x++].optionString="-XX:+UseConcMarkSweepGC";
    snprintf(temp,1023,"-Djava.security.policy=%s/etc/eucalyptus/cloud.d/security.policy",GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    snprintf(temp,1023,"-Djava.library.path=%s/usr/lib/eucalyptus",GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    snprintf(temp,1023,"-Deuca.home=%s/",GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    snprintf(temp,1023,"-Deuca.var.dir=%s/var/lib/eucalyptus",GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    snprintf(temp,1023,"-Deuca.lib.dir=%s/usr/share/eucalyptus",GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    snprintf(temp,1023,"-Deuca.conf.dir=%s/etc/eucalyptus/cloud.d",GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    snprintf(temp,1023,"-Deuca.log.dir=%s/var/log/eucalyptus",GETARG(args,home));
    opt[x++].optionString=strdup(temp);
    opt[x++].optionString="-Deuca.version=1.6-devel";

    for (x=11; x<(args->jvm_args_given+11); x++) {
    	char* jvmarg = (char*) malloc((strlen(args->jvm_args_arg[x])+3)*sizeof(char));
    	sprintf(jvmarg,"-X%s",args->jvm_args_arg[x]);
    	opt[x].optionString=jvmarg;
        opt[x].extraInfo=NULL;
    }
    for (x=(args->jvm_args_given+11); x<(args->jvm_args_given+args->define_given+11); x++) {
    	char* jvmarg = (char*) malloc((strlen(args->define_arg[x])+2)*sizeof(char));
    	sprintf(jvmarg,"-D%s",args->define_arg[x]);
    	opt[x].optionString=jvmarg;
        opt[x].extraInfo=NULL;
    }
    opt[x].optionString=jar_list;
    opt[x++].extraInfo=NULL;
    opt[x].optionString=strdup("abort");
    opt[x].extraInfo=java_abort123;
    arg.options=opt;
    if (debug==1) {
        log_debug("+-- DUMPING JAVA VM CREATION ARGUMENTS -----------------");
        log_debug("| Version:                       %x",arg.version);
        log_debug("| Ignore Unrecognized Arguments: %s",
                  arg.ignoreUnrecognized==1?"1":"0");
        log_debug("| Extra options:                 %d",arg.nOptions);

        for (x=0; x<arg.nOptions; x++) {
            log_debug("|   \"%-80.80s\" (0x%p)",opt[x].optionString,
                      opt[x].extraInfo);
        }
        log_debug("+-------------------------------------------------------");
    }
    ret=(*symb)(&jvm, &env, &arg);
    if (ret<0) {
        log_error("Cannot create Java VM");
        return 0;
    }
    log_debug("Java VM created successfully");
    cls=(*env)->FindClass(env,euca_boot_class);
    if (cls==NULL) {
        log_error("Cannot find Eucalyptus loader %s",euca_boot_class);
        return 0;
    }
    log_debug("Class %s found",euca_boot_class);
    nativemethod.name=shutdownclass;
    nativemethod.signature=shutdownparams;
    nativemethod.fnPtr=shutdown;
    if((*env)->RegisterNatives(env,cls,&nativemethod,1)!=0) {
        log_error("Cannot register native methods");
        return 0;
    }
    hello_method.name=hello_class;
    hello_method.signature=hello_params;
    hello_method.fnPtr=hello;
    if((*env)->RegisterNatives(env,cls,&hello_method,1)!=0) {
        log_error("Cannot register native methods");
        return 0;
    }
    log_debug("Native methods registered");
    return 1;
}

int JVM_destroy(int exit) {
    jclass system=NULL;
    jmethodID method;
    char System[]="java/lang/System";
    char exitclass[]="exit";
    char exitparams[]="(I)V";
    system=(*env)->FindClass(env,System);
    if (system==NULL) {
        log_error("Cannot find class %s",System);
        return 0;
    }
    method=(*env)->GetStaticMethodID(env,system,exitclass,exitparams);
    if (method==NULL) {
        log_error("Cannot find \"System.exit(int)\" entry point");
        return 0;
    }
    log_debug("Calling System.exit(%d)",exit);
    (*env)->CallStaticVoidMethod(env,system,method,(jint)exit);
    log_debug("Destroying the Java VM");
    if ((*jvm)->DestroyJavaVM(jvm)!=0) return 0;
    log_debug("Java VM destroyed");
    return 1;
}

int java_load(euca_opts *args) {
    jclass stringClass=NULL;
    jstring className=NULL;
    jobjectArray stringArray=NULL;
    jmethodID method=NULL;
    jboolean ret=0;
    char lang[]="java/lang/String";
    char load[]="load";
    char loadparams[]="(Ljava/lang/String;[Ljava/lang/String;)Z";
    className=(*env)->NewStringUTF(env,EUCA_MAIN);
    if (className==NULL) {
        log_error("Cannot create string for class name");
        return 0;
    }
    stringClass=(*env)->FindClass(env,lang);
    if (stringClass==NULL) {
        log_error("Cannot find class java/lang/String");
        return 0;
    }
    stringArray=(*env)->NewObjectArray(env,0,stringClass,NULL);
    if (stringArray==NULL) {
        log_error("Cannot create arguments array");
        return 0;
    }
    method=(*env)->GetStaticMethodID(env,cls,load,loadparams);
    if (method==NULL) {
        return 0;
    }
    ret=(*env)->CallStaticBooleanMethod(env,cls,method,className,stringArray);
    if (ret==0) {
        log_error("Cannot load Eucalyptus");
        return 0;
    }
    return 1;
}

int java_start(void) {
    jmethodID method;
    jboolean ret;
    char start[]="start";
    char startparams[]="()Z";
    method=(*env)->GetStaticMethodID(env,cls,start,startparams);
    if (method==NULL) {
        log_error("Cannot find Eucalyptus Loader \"start\" entry point");
        return 0;
    }
    ret=(*env)->CallStaticBooleanMethod(env,cls,method);
    if (ret==0) {
        log_error("Cannot start Eucalyptus");
        return 0;
    }
    log_debug("Eucalyptus started successfully");
    return 1;
}

void java_sleep(int wait) {
    jclass clsThread;
    jmethodID method;
    char jsleep[]="sleep";
    char jsleepparams[]="(J)V";
    char jthread[]="java/lang/Thread";
    clsThread = (*env)->FindClass(env,jthread);
    if (clsThread==NULL) {
        log_error("Cannot find java/lang/Thread class");
        return;
    }
    method=(*env)->GetStaticMethodID(env,clsThread,jsleep,jsleepparams);
    if (method==NULL) {
        log_error("Cannot found the sleep entry point");
        return;
    }
    (*env)->CallStaticVoidMethod(env,clsThread,method,(jlong)wait*1000);
}

int java_stop(void) {
    jmethodID method;
    jboolean ret;
    char stop[]="stop";
    char stopparams[]="()Z";
    method=(*env)->GetStaticMethodID(env,cls,stop,stopparams);
    if (method==NULL) {
        log_error("Cannot found Eucalyptus Loader \"stop\" entry point");
        return 0;
    }
    ret=(*env)->CallStaticBooleanMethod(env,cls,method);
    if (ret==0) {
        log_error("Cannot stop Eucalyptus");
        return 0;
    }
    log_debug("Eucalyptus stopped successfully");
    return 1;
}

int java_version(void) {
    jmethodID method;
    char version[]="version";
    char versionparams[]="()Z";
    method=(*env)->GetStaticMethodID(env,cls,version,versionparams);
    if (method==NULL) {
        log_error("Cannot found Eucalyptus Loader \"version\" entry point");
        return 0;
    }
    (*env)->CallStaticVoidMethod(env,cls,method);
    return 1;
}

int java_check(euca_opts *args) {
    jstring className=NULL;
    jmethodID method=NULL;
    jboolean ret=0;
    char check[]="check";
    char checkparams[]="(Ljava/lang/String;)Z";
    log_debug("Checking Eucalyptus");
    className=(*env)->NewStringUTF(env,EUCA_MAIN);
    if (className==NULL) {
        log_error("Cannot create string for class name");
        return 0;
    }
    method=(*env)->GetStaticMethodID(env,cls,check,checkparams);
    if (method==NULL) {
        log_error("Cannot found Eucalyptus Loader \"check\" entry point");
        return 0;
    }
    ret=(*env)->CallStaticBooleanMethod(env,cls,method,className);
    if (ret==0) {
        log_error("An error was detected checking the %s Eucalyptus",EUCA_MAIN);
        return 0;
    }
    log_debug("Eucalyptus checked successfully.");
    return 1;
}

int java_destroy(void) {
    jmethodID method;
    jboolean ret;
    char destroy[]="destroy";
    char destroyparams[]="()Z";
    method=(*env)->GetStaticMethodID(env,cls,destroy,destroyparams);
    if (method==NULL) {
        log_error("Failed to invoked destroy on Eucalyptus.");
        return 0;
    }
    ret=(*env)->CallStaticBooleanMethod(env,cls,method);
    if (ret==0) {
        log_error("Cannot destroy Eucalyptus.");
        return 0;
    }
    log_debug("Eucalyptus destroyed successfully.");
    return 1;
}

int replace(char *new, int len, char *old, char *mch, char *rpl) {
    char *tmp;
    int count,shift,nlen,olen,mlen,rlen,x;
    if (new==NULL) return -1;
    if (len<0) return -2;
    if (old==NULL) return -3;
    if ((mch==NULL)||(strlen(mch)==0)) {
        olen=strlen(old);
        if (len<=olen) return(olen+1);
        strcpy(new,old);
        return 0;
    }
    if (rpl==NULL) rpl="";
    olen=strlen(old);
    mlen=strlen(mch);
    rlen=strlen(rpl);
    tmp=old;
    count=0;
    while((tmp=strstr(tmp,mch))!=NULL) {
        count++;
        tmp+=mlen;
    }
    if (count==0) {
        olen=strlen(old);
        if (len<=olen) return(olen+1);
        strcpy(new,old);
        return 0;
    }
    shift=rlen-mlen;
    nlen=olen+(shift*count);
    if (nlen>=len) return(nlen+1);
    strcpy(new,old);
    tmp=new;
    while((tmp=strstr(tmp,mch))!=NULL) {
        if (shift>0) {
            for (x=(strlen(tmp)+shift);x>shift;x--) {
                tmp[x]=tmp[x-shift];
            }
        } else if (shift<0) {
            for (x=mlen;x<strlen(tmp)-shift;x++) {
                tmp[x+shift]=tmp[x];
            }
        }
        strncpy(tmp,rpl,rlen);
        tmp+=rlen;
    }
    return 0;
}
