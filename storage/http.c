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
//! @file storage/http.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdlib.h>
#include <time.h>
#include <unistd.h>                    // close, stat
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h>                     // open
#include <ctype.h>                     // tolower, isdigit
#include <sys/types.h>                 // stat
#include <sys/stat.h>                  // stat
#include <curl/curl.h>
#include <curl/easy.h>

#include <eucalyptus.h>
#include <log.h>

#ifndef _UNIT_TEST
// http_ functions aren't part of the unit test
#include <config.h>
#include "http.h"
#endif /* ! _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifndef _UNIT_TEST
#define TOTAL_RETRIES                              3    //!< download is retried in case of connection problems
#define FIRST_TIMEOUT                              4    //!< in seconds, goes in powers of two afterwards
#define STRSIZE                                  245    //!< for short strings: files, hosts, URLs
#endif /* ! _UNIT_TEST */

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

#ifndef _UNIT_TEST
struct read_request {
    FILE *fp;                          //!< input file pointer to be used by curl READERs
    long long total_read;              //!< bytes written during the operation
    long long total_calls;             //!< write calls made during the operation
    time_t timestamp;                  //!< timestamp for periodically printing progress messages
    long long file_size;               //!< file size in bytes, to print in progress messages
};

struct write_request {
    FILE *fp;                          //!< output file pointer to be used by curl WRITERs
    long long total_wrote;             //!< bytes written during the operation
    long long total_calls;             //!< write calls made during the operation
#if defined (CAN_GZIP)
    z_stream strm;                     //!< stream struct used by zlib
    int ret;                           //!< return value of last inflate() call
#endif                                 /* CAN_GZIP */
};
#endif /* ! _UNIT_TEST */

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

#ifndef _UNIT_TEST
static boolean curl_initialized = FALSE;    //!< boolean to indicate if we have already initialize libcurl
#endif /* ! _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static size_t read_data(char *buffer, size_t size, size_t nitems, void *params);
static size_t write_data(void *buffer, size_t size, size_t nmemb, void *params);
static char hch_to_int(char ch);
static char int_to_hch(char i);

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
//! Process an HTTP put request to the given URL.
//!
//! @param[in] file_path path to the input file to be used by curl READERs
//! @param[in] url the request URL
//! @param[in] login the login for the request (optional)
//! @param[in] password the passwor for the request (optional)
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: on failure.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_ACCESS_ERROR: if we fail to access file_path file
//!         \li EUCA_TIMEOUT_ERROR: if the execution timed out
//!
//! @pre Both file_path and url parameters must not be NULL
//!
//! @post On success, the put request has been processed successfully
//!
//! @note On execution failure, the operation will be re-attempted up to TOTA_RETRIES times. Each
//!       time we retrie, the timeout value will double.
//!
int http_put(const char *file_path, const char *url, const char *login, const char *password)
{
    int code = EUCA_ERROR;
    int retries = TOTAL_RETRIES;
    int timeout = FIRST_TIMEOUT;
    long httpcode = 0L;
    FILE *fp = NULL;
    char error_msg[CURL_ERROR_SIZE] = { 0 };
    char userpwd[STRSIZE] = { 0 };
    struct stat64 mystat = { 0 };
    struct read_request params = { 0 };
    CURL *curl = NULL;
    CURLcode result = CURLE_OK;

    if (!curl_initialized) {
        curl_global_init(CURL_GLOBAL_SSL);
        curl_initialized = TRUE;
    }

    if (!file_path || !url) {
        LOGERROR("invalid params: file_path=%s, url=%s\n", SP(file_path), SP(url));
        return (EUCA_INVALID_ERROR);
    }

    if (stat64(file_path, &mystat)) {
        LOGERROR("failed to stat %s\n", file_path);
        return (EUCA_ACCESS_ERROR);
    }

    if (!S_ISREG(mystat.st_mode)) {
        LOGERROR("%s is not a regular file\n", file_path);
        return (EUCA_ERROR);
    }

    if ((fp = fopen64(file_path, "r")) == NULL) {
        LOGERROR("failed to open %s for reading\n", file_path);
        return (EUCA_ACCESS_ERROR);
    }

    if ((curl = curl_easy_init()) == NULL) {
        LOGERROR("could not initialize libcurl\n");
        fclose(fp);
        return (EUCA_ERROR);
    }

    LOGDEBUG("uploading %s\n", file_path);
    LOGDEBUG("       to %s\n", url);

    curl_easy_setopt(curl, CURLOPT_ERRORBUFFER, error_msg);
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, ((curl_off_t) mystat.st_size));
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L); //! @TODO: make this optional?
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_LIMIT, 360L);  // must have at least a 360 baud modem
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_TIME, 10L);    // abort if below speed limit for this many seconds

    if ((login != NULL) && (password != NULL)) {
        snprintf(userpwd, STRSIZE, "%s:%s", login, password);
        curl_easy_setopt(curl, CURLOPT_USERPWD, userpwd);
    }

    params.fp = fp;
    params.timestamp = time(NULL);
    params.file_size = ((long long)mystat.st_size);
    curl_easy_setopt(curl, CURLOPT_READDATA, &params);
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, read_data);

    do {
        params.total_read = 0L;
        params.total_calls = 0L;
        result = curl_easy_perform(curl);   /* do it */
        LOGDEBUG("uploaded %lld bytes in %lld sends\n", params.total_read, params.total_calls);

        if (result) {
            // curl error (connection or transfer failed)
            LOGERROR("%s (%d)\n", error_msg, result);
        } else {
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
            //! @TODO pull out response message, too?
            switch (httpcode) {
            case 200L:
                // all good
                LOGDEBUG("file updated sucessfully\n");
                code = EUCA_OK;
                break;
            case 201L:
                // all good, created
                LOGDEBUG("file created sucessfully\n");
                code = EUCA_OK;
                break;
            case 408L:
                // timeout, retry
                LOGWARN("server responded with HTTP code %ld (timeout) for %s\n", httpcode, url);
                code = EUCA_TIMEOUT_ERROR;
                break;
            case 500L:
                // internal server error (could be a fluke, so we'll retry)
                LOGWARN("server responded with HTTP code %ld (transient?) for %s\n", httpcode, url);
                break;
            default:
                // some kind of error, will not retry
                LOGERROR("server responded with HTTP code %ld for %s\n", httpcode, url);
                retries = 0;
                break;
            }
        }

        if ((code != EUCA_OK) && (retries > 0)) {
            LOGERROR("upload retry %d of %d will commence in %d seconds for %s\n", TOTAL_RETRIES - retries + 1, TOTAL_RETRIES, timeout, url);
            sleep(timeout);
            fseek(fp, 0L, SEEK_SET);
            timeout <<= 1;
        }

        retries--;
    } while ((code != EUCA_OK) && (retries > 0));
    fclose(fp);

    curl_easy_cleanup(curl);
    return (code);
}

//!
//! Libcurl read callback handler
//!
//! @param[out] buffer the string buffer we write to when reading data
//! @param[in]  size the size of each members for fread()
//! @param[in]  nitems the number of items passed to fread()
//! @param[in]  params a transparent pointer to the libcurl read_request structure.
//!
//! @return The number of bytes read on success or -1 on error.
//!
//! @pre Both buffer and params must not be NULL.
//!
//! @post On success, the data is read and contained into our buffer parameter. If buffer or params is NULL,
//!       than a SIGABORT signal will be thrown.
//!
static size_t read_data(char *buffer, size_t size, size_t nitems, void *params)
{
    int items_read = 0;
    int percent = 0;
    FILE *fp = NULL;
    time_t prev = 0;
    time_t now = 0;
    long long bytes_read = 0;
    long long bytes_file = 0;

    assert(buffer != NULL);
    assert(params != NULL);

    fp = ((struct read_request *)params)->fp;
    do {
        items_read += fread(buffer, size, (nitems - items_read), fp);
    } while ((items_read != nitems) && !feof(fp));

    ((struct read_request *)params)->total_read += (items_read * size);
    ((struct read_request *)params)->total_calls++;

    if (((struct read_request *)params)->total_calls % 50 == 0) {
        prev = ((struct read_request *)params)->timestamp;
        now = time(NULL);
        if ((now - prev) > 10) {
            ((struct read_request *)params)->timestamp = now;
            bytes_read = ((struct read_request *)params)->total_read;
            bytes_file = ((struct read_request *)params)->file_size;
            percent = (int)((bytes_read * 100) / bytes_file);
            LOGDEBUG("upload progress %lld/%lld bytes (%d%%)\n", bytes_read, bytes_file, percent);
        }
    }

    return (items_read);
}

//!
//! libcurl write handler
//!
//! @param[in] buffer the string buffer we write into when reading.
//! @param[in] size the size of each members for fread()
//! @param[in] nmemb the number of items passed to fread()
//! @param[in] params a transparent pointer to the libcurl read_request structure.
//!
//! @return The number of bytes written or -1 on failture.
//!
//! @pre Both params and buffer parameters must not be NULL.
//!
//! @post On success, we wrote our buffer content to the file descriptor. If buffer or params is NULL,
//!       than a SIGABORT signal will be thrown.
//!
static size_t write_data(void *buffer, size_t size, size_t nmemb, void *params)
{
    FILE *fp = NULL;
    int wrote = -1;

    assert(buffer != NULL);
    assert(params != NULL);

    fp = ((struct write_request *)params)->fp;
    wrote = fwrite(buffer, size, nmemb, fp);
    ((struct write_request *)params)->total_wrote += wrote;
    ((struct write_request *)params)->total_calls++;

    return (wrote);
}

//!
//! Converts hex character to integer
//!
//! @param[in] ch the character to convert
//!
//! @return The converted value
//!
static char hch_to_int(char ch)
{
    return (isdigit(ch) ? (ch - '0') : (10 + tolower(ch) - 'a'));
}

//!
//! Converts integer to hex character
//!
//! @param[in] i the byte to convert
//!
//! @return the converted value
//!
static char int_to_hch(char i)
{
    static char hex[] = "0123456789ABCDEF";
    return (hex[i & 15]);
}

//!
//! Converts a string to url-encoded string (which must be freed)
//!
//! @param[in] unencoded the URL string to encode
//!
//! @return The encoded string on success or NULL on failure.
//!
//! @pre The unencoded parameter must not be NULL
//!
//! @note Caller is responsible to free the returned memory
//!
char *url_encode(const char *unencoded)
{
    char *pe = NULL;
    char *encoded = NULL;
    const char *pu = NULL;

    if (!unencoded)
        return (NULL);

    if ((encoded = EUCA_ALLOC(((strlen(unencoded) * 3) + 1), sizeof(char))) == NULL)
        return (NULL);

    pu = unencoded;
    pe = encoded;
    while (*pu) {
        if (isalnum(*pu) || (*pu == '-') || (*pu == '_') || (*pu == '.') || (*pu == '~'))
            *pe++ = *pu;
        else if (*pu == ' ')
            *pe++ = '+';
        else {
            *pe++ = '%';
            *pe++ = int_to_hch((*pu) >> 4);
            *pe++ = int_to_hch((*pu) & 15);
        }
        pu++;
    }
    *pe = '\0';

    return (encoded);
}

//!
//! Converts a url-encoded string to regular (which must be freed)
//!
//! @param[in] encoded the URL string to decode
//!
//! @return The unencoded string on success or NULL on failure.
//!
//! @pre The encoded parameter must not be NULL
//!
//! @note Caller is responsible to free the returned memory
//!
char *url_decode(const char *encoded)
{
    char *pu = NULL;
    char *unencoded = NULL;
    const char *pe = NULL;

    if (!encoded)
        return (NULL);

    if ((unencoded = EUCA_ALLOC((strlen(encoded) + 1), sizeof(char))) == NULL)
        return (NULL);

    pe = encoded;
    pu = unencoded;
    while (*pe) {
        if (*pe == '%') {
            if (pe[1] && pe[2]) {
                *pu++ = ((hch_to_int(pe[1]) << 4) | hch_to_int(pe[2]));
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

    return (unencoded);
}

//! @TODO merge this with objectstorage_get_digest
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
char *http_get2str(const char *url)
{
    char *http_reply_str = NULL;
    char *http_reply_path = strdup("/tmp/http-reply-XXXXXX");

    if (!http_reply_path) {
        LOGERROR("out of memory (failed to allocate an http reply path)\n");
        return http_reply_path;
    }

    int tmp_fd = safe_mkstemp(http_reply_path);
    if (tmp_fd < 0) {
        LOGERROR("failed to create an http reply file %s\n", http_reply_path);
    } else {
        close(tmp_fd);                 // objectstorage_ routine will reopen the file

        // download a fresh http_reply
        if (http_get(url, http_reply_path) != 0) {
            LOGERROR("failed to download http reply to %s\n", http_reply_path);
        } else {
            http_reply_str = file2strn(http_reply_path, 2000000);
        }
        unlink(http_reply_path);
    }
    EUCA_FREE(http_reply_path);
    return http_reply_str;
}

//!
//! Process an HTTP get request to the given URL.
//!
//! @param[in] url the request URL
//! @param[in] outfile path to the input file to be used by curl WRITERs
//!
//! @return The result of http_get_timeout()
//!
//! @see http_get_timeout()
//!
int http_get(const char *url, const char *outfile)
{
    return (http_get_timeout(url, outfile, TOTAL_RETRIES, FIRST_TIMEOUT, 0, 0));
}

//!
//! Process an HTTP get request to the given URL with a given timeout.
//!
//! @param[in] url the request URL
//! @param[in] outfile path to the input file to be used by curl WRITERs
//! @param[in] total_retries number of retries to execute the get operation
//! @param[in] first_timeout number of seconds to wait between attemps. Each attemp will multiply the value by 2.
//! @param[in] connect_timeout the libcurl connect timeout (libcurl option CURLOPT_CONNECTTIMEOUT)
//! @param[in] total_timeout the libcurl total timeout value (libcurl option CURLOPT_TIMEOUT)
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: on failure
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_TIMEOUT_ERROR: if the operation timed out
//!         \li EUCA_ACCESS_ERROR: if we fail to access the outfile.
//!
//! @pre \li Both url and outfile parameters must not be NULL.
//!      \li The url parameter must start with "http://"
//!
//! @post On success, the get request has been processed successfully
//!
int http_get_timeout(const char *url, const char *outfile, int total_retries, int first_timeout, int connect_timeout, int total_timeout)
{
    int code = EUCA_ERROR;
    int retries = 0;
    int timeout = 0;
    long httpcode = 0L;
    char error_msg[CURL_ERROR_SIZE] = { 0 };
    FILE *fp = NULL;
    CURL *curl = NULL;
    CURLcode result = CURLE_OK;
    struct write_request params = { 0 };

    if (!url || !outfile) {
        LOGERROR("invalid params: outfile=%s, url=%s\n", SP(outfile), SP(url));
        return (EUCA_INVALID_ERROR);
    }

    LOGDEBUG("downloading %s\n", outfile);
    LOGDEBUG("from %s\n", url);

    /* isolate the PATH in the URL as it will be needed for signing */
    if (strncasecmp(url, "http://", 7) != 0) {
        LOGERROR("URL must start with http://...\n");
        return (EUCA_INVALID_ERROR);
    }

    if ((fp = fopen64(outfile, "w")) == NULL) {
        LOGERROR("failed to open %s for writing\n", outfile);
        return (EUCA_ACCESS_ERROR);
    }
    setbuf(fp, NULL);

    if ((curl = curl_easy_init()) == NULL) {
        LOGERROR("could not initialize libcurl\n");
        fclose(fp);
        return (EUCA_ERROR);
    }

    curl_easy_setopt(curl, CURLOPT_ERRORBUFFER, error_msg);
    curl_easy_setopt(curl, CURLOPT_URL, url);

    curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L);

    /* set up the default write function, but possibly override it below, if compression is desired and possible */
    params.fp = fp;
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &params);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);

    if (connect_timeout > 0) {
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, connect_timeout);
    }

    if (total_timeout > 0) {
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, total_timeout);
    }

    LOGDEBUG("writing %s output to %s\n", "GET", outfile);

    retries = total_retries;
    timeout = first_timeout;
    do {
        params.total_wrote = 0L;
        params.total_calls = 0L;

        result = curl_easy_perform(curl);   /* do it */
        LOGDEBUG("wrote %lld bytes in %lld writes\n", params.total_wrote, params.total_calls);

        if (result) {
            // curl error (connection or transfer failed)
            LOGERROR("%s (%d)\n", error_msg, result);
        } else {
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpcode);
            //! @TODO pull out response message, too
            switch (httpcode) {
            case 200L:
                // all good
                LOGDEBUG("saved image in %s\n", outfile);
                code = EUCA_OK;
                break;
            case 408L:
                // timeout, retry
                LOGWARN("server responded with HTTP code %ld (timeout) for %s\n", httpcode, url);
                code = EUCA_TIMEOUT_ERROR;
                break;
            case 404L:
                LOGWARN("server responded with HTTP code %ld (file not found) for %s\n", httpcode, url);
                break;
            default:
                // some kind of error
                LOGERROR("server responded with HTTP code %ld for %s\n", httpcode, url);
                retries = 0;
                break;
            }
        }

        if ((code != EUCA_OK) && (retries > 0)) {
            LOGERROR("download retry %d of %d will commence in %d sec for %s\n", retries, total_retries, timeout, url);
            sleep(timeout);
            fseek(fp, 0L, SEEK_SET);
            timeout <<= 1;
        }

        retries--;
    } while ((code != EUCA_OK) && (retries > 0));
    fclose(fp);

    if (code != EUCA_OK) {
        LOGWARN("removing %s\n", outfile);
        remove(outfile);
    }
    curl_easy_cleanup(curl);
    return (code);
}

#ifdef _UNIT_TEST
//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK
//!
int main(int argc, char **argv)
{
#define _T(_S)                                                \
{                                                             \
	char *__e = url_encode (_S);                              \
	char *__u = url_decode (__e);                             \
	printf("orig: %s\nenco: %s\ndeco: %s\n\n", _S, __e, __u); \
	EUCA_FREE(__e);                                           \
	EUCA_FREE(__u);                                           \
}

    _T("hello world");
    _T("~`!1@2#3$4%5^6&7*8(9)0_-+={[}]|\\:;\"'<,>.?/");
    _T("[datastore1 (1)] windows 2003 enterprise/windows 2003 enterprise.vmx");
    return (EUCA_OK);

#undef _T
}
#endif /* _UNIT_TEST */
