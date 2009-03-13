#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h> /* close */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <curl/curl.h>
#include <curl/easy.h>
#include <zlib.h>
#include <fcntl.h> /* open */
#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"

#define CHUNK 262144 /* buffer size for decompression operations */
#define BUFSIZE 4096 /* should be big enough for CERT and the signature */
#define STRSIZE 245 /* for short strings: files, hosts, URLs */
#define WALRUS_ENDPOINT "/services/Walrus"
#define DEFAULT_HOST_PORT "localhost:8773"
#define GET_IMAGE_CMD "GetDecryptedImage"
#define GET_OBJECT_CMD "GetObject"

static size_t write_data      (void *buffer, size_t size, size_t nmemb, void *userp);
static size_t write_data_zlib (void *buffer, size_t size, size_t nmemb, void *userp);
static size_t write_header    (void *buffer, size_t size, size_t nmemb, void *userp);
static long long total_wrote;
static long long total_calls;

struct zlib_and_fp {
	z_stream strm; /* stream struct used by zlib */
	FILE * fp; /* output file pointer to be used by curl WRITERs */
	int ret; /* return value of last inflate() call */
};

/* downloads a decrypted image from Walrus based on the manifest URL,
 * saves it to outfile */
static int walrus_request (const char * walrus_op, const char * verb, const char * requested_url, const char * outfile, const int do_compress)
{
	int code = ERROR;
	char url [BUFSIZE];

    snprintf (url, BUFSIZE, "%s%s", requested_url, do_compress?"?IsCompressed=true":"");
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
	curl_easy_setopt (curl, CURLOPT_HEADERFUNCTION, write_header);
    //curl_easy_setopt (curl, CURLOPT_TIMEOUT, 3600L);      /* TODO: decrease? increase? */
    //curl_easy_setopt (curl, CURLOPT_CONNECTTIMEOUT, 10L); /* TODO: decrease? increase? */
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
	
	/* set up the write function, which depends on compression */
	struct zlib_and_fp zfp;
	if (do_compress) {
		/* allocate zlib inflate state */
		zfp.strm.zalloc = Z_NULL;
	    zfp.strm.zfree = Z_NULL;
	    zfp.strm.opaque = Z_NULL;
	    zfp.strm.avail_in = 0;
	    zfp.strm.next_in = Z_NULL;
	    int ret = inflateInit(&(zfp.strm));
	    if (ret != Z_OK) {
			logprintfl (EUCAERROR, "walrus_request(): failed to initialize zlib (err=%d)\n", ret);
			return ERROR;
		}
		curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data_zlib);
		curl_easy_setopt (curl, CURLOPT_WRITEDATA, &zfp);
	} else {
		curl_easy_setopt (curl, CURLOPT_WRITEFUNCTION, write_data);
		curl_easy_setopt (curl, CURLOPT_WRITEDATA, fp);
	}

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
	if (do_compress) {
		(void)inflateEnd(&(zfp.strm));
		if (zfp.ret != Z_STREAM_END) {
			logprintfl (EUCAERROR, "walrus_request(): broken compressed stream (err=%d)\n", zfp.ret);
			remove_outfile = 1;
		}
	}
	
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

/* report a zlib or i/o error */
static void zerr(int ret)
{
    switch (ret) {
    case Z_ERRNO:
        logprintfl (EUCAERROR, "error: zlib: failed to write\n");
        break;
    case Z_STREAM_ERROR:
        logprintfl (EUCAERROR, "error: zlib: invalid compression level\n");
        break;
    case Z_DATA_ERROR:
        logprintfl (EUCAERROR, "error: zlib: invalid or incomplete deflate data\n");
        break;
    case Z_MEM_ERROR:
        logprintfl (EUCAERROR, "error: zlib: out of memory\n");
        break;
    case Z_VERSION_ERROR:
        logprintfl (EUCAERROR, "error: zlib: zlib version mismatch!\n");
    }
}

static void print_data (unsigned char *buf, const int size)
{
    int i;

    for (i=0; i<size; i++) {
        int c = buf [i];
        if (c>=' ' && c<='~') 
            printf ("%c", c);
        else
            printf ("\\%o", c);
    }
    printf ("\n");
}

static size_t write_data_zlib (void *buffer, size_t size, size_t nmemb, void *param)
{
	if (param==NULL) return -1;
	z_stream * strm = &(((struct zlib_and_fp *)param)->strm);
	FILE * fp = ((struct zlib_and_fp *)param)->fp;
	
	int ret;
	int wrote = 0;
	unsigned char out [CHUNK];

    print_data (buffer, size*nmemb);

	strm->avail_in = size * nmemb;	
	strm->next_in = (unsigned char *)buffer;
	do {
		strm->avail_out = CHUNK;
		strm->next_out = out;

		((struct zlib_and_fp *)param)->ret = ret = inflate (strm, Z_NO_FLUSH);
		assert (ret != Z_STREAM_ERROR);  /* state not clobbered */
		switch (ret) {
			case Z_NEED_DICT:
				ret = Z_DATA_ERROR;     /* and fall through */
			case Z_DATA_ERROR:
			case Z_MEM_ERROR:
				(void)inflateEnd(strm);
                logprintfl (EUCAERROR, "error: write_data_zlib(): inflate() failed with %d\n", ret);
                zerr (ret);
				return ret;
		}

		unsigned have = CHUNK - strm->avail_out;
		if (fwrite (out, 1, have, fp) != have || ferror(fp)) {
            logprintfl (EUCAERROR, "error: write_data_zlib(): failed to write\n");
			(void)inflateEnd(strm);
			return Z_ERRNO;
		}
		wrote += have;
	} while (strm->avail_out == 0); 

	total_wrote += wrote;
	total_calls++;
	return wrote;
}

static size_t write_header (void *buffer, size_t size, size_t nmemb, void *userp)
{
    /* here in case we want to do something with headers */
	return size * nmemb;
}

