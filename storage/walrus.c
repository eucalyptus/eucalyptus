#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h> /* close */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <curl/curl.h>
#include <curl/easy.h>
#include <fcntl.h> /* open */
#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"

#define BUFSIZE 4096 /* should be big enough for CERT and the signature */
#define STRSIZE 245 /* for short strings: files, hosts, URLs */
#define WALRUS_ENDPOINT "/services/Walrus"
#define DEFAULT_HOST_PORT "localhost:8773"
#define GET_IMAGE_CMD "GetDecryptedImage"
#define GET_OBJECT_CMD "GetObject"

static size_t write_data   (void *buffer, size_t size, size_t nmemb, void *userp);
static size_t write_header (void *buffer, size_t size, size_t nmemb, void *userp);
static long long total_wrote;
static long long total_calls;

/* downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
static int walrus_request (const char * walrus_op, const char * verb, const char * url, const char * outfile)
{
	int code = ERROR;
	
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
		return code;
	}

	char error_msg [CURL_ERROR_SIZE];
	curl_easy_setopt (curl, CURLOPT_ERRORBUFFER, error_msg);
	curl_easy_setopt (curl, CURLOPT_URL, url); 
	curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data);
	curl_easy_setopt (curl, CURLOPT_HEADERFUNCTION, write_header);
	curl_easy_setopt (curl, CURLOPT_WRITEDATA, fp);
    curl_easy_setopt (curl, CURLOPT_TIMEOUT, 3600L);         /* TODO: decrease? increase? */
    curl_easy_setopt (curl, CURLOPT_CONNECTTIMEOUT, 3600L);  /* TODO: decrease? increase? */
    if (strncmp (verb, "GET", 4)==0) {
        curl_easy_setopt (curl, CURLOPT_HTTPGET, 1L);
    } else if (strncmp (verb, "HEAD", 5)==0) {
        /* TODO: HEAD isn't very useful atm since we don't look at headers */
        curl_easy_setopt (curl, CURLOPT_NOBODY, 1L);
    } else {
        logprintfl (EUCAERROR, "walrus_request(): invalid HTTP verb %s\n", verb);
        return ERROR; /* TODO: dealloc structs before returning! */
    }
	/* curl_easy_setopt (curl, CURLOPT_IGNORE_CONTENT_LENGTH, 1L); *//* potentially useful? */
    /* curl_easy_setopt (curl, CURLOPT_VERBOSE, 1); *//* too much information */

	struct curl_slist * headers = NULL; /* beginning of a DLL with headers */
	headers = curl_slist_append (headers, "Authorization: Euca");

	char op_hdr [STRSIZE];
	if(walrus_op != NULL) {
	    snprintf (op_hdr, STRSIZE, "EucaOperation: %s", walrus_op);
	    headers = curl_slist_append (headers, op_hdr);
	}

	time_t t = time(NULL);
	char * date_str = asctime(localtime(&t)); /* points to a static area */
	if (date_str==NULL) return ERROR;
	assert (strlen(date_str)+7<=STRSIZE);
	date_str [strlen(date_str)-1] = '\0'; /* trim off the newline */
	char date_hdr [STRSIZE];
	snprintf (date_hdr, STRSIZE, "Date: %s", date_str);
	headers = curl_slist_append (headers, date_hdr);

	char * cert_str = euca_get_cert (0); /* read the cloud-wide cert */
	if (cert_str==NULL) return ERROR;
	char * cert64_str = base64_enc ((unsigned char *)cert_str, strlen(cert_str));
	assert (strlen(cert64_str)+11<=BUFSIZE);
	char cert_hdr [BUFSIZE];
	snprintf (cert_hdr, BUFSIZE, "EucaCert: %s", cert64_str);
    logprintfl (EUCADEBUG2, "walrus_request(): base64 certificate, %s\n", get_string_stats(cert64_str));
	headers = curl_slist_append (headers, cert_hdr);
	free (cert_str);

	char * sig_str = euca_sign_url (verb, date_str, url_path); /* create Walrus-compliant sig */
	if (sig_str==NULL) return ERROR;
	assert (strlen(sig_str)+16<=BUFSIZE);
	char sig_hdr [BUFSIZE];
	snprintf (sig_hdr, BUFSIZE, "EucaSignature: %s", sig_str);
	headers = curl_slist_append (headers, sig_hdr);

	curl_easy_setopt (curl, CURLOPT_HTTPHEADER, headers); /* register headers */

	total_wrote = total_calls = 0L;
	if (walrus_op) {
        logprintfl (EUCADEBUG, "walrus_request(): writing %s/%s output to %s\n", verb, walrus_op, outfile);
	} else {
        logprintfl (EUCADEBUG, "walrus_request(): writing %s output to %s\n", verb, outfile);
	}
	result = curl_easy_perform (curl); /* do it */
    logprintfl (EUCADEBUG, "walrus_request(): wrote %ld bytes in %ld writes\n", total_wrote, total_calls);
	fclose (fp);
	
    int remove_outfile = 0;
	if (result) {
		logprintfl (EUCAERROR, "walrus_request(): (%d): %s\n", result, error_msg);
        remove_outfile = 1;
		
	} else {
		long httpcode;
		
		curl_easy_getinfo (curl, CURLINFO_RESPONSE_CODE, &httpcode);
        /* TODO: pull out response message, too */
		switch (httpcode) {
		case 200L: /* all good */
			logprintfl (EUCAINFO, "walrus_request(): saved image in %s\n", outfile);
			code = OK;
			break;
			
		default: /* some kind of error */
			logprintfl (EUCAERROR, "walrus_request(): server responded with HTTP code %ld\n", httpcode);
            logcat (EUCADEBUG, outfile); /* dump the error from outfile into the log */
            remove_outfile = 1; 
		}
	}
    
    if ( remove_outfile ) {
        logprintfl (EUCAINFO, "walrus_request(): due to error, removing %s\n", outfile);
        remove (outfile);
    }

	free (cert64_str);
	free (sig_str);
	curl_slist_free_all (headers);
	curl_easy_cleanup (curl);
	return code;
}

/* downloads a Walrus object from the URL, saves it to outfile */
int walrus_object_by_url (const char * url, const char * outfile)
{
    return walrus_request (NULL, "GET", url, outfile);
}

/* downloads a Walrus object from the default Walrus endpoing,
 * so only the path is needed; saves object to outfile */
int walrus_object_by_path (const char * path, const char * outfile)
{
	char url [STRSIZE];
	snprintf (url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, WALRUS_ENDPOINT, path);
	return walrus_object_by_url (url, outfile);
}

/* downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
int walrus_image_by_manifest_url (const char * url, const char * outfile)
{
    return walrus_request (GET_IMAGE_CMD, "GET", url, outfile);
}

/* gets a decrypted image from the default Walrus endpoint, 
 * so only manifest path is needed; saves image to outfile */
int walrus_image_by_manifest_path (const char * manifest_path, const char * outfile)
{
	char url [STRSIZE];
	snprintf (url, STRSIZE, "http://%s%s/%s", DEFAULT_HOST_PORT, WALRUS_ENDPOINT, manifest_path);
	return walrus_image_by_manifest_url (url, outfile);
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
        if ( (e=walrus_object_by_url (url, new_digest)) != 0 ) {
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

/* libcurl write handlers */
static size_t write_data (void *buffer, size_t size, size_t nmemb, void *userp)
{
	int wrote = 0;
	if (userp) {
		wrote = fwrite (buffer, size, nmemb, (FILE *) userp);
        total_wrote += wrote;
        total_calls++;
	}
	return wrote;
}

static size_t write_header (void *buffer, size_t size, size_t nmemb, void *userp)
{
    /* here in case we want to do something with headers */
	return size * nmemb;
}
