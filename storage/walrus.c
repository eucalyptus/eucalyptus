// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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

#include "config.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>             /* close */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h>              /* open */
#include <curl/curl.h>
#include <curl/easy.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/stat.h>
#if defined(HAVE_ZLIB_H)
#include <zlib.h>
#endif
#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"

#define TOTAL_RETRIES 10        // download is retried in case of connection problems
#define FIRST_TIMEOUT 4         // in seconds, goes in powers of two afterwards
#define MAX_TIMEOUT 300         // in seconds, the cap for growing timeout values
#define CHUNK 262144            // buffer size for decompression operations
#define BUFSIZE 262144          // should be big enough for CERT and the signature
#define STRSIZE 1024            // for short strings: files, hosts, URLs
#define WALRUS_ENDPOINT "/services/Walrus"
#define DEFAULT_HOST_PORT "localhost:8773"
#define GET_IMAGE_CMD "GetDecryptedImage"
#define GET_OBJECT_CMD "GetObject"

static size_t write_data(void *buffer, size_t size, size_t nmemb, void *userp);
static size_t write_header(void *buffer, size_t size, size_t nmemb, void *userp);
#if defined(ZLIB_VERNUM) && (ZLIB_VERNUM >= 0x1204)
static size_t write_data_zlib(void *buffer, size_t size, size_t nmemb, void *userp);
static void zerr(int ret, char *where);
#define CAN_GZIP
#endif

struct request {
    int fd;                     /* output file descriptor to be used by curl WRITERs */
    long long total_wrote;      /* bytes written during the operation */
    long long total_calls;      /* write calls made during the operation */
#if defined (CAN_GZIP)
    z_stream strm;              /* stream struct used by zlib */
    int ret;                    /* return value of last inflate() call */
#endif
};

/* walrus_request internal lock to prevent apparent race in curl ssl dependency */
static pthread_mutex_t wreq_mutex = PTHREAD_MUTEX_INITIALIZER;

/* downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
static int fallback_walrus_request_timeout (const char * walrus_op, const char * verb, const char * requested_url, const char * outfile, const int do_compress, int connect_timeout, int total_timeout)
{
    int code = ERROR;
    char url [BUFSIZE];
    
    pthread_mutex_lock(&wreq_mutex); /* lock for curl construction */

    safe_strncpy(url, requested_url, BUFSIZE);
#if defined(CAN_GZIP)
    if (do_compress)
        snprintf(url, BUFSIZE, "%s%s", requested_url, "?IsCompressed=true");
#endif

    /* isolate the PATH in the URL as it will be needed for signing */
    char *url_path;
    if (strncasecmp(url, "http://", 7) != 0 && strncasecmp(url, "https://", 8) != 0) {
        logprintfl(EUCAERROR, "Walrus URL must start with http(s)://...\n");
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }
    if ((url_path = strchr(url + 8, '/')) == NULL) {    /* find first '/' after hostname */
        logprintfl(EUCAERROR, "Walrus URL has no path\n");
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    if (euca_init_cert()) {
        logprintfl(EUCAERROR, "failed to initialize certificate for Walrus request\n");
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    int fd = open(outfile, O_CREAT | O_WRONLY, S_IRUSR | S_IWUSR);  // we do not truncate the file
    if (fd == -1 || lseek(fd, 0, SEEK_SET) == -1) {
        logprintfl(EUCAERROR, "failed to open %s for writing Walrus request\n", outfile);
        pthread_mutex_unlock(&wreq_mutex);
        if (fd >= 0)
            close(fd);
        return code;
    }

    logprintfl(EUCADEBUG, "will use URL: %s\n", url);

    CURL *curl;
    CURLcode result;
    curl = curl_easy_init();
    if (curl == NULL) {
        logprintfl(EUCAERROR, "could not initialize libcurl for Walrus request\n");
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    char error_msg[CURL_ERROR_SIZE];
    curl_easy_setopt(curl, CURLOPT_ERRORBUFFER, error_msg);
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, write_header);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L); // TODO: make this optional?
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_LIMIT, 360L);  // must have at least a 360 baud modem
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_TIME, 10L);    // abort if below speed limit for this many seconds
    // curl_easy_setopt (curl, CURLOPT_FOLLOWLOCATION, 1); // TODO: remove the comment once we want to follow redirects (e.g., on HTTP 407)

    if (strncmp(verb, "GET", 4) == 0) {
        curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L);
    } else if (strncmp(verb, "HEAD", 5) == 0) {
        /* TODO: HEAD isn't very useful atm since we don't look at headers */
        curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
    } else {
        close(fd);
        logprintfl(EUCAERROR, "invalid HTTP verb %s in Walrus request\n", verb);
        pthread_mutex_unlock(&wreq_mutex);
        return ERROR;           /* TODO: dealloc structs before returning! */
    }

    if (connect_timeout > 0) {
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, connect_timeout);
    }
    if (total_timeout > 0) {
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, total_timeout);
    }

    /* set up the default write function, but possibly override
     * it below, if compression is desired and possible */
    struct request params;
    params.fd = fd;
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &params);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
#if defined(CAN_GZIP)
    if (do_compress) {
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data_zlib);
    }
#endif

    struct curl_slist *headers = NULL;  /* beginning of a DLL with headers */
    headers = curl_slist_append(headers, "Authorization: Euca");

    char op_hdr[STRSIZE];
    if (walrus_op != NULL) {
        snprintf(op_hdr, STRSIZE, "EucaOperation: %s", walrus_op);
        headers = curl_slist_append(headers, op_hdr);
    }

    time_t t = time(NULL);
    char date_str[26];
    if (ctime_r(&t, date_str) == NULL) {
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return ERROR;
    }
    assert(strlen(date_str) + 7 <= STRSIZE);
    char *newline = strchr(date_str, '\n');
    if (newline != NULL) {
        *newline = '\0';
    }                           // remove newline that terminates asctime() output
    char date_hdr[STRSIZE];
    snprintf(date_hdr, STRSIZE, "Date: %s", date_str);
    headers = curl_slist_append(headers, date_hdr);

    char *cert_str = euca_get_cert(0);  /* read the cloud-wide cert */
    if (cert_str == NULL) {
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return ERROR;
    }
    char *cert64_str = base64_enc((unsigned char *)cert_str, strlen(cert_str));
    assert(strlen(cert64_str) + 11 <= BUFSIZE);
    char cert_hdr[BUFSIZE];
    snprintf(cert_hdr, BUFSIZE, "EucaCert: %s", cert64_str);
    logprintfl(EUCATRACE, "base64 certificate: %s\n", get_string_stats(cert64_str));
    headers = curl_slist_append(headers, cert_hdr);
    free(cert64_str);
    free(cert_str);

    char *sig_str = euca_sign_url(verb, date_str, url_path);    /* create Walrus-compliant sig */
    if (sig_str == NULL) {
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return ERROR;
    }
    assert(strlen(sig_str) + 16 <= BUFSIZE);
    char sig_hdr[BUFSIZE];
    snprintf(sig_hdr, BUFSIZE, "EucaSignature: %s", sig_str);
    headers = curl_slist_append(headers, sig_hdr);

    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);    /* register headers */
    if (walrus_op) {
        logprintfl(EUCADEBUG, "writing %s/%s output\n", verb, walrus_op);
        logprintfl(EUCADEBUG, "        from %s\n", url);
        logprintfl(EUCADEBUG, "        to %s\n", outfile);
    } else {
        logprintfl(EUCADEBUG, "writing %s output to %s\n", verb, outfile);
    }
    int retries = TOTAL_RETRIES;
    int timeout = FIRST_TIMEOUT;
    do {
        params.total_wrote = 0L;
        params.total_calls = 0L;
#if defined(CAN_GZIP)
        if (do_compress) {
            /* allocate zlib inflate state */
            params.strm.zalloc = Z_NULL;
            params.strm.zfree = Z_NULL;
            params.strm.opaque = Z_NULL;
            params.strm.avail_in = 0;
            params.strm.next_in = Z_NULL;
            params.ret = inflateInit2(&(params.strm), 31);
            if (params.ret != Z_OK) {
                zerr(params.ret, "walrus_request");
                break;
            }
        }
#endif

        // There used to be a 'pthread_mutex_unlock(&wreq_mutex)' before curl invocation
        // and a 'lock' after it, but under heavy load we were seeing failures inside
        // libcurl code that would propagate to NC, implying lack of thread safety in
        // the library. For now, we will serialize all curl operations, but in the future
        // an approach to parallelizing Walrus downloads is necessary (TODO)
        result = curl_easy_perform(curl);   /* do it */
        logprintfl(EUCADEBUG, "wrote %lld byte(s) in %lld write(s)\n", params.total_wrote, params.total_calls);

#if defined(CAN_GZIP)
        if (do_compress) {
            inflateEnd(&(params.strm));
            if (params.ret != Z_STREAM_END) {
                zerr(params.ret, "walrus_request");
            }
        }
#endif

        if (result) {           // curl error (connection or transfer failed)
            logprintfl(EUCAERROR, "curl error: %s (%d)\n", error_msg, result);

        } else {
            long httpcode;
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
            /* TODO: pull out response message, too */

            switch (httpcode) {
            case 200L:         /* all good */
                logprintfl(EUCAINFO, "downloaded %s\n", outfile);
                code = OK;
                break;
            case 408L:         /* timeout, retry */
                logprintfl(EUCAWARN, "server responded with HTTP code %ld (timeout) for %s\n", httpcode, url);
                //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
                break;
            default:           /* some kind of error */
                logprintfl(EUCAERROR, "server responded with HTTP code %ld for %s\n", httpcode, url);
                //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
                retries = 0;
            }
        }

        if (code != OK && retries > 0) {
            logprintfl(EUCAWARN, "download retry %d of %d will commence in %d sec for %s\n", retries, TOTAL_RETRIES, timeout, url);
            sleep(timeout);
            lseek(fd, 0L, SEEK_SET);
            timeout <<= 1;
            if (timeout > MAX_TIMEOUT)
                timeout = MAX_TIMEOUT;
        }

        retries--;
    } while (code != OK && retries > 0);
    close(fd);

    if (code != OK) {
        logprintfl(EUCAWARN, "removing %s\n", outfile);
        remove(outfile);
    }

    free(sig_str);
    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    pthread_mutex_unlock(&wreq_mutex);
    return code;
}


/*
 * Uses EucaV2 signing for the request. We keep both functions to enable backwards compatibility.
 *
 * downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
static int walrus_request_timeout (const char * walrus_op, const char * verb, const char * requested_url, const char * outfile, const int do_compress, int connect_timeout, int total_timeout)
{
    int code = ERROR;
    char url [BUFSIZE];

    pthread_mutex_lock(&wreq_mutex); /* lock for curl construction */

    safe_strncpy (url, requested_url, BUFSIZE);
#if defined(CAN_GZIP)
    if (do_compress)
        snprintf (url, BUFSIZE, "%s%s", requested_url, "?IsCompressed=true");
#endif

    /* isolate the PATH in the URL as it will be needed for signing */
    char * url_path;
    if (strncasecmp (url, "http://", 7)!=0 &&
        strncasecmp (url, "https://", 8)!=0) {
        logprintfl (EUCAERROR, "{%u} walrus_request: URL must start with http(s)://...\n",(unsigned int)pthread_self());
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    if ((url_path=strchr(url+8, '/'))==NULL) { /* find first '/' after hostname */
        logprintfl (EUCAERROR, "{%u} walrus_request: URL has no path\n",(unsigned int)pthread_self());
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    if (euca_init_cert()) {
        logprintfl (EUCAERROR, "{%u} walrus_request: failed to initialize certificate\n",(unsigned int)pthread_self());
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    int fd = open (outfile, O_CREAT | O_WRONLY, S_IRUSR | S_IWUSR); // we do not truncate the file
    if (fd==-1 || lseek (fd, 0, SEEK_SET)==-1) {
        logprintfl (EUCAERROR, "{%u} walrus_request: failed to open %s for writing\n", (unsigned int)pthread_self(), outfile);
        pthread_mutex_unlock(&wreq_mutex);
        if(fd >= 0) close(fd);
        return code;
    }

    logprintfl(EUCADEBUG, "{%u} walrus_request: calling URL=%s\n", (unsigned int)pthread_self(), url);

    CURL * curl;
    CURLcode result;
    curl = curl_easy_init ();
    if (curl==NULL) {
        logprintfl (EUCAERROR, "{%u} walrus_request: could not initialize libcurl\n",(unsigned int)pthread_self());
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    char error_msg [CURL_ERROR_SIZE];
    curl_easy_setopt (curl, CURLOPT_ERRORBUFFER, error_msg);
    curl_easy_setopt (curl, CURLOPT_URL, url);
    curl_easy_setopt (curl, CURLOPT_HEADERFUNCTION, write_header);
    curl_easy_setopt (curl, CURLOPT_SSL_VERIFYPEER, 0L); // TODO: make this optional?
    curl_easy_setopt (curl, CURLOPT_SSL_VERIFYHOST, 0L);
    // curl_easy_setopt (curl, CURLOPT_FOLLOWLOCATION, 1); // TODO: remove the comment once we want to follow redirects (e.g., on HTTP 407)

    if (strncmp (verb, "GET", 4)==0) {
        curl_easy_setopt (curl, CURLOPT_HTTPGET, 1L);
    } else if (strncmp (verb, "HEAD", 5)==0) {
        /* TODO: HEAD isn't very useful atm since we don't look at headers */
        curl_easy_setopt (curl, CURLOPT_NOBODY, 1L);
    } else {
        close(fd);
        logprintfl (EUCAERROR, "{%u} walrus_request: invalid HTTP verb %s\n", (unsigned int)pthread_self(), verb);
        pthread_mutex_unlock(&wreq_mutex);
        return ERROR; /* TODO: dealloc structs before returning! */
    }

	if (connect_timeout > 0) {
        curl_easy_setopt (curl, CURLOPT_CONNECTTIMEOUT, connect_timeout);
	}
	if (total_timeout > 0) {
        curl_easy_setopt (curl, CURLOPT_TIMEOUT, total_timeout);
	}

    /* set up the default write function, but possibly override
     * it below, if compression is desired and possible */
    struct request params;
    params.fd = fd;
    curl_easy_setopt (curl, CURLOPT_WRITEDATA, &params);
    curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data);
#if defined(CAN_GZIP)
    if (do_compress) {
        curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data_zlib);
    }
#endif

    struct curl_slist * headers = NULL; /* beginning of a DLL with headers */

    char op_hdr [STRSIZE];
    if(walrus_op != NULL) {
        snprintf (op_hdr, STRSIZE, "EucaOperation: %s", walrus_op);
        headers = curl_slist_append (headers, op_hdr);
    }

    time_t t;
    t = time(&t);
    struct tm tmp_t;
    gmtime_r(&t, &tmp_t);
    char date_str [17];

    //Format for time
    if(strftime(date_str, 17, "%Y%m%dT%H%M%SZ",&tmp_t)<0) {
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return ERROR;
    }

    assert (strlen(date_str)+7<=STRSIZE);
    char * newline = strchr (date_str, '\n');
    if (newline!=NULL) { * newline = '\0'; } // remove newline if found
    char date_hdr [STRSIZE];
    snprintf (date_hdr, STRSIZE, "Date: %s", date_str);
    headers = curl_slist_append (headers, date_hdr);

    char host_hdr [STRSIZE];
    char * url_host = NULL;
    if((url_host=process_url(url,URL_HOSTNAME)) == NULL)
    {
    	logprintfl (EUCAERROR, "{%u} walrus_request: URL has no host\n",(unsigned int)pthread_self());
    	pthread_mutex_unlock(&wreq_mutex);
    	return code;
    }

    snprintf (host_hdr, STRSIZE, "Host: %s", url_host);
    headers = curl_slist_append (headers, host_hdr);

    char * auth_str = eucav2_sign_request (verb, url, headers); /* create Walrus-compliant sig */
    if (auth_str==NULL) {
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return ERROR;
    }
    assert (strlen(auth_str)+16<=BUFSIZE);
    headers = curl_slist_append (headers, auth_str);

    curl_easy_setopt (curl, CURLOPT_HTTPHEADER, headers); /* register headers */
    if (walrus_op) {
        logprintfl (EUCADEBUG, "{%u} walrus_request: writing %s/%s output\n", (unsigned int)pthread_self(), verb, walrus_op);
        logprintfl (EUCADEBUG, "{%u}                 from %s\n", (unsigned int)pthread_self(), url);
        logprintfl (EUCADEBUG, "{%u}                 to %s\n", (unsigned int)pthread_self(), outfile);
    } else {
        logprintfl (EUCADEBUG, "{%u} walrus_request: writing %s output to %s\n", (unsigned int)pthread_self(), verb, outfile);
    }
    int retries = TOTAL_RETRIES;
    int timeout = FIRST_TIMEOUT;
    do {
        params.total_wrote = 0L;
        params.total_calls = 0L;
#if defined(CAN_GZIP)
        if (do_compress) {
            /* allocate zlib inflate state */
            params.strm.zalloc = Z_NULL;
            params.strm.zfree = Z_NULL;
            params.strm.opaque = Z_NULL;
            params.strm.avail_in = 0;
            params.strm.next_in = Z_NULL;
            params.ret = inflateInit2 (&(params.strm), 31);
            if (params.ret != Z_OK) {
                zerr (params.ret, "walrus_request");
                break;
            }
        }
#endif

        pthread_mutex_unlock(&wreq_mutex); /* unlock for message exchange */
        result = curl_easy_perform (curl); /* do it */
        pthread_mutex_lock(&wreq_mutex); /* relock for curl teardown */
        logprintfl (EUCADEBUG, "{%u} walrus_request: wrote %lld byte(s) in %ld write(s)\n", (unsigned int)pthread_self(), params.total_wrote, params.total_calls);

#if defined(CAN_GZIP)
        if (do_compress) {
            inflateEnd(&(params.strm));
            if (params.ret != Z_STREAM_END) {
                zerr (params.ret, "walrus_request");
            }
        }
#endif

        if (result) { // curl error (connection or transfer failed)
            logprintfl (EUCAERROR,     "{%u} walrus_request: %s (%d)\n", (unsigned int)pthread_self(), error_msg, result);

        } else {
            long httpcode;
            curl_easy_getinfo (curl, CURLINFO_RESPONSE_CODE, &httpcode);
            /* TODO: pull out response message, too */

            switch (httpcode) {
            case 200L: /* all good */
                logprintfl (EUCAINFO, "{%u} walrus_request: to %s\n", (unsigned int)pthread_self(), outfile);
                code = OK;
                break;
            case 408L: /* timeout, retry */
                logprintfl (EUCAWARN, "{%u} walrus_request: server responded with HTTP code %ld (timeout)\n", (unsigned int)pthread_self(), httpcode);
                //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
                break;
            default: /* some kind of error */
                logprintfl (EUCAERROR, "{%u} walrus_request: server responded with HTTP code %ld\n", (unsigned int)pthread_self(), httpcode);
                //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
                retries=0;
            }
        }

        if (code!=OK && retries>0) {
            logprintfl (EUCAERROR, "                  download retry %d of %d will commence in %d seconds\n", retries, TOTAL_RETRIES, timeout);
            sleep (timeout);
            lseek (fd, 0L, SEEK_SET);
            timeout <<= 1;
            if (timeout > MAX_TIMEOUT)
                timeout = MAX_TIMEOUT;
        }

        retries--;
    } while (code!=OK && retries>0);
    close (fd);

    if ( code != OK ) {
        logprintfl (EUCAINFO, "{%u} walrus_request: due to error, removing %s\n", (unsigned int)pthread_self(), outfile);
        remove (outfile);
    }

    free (auth_str);
    curl_slist_free_all (headers);
    curl_easy_cleanup (curl);
    pthread_mutex_unlock(&wreq_mutex);
    return code;
}

#if 0
/* Unused function */
static int walrus_request(const char *walrus_op, const char *verb, const char *requested_url, const char *outfile, const int do_compress)
{
    return (walrus_request_timeout(walrus_op, verb, requested_url, outfile, do_compress, 0, 0));
}
#endif /* 0 */

/* downloads a Walrus object from the URL, is it to outfile */
int walrus_object_by_url (const char * url, const char * outfile, const int do_compress)
{
    return walrus_request_timeout(NULL, "GET", url, outfile, do_compress, 120, 0);
}

/* downloads a Walrus object from the default Walrus endpoint,
 * so only the path is needed; saves object to outfile */
int walrus_object_by_path(const char *path, const char *outfile, const int do_compress)
{
    char url[STRSIZE];
    snprintf(url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, WALRUS_ENDPOINT, path);
    return walrus_object_by_url(url, outfile, do_compress);
}

/* downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
int walrus_image_by_manifest_url(const char *url, const char *outfile, const int do_compress)
{
    return walrus_request_timeout(GET_IMAGE_CMD, "GET", url, outfile, do_compress, 120, 0);
}

/* gets a decrypted image from the default Walrus endpoint,
 * so only manifest path is needed; saves image to outfile */
int walrus_image_by_manifest_path(const char *manifest_path, const char *outfile, const int do_compress)
{
    char url[STRSIZE];
    snprintf(url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, WALRUS_ENDPOINT, manifest_path);
    return walrus_image_by_manifest_url(url, outfile, do_compress);
}

// downloads a digest and returns it as a new string (or NULL if error)
// that the caller must free
char *walrus_get_digest(const char *url)
{
    char *digest_str = NULL;
    char *digest_path = strdup("/tmp/walrus-digest-XXXXXX");

    if (!digest_path) {
        logprintfl(EUCAERROR, "out of memory (failed to allocate digest path)\n");
        return digest_path;
    }

    int tmp_fd = safe_mkstemp(digest_path);
    if (tmp_fd < 0) {
        logprintfl(EUCAERROR, "failed to create a digest file %s\n", digest_path);
    } else {
        close(tmp_fd);          // walrus_ routine will reopen the file

        // download a fresh digest
        if (walrus_object_by_url(url, digest_path, 0) != 0) {
            logprintfl(EUCAERROR, "failed to download digest to %s\n", digest_path);
        } else {
            digest_str = file2strn(digest_path, 2000000);
        }
        unlink(digest_path);
    }
    if (digest_path) {
        free(digest_path);
    }
    return digest_str;
}

/* downloads a digest of an image and compares it to file at old_digest_path
 * returns 0 if same, -N if different, N if error */
int walrus_verify_digest(const char *url, const char *old_digest_path)
{
    int e = ERROR;

    char *new_digest;
    char *old_digest = file2strn(old_digest_path, 2000000);
    if (old_digest == NULL) {
        logprintfl(EUCAERROR, "failed to read old digest %s\n", old_digest_path);
        return e;
    }

    if ((new_digest = walrus_get_digest(url)) != NULL) {
        // compare the two
        if (strcmp(new_digest, old_digest)) {
            e = -1;
        } else {
            e = 0;
        }

        free(new_digest);
    }
    free(old_digest);

    return e;
}

/* libcurl header write handler */
static size_t write_header(void *buffer, size_t size, size_t nmemb, void *params)
{
    /* here in case we want to do something with headers */
    return size * nmemb;
}

/* libcurl write handler */
static size_t write_data(void *buffer, size_t size, size_t nmemb, void *params)
{
    assert(params != NULL);

    int fd = ((struct request *)params)->fd;
    int wrote = write(fd, buffer, size * nmemb);
    ((struct request *)params)->total_wrote += wrote;
    ((struct request *)params)->total_calls++;

    return wrote;
}

#if defined(CAN_GZIP)

/* unused testing function */
static void print_data(unsigned char *buf, const int size)
{
    int i;

    for (i = 0; i < size; i++) {
        int c = buf[i];
        if (c > ' ' && c <= '~')
            printf(" %c", c);
        else
            printf(" %x", c);
    }
    printf("\n");
}

/* report on a zlib error */
static void zerr(int ret, char *where)
{
    switch (ret) {
    case Z_ERRNO:
        logprintfl(EUCAERROR, "%s: zlib: failed to write\n", where);
        break;
    case Z_STREAM_ERROR:
        logprintfl(EUCAERROR, "%s: zlib: invalid compression level\n", where);
        break;
    case Z_DATA_ERROR:
        logprintfl(EUCAERROR, "%s: zlib: invalid or incomplete deflate data\n", where);
        break;
    case Z_MEM_ERROR:
        logprintfl(EUCAERROR, "%s: zlib: out of memory\n", where);
        break;
    case Z_VERSION_ERROR:
        logprintfl(EUCAERROR, "%s: zlib: zlib version mismatch!\n", where);
    }
}

/* libcurl write handler for gzipped streams */
static size_t write_data_zlib(void *buffer, size_t size, size_t nmemb, void *params)
{
    assert(params != NULL);
    z_stream *strm = &(((struct request *)params)->strm);
    int fd = ((struct request *)params)->fd;
    unsigned char out[CHUNK];
    int wrote = 0;
    int ret;

    strm->avail_in = size * nmemb;
    strm->next_in = (unsigned char *)buffer;
    do {
        strm->avail_out = CHUNK;
        strm->next_out = out;

        ((struct request *)params)->ret = ret = inflate(strm, Z_NO_FLUSH);
        switch (ret) {
        case Z_NEED_DICT:
            ret = Z_DATA_ERROR; // ok to fall through
        case Z_DATA_ERROR:
        case Z_MEM_ERROR:
        case Z_STREAM_ERROR:
            inflateEnd(strm);
            zerr(ret, "write_data_zlib");
            return ret;
        }

        unsigned have = CHUNK - strm->avail_out;
        if (write(fd, out, have) != have) {
            logprintfl(EUCAERROR, "write call with compressed data failed\n");
            inflateEnd(strm);
            return Z_ERRNO;
        }
        wrote += have;
    } while (strm->avail_out == 0);

    ((struct request *)params)->total_wrote += wrote;
    ((struct request *)params)->total_calls++;
    return size * nmemb;
}

#endif /* CAN_GZIP */
