/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h> // close, stat
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h> // open
#include <ctype.h> // tolower, isdigit
#include <sys/types.h> // stat
#include <sys/stat.h> // stat 
#include <curl/curl.h>
#include <curl/easy.h>

#ifndef _UNIT_TEST // http_ functions aren't part of the unit test
#include "config.h"
#include "eucalyptus.h"
#include "misc.h"
#include "http.h"

#define TOTAL_RETRIES 3 /* download is retried in case of connection problems */
#define FIRST_TIMEOUT 4 /* in seconds, goes in powers of two afterwards */
#define STRSIZE 245 /* for short strings: files, hosts, URLs */

static size_t read_data (char *bufptr, size_t size, size_t nitems, void *userp);

struct read_request {
	FILE * fp; /* input file pointer to be used by curl READERs */
    long long total_read; /* bytes written during the operation */
    long long total_calls; /* write calls made during the operation */
    time_t timestamp; // timestamp for periodically printing progress messages
    long long file_size; // file size in bytes, to print in progress messages
};

struct write_request {
	FILE * fp; /* output file pointer to be used by curl WRITERs */
    long long total_wrote; /* bytes written during the operation */
    long long total_calls; /* write calls made during the operation */
#if defined (CAN_GZIP)
	z_stream strm; /* stream struct used by zlib */
	int ret; /* return value of last inflate() call */
#endif
};

static int curl_initialized = 0;

int http_put (const char * file_path, const char * url, const char * login, const char * password) 
{
    int code = ERROR;

    if (curl_initialized!=1) {
        curl_global_init(CURL_GLOBAL_SSL);
        curl_initialized = 1;
    }

    struct stat64 mystat;
    if (stat64 (file_path, &mystat)) {
        logprintfl (EUCAERROR, "http_put(): failed to stat %s\n", file_path);
		return code;
    }
    if (!S_ISREG(mystat.st_mode)) {
        logprintfl (EUCAERROR, "http_put(): %s is not a regular file\n", file_path);
		return code;
    }

	FILE * fp = fopen64 (file_path, "r");
	if (fp==NULL) {
		logprintfl (EUCAERROR, "http_put(): failed to open %s for reading\n", file_path);
		return code;
	}

	CURL * curl;
	CURLcode result;
	curl = curl_easy_init ();
	if (curl==NULL) {
		logprintfl (EUCAERROR, "http_put(): could not initialize libcurl\n");
		fclose (fp);
		return code;
	}

    logprintfl (EUCAINFO, "http_put(): uploading %s\n", file_path);
    logprintfl (EUCAINFO, "            to %s\n", url);

	char error_msg [CURL_ERROR_SIZE];
	curl_easy_setopt (curl, CURLOPT_ERRORBUFFER, error_msg);
	curl_easy_setopt (curl, CURLOPT_URL, url); 
    curl_easy_setopt (curl, CURLOPT_UPLOAD, 1L);
    curl_easy_setopt (curl, CURLOPT_INFILESIZE_LARGE, (curl_off_t)mystat.st_size);
    curl_easy_setopt (curl, CURLOPT_SSL_VERIFYPEER, 0L); // TODO: make this optional?
    curl_easy_setopt (curl, CURLOPT_SSL_VERIFYHOST, 0L);
    curl_easy_setopt (curl, CURLOPT_LOW_SPEED_LIMIT, 360L); // must have at least a 360 baud modem
    curl_easy_setopt (curl, CURLOPT_LOW_SPEED_TIME, 10L); // abort if below speed limit for this many seconds

    if (login!=NULL && password!=NULL) {
        char userpwd [STRSIZE];
        snprintf (userpwd, STRSIZE, "%s:%s", login, password);
        curl_easy_setopt (curl, CURLOPT_USERPWD, userpwd);
    }

	struct read_request params;
    params.fp = fp;
    params.timestamp = time(NULL);
    params.file_size = (long long)mystat.st_size;
    curl_easy_setopt (curl, CURLOPT_READDATA, &params);
    curl_easy_setopt (curl, CURLOPT_READFUNCTION, read_data);

    int retries = TOTAL_RETRIES;
    int timeout = FIRST_TIMEOUT;
    do {
        params.total_read = 0L;
        params.total_calls = 0L;
        result = curl_easy_perform (curl); /* do it */
        logprintfl (EUCADEBUG, "http_put(): uploaded %ld bytes in %ld sends\n", params.total_read, params.total_calls);

        if (result) { // curl error (connection or transfer failed)
            logprintfl (EUCAERROR,     "http_put(): %s (%d)\n", error_msg, result);

        } else {
            long httpcode;
            curl_easy_getinfo (curl, CURLINFO_RESPONSE_CODE, &httpcode);
            // TODO: pull out response message, too?
            
            switch (httpcode) {
            case 200L: // all good
                logprintfl (EUCAINFO, "http_put(): file updated sucessfully\n");
                code = OK;
                break;
            case 201L: // all good, created
                logprintfl (EUCAINFO, "http_put(): file created sucessfully\n");
                code = OK;
                break;
            case 408L: // timeout, retry
                logprintfl (EUCAWARN, "http_put(): server responded with HTTP code %ld (timeout)\n", httpcode);
                break;
	    case 500L: // internal server error (could be a fluke, so we'll retry)
	      logprintfl (EUCAWARN, "http_put(): server responded with HTTP code %ld (transient?)\n", httpcode);
                break;
            default: // some kind of error, will not retry
                logprintfl (EUCAERROR, "http_put(): server responded with HTTP code %ld\n", httpcode);
                retries = 0;
            }
        }

        if (code!=OK && retries > 0) {
            logprintfl (EUCAERROR, "            upload retry %d of %d will commence in %d seconds\n", TOTAL_RETRIES-retries+1, TOTAL_RETRIES, timeout);
            sleep (timeout);
            fseek (fp, 0L, SEEK_SET);
            timeout <<= 1;
        }

        retries--;
    } while (code!=OK && retries>0);
    fclose (fp);
    
	curl_easy_cleanup (curl);
    return code;
}

/* libcurl read handler */
static size_t read_data (char *buffer, size_t size, size_t nitems, void *params)
{
    assert (params != NULL);

    FILE * fp = ((struct read_request *)params)->fp;
    int items_read = 0;
    do {
        items_read += fread (buffer, size, nitems-items_read, fp);
    } while (items_read!=nitems && !feof(fp));
        
    ((struct read_request *)params)->total_read += items_read * size;
    ((struct read_request *)params)->total_calls++;

    if (((struct read_request *)params)->total_calls%50==0) {
        time_t prev = ((struct read_request *)params)->timestamp;
        time_t now = time(NULL);
        if ((now-prev)>10) {
            ((struct read_request *)params)->timestamp = now;
            long long bytes_read = ((struct read_request *)params)->total_read;
            long long bytes_file = ((struct read_request *)params)->file_size;
            int percent = (int)((bytes_read*100)/bytes_file);
            logprintfl (EUCADEBUG, "http_put(): upload progress %ld/%ld bytes (%d%%)\n", bytes_read, bytes_file, percent);
        }
    }

    return items_read;
}

/* libcurl write handler */
static size_t write_data (void *buffer, size_t size, size_t nmemb, void *params)
{
	assert (params !=NULL);
	FILE * fp = ((struct write_request *)params)->fp;
	int wrote = fwrite (buffer, size, nmemb, fp);
    ((struct write_request *)params)->total_wrote += wrote;
    ((struct write_request *)params)->total_calls++;

	return wrote;
}

#endif

// converts hex character to integer
static char hch_to_int (char ch) {
  return isdigit (ch) ? (ch - '0') : (10 + tolower (ch) - 'a');
}

// converts integer to hex character
static char int_to_hch (char i) {
  static char hex[] = "0123456789ABCDEF";
  return hex [i & 15];
}

// converts a string to url-encoded string (which must be freed)
char * url_encode (const char * unencoded) {
  char * encoded = malloc (strlen (unencoded) * 3 + 1);
  if (encoded==NULL) return NULL;

  const char * pu = unencoded;
  char * pe = encoded;  
  while (*pu) {
    if (isalnum (*pu) 
	|| *pu == '-' 
	|| *pu == '_' 
	|| *pu == '.' 
	|| *pu == '~') 
      *pe++ = *pu;
    else if (*pu == ' ') 
      *pe++ = '+';
    else {
      *pe++ = '%';
      *pe++ = int_to_hch (*pu >> 4);
      *pe++ = int_to_hch (*pu & 15);
    }
    pu++;
  }
  *pe = '\0';

  return encoded;
}

// converts a url-encoded string to regular (which must be freed)
char * url_decode (const char * encoded) {
  char * unencoded = malloc (strlen (encoded) + 1);
  if (unencoded==NULL) return NULL;
  
  const char * pe = encoded;
  char * pu = unencoded;
  while (*pe) {
    if (*pe == '%') {
      if (pe[1] && pe[2]) {
        *pu++ = hch_to_int (pe[1]) << 4 | hch_to_int (pe[2]);
        pe += 2;
      }
    } else if (*pe == '+') { 
      *pu++ = ' ';
    } else {
      *pu++ = *pe;
    }
    pe++;
  }
  *pu = '\0';
  
  return unencoded;
}

int http_get (const char * url, const char * outfile)
{
  return(http_get_timeout(url, outfile, TOTAL_RETRIES, FIRST_TIMEOUT, 0, 0));
}

int http_get_timeout (const char * url, const char * outfile, int total_retries, int first_timeout, int connect_timeout, int total_timeout)
{
	int code = ERROR;

	logprintfl (EUCAINFO, "http_get(): downloading %s\n", outfile);
	logprintfl (EUCAINFO, "            from %s\n", url);

	/* isolate the PATH in the URL as it will be needed for signing */
	if (strncasecmp (url, "http://", 7)!=0) {
		logprintfl (EUCAERROR, "http_get(): URL must start with http://...\n");
		return code;
	}

	FILE * fp = fopen64 (outfile, "w");
	if (fp==NULL) {
		logprintfl (EUCAERROR, "http_get(): failed to open %s for writing\n", outfile);
		return code;
	}

	CURL * curl;
	CURLcode result;
	curl = curl_easy_init ();
	if (curl==NULL) {
		logprintfl (EUCAERROR, "http_get(): could not initialize libcurl\n");
		fclose(fp);
		return code;
	}

	char error_msg [CURL_ERROR_SIZE];
	curl_easy_setopt (curl, CURLOPT_ERRORBUFFER, error_msg);
	curl_easy_setopt (curl, CURLOPT_URL, url); 
	//	curl_easy_setopt (curl, CURLOPT_HEADERFUNCTION, write_header);
	
        curl_easy_setopt (curl, CURLOPT_HTTPGET, 1L);
	
	/* set up the default write function, but possibly override it below, if compression is desired and possible */
	struct write_request params;
	params.fp = fp;
	curl_easy_setopt (curl, CURLOPT_WRITEDATA, &params);
	curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data);

	if (connect_timeout > 0) {
	  curl_easy_setopt (curl, CURLOPT_CONNECTTIMEOUT, connect_timeout);
	}
	if (total_timeout > 0) {
	  curl_easy_setopt (curl, CURLOPT_TIMEOUT, total_timeout);
	}
	//	curl_easy_setopt (curl, CURLOPT_HTTPHEADER, headers); /* register headers */

        logprintfl (EUCADEBUG, "http_get(): writing %s output to %s\n", "GET", outfile);

	int retries = total_retries;
	int timeout = first_timeout;
	do {
	  params.total_wrote = 0L;
	  params.total_calls = 0L;

	  result = curl_easy_perform (curl); /* do it */
	  logprintfl (EUCADEBUG, "http_get(): wrote %ld bytes in %ld writes\n", params.total_wrote, params.total_calls);


	  if (result) { // curl error (connection or transfer failed)
            logprintfl (EUCAERROR,     "http_get(): %s (%d)\n", error_msg, result);
	    
	  } else {
            long httpcode;
            curl_easy_getinfo (curl, CURLINFO_RESPONSE_CODE, &httpcode);
            /* TODO: pull out response message, too */

            switch (httpcode) {
            case 200L: /* all good */
                logprintfl (EUCAINFO, "http_get(): saved image in %s\n", outfile);
                code = OK;
                break;
	    case 408L: /* timeout, retry */
	      logprintfl (EUCAWARN, "http_get(): server responded with HTTP code %ld (timeout)\n", httpcode);
	      //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
	      break;
	    case 404L:
	      logprintfl (EUCAWARN, "http_get(): server responded with HTTP code %ld (file not found)\n", httpcode);
	      break;
            default: /* some kind of error */
                logprintfl (EUCAERROR, "http_get(): server responded with HTTP code %ld\n", httpcode);
                //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
                retries=0;
            }
	  }
        
	  if (code!=OK && retries>0) {
            logprintfl (EUCAERROR, "                  download retry %d of %d will commence in %d seconds\n", retries, total_retries, timeout);
            sleep (timeout);
            fseek (fp, 0L, SEEK_SET);
            timeout <<= 1;
	  }
        
	  retries--;
	} while (code!=OK && retries>0);
	fclose (fp);
	
	if ( code != OK ) {
	  logprintfl (EUCAINFO, "http_get(): due to error, removing %s\n", outfile);
	  remove (outfile);
	}
	
	//	curl_slist_free_all (headers);
	curl_easy_cleanup (curl);
	return code;
}

#ifdef _UNIT_TEST
int main (int argc, char ** argv)
{
#define _T(_S) { char * e = url_encode (_S); char * u = url_decode (e); printf ("orig: %s\nenco: %s\ndeco: %s\n\n", _S, e, u); free (e); free (u); }
  _T("hello world");
  _T("~`!1@2#3$4%5^6&7*8(9)0_-+={[}]|\\:;\"'<,>.?/");
  _T("[datastore1 (1)] windows 2003 enterprise/windows 2003 enterprise.vmx");
}
#endif
