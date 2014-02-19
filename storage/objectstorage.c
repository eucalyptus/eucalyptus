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

//!
//! @file storage/objectstorage.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>                    /* close */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h>                     /* open */
#include <curl/curl.h>
#include <curl/easy.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/stat.h>
#if defined(HAVE_ZLIB_H)
#include <zlib.h>
#endif /* HAVE_ZLIB_H */

#include <eucalyptus.h>
#include <misc.h>
#include <config.h>
#include <euca_auth.h>
#include <euca_string.h>

#include "objectstorage.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define TOTAL_TIMEOUT_SEC                             0 //!< we do not impose a total timeout, since downloads can take a long long time
#define CONNECT_TIMEOUT_SEC                          10 //!< initial connection timeout for objectstorage requests, in seconds
#define TOTAL_ATTEMPTS                                9 //!< download is retried in case of connection problems (13+ min of retrying)
#define FIRST_TIMEOUT                                 2 //!< in seconds, goes in powers of two afterwards
#define MAX_TIMEOUT                                 300 //!< in seconds, the cap for growing timeout values
#define CHUNK                                    262144 //!< buffer size for decompression operations
#define BUFSIZE                                  262144 //!< should be big enough for CERT and the signature
#define STRSIZE                                    1024 //!< for short strings: files, hosts, URLs
#define PROGRESS_UPDATE_SEC                           3 //!< how often to report on progress of long downloads

#define OBJECT_STORAGE_ENDPOINT                          "/services/objectstorage"
#define DEFAULT_HOST_PORT                        "localhost:8773"
#define GET_IMAGE_CMD                            "GetDecryptedImage"
#define GET_OBJECT_CMD                           "GetObject"

#if defined(ZLIB_VERNUM) && (ZLIB_VERNUM >= 0x1204)
#define CAN_GZIP
#endif /* ZLIB_VERNUM && (ZLIB_VERNUM >= 0x1204) */

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

//! Defines the OBJECT_STORAGE request structure
struct request {
    int fd;                            //!< output file descriptor to be used by curl WRITERs
    long long total_wrote;             //!< bytes written during the operation
    long long total_calls;             //!< write calls made during the operation
#if defined (CAN_GZIP)
    z_stream strm;                     //!< stream struct used by zlib
    int ret;                           //!< return value of last inflate() call
#endif                                 /* CAN_GZIP */
};

//! Defines the struct for passing information into curl progress function
struct progress_data_t {
    char *url;
    time_t last_update;
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! objectstorage_request internal lock to prevent apparent race in curl ssl dependency
static pthread_mutex_t wreq_mutex = PTHREAD_MUTEX_INITIALIZER;
static unsigned short total_attempts = TOTAL_ATTEMPTS;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int objectstorage_request_timeout(const char *objectstorage_op, const char *verb, const char *requested_url, const char *outfile, const int do_compress,
                                         int connect_timeout, int total_timeout);
static size_t write_header(void *buffer, size_t size, size_t nmemb, void *params);
static size_t write_data(void *buffer, size_t size, size_t nmemb, void *params);

#if defined(CAN_GZIP)
static void print_data(unsigned char *buf, const int size);
static void zerr(int ret, char *where);
static size_t write_data_zlib(void *buffer, size_t size, size_t nmemb, void *params);
#endif /* CAN_GZIP */

static int progress_function(void *clientp, double dltotal, double dlnow, double ultotal, double ulnow);

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
//! downloads a decrypted image from objectstorage based on the manifest URL,
//! saves it to outfile. Uses EucaV2 signing for the request. We keep
//! both functions to enable backwards compatibility.
//!
//! @param[in] objectstorage_op
//! @param[in] verb
//! @param[in] requested_url
//! @param[in] outfile
//! @param[in] do_compress
//! @param[in] connect_timeout
//! @param[in] total_timeout
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR.
//!
static int objectstorage_request_timeout(const char *objectstorage_op, const char *verb, const char *requested_url, const char *outfile, const int do_compress,
                                         int connect_timeout, int total_timeout)
{
    int fd = -1;
    int code = EUCA_ERROR;
    int timeout = FIRST_TIMEOUT;
    long httpcode = 0;
    char *url_path = NULL;
    char *newline = NULL;
    char *url_host = NULL;
    char *auth_str = NULL;
    char host_hdr[STRSIZE] = "";
    char date_hdr[STRSIZE] = "";
    char date_str[17] = "";
    char url[BUFSIZE] = "";
    char op_hdr[STRSIZE] = "";
    char error_msg[CURL_ERROR_SIZE] = "";
    CURL *curl = 0;
    CURLcode result = CURLE_OK;
    time_t t = 0;
    struct tm tmp_t = { 0 };
    struct request params = { 0 };
    struct curl_slist *headers = NULL; // beginning of a DLL with headers

    pthread_mutex_lock(&wreq_mutex);   // lock for curl construction

    euca_strncpy(url, requested_url, BUFSIZE);
#if defined(CAN_GZIP)
    if (do_compress)
        snprintf(url, BUFSIZE, "%s%s", requested_url, "?IsCompressed=true");
#endif

    // isolate the PATH in the URL as it will be needed for signing
    if (strncasecmp(url, "http://", 7) != 0 && strncasecmp(url, "https://", 8) != 0) {
        LOGERROR("objectstorage URL must start with http(s)://...\n");
        pthread_mutex_unlock(&wreq_mutex);
        return (code);
    }

    if ((url_path = strchr(url + 8, '/')) == NULL) {    // find first '/' after hostname
        LOGERROR("objectstorage URL has no path\n");
        pthread_mutex_unlock(&wreq_mutex);
        return (code);
    }

    if (euca_init_cert()) {
        LOGERROR("failed to initialize certificate for objectstorage request\n");
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }
    // we do not truncate the file because its size was set at blobstore allocation and
    // it should reflect the size of the stored blob for accounting to work
    fd = open(outfile, O_CREAT | O_WRONLY, S_IRUSR | S_IWUSR);
    if ((fd == -1) || (lseek(fd, 0, SEEK_SET) == -1)) {
        LOGERROR("failed to open %s for writing result of objectstorage request\n", outfile);
        pthread_mutex_unlock(&wreq_mutex);
        if (fd >= 0)
            close(fd);
        return (code);
    }

    if ((curl = curl_easy_init()) == NULL) {
        LOGERROR("could not initialize libcurl\n");
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return (code);
    }

    curl_easy_setopt(curl, CURLOPT_ERRORBUFFER, error_msg);
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, write_header);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L); //! TODO: make this optional?
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_LIMIT, 360L);  // must have at least a 360 baud modem
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_TIME, 10L);    // abort if below speed limit for this many seconds
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1);  //! @todo remove the comment once we want to follow redirects (e.g., on HTTP 407)

    // enable periodic progress statements in the log
    curl_easy_setopt(curl, CURLOPT_NOPROGRESS, 0L); // enable progress function invocation
    curl_easy_setopt(curl, CURLOPT_PROGRESSFUNCTION, progress_function);
    struct progress_data_t progress_data = {
        .url = url,
        .last_update = time(NULL)
    };
    curl_easy_setopt(curl, CURLOPT_PROGRESSDATA, &progress_data);

    if (strncmp(verb, "GET", 4) == 0) {
        curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L);
    } else if (strncmp(verb, "HEAD", 5) == 0) {
        //! TODO: HEAD isn't very useful atm since we don't look at headers
        curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
    } else {
        close(fd);
        LOGERROR("invalid HTTP verb %s for objectstorage request\n", verb);
        pthread_mutex_unlock(&wreq_mutex);
        return EUCA_ERROR;             //! TODO: dealloc structs before returning!
    }

    if (connect_timeout > 0) {
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, connect_timeout);
    }

    if (total_timeout > 0) {
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, total_timeout);
    }
    // set up the default write function, but possibly override it below, if compression is desired and possible
    params.fd = fd;
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &params);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
#if defined(CAN_GZIP)
    if (do_compress) {
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data_zlib);
    }
#endif

    if (objectstorage_op != NULL) {
        snprintf(op_hdr, STRSIZE, "EucaOperation: %s", objectstorage_op);
        headers = curl_slist_append(headers, op_hdr);
    }

    t = time(&t);
    gmtime_r(&t, &tmp_t);

    //Format for time
    if (strftime(date_str, 17, "%Y%m%dT%H%M%SZ", &tmp_t) == 0) {
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        return (EUCA_ERROR);
    }

    assert(strlen(date_str) + 7 <= STRSIZE);

    // remove newline if found
    if ((newline = strchr(date_str, '\n')) != NULL) {
        *newline = '\0';
    }

    snprintf(date_hdr, STRSIZE, "Date: %s", date_str);
    headers = curl_slist_append(headers, date_hdr);

    if ((url_host = process_url(url, URL_HOSTNAME)) == NULL) {
        LOGERROR("objectstorage URL has no host\n");
        pthread_mutex_unlock(&wreq_mutex);
        return code;
    }

    snprintf(host_hdr, STRSIZE, "Host: %s", url_host);
    headers = curl_slist_append(headers, host_hdr);

    // create objectstorage-compliant sig
    if ((auth_str = eucav2_sign_request(verb, url, headers)) == NULL) {
        close(fd);
        pthread_mutex_unlock(&wreq_mutex);
        EUCA_FREE(url_host);
        return (EUCA_ERROR);
    }

    assert(strlen(auth_str) + 16 <= BUFSIZE);
    headers = curl_slist_append(headers, auth_str);

    // register headers
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    if (objectstorage_op) {
        LOGDEBUG("writing %s/%s output\n", verb, objectstorage_op);
        LOGDEBUG("        from %s\n", url);
        LOGDEBUG("        to %s\n", outfile);
    } else {
        LOGDEBUG("writing %s output to %s\n", verb, outfile);
    }

    for (int attempt = 1; attempt <= total_attempts; attempt++) {
        params.total_wrote = 0L;
        params.total_calls = 0L;
#if defined(CAN_GZIP)
        if (do_compress) {
            // allocate zlib inflate state
            params.strm.zalloc = Z_NULL;
            params.strm.zfree = Z_NULL;
            params.strm.opaque = Z_NULL;
            params.strm.avail_in = 0;
            params.strm.next_in = Z_NULL;
            params.ret = inflateInit2(&(params.strm), 31);
            if (params.ret != Z_OK) {
                zerr(params.ret, "objectstorage_request");
                break;
            }
        }
#endif

        //! @todo There used to be a 'pthread_mutex_unlock(&wreq_mutex)' before curl invocation
        //! and a 'lock' after it, but under heavy load we were seeing failures inside
        //! libcurl code that would propagate to NC, implying lack of thread safety in
        //! the library. For now, we will serialize all curl operations, but in the future
        //! an approach to parallelizing objectstorage downloads is necessary
        LOGINFO("downloading %s\n", url);
        result = curl_easy_perform(curl);   // do it
        LOGDEBUG("wrote %lld byte(s) in %lld write(s)\n", params.total_wrote, params.total_calls);

#if defined(CAN_GZIP)
        if (do_compress) {
            inflateEnd(&(params.strm));
            if (params.ret != Z_STREAM_END) {
                zerr(params.ret, "objectstorage_request");
            }
        }
#endif

        boolean bail = FALSE;

        if (result) {                  // curl error (connection or transfer failed)
            LOGERROR("connection to objectstorage failed: %s (%d)\n", error_msg, result);
        } else {
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
            //! @todo pull out response message, too

            switch (httpcode) {
            case 200L:                // all good
                LOGINFO("downloaded %s\n", outfile);
                code = EUCA_OK;
                break;
            case 408L:                // timeout, retry
                LOGWARN("server responded with HTTP code %ld (timeout) for %s\n", httpcode, url);
                break;
            default:                  // some kind of error
                LOGERROR("server responded with HTTP code %ld for %s\n", httpcode, url);
                bail = TRUE;
                break;
            }
        }

        if (code == EUCA_OK || bail == TRUE) {
            break;                     // bail out of the retry loop

        } else if ((attempt + 1) <= total_attempts) {
            LOGWARN("download attempt %d of %d will commence in %d sec for %s\n", (attempt + 1), total_attempts, timeout, url);
            sleep(timeout);
            timeout <<= 1;
            if (timeout > MAX_TIMEOUT)
                timeout = MAX_TIMEOUT;

            lseek(fd, 0L, SEEK_SET);   // move the file pointer to the beginning for the retry
        }
    }
    close(fd);

    if (code != EUCA_OK) {
        LOGINFO("due to error, removing %s\n", outfile);
        remove(outfile);
    }

    EUCA_FREE(auth_str);
    EUCA_FREE(url_host);
    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    pthread_mutex_unlock(&wreq_mutex);
    return (code);
}

//!
//! Sets the maximum number of connection attempts that the library will make
//! to objectstorage. The default is MAX_ATTEMPTS value defined above. The attempts
//! back off exponentially up to a point, so the actual delay experienced by
//! those invoking is non-trivial to predict. 
//!
//! @param[in] new_max_attempts The number of attempts to use
//! 
//! @return the previous value of max download attempts

int objectstorage_set_max_download_attempts(unsigned short new_max_attempts)
{
    assert(new_max_attempts > 0 && new_max_attempts < 99);
    unsigned short old_max_attempts = total_attempts;
    total_attempts = new_max_attempts;
    return old_max_attempts;
}

//!
//! downloads a objectstorage object from the URL, saves it to outfile
//!
//! @param[in] url
//! @param[in] outfile
//! @param[in] do_compress
//!
//! @return the result of the objectstorage_request_timeout() call.
//!
//! @see objectstorage_request_timeout()
//!
int objectstorage_object_by_url(const char *url, const char *outfile, const int do_compress)
{
    return objectstorage_request_timeout(NULL, "GET", url, outfile, do_compress, CONNECT_TIMEOUT_SEC, TOTAL_TIMEOUT_SEC);
}

//!
//! downloads a objectstorage object from the default objectstorage endpoint,
//! so only the path is needed; saves object to outfile
//!
//! @param[in] path
//! @param[in] outfile
//! @param[in] do_compress
//!
//! @return the result of the objectstorage_object_by_url() call.
//!
//! @see objectstorage_object_by_url()
//! @see objectstorage_request_timeout()
//!
int objectstorage_object_by_path(const char *path, const char *outfile, const int do_compress)
{
    char url[STRSIZE];
    snprintf(url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, OBJECT_STORAGE_ENDPOINT, path);
    return objectstorage_object_by_url(url, outfile, do_compress);
}

//!
//! downloads a decrypted image from objectstorage based on the manifest URL,
//! saves it to outfile
//!
//! @param[in] url
//! @param[in] outfile
//! @param[in] do_compress
//!
//! @return the result of the objectstorage_request_timeout() call.
//!
//! @see objectstorage_request_timeout()
//!
int objectstorage_image_by_manifest_url(const char *url, const char *outfile, const int do_compress)
{
    return objectstorage_request_timeout(GET_IMAGE_CMD, "GET", url, outfile, do_compress, CONNECT_TIMEOUT_SEC, TOTAL_TIMEOUT_SEC);
}

//!
//! gets a decrypted image from the default objectstorage endpoint,
//! so only manifest path is needed; saves image to outfile
//!
//! @param[in] manifest_path
//! @param[in] outfile
//! @param[in] do_compress
//!
//! @return the result of the objectstorage_image_by_manifest_url() call.
//!
//! @see objectstorage_image_by_manifest_url()
//! @see objectstorage_request_timeout()
//!
int objectstorage_image_by_manifest_path(const char *manifest_path, const char *outfile, const int do_compress)
{
    char url[STRSIZE];
    snprintf(url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, OBJECT_STORAGE_ENDPOINT, manifest_path);
    return objectstorage_image_by_manifest_url(url, outfile, do_compress);
}

//!
//! downloads a digest and returns it as a new string (or NULL if error)
//! that the caller must free
//!
//! @param[in] url
//!
//! @return the digested string.
//!
//! @see file2strn()
//!
//! @note the caller must free the returned memory when done.
//!
char *objectstorage_get_digest(const char *url)
{
    char *digest_str = NULL;
    char *digest_path = strdup("/tmp/objectstorage-digest-XXXXXX");

    if (!digest_path) {
        LOGERROR("out of memory (failed to allocate digest path)\n");
        return digest_path;
    }

    int tmp_fd = safe_mkstemp(digest_path);
    if (tmp_fd < 0) {
        LOGERROR("failed to create a digest file %s\n", digest_path);
    } else {
        close(tmp_fd);                 // objectstorage_ routine will reopen the file

        // download a fresh digest
        if (objectstorage_object_by_url(url, digest_path, 0) != 0) {
            LOGERROR("failed to download digest to %s\n", digest_path);
        } else {
            digest_str = file2strn(digest_path, 2000000);
        }
        unlink(digest_path);
    }
    EUCA_FREE(digest_path);
    return digest_str;
}

//!
//! downloads a digest of an image and compares it to file at old_digest_path
//!
//! @param[in] url
//! @param[in] old_digest_path
//!
//! @return 0 if same, -1 if different, EUCA_INVALID_ERROR if error
//!
int objectstorage_verify_digest(const char *url, const char *old_digest_path)
{
    int e = EUCA_INVALID_ERROR;

    char *new_digest;
    char *old_digest = file2strn(old_digest_path, 2000000);
    if (old_digest == NULL) {
        LOGERROR("failed to read old digest %s\n", old_digest_path);
        return e;
    }

    if ((new_digest = objectstorage_get_digest(url)) != NULL) {
        // compare the two
        if (strcmp(new_digest, old_digest)) {
            e = -1;
        } else {
            e = EUCA_OK;
        }

        EUCA_FREE(new_digest);
    }
    EUCA_FREE(old_digest);

    return e;
}

//!
//! libcurl header write handler
//!
//! @param[in] buffer
//! @param[in] size
//! @param[in] nmemb
//! @param[in] params
//!
//! @return the number of bytes written in the header
//!
static size_t write_header(void *buffer, size_t size, size_t nmemb, void *params)
{
    /* here in case we want to do something with headers */
    return (size * nmemb);
}

//!
//! libcurl write handler
//!
//! @param[in] buffer
//! @param[in] size
//! @param[in] nmemb
//! @param[in] params
//!
//! @return the number of bytes written. If the returned value does not match
//!         size*nmemb, then libcurl will return an error.
//!
static size_t write_data(void *buffer, size_t size, size_t nmemb, void *params)
{
    assert(params != NULL);

    int fd = ((struct request *)params)->fd;
    int wrote = write(fd, buffer, size * nmemb);    // any blocking in this call is not subject to connection timeouts
    ((struct request *)params)->total_wrote += wrote;
    ((struct request *)params)->total_calls++;

    return wrote;
}

#if defined(CAN_GZIP)
//!
//! unused testing function
//!
//! @param[in] buf
//! @param[in] size
//!
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

//!
//! report on a zlib error
//!
//! @param[in] ret
//! @param[in] where
//!
static void zerr(int ret, char *where)
{
    switch (ret) {
    case Z_ERRNO:
        LOGERROR("%s: zlib: failed to write\n", where);
        break;
    case Z_STREAM_ERROR:
        LOGERROR("%s: zlib: invalid compression level\n", where);
        break;
    case Z_DATA_ERROR:
        LOGERROR("%s: zlib: invalid or incomplete deflate data\n", where);
        break;
    case Z_MEM_ERROR:
        LOGERROR("%s: zlib: out of memory\n", where);
        break;
    case Z_VERSION_ERROR:
        LOGERROR("%s: zlib: zlib version mismatch!\n", where);
    }
}

//!
//! libcurl write handler for gzipped streams
//!
//! @param[in] buffer
//! @param[in] size
//! @param[in] nmemb
//! @param[in] params
//!
//! @return the number of bytes written. If the returned value does not match
//!         size*nmemb, then libcurl will return an error.
//!
static size_t write_data_zlib(void *buffer, size_t size, size_t nmemb, void *params)
{
    assert(params != NULL);
    z_stream *strm = &(((struct request *)params)->strm);
    int fd = ((struct request *)params)->fd;
    unsigned char out[CHUNK];
    int wrote = 0;
    int ret;

    // any blocking in this function is not subject to connection timeouts

    strm->avail_in = size * nmemb;
    strm->next_in = (unsigned char *)buffer;
    do {
        strm->avail_out = CHUNK;
        strm->next_out = out;

        ((struct request *)params)->ret = ret = inflate(strm, Z_NO_FLUSH);
        switch (ret) {
        case Z_NEED_DICT:
            ret = Z_DATA_ERROR;        // ok to fall through
        case Z_DATA_ERROR:
        case Z_MEM_ERROR:
        case Z_STREAM_ERROR:
            inflateEnd(strm);
            zerr(ret, "write_data_zlib");
            return ret;
        }

        unsigned have = CHUNK - strm->avail_out;
        if (write(fd, out, have) != have) {
            LOGERROR("write call with compressed data failed\n");
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

static int progress_function(void *clientp, double dltotal, double dlnow, double ultotal, double ulnow)
{
    struct progress_data_t *progress_data = (struct progress_data_t *)clientp;
    time_t now = time(NULL);

    if (dltotal <= 0.0)                // can't do anything with that
        return 0;

    if ((progress_data->last_update + PROGRESS_UPDATE_SEC) <= now) {
        double percent = (dlnow / dltotal) * 100.0;
        LOGINFO("downloaded %.1f%% of %s\n", percent, progress_data->url);
        progress_data->last_update = now;
    }
    return 0;
}
