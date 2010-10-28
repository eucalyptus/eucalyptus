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
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include "config.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h> /* close */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h> /* open */
#include <curl/curl.h>
#include <curl/easy.h>
#if defined(HAVE_ZLIB_H)
#include <zlib.h>
#endif
#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"

#define TOTAL_RETRIES 10 /* download is retried in case of connection problems */
#define FIRST_TIMEOUT 4 /* in seconds, goes in powers of two afterwards */
#define CHUNK 262144 /* buffer size for decompression operations */
#define BUFSIZE 4096 /* should be big enough for CERT and the signature */
#define STRSIZE 245 /* for short strings: files, hosts, URLs */
#define WALRUS_ENDPOINT "/services/Walrus"
#define DEFAULT_HOST_PORT "localhost:8773"
#define GET_IMAGE_CMD "GetDecryptedImage"
#define GET_OBJECT_CMD "GetObject"

static size_t write_data      (void *buffer, size_t size, size_t nmemb, void *userp);
static size_t write_header    (void *buffer, size_t size, size_t nmemb, void *userp);
#if defined(ZLIB_VERNUM) && (ZLIB_VERNUM >= 0x1204)
static size_t write_data_zlib (void *buffer, size_t size, size_t nmemb, void *userp);
static void zerr (int ret, char * where);
#define CAN_GZIP
#endif

struct request {
	FILE * fp; /* output file pointer to be used by curl WRITERs */
    long long total_wrote; /* bytes written during the operation */
    long long total_calls; /* write calls made during the operation */
#if defined (CAN_GZIP)
	z_stream strm; /* stream struct used by zlib */
	int ret; /* return value of last inflate() call */
#endif
};

/* downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
static int walrus_request (const char * walrus_op, const char * verb, const char * requested_url, const char * outfile, const int do_compress)
{
	int code = ERROR;
	char url [BUFSIZE];

    strncpy (url, requested_url, BUFSIZE);
#if defined(CAN_GZIP)
    if (do_compress)
        snprintf (url, BUFSIZE, "%s%s", requested_url, "?IsCompressed=true");
#endif
    logprintfl (EUCAINFO, "walrus_request(): downloading %s\n", outfile);
    logprintfl (EUCAINFO, "                  from %s\n", url);

	/* isolate the PATH in the URL as it will be needed for signing */
	char * url_path; 
	if (strncasecmp (url, "http://", 7)!=0) {
		logprintfl (EUCAERROR, "walrus_request(): URL must start with http://...\n");
		return code;
	}
	if ((url_path=strchr(url+7, '/'))==NULL) { /* find first '/' after hostname */
		logprintfl (EUCAERROR, "walrus_request(): URL has no path\n");
		return code;
	}
	
	if (euca_init_cert()) {
		logprintfl (EUCAERROR, "walrus_request(): failed to initialize certificate\n");
		return code;
	} 

	FILE * fp = fopen64 (outfile, "w");
	if (fp==NULL) {
		logprintfl (EUCAERROR, "walrus_request(): failed to open %s for writing\n", outfile);
		return code;
	}

	CURL * curl;
	CURLcode result;
	curl = curl_easy_init ();
	if (curl==NULL) {
		logprintfl (EUCAERROR, "walrus_request(): could not initialize libcurl\n");
		fclose(fp);
		return code;
	}

	char error_msg [CURL_ERROR_SIZE];
	curl_easy_setopt (curl, CURLOPT_ERRORBUFFER, error_msg);
	curl_easy_setopt (curl, CURLOPT_URL, url); 
	curl_easy_setopt (curl, CURLOPT_HEADERFUNCTION, write_header);

    if (strncmp (verb, "GET", 4)==0) {
        curl_easy_setopt (curl, CURLOPT_HTTPGET, 1L);
    } else if (strncmp (verb, "HEAD", 5)==0) {
        /* TODO: HEAD isn't very useful atm since we don't look at headers */
        curl_easy_setopt (curl, CURLOPT_NOBODY, 1L);
    } else {
	fclose(fp);
        logprintfl (EUCAERROR, "walrus_request(): invalid HTTP verb %s\n", verb);
        return ERROR; /* TODO: dealloc structs before returning! */
    }
	
	/* set up the default write function, but possibly override
     * it below, if compression is desired and possible */
	struct request params;
    params.fp = fp;
    curl_easy_setopt (curl, CURLOPT_WRITEDATA, &params);
    curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data);
#if defined(CAN_GZIP)
	if (do_compress) {
		curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data_zlib);
	}
#endif

	struct curl_slist * headers = NULL; /* beginning of a DLL with headers */
	headers = curl_slist_append (headers, "Authorization: Euca");

	char op_hdr [STRSIZE];
	if(walrus_op != NULL) {
	    snprintf (op_hdr, STRSIZE, "EucaOperation: %s", walrus_op);
	    headers = curl_slist_append (headers, op_hdr);
	}

	time_t t = time(NULL);
	char date_str [26];
	if (ctime_r(&t, date_str)==NULL) {
        fclose(fp);
        return ERROR;
	}
	assert (strlen(date_str)+7<=STRSIZE);
    char * newline = strchr (date_str, '\n');
    if (newline!=NULL) { * newline = '\0'; } // remove newline that terminates asctime() output
	char date_hdr [STRSIZE];
	snprintf (date_hdr, STRSIZE, "Date: %s", date_str);
	headers = curl_slist_append (headers, date_hdr);

	char * cert_str = euca_get_cert (0); /* read the cloud-wide cert */
	if (cert_str==NULL) {
	       fclose(fp);
       	       return ERROR;
	}
	char * cert64_str = base64_enc ((unsigned char *)cert_str, strlen(cert_str));
	assert (strlen(cert64_str)+11<=BUFSIZE);
	char cert_hdr [BUFSIZE];
	snprintf (cert_hdr, BUFSIZE, "EucaCert: %s", cert64_str);
    logprintfl (EUCADEBUG2, "walrus_request(): base64 certificate, %s\n", get_string_stats(cert64_str));
	headers = curl_slist_append (headers, cert_hdr);
	free (cert64_str);
	free (cert_str);

	char * sig_str = euca_sign_url (verb, date_str, url_path); /* create Walrus-compliant sig */
	if (sig_str==NULL) {
	       fclose(fp);
       	       return ERROR;
	}
	assert (strlen(sig_str)+16<=BUFSIZE);
	char sig_hdr [BUFSIZE];
	snprintf (sig_hdr, BUFSIZE, "EucaSignature: %s", sig_str);
	headers = curl_slist_append (headers, sig_hdr);

	curl_easy_setopt (curl, CURLOPT_HTTPHEADER, headers); /* register headers */
    if (walrus_op) {
        logprintfl (EUCADEBUG, "walrus_request(): writing %s/%s output to %s on '%s'\n", verb, walrus_op, outfile, date_str);
    } else {
        logprintfl (EUCADEBUG, "walrus_request(): writing %s output to %s on '%s'\n", verb, outfile, date_str);
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

        result = curl_easy_perform (curl); /* do it */
        logprintfl (EUCADEBUG, "walrus_request(): wrote %ld bytes in %ld writes\n", params.total_wrote, params.total_calls);

#if defined(CAN_GZIP)
        if (do_compress) {
            inflateEnd(&(params.strm));
            if (params.ret != Z_STREAM_END) {
                zerr (params.ret, "walrus_request");
            }
        }
#endif

        if (result) { // curl error (connection or transfer failed)
            logprintfl (EUCAERROR,     "walrus_request(): %s (%d)\n", error_msg, result);

        } else {
            long httpcode;
            curl_easy_getinfo (curl, CURLINFO_RESPONSE_CODE, &httpcode);
            /* TODO: pull out response message, too */

            switch (httpcode) {
            case 200L: /* all good */
                logprintfl (EUCAINFO, "walrus_request(): saved image in %s\n", outfile);
                code = OK;
                break;
	    case 408L: /* timeout, retry */
	      logprintfl (EUCAWARN, "walrus_request(): server responded with HTTP code %ld (timeout)\n", httpcode);
	      //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
	      break;
            default: /* some kind of error */
                logprintfl (EUCAERROR, "walrus_request(): server responded with HTTP code %ld\n", httpcode);
                //logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
                retries=0;
            }
        }
        
        if (code!=OK && retries>0) {
            logprintfl (EUCAERROR, "                  download retry %d of %d will commence in %d seconds\n", retries, TOTAL_RETRIES, timeout);
            sleep (timeout);
            fseek (fp, 0L, SEEK_SET);
            timeout <<= 1;
        }
        
        retries--;
    } while (code!=OK && retries>0);
    fclose (fp);

    if ( code != OK ) {
        logprintfl (EUCAINFO, "walrus_request(): due to error, removing %s\n", outfile);
        remove (outfile);
    }

	free (sig_str);
	curl_slist_free_all (headers);
	curl_easy_cleanup (curl);
	return code;
}

/* downloads a Walrus object from the URL, saves it to outfile */
int walrus_object_by_url (const char * url, const char * outfile, const int do_compress)
{
    return walrus_request (NULL, "GET", url, outfile, do_compress);
}

/* downloads a Walrus object from the default Walrus endpoint,
 * so only the path is needed; saves object to outfile */
int walrus_object_by_path (const char * path, const char * outfile, const int do_compress)
{
	char url [STRSIZE];
	snprintf (url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, WALRUS_ENDPOINT, path);
	return walrus_object_by_url (url, outfile, do_compress);
}

/* downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
int walrus_image_by_manifest_url (const char * url, const char * outfile, const int do_compress)
{
    return walrus_request (GET_IMAGE_CMD, "GET", url, outfile, do_compress);
}

/* gets a decrypted image from the default Walrus endpoint, 
 * so only manifest path is needed; saves image to outfile */
int walrus_image_by_manifest_path (const char * manifest_path, const char * outfile, const int do_compress)
{
	char url [STRSIZE];
	snprintf (url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, WALRUS_ENDPOINT, manifest_path);
	return walrus_image_by_manifest_url (url, outfile, do_compress);
}

/* downloads a digest of an image and compare it to file at digest_path 
 * returns 0 if same, -N if different, N if error */
int walrus_verify_digest (const char * url, const char * old_digest)
{
    int e = ERROR;
    char * new_digest = strdup ("/tmp/walrus-digest-XXXXXX");
    int tmp_fd = mkstemp (new_digest);
    if (tmp_fd<0) {
        logprintfl (EUCAERROR, "error: failed to create a digest file %s\n", new_digest); 
    } else {
        close (tmp_fd); /* walrus routine will reopen the file */
 
        /* download a fresh digest */
        if ( (e=walrus_object_by_url (url, new_digest, 0)) != 0 ) {
            logprintfl (EUCAERROR, "error: failed to download digest to %s\n", new_digest);

        } else {
            /* compare the two */
            e = diff (new_digest, old_digest);
        }
    }
    unlink (new_digest);
    free (new_digest);
    return e;
}

/* libcurl header write handler */
static size_t write_header (void *buffer, size_t size, size_t nmemb, void *params)
{
    /* here in case we want to do something with headers */
	return size * nmemb;
}

/* libcurl write handler */
static size_t write_data (void *buffer, size_t size, size_t nmemb, void *params)
{
	assert (params !=NULL);
	FILE * fp = ((struct request *)params)->fp;
	int wrote = fwrite (buffer, size, nmemb, fp);
    ((struct request *)params)->total_wrote += wrote;
    ((struct request *)params)->total_calls++;

	return wrote;
}

#if defined(CAN_GZIP)

/* unused testing function */
static void print_data (unsigned char *buf, const int size)
{
    int i;

    for (i=0; i<size; i++) {
        int c = buf [i];
        if (c>' ' && c<='~') 
            printf (" %c", c);
        else
            printf (" %x", c);
    }
    printf ("\n");
}

/* report on a zlib error */
static void zerr (int ret, char * where)
{
    switch (ret) {
    case Z_ERRNO:
        logprintfl (EUCAERROR, "error: %s(): zlib: failed to write\n", where);
        break;
    case Z_STREAM_ERROR:
        logprintfl (EUCAERROR, "error: %s(): zlib: invalid compression level\n", where);
        break;
    case Z_DATA_ERROR:
        logprintfl (EUCAERROR, "error: %s(): zlib: invalid or incomplete deflate data\n", where);
        break;
    case Z_MEM_ERROR:
        logprintfl (EUCAERROR, "error: %s(): zlib: out of memory\n", where);
        break;
    case Z_VERSION_ERROR:
        logprintfl (EUCAERROR, "error: %s(): zlib: zlib version mismatch!\n", where);
    }
}

/* libcurl write handler for gzipped streams */
static size_t write_data_zlib (void *buffer, size_t size, size_t nmemb, void *params)
{
	assert (params !=NULL);
	z_stream * strm = &(((struct request *)params)->strm);
	FILE * fp = ((struct request *)params)->fp;
    unsigned char out [CHUNK];
	int wrote = 0;
	int ret;

	strm->avail_in = size * nmemb;	
	strm->next_in = (unsigned char *)buffer;
	do {
		strm->avail_out = CHUNK;
		strm->next_out = out;

		((struct request *)params)->ret = ret = inflate (strm, Z_NO_FLUSH);
		switch (ret) {
        case Z_NEED_DICT:
            ret = Z_DATA_ERROR; // ok to fall through 
        case Z_DATA_ERROR:
        case Z_MEM_ERROR:
        case Z_STREAM_ERROR:
            inflateEnd(strm);
            zerr (ret, "write_data_zlib");
            return ret;
		}

		unsigned have = CHUNK - strm->avail_out;
		if (fwrite (out, 1, have, fp) != have || ferror(fp)) {
            logprintfl (EUCAERROR, "error: write_data_zlib(): failed to write\n");
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

