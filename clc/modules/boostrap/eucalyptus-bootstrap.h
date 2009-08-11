#ifndef __EUCALYPTUS_BOOTSTRAP_H__
#define __EUCALYPTUS_BOOTSTRAP_H__

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "eucalyptus-opts.h"
#define PRINT_NULL(x) ((x) == NULL ? "null" : (x))
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
char *java_library(euca_opts*, java_home_t*);
int java_init(euca_opts*, java_home_t*);
int java_destroy(void);
int java_load(euca_opts*);
int java_start(void);
int java_stop(void);
int java_version(void);
int java_check(euca_opts*);
int JVM_destroy(int);
int replace(char*, int, char*, char*, char*);
#define GETARG(a,x) (a->x##_arg)
static int debug = 0;
#define log_debug(format,...) do { if(debug){fprintf(stdout,"[debug:%s:%d] ", __FILE__, __LINE__);fprintf(stdout, format "\n", ##__VA_ARGS__ ); } } while(0)
#define log_error(format,...) do { fprintf(stderr,"[error:%s:%d] ", __FILE__, __LINE__);fprintf(stderr, format "\n", ##__VA_ARGS__ ); } while(0)
#define EUCA_MAIN "com/eucalyptus/Bootstrap"
#define EUCA_LIB_DIR "/usr/share/eucalyptus"
#define EUCA_ETC_DIR "/etc/eucalyptus/cloud.d"
static struct stat home;
#define CHECK_ISDIR(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISDIR(home.st_mode) )
#define CHECK_ISREG(path) (( path == NULL || ( stat( path, &home ) != 0 ) ) ? 0 : S_ISREG(home.st_mode) )
int main( int argc, char *argv[ ] );
void main_reload( void );
void main_shutdown( void );

#endif /* ifndef __EUCALYPTUS_BOOTSTRAP_H__ */
