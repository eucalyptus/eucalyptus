#ifndef INCLUDE_MISC_H
#define INCLUDE_MISC_H

#include <stdarg.h>

#ifndef NO_AXIS /* for compiling on systems without Axis */
#include <neethi_policy.h>
#include <neethi_util.h>
#include <axutil_utils.h>
#include <axis2_client.h>
#include <axis2_stub.h>
int InitWSSEC(axutil_env_t *env, axis2_stub_t *stub, char *policyFile);
#endif

enum {EUCADEBUG2, EUCADEBUG, EUCAINFO, EUCAWARN, EUCAERROR, EUCAFATAL};

char * replace_string (char ** stringp, char * source, char * destination );
int sscanf_lines (char * lines, char * format, void * varp);
char * system_output (char * shell_command );
char *getConfString(char *configFile, char *key);

/**
 * Search in file #path# for a variable named #name#. It will put
 * whatever after the = in value (which will need to be freed by the
 * caller). 
 *
 * Returns -1 on error (open file, out of memory, parse error ...) 
 *          0 if variable not found in file
 *          1 if found and value is indeed valid
 *
 * Examples of parsed line:
 * TEST="test uno due tre"
 *      TEST = prova
 * TEST=prova
 */
int
get_conf_var(	const char *path,
		const char *name,
		char **value);

/**
 * The next 2 functions deal with turning a variable values (that is a
 * string) into a NULL terminated array of strings (char **). Example:
 * 	var="hostname1 hostname2"
 * it will return
 * 	()[0] = hostname1
 * 	()[1] = hostname2
 * 	()[2] = NULL
 *
 * the return array needs to be freed and you can use free_char_list() to
 * do so.
 *
 * Return NULL if something went wrong (probably out of memory, or an
 * array of strings. Notice that if something is wrong in the parsing
 * (the variable contains only spaces) you'll get back an array with only
 * one element and the element is NULL.
 */
void
free_char_list(char **value);

char **
from_var_to_char_list(const char *var);

// dan's functions
int logprintf(const char *format, ...);
int logprintfl(int level, const char *format, ...);
void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other);
int logfile(char *file, int in_loglevel);
int check_directory(char *dir);
int check_file(char *file);
int check_file_newer_than(char *file, time_t mtime);

// argument checker
int param_check(char *func, ...);
// end of dan't functions

#ifdef DEBUG
#define PRINTF(a) logprintf a
#else
#define PRINTF(a)
#endif

#ifdef DEBUG1
#define PRINTF1(a) logprintf a
#else
#define PRINTF1(a)
#endif

#ifdef DEBUGXML
#define PRINTF_XML(a) logprintf a
#else
#define PRINTF_XML(a)
#endif

int hash_code (const char * s);
char * get_string_stats (const char * s);
int daemonrun(char *cmd, int *dpid);
int daemonmaintain(char *cmd, char *procname, char *pidfile, int force, char *rootwrap, int *dpid);
int run (const char * arg1, ...);
int vrun (const char * fmt, ...);
int cat (const char * file_name);
int logcat (int debug_level, const char * file_name);
int touch (const char * path);
int diff (const char * path1, const char * path2);
long long dir_size (const char * path);
char * file2str (const char * path); /* read file 'path' into a new string */
int write2file(const char *path, char *str);
long long str2longlong (const char * str, const char * begin, const char * end); /* extract integer from str bound by 'begin' and 'end' */
pid_t timewait(pid_t pid, int *status, int timeout);
int uint32compar(const void *ina, const void *inb);
int safekill(pid_t pid, char *procname, int sig, char *rootwrap);
int safekillfile(char *pidfile, char *procname, int sig, char *rootwrap);

int verify_helpers(char **helpers, char **helpers_path, int LASTHELPER);



#endif
