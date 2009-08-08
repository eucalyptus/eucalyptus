#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h> /* strlen, strcpy */
#include <ctype.h> /* isspace */
#include "misc.h"
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <time.h>
#include <math.h> /* powf */
#include <vnetwork.h>
#include <fcntl.h> /* open */
#include <utime.h> /* utime */
#include <sys/wait.h>

#ifndef NO_AXIS /* for compiling on systems without Axis */
#include <neethi_policy.h>
#include <neethi_util.h>
#include <axutil_utils.h>
#include <axis2_client.h>
#include <axis2_stub.h>

pid_t timewait(pid_t pid, int *status, int timeout) {
  time_t timer=0;
  int rc;

  if (timeout <= 0) timeout = 1;

  *status = 1;
  rc = waitpid(pid, status, WNOHANG);
  while(rc <= 0 && timer < (timeout * 1000000)) {
    usleep(50000);
    timer += 50000;
    rc = waitpid(pid, status, WNOHANG);
  }
  if (rc < 0) {
    logprintfl(EUCAERROR, "waitpid() timed out: pid=%d\n", pid);
  }
  return(rc);
}

int InitWSSEC(axutil_env_t *env, axis2_stub_t *stub, char *policyFile) {
  axis2_svc_client_t *svc_client = NULL;
  neethi_policy_t *policy = NULL;
  axis2_status_t status = AXIS2_FAILURE;

  //return(0);

  svc_client =  axis2_stub_get_svc_client(stub, env);
  if (!svc_client) {
    logprintfl (EUCAERROR, "InitWSSEC(): ERROR could not get svc_client from stub\n");
    return(1);
  }
  axis2_svc_client_engage_module(svc_client, env, "rampart");

  policy = neethi_util_create_policy_from_file(env, policyFile);
  if (!policy) {
    logprintfl (EUCAERROR, "InitWSSEC(): ERROR could not initialize policy file %s\n", policyFile);
    return(1);
  }
  status = axis2_svc_client_set_policy(svc_client, env, policy);
    
  return(0);
}
#endif /* NO_AXIS */

int timelog=0; /* change to 1 for TIMELOG entries */

int logging=0;
int loglevel=EUCADEBUG;
FILE *LOGFH=NULL;
char logFile[1024];

int param_check(char *func, ...) {
  int fail;
  va_list al;

  if (!func) {
    return(1);
  }
  
  va_start(al, func);
  fail=0;
  if (!strcmp(func, "vnetGenerateDHCP") || !strcmp(func, "vnetKickDHCP")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    if (!a) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetAddPublicIP") || !strcmp(func, "vnetAddDev")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    if (!a || !b) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetAddHost")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    int d = va_arg(al, int);
    if (!a || !b || (d < 0) || (d > NUMBER_OF_VLANS-1)) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetGetNextHost")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    int d = va_arg(al, int);
    if (!a || !b || !c || d < 0 || d > NUMBER_OF_VLANS-1) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetDelHost") || !strcmp(func, "vnetEnableHost") || !strcmp(func, "vnetDisableHost")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    int d = va_arg(al, int);
    if (!a || (!b && !c) || d < 0 || d > NUMBER_OF_VLANS-1) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetDeleteChain") || !strcmp(func, "vnetCreateChain")) { 
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    if (!a || !b || !c) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetTableRule")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    char *d = va_arg(al, char *);
    char *e = va_arg(al, char *);
    char *f = va_arg(al, char *);
    char *g = va_arg(al, char *);
    if (!a || !b || !c || !d || (!e && !f && !g)) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetSetVlan")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    int b = va_arg(al, int);
    char *c = va_arg(al, char *);
    char *d = va_arg(al, char *);
    if (!a || b < 0 || b >= NUMBER_OF_VLANS || !c || !d) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetDelVlan")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    int b = va_arg(al, int);
    if (!a || b < 0 || b >= NUMBER_OF_VLANS) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetInit")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    char *d = va_arg(al, char *);
    int e = va_arg(al, int);
    if (!a || !b || !c || d<0) {
      fail=1;
    }
  }

  va_end(al);

  if (fail) {
    logprintfl (EUCAERROR, "INTERNAL ERROR: incorrect input parameters to function %s\n", func);
    return(1);
  }
  return(0);
}

int check_directory(char *dir) {
  int rc;
  struct stat mystat;
  
  if (!dir) {
    return(1);
  }
  
  rc = lstat(dir, &mystat);
  if (rc < 0 || !S_ISDIR(mystat.st_mode)) {
    return(1);
  }
  return(0);
}

int check_file(char *file) {
  int rc;
  struct stat mystat;
  
  if (!file) {
    return(1);
  }
  
  rc = lstat(file, &mystat);
  if (rc < 0 || !S_ISREG(mystat.st_mode)) {
    return(1);
  }
  return(0);
}

/* given string *stringp, replace occurences of <source> with <destination>
 * and return the new string in stringp */
char * replace_string (char ** stringp, char * source, char * destination )
{
    char *start=NULL, *substart=NULL, *tok=NULL, * new_string=NULL;
    if (source==NULL || destination==NULL) return NULL;
    char * buf;
    int maxlen = 32768;
    
    buf = malloc(sizeof(char) * maxlen);
    new_string = malloc(sizeof(char) * maxlen); /* TODO: this has to be dynamic */
    bzero(new_string, maxlen);
    
    start = * stringp;
    substart = start;
    tok = strstr(start, source);
    while(tok != NULL) {
        *tok = '\0';
        snprintf (buf, maxlen, "%s%s%s", new_string, substart, destination);
        strncpy (new_string, buf, maxlen);
        tok+=strlen(source);
        substart = tok;
        tok = strstr(substart, source);
    }
    snprintf (buf, maxlen, "%s%s", new_string, substart);
    strncpy (new_string, buf, maxlen);
    if (buf) free(buf);

    free (* stringp);
    * stringp = new_string;

    
    return new_string;
}

/* do sscanf() on each line in lines[], returning upon first match */
/* returns 1 if there was match and 0 otherwise */
int sscanf_lines (char * lines, char * format, void * varp) 
{
    char * copy = strdup(lines);
    char * start, * end;
    int found = 0;

    for (start = copy; start && *start!='\0' && !found; start = end+1 ) {
        int newline = 0;

        for (end = start+1; *end!='\n' && *end!='\0'; end++);
        if (*end=='\n') {
            *end='\0';
            newline = 1;
        }

        if (sscanf (start, format, varp)==1) 
            found=1;

        if (!newline) {
            end--; /* so that start=='\0' */
        }
    }
        
    return found;
}

/* execute system(shell_command) and return stdout in new string
 * pointed to by *stringp */
char * system_output (char * shell_command )
{
#   define INCREMENT 512
    int buf_max = INCREMENT;
    int buf_current = 0;
    char * buf = NULL;
    char * last_read;
    FILE * fp;

    /* forks off command (this doesn't fail if command doesn't exist */
    logprintfl (EUCADEBUG, "system_output(): [%s]\n", shell_command);
    if ( (fp=popen(shell_command, "r")) == NULL) 
        return NULL; /* caller can check errno */
    
    do {
        /* create/enlarge the buffer */
        void * new_buf;
        if ((new_buf = realloc (buf, buf_max)) == NULL) {
            if ( buf != NULL ) { /* previous realloc()s worked */
                free (buf); /* free partial buffer */
                buf = NULL;
            }
            break;
        }
        buf = new_buf;
        logprintfl (EUCADEBUG2, "system_output: enlarged buf to %d\n", buf_max);

        do { /* read in output until EOF or buffer is full */
            last_read = fgets (buf+buf_current, buf_max-buf_current, fp);
            if ( last_read != NULL )
                buf_current = strlen(buf);
            logprintfl (EUCADEBUG2, "system_output: read %d characters so far (max=%d, last=%s)\n", buf_current, buf_max, last_read?"no":"yes");
        } while ( last_read && buf_max > buf_current+1 ); /* +1 is needed for fgets() to put \0 */
        
        buf_max += INCREMENT; /* in case it is full */
    } while (last_read);

    if ( buf_current < 1 ) {
        free (buf);
        buf = NULL;
    }
    pclose(fp);
    return buf;
}


char *getConfString(char *configFile, char *key) {
  int rc;
  char *tmpstr;
  
  rc = get_conf_var(configFile, key, &tmpstr);
  if (rc != 1) {
    logprintfl (EUCAWARN, "%s is not defined in config\n", key);
    return(NULL);
  }
  return(tmpstr);
}


/* search for variable #name# in file #path# and return whatever is after
 * = in value (which will need to be freed).
 *
 * Example of what we are able to parse:
 * TEST="test"
 * TEST=test
 *     TEST   = test
 *
 * Return 1 on success, 0 on variable not found and -1 on error (parse or
 * file not found)
 */
int
get_conf_var(	const char *path, 
		const char *name,
		char **value) {
	FILE *f;
	char *buf, *ptr, *ret;
	int len;

	/* sanity check */
	if (path == NULL || path[0] == '\0' || name == NULL  
			|| name[0] == '\0'|| value == NULL) {
		return -1;
	}
	*value = NULL;

	f = fopen(path, "r");
	if (f == NULL) {
		return -1;
	}

	len = strlen(name);
	buf = malloc(sizeof(char) * 4096);
	while (fgets(buf, 4096, f)) {
		/* the process here is fairly simple: spaces are not
		 * considered (unless between "") so we remove them
		 * before every step. We look for the variable *name*
		 * first, then for an = then for the value */
		for (ptr = buf; (*ptr != '\0') && isspace((int)*ptr); ptr++)
			; 
		if (strncmp(ptr, name, len) != 0) {
			continue;
		}
		for (ptr += len; (*ptr != '\0') && isspace((int)*ptr); ptr++)
			;
		if (*ptr != '=') {
			continue;
		}
		/* we are in business */
		for (ptr++; (*ptr != '\0') && isspace((int)*ptr); ptr++)
			;
		if (*ptr == '"') {
			/* we have a quote, we need the companion */
			ret = ++ptr;
			while ((*ptr != '"')) {
				if (*ptr == '\0') {
					/* something wrong happened */
				  fclose(f);
				  free(buf);
				  return -1;
				}
				ptr++;
			}
		} else {
			/* well we get the single word right after the = */
			ret = ptr;
			while (!isspace((int)*ptr) && *ptr != '#' 
					&& *ptr != '\0') {
				ptr++;
			}
		}
		*ptr = '\0';
		*value = strdup(ret);
		if (*value == NULL) {
		  free(buf);
		  return -1;
		}
		fclose(f);
		free(buf);
		return 1;
	}
	fclose(f);
	free(buf);
	return 0;
}

void
free_char_list(char **value) {
	int i;

	if (value == NULL || *value == NULL) {
		return;
	}

	for (i = 0; value[i] != NULL; i++) {
		free(value[i]);
	}
	free(value);
}


char **
from_var_to_char_list(	const char *v) {
	char *value, **tmp, *ptr, *w, a;
	int i;

	/* sanity check */
	if (v == NULL || v[0] == '\0') {
		return NULL;
	}
	tmp = malloc(sizeof(char*));
	value = strdup(v);
	if (tmp == NULL || value == NULL) {
		return NULL;
	}
	tmp[0] = NULL;

	i = 0;
	ptr = value;
	for (i = 0, ptr = value; *ptr != '\0'; ptr++) {
		/* let's look for the beginning of the word */
		for (; *ptr != '\0' && isspace((int)*ptr); ptr++)
			;
		if (*ptr == '\0') {
			/* end of string with no starting word: we are
			 * done here */
			break;
		}
		w = ptr;		/* beginning of word */
		for (ptr++; *ptr != '\0' && !isspace((int)*ptr); ptr++)
			;

		/* found the end of word */
		a = *ptr;
		*ptr = '\0';

		tmp = realloc(tmp, sizeof(char *)*(i+2));
		if (tmp == NULL) {
			free(value);
			return NULL;
		}
		tmp[i] = strdup(w);
		if (tmp[i] == NULL) {
			free_char_list(tmp);
			free(value);
			return NULL;
		}
		tmp[++i] = NULL;

		/* now we need to check if we were at the end of the
		 * string */
		if (a == '\0') {
			break;
		}
	}
	free(value);

	return tmp;
}

int logfile(char *file, int in_loglevel) {
  logging = 0;
  if (in_loglevel >= EUCADEBUG2 && in_loglevel <= EUCAFATAL) {
    loglevel = in_loglevel;
  } else {
    loglevel = EUCADEBUG;
  }
  if (file == NULL) {
    LOGFH = NULL;
  } else {
    if (LOGFH != NULL) {
      fclose(LOGFH);
    }
    
    snprintf(logFile, 1024, "%s", file);
    LOGFH = fopen(file, "a");
    if (LOGFH) {
      logging=1;
    }
  }
  return(1-logging);
}

void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other) {
  double ts;
  struct timeval tv;
  char hostTagFull[256];
  char hostName [256];
  FILE *PH;

  if (!timelog) return;

  hostTagFull[0] = '\0';
  PH = popen("hostname", "r");
  fscanf(PH, "%256s", hostName);
  pclose(PH);
  snprintf (hostTagFull, 256, "%s/%s", hostName, hostTag);
  
  gettimeofday(&tv, NULL);
  ts = (double)tv.tv_sec + ((double)tv.tv_usec / 1000000.0);

  logprintf("TIMELOG %s:%s:%s:%s:%f:%s\n", hostTagFull, userTag, cid, eventTag, ts, other);
}

int logprintf(const char *format, ...) {
  va_list ap;
  int rc;
  char buf[27], *eol;
  time_t t;
  FILE *file;
  
  rc = 1;
  va_start(ap, format);
  
  if (logging) {
    file = LOGFH;
  } else {
    file = stdout;
  }
  
  t = time(NULL);
  if (ctime_r(&t, buf)) {
    eol = strchr(buf, '\n');
    if (eol) {
      *eol = '\0';
    }
    fprintf(file, "[%s] ", buf);
  }
  rc = vfprintf(file, format, ap);
  fflush(file);
  
  va_end(ap);
  return(rc);
}

int logprintfl(int level, const char *format, ...) {
  va_list ap;
  int rc, fd;
  char buf[27], *eol;
  time_t t;
  struct stat statbuf;
  FILE *file;
  
  if (level < loglevel) {
    return(0);
  }
  
  rc = 1;
  va_start(ap, format);
  
  if (logging) {
    file = LOGFH;
    fd = fileno(file);
    if (fd > 0) {
      rc = fstat(fd, &statbuf);
      if (!rc && ((int)statbuf.st_size > MAXLOGFILESIZE)) {
	int i;
	char oldFile[1024], newFile[1024];
	
	rc = stat(logFile, &statbuf);
	if (!rc && ((int)statbuf.st_size > MAXLOGFILESIZE)) {
	  for (i=4; i>=0; i--) {
	    snprintf(oldFile, 1024, "%s.%d", logFile, i);
	    snprintf(newFile, 1024, "%s.%d", logFile, i+1);
	    rename(oldFile, newFile);
	  }
	  snprintf(oldFile, 1024, "%s", logFile);
	  snprintf(newFile, 1024, "%s.%d", logFile, 0);
	  rename(oldFile, newFile);
	}
	fclose(LOGFH);
	LOGFH = fopen(logFile, "a");
	if (LOGFH) {
	  file = LOGFH;
	} else {
	  file = stdout;
	}
      }
    }
  } else {
    file = stdout;
  }

  
  t = time(NULL);
  if (ctime_r(&t, buf)) {
    eol = strchr(buf, '\n');
    if (eol) {
      *eol = '\0';
    }
    fprintf(file, "[%s]", buf);
  }

  fprintf(file, "[%06d]", getpid());
  if (level == EUCADEBUG2) {fprintf(file, "[%-10s] ", "EUCADEBUG2");}
  else if (level == EUCADEBUG) {fprintf(file, "[%-10s] ", "EUCADEBUG");}
  else if (level == EUCAINFO) {fprintf(file, "[%-10s] ", "EUCAINFO");}
  else if (level == EUCAWARN) {fprintf(file, "[%-10s] ", "EUCAWARN");}
  else if (level == EUCAERROR) {fprintf(file, "[%-10s] ", "EUCAERROR");}
  else if (level == EUCAFATAL) {fprintf(file, "[%-10s] ", "EUCAFATAL");}
  else {fprintf(file, "[%-10s] ", "EUCADEBUG");}
  rc = vfprintf(file, format, ap);
  fflush(file);
  
  va_end(ap);
  return(rc);
}

/* implements Java's String.hashCode() */
int hash_code (const char * s)
{
	int code = 0;
	int i;
	
	if (!s) return code;
	
	int len = strlen((char *)s);
	for (i = 0; i<len; i++) {
		code = 31 * code + (unsigned char) s[i];
	}
	
	return code;
}

/* given a string, returns 3 relevant statistics as a static string */
#define OUTSIZE 50
char * get_string_stats (const char * s)
{
	static char out [OUTSIZE]; // TODO: malloc this?
	int len = (int)strlen(s);
	snprintf (out, OUTSIZE, "length=%d buf[n-1]=%i hash=%d", len, (int)((signed char)s[len-1]), hash_code(s));
	return out;
}

#define BUFSIZE 1024

/* daemonize and run 'cmd', returning pid of the daemonized process */
int daemonrun(char *cmd, int *dpid) {
  int pid, sid, i;
  char **argv=NULL;

  *dpid = -1;
  pid = fork();
  if (pid < 0) {
    return(1);
  }

  if (!pid) {
    char *tok, *ptr;
    int idx;

    // become parent of session                                                 
    sid = setsid();

    // construct argv                                                           
    idx=0;
    argv = realloc(NULL, sizeof(char *));
    tok = strtok_r(cmd, " ", &ptr);
    while(tok) {
      argv[idx] = strdup(tok);
      idx++;
      tok = strtok_r(NULL, " ", &ptr);
      argv = realloc(argv, sizeof(char *) * (idx+1));
    }
    argv[idx] = NULL;

    // close all fds                                                            
    for (i=0; i<sysconf(_SC_OPEN_MAX); i++) {
      close(i);
    }

    // run                                                                      
    exit(execvp(*argv, argv));
  }

  *dpid = pid;
  return(0);
}

/* given printf-style arguments, run the resulting string in the shell */
int vrun (const char * fmt, ...)
{
	char buf [BUFSIZE];
	int e;
	
	va_list ap;
    va_start (ap, fmt);
	vsnprintf (buf, BUFSIZE, fmt, ap);
	va_end (ap);
	
    logprintfl (EUCAINFO, "vrun(): [%s]\n", buf);
	if ((e = system (buf)) != 0) {
		logprintfl (EUCAERROR, "system(%s) failed with %d\n", buf, e); /* TODO: remove? */
	}
	return e;
}

/* given a file path, prints it to stdout */
int cat (const char * file_name)
{
	int got;
	int put = 0;
	char buf [BUFSIZE];
	
	int fd = open (file_name, O_RDONLY);
	while ( ( got = read (fd, buf, BUFSIZE)) > 0) {
		put += write (1, buf, got);
	}
	close (fd);
	return put;
}

/* prints contents of a file with logprintf */
int logcat (int debug_level, const char * file_name)
{
	int got = 0;
	char buf [BUFSIZE];
	
	FILE *fp = fopen (file_name, "r");
    while ( fgets (buf, BUFSIZE, fp) ) {
        int l = strlen (buf);
        if ( l<0 ) 
            break;
        if ( l+1<BUFSIZE && buf[l-1]!='\n' ) {
            buf [l++] = '\n';
            buf [l] = '\0';
        }
        logprintfl (debug_level, buf);
        got += l;
	}
    fclose (fp);
	return got;
}

/* "touch" a file, creating if necessary */
int touch (const char * path)
{
    int ret = 0;
    int fd;
    
    if ( (fd = open (path, O_WRONLY | O_CREAT | O_NONBLOCK, 0644)) >= 0 ) {
        close (fd);
        if (utime (path, NULL)!=0) {
            logprintfl (EUCAERROR, "error: touch(): failed to adjust time for %s (%s)\n", path, strerror (errno));
            ret = 1;
        }
    } else {
        logprintfl (EUCAERROR, "error: touch(): failed to create/open file %s (%s)\n", path, strerror (errno));
        ret = 1;
    }
    return ret;
}

/* diffs two files: 0=same, -N=different, N=error */
int diff (const char * path1, const char * path2)
{
    int fd1, fd2;
    char buf1 [BUFSIZE], buf2 [BUFSIZE];

    if ( (fd1 = open (path1, O_RDONLY)) < 0 ) {
        logprintfl (EUCAERROR, "error: diff(): failed to open %s\n", path1);
    } else if ( (fd2 = open (path2, O_RDONLY)) < 0 ) {
        logprintfl (EUCAERROR, "error: diff(): failed to open %s\n", path2);
    } else {
        int read1, read2;
        do {
            read1 = read (fd1, buf1, BUFSIZE);
            read2 = read (fd2, buf2, BUFSIZE);
            if (read1!=read2) break;
            if (read1 && memcmp (buf1, buf2, read1)) break;
        } while (read1);
        close (fd1);
        close (fd2);
        return (-(read1 + read2)); /* both should be 0s if files are equal */
    }
    return ERROR;
}

/* sums up sizes of files in the directory, as well as the size of the
 * directory itself; no subdirectories are allowed - if there are any, this
 * returns -1 */
long long dir_size (const char * path)
{
    struct stat mystat;
    DIR * dir;
    long long size = 0;

    if ((dir=opendir(path))==NULL) {
        logprintfl (EUCAWARN, "warning: unopeneable directory %s\n", path);
        return -1;
    }
    if (stat (path, &mystat) < 0) {
        logprintfl (EUCAWARN, "warning: could not stat %s\n", path);
        return -1;
    }
    size += (long long)mystat.st_size;

    struct dirent * dir_entry;
    while ((dir_entry=readdir(dir))!=NULL) {
        char * name = dir_entry->d_name;
        unsigned char type = dir_entry->d_type;

        if (!strcmp(".", name) ||
            !strcmp("..", name))
            continue;

        if (DT_REG!=type) {
            logprintfl (EUCAWARN, "warning: non-regular (type=%d) file %s/%s\n", type, path, name);
            size = -1;
            break;
        }
        
        char filepath [BUFSIZE];
        snprintf (filepath, BUFSIZE, "%s/%s", path, name);
        if (stat (filepath, &mystat) < 0 ) {
            logprintfl (EUCAWARN, "warning: could not stat file %s\n", filepath);
            size = -1;
            break;
        }
        
        size += (long long)mystat.st_size;
    }

    closedir (dir);
    return size;
}

int write2file(const char *path, char *str) {
  FILE *FH=NULL;
  
  FH = fopen(path, "w");
  if (FH) {
    fprintf(FH, "%s", str);
    fclose(FH);
  } else {
    return(1);
  }
  return(0);
}

/* read file 'path' into a new string */
char * file2str (const char * path)
{
    char * content = NULL;
    int file_size;

    struct stat mystat;
    if (stat (path, &mystat) < 0) {
        logprintfl (EUCAERROR, "error: file2str() could not stat file %s\n", path);
        return content;
    }
    file_size = mystat.st_size;

    if ( (content = malloc (file_size+1)) == NULL ) {
        logprintfl (EUCAERROR, "error: file2str() out of memory reading file %s\n", path);
        return content;
    }

    int fp;
    if ( ( fp = open (path, O_RDONLY) ) < 1 ) {
        logprintfl (EUCAERROR, "error: file2str() failed to open file %s\n", path);
        free (content);
        content = NULL;
        return content;
    }

    int bytes;
    int bytes_total = 0;
    int to_read = (SSIZE_MAX)<file_size?(SSIZE_MAX):file_size;
    char * p = content;
    while ( (bytes = read (fp, p, to_read)) > 0) {
        bytes_total += bytes;
        p += bytes;
        if (to_read > (file_size-bytes_total)) {
            to_read = file_size-bytes_total;
        }
    }

    if ( bytes < 0 ) {
        logprintfl (EUCAERROR, "error: file2str() failed to read file %s\n", path);
        free (content);
        content = NULL;
        return content;
    }

    * p = '\0';
    return content;
}

/* extract integer from str bound by 'begin' and 'end' */
long long str2longlong (const char * str, const char * begin, const char * end)
{
    long long value = -1L;

    if ( str==NULL || begin==NULL || end==NULL || strlen (str)<3 || strlen (begin)<1 || strlen (end)<1 ) {
        logprintfl (EUCAERROR, "error: str2int() called with bad parameters\n");
        return value;
    }

    char * b = strstr ( str, begin );
    if ( b==NULL ) {
        logprintfl (EUCAERROR, "error: str2int() beginning string '%s' not found\n", begin);
        return value;
    }

    char * e = strstr ( str, end );
    if ( e==NULL ) {
        logprintfl (EUCAERROR, "error: str2int() end string '%s' not found\n", end);
        return value;
    }

    b += strlen (begin); // b now points at the supposed number
    int len = e-b;
    if ( len < 0 ) {
        logprintfl (EUCAERROR, "error: str2int() there is nothing between '%s' and '%s'\n", begin, end);
        return value;
    }

    if ( len > BUFSIZE-1 ) {
        logprintfl (EUCAERROR, "error: str2int() string between '%s' and '%s' is too long\n", begin, end);
        return value;
    }

    char buf [BUFSIZE];
    strncpy (buf, b, len);
    value = atoll (buf);

    return value;
}

int uint32compar(const void *ina, const void *inb) {
  uint32_t a, b;
  a = *(uint32_t *)ina;
  b = *(uint32_t *)inb;
  if (a < b) {
    return(-1);
  } else if (a > b) {
    return(1);
  }
  return(0);
}
