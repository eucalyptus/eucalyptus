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

#define _FILE_OFFSET_BITS 64    // so large-file support works on 32-bit systems
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
#include <openssl/sha.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/ssl.h>
#include <openssl/x509.h>
#include <openssl/err.h>
#include <curl/curl.h>
#include <pthread.h>
#include <regex.h>

#include "euca_auth.h"
#include "misc.h"               /* get_string_stats, logprintf */

#include "eucalyptus.h"

static int initialized = 0;

#define FILENAME 512
static char cert_file[FILENAME];
static char pk_file[FILENAME];
static char hex_digits[16] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

static regex_t *uri_regex = NULL;
//Mutex to guard initialization and compile of the uri_regex
static pthread_mutex_t regex_init_mutex = PTHREAD_MUTEX_INITIALIZER;
//TODO: this pattern does not exclude some invalid URLs, but does require a protocol section
static const char *url_pattern = "([^:?&]+://)([^:/?&]+)(:([0-9]+)?)?(/[^?&=]*)?(\\?(.*)?)?($)";

//Mutex to guard certificate and ssl init to enforce the function as a singleton.
static pthread_mutex_t init_mutex = PTHREAD_MUTEX_INITIALIZER;

int euca_init_cert(void)
{
    if (initialized)
        return 0;               //no need for lock if this is true

    //Lock to enforce the singleton nature of this method, should only actually lock on first use
    pthread_mutex_lock(&init_mutex);
    if (initialized) {
        //Previous holder of lock initialized, so this thread can skip
        pthread_mutex_unlock(&init_mutex);
        return 0;
    }

    char root[] = "";
    char *euca_home = getenv("EUCALYPTUS");
    if (!euca_home) {
        euca_home = root;
    }
    snprintf(cert_file, FILENAME, EUCALYPTUS_KEYS_DIR "/node-cert.pem", euca_home);
    snprintf(pk_file, FILENAME, EUCALYPTUS_KEYS_DIR "/node-pk.pem", euca_home);

#define ERR "Error: required file %s not found by euca_init_cert(). Is $EUCALYPTUS set?\n"
#define OK_MSG  "euca_init_cert(): using file %s\n"
#define CHK_FILE(n) \
        if ((fd=open(n, O_RDONLY))<0) {\
                logprintfl (EUCAERROR, ERR, n); pthread_mutex_unlock(&init_mutex); return 1; \
        } else {\
                close (fd); logprintfl (EUCAINFO, OK_MSG, n); \
        }

    int fd;
    CHK_FILE(cert_file)
        CHK_FILE(pk_file)
        // initialize OpenSSL, must ONLY be called once. Not reentrant
        SSL_load_error_strings();
    if (!SSL_library_init()) {
        logprintfl(EUCAERROR, "euca_init_cert: ssl library init failed\n");
        initialized = 0;
        pthread_mutex_unlock(&init_mutex);
        return 1;
    }

    initialized = 1;
    pthread_mutex_unlock(&init_mutex);
    return 0;
}

/* caller must free the returned string */
char *euca_get_cert(unsigned char options)
{
    if (!initialized)
        euca_init_cert();

    char *cert_str = NULL;
    int s, fp;

    struct stat st;
    if (stat(cert_file, &st) != 0) {
        logprintfl(EUCAERROR, "cannot stat the certificate file %s\n", cert_file);

    } else if ((s = st.st_size * 2) < 1) {  /* *2 because we'll add characters */
        logprintfl(EUCAERROR, "certificate file %s is too small\n", cert_file);

    } else if ((cert_str = malloc(s + 1)) == NULL) {
        logprintfl(EUCAERROR, "out of memory\n");

    } else if ((fp = open(cert_file, O_RDONLY)) < 0) {
        logprintfl(EUCAERROR, "failed to open certificate file %s\n", cert_file);
        EUCA_FREE(cert_str);
        cert_str = NULL;

    } else {
        ssize_t ret = -1;
        int got = 0;

        while (got < s && (ret = read(fp, cert_str + got, 1)) == 1) {
            if (options & CONCATENATE_CERT) {   /* omit all newlines */
                if (cert_str[got] == '\n')
                    continue;
            } else {
                if (options & INDENT_CERT)  /* indent lines 2 through N with TABs */
                    if (cert_str[got] == '\n')
                        cert_str[++got] = '\t';
            }
            got++;
        }

        if (ret != 0) {
            logprintfl(EUCAERROR, "failed to read whole certificate file %s\n", cert_file);
            EUCA_FREE(cert_str);
            cert_str = NULL;

        } else {
            if (options & TRIM_CERT) {
                if (cert_str[got - 1] == '\t' || cert_str[got - 1] == '\n')
                    got--;
                if (cert_str[got - 1] == '\n')
                    got--;      /* because of indenting */
            }
            cert_str[got] = '\0';
        }
        close(fp);
    }
    return cert_str;
}

/* caller must free the returned string */
char *base64_enc(unsigned char *in, int size)
{
    char *out_str = NULL;
    BIO *biomem, *bio64;

    if ((bio64 = BIO_new(BIO_f_base64())) == NULL) {
        logprintfl(EUCAERROR, "BIO_new(BIO_f_base64()) failed\n");
    } else {
        BIO_set_flags(bio64, BIO_FLAGS_BASE64_NO_NL);   /* no long-line wrapping */
        if ((biomem = BIO_new(BIO_s_mem())) == NULL) {
            logprintfl(EUCAERROR, "BIO_new(BIO_s_mem()) failed\n");
        } else {
            bio64 = BIO_push(bio64, biomem);
            if (BIO_write(bio64, in, size) != size) {
                logprintfl(EUCAERROR, "BIO_write() failed\n");
            } else {
                BUF_MEM *buf;
                (void)BIO_flush(bio64);
                BIO_get_mem_ptr(bio64, &buf);
                if ((out_str = malloc(buf->length + 1)) == NULL) {
                    logprintfl(EUCAERROR, "out of memory for Base64 buf\n");
                } else {
                    memcpy(out_str, buf->data, buf->length);
                    out_str[buf->length] = '\0';
                }
            }
        }
        BIO_free_all(bio64);    /* frees both bio64 and biomem */
    }
    return out_str;
}

/* caller must free the returned string */
char *base64_dec(unsigned char *in, int size)
{
    BIO *bio64, *biomem;
    char *buf = NULL;

    buf = malloc(sizeof(char) * size);
    bzero(buf, size);

    if ((bio64 = BIO_new(BIO_f_base64())) == NULL) {
        logprintfl(EUCAERROR, "BIO_new(BIO_f_base64()) failed\n");
    } else {
        BIO_set_flags(bio64, BIO_FLAGS_BASE64_NO_NL);   /* no long-line wrapping */

        if ((biomem = BIO_new_mem_buf(in, size)) == NULL) {
            logprintfl(EUCAERROR, "BIO_new_mem_buf() failed\n");
        } else {
            biomem = BIO_push(bio64, biomem);

            if ((BIO_read(biomem, buf, size)) <= 0) {
                logprintfl(EUCAERROR, "BIO_read() read failed\n");
            }
            //      BIO_free_all(biomem);
        }
        BIO_free_all(bio64);
    }

    return buf;
}

/* Functions for EucalyptusV2 Internal Signing (EUCA2-SHA256-RSA) */

char *hexify(unsigned char *data, int data_len)
{
    if (data == NULL)
        return NULL;
    char *hex_str = (char *)malloc(data_len * 2 + 1);   //2 hex digit chars for each byte plus one null
    if (hex_str == NULL) {
        logprintfl(EUCAERROR, "hexify: cannot allocate memory for the hex string. Returing null.");
        return NULL;
    }
    for (int i = 0; i < data_len; i++) {
        hex_str[i * 2] = hex_digits[(data[i] / 16)];
        hex_str[i * 2 + 1] = hex_digits[(data[i] % 16)];
    }
    hex_str[data_len * 2] = '\0';   //make sure it's null terminated
    return hex_str;
}

char *calc_fingerprint(const char *cert_filename)
{
    if (cert_filename == NULL) {
        logprintfl(EUCAERROR, "calc_fingerprint: got a null filename, returning null");
        return NULL;
    }

    struct stat cert_file_stats;    //file stat structure for getting the size of the file
    unsigned char *cert_buffer = NULL;  //read buffer for certificate
    ssize_t file_buffer_size;   //size of read buffer
    int fd;                     //file descriptor for reading cert file
    int read_size;
    BIO *bio = NULL;
    X509 *x509_cert = NULL;
    const EVP_MD *digest_function;  //digest of the cert
    unsigned int n;

    //Check the file and get the file size for allocation of buffers
    if (stat(cert_filename, &cert_file_stats) == -1) {
        logprintfl(EUCAERROR, "calc_fingerprint: error in stat() of %s\n", cert_filename);
        return NULL;
    }

    file_buffer_size = cert_file_stats.st_size * 2; //double the byte-count, for extra TODO: can this be removed?
    if (!(cert_buffer = malloc(file_buffer_size))) {
        logprintfl(EUCAERROR, "calc_fingerprint: could not allocate memory to read certificate file for fingerprint calculation\n");
        return NULL;
    }

    if ((fd = open(cert_filename, O_RDONLY)) == -1) {
        logprintfl(EUCAERROR, "calc_fingerprint: could not open %s to read certificate for fingerprint calculation\n", cert_filename);
        EUCA_FREE(cert_buffer);
        return NULL;
    }
    //Read in the file, the whole thing
    if ((read_size = read(fd, cert_buffer, file_buffer_size)) == -1) {
        logprintfl(EUCAERROR, "calc_fingerprint: error reading certificate file %s\n", cert_file);
        EUCA_FREE(cert_buffer);
        close(fd);
        return NULL;
    }
    close(fd);

    // create a BIO buffer for the cert contents
    bio = BIO_new_mem_buf(cert_buffer, read_size);

    // decode buffer
    int err;
    char errmsg[1024];
    if (!(x509_cert = PEM_read_bio_X509(bio, NULL, 0L, NULL))) {
        //Construct an error message
        while ((err = ERR_get_error())) {
            errmsg[1023] = '\0';
            ERR_error_string_n(err, errmsg, 1023);
            logprintfl(EUCAERROR, "calc_fingerprint: PEM_read_bio_x509 error message: %s\n", errmsg);
        }

        BIO_free(bio);
        X509_free(x509_cert);
        EUCA_FREE(cert_buffer);
        return NULL;
    }
    // calculate fingerprint, first get digest
    digest_function = EVP_get_digestbyname("md5");

    //allocate the fingerprint buffer
    unsigned char fingerprint[EVP_MAX_MD_SIZE];
    if (!X509_digest(x509_cert, digest_function, fingerprint, &n)) {
        logprintfl(EUCAERROR, "calc_fingerprint: X509 digest failed.");
        BIO_free(bio);
        X509_free(x509_cert);
        EUCA_FREE(cert_buffer);
        return NULL;
    }
#define MD5_FINGERPRINT_SIZE 16

    BIO_free(bio);
    X509_free(x509_cert);
    EUCA_FREE(cert_buffer);

    //Get the hex string of the digest
    char *fingerprint_str = hexify(fingerprint, MD5_FINGERPRINT_SIZE);
    if (fingerprint_str == NULL) {
        //malloc failed or something broke
        logprintfl(EUCAERROR, "calc_fingerprint: hexify returned null. Cleaning up and returning null");
    }

    return fingerprint_str;
}

/*
 * The true types of arg0 and arg1 must be: (struct key_value_pair**)
 * Compares two key_value_pair structs for sorting.
 * Uses the strcmp() value of the key strings.
 */
static int compare_keys(const void *arg0, const void *arg1)
{
    char *arg0_name = (*(struct key_value_pair **)arg0)->key;
    char *arg1_name = (*(struct key_value_pair **)arg1)->key;
    int result = strcmp(arg0_name, arg1_name);
    return result;
}

void free_key_value_pair_array(struct key_value_pair_array *kv_array)
{
    if (kv_array != NULL) {
        for (int i = 0; i < kv_array->size; i++) {
            if (kv_array->data[i] != NULL) {
                EUCA_FREE(kv_array->data[i]->key);
                EUCA_FREE(kv_array->data[i]->value);
                EUCA_FREE(kv_array->data[i]);
            }
        }
        EUCA_FREE(kv_array->data);
        EUCA_FREE(kv_array);
    }
}

struct key_value_pair *deconstruct_header(const char *header_str, char delimiter)
{
    if (header_str == NULL) {
        logprintfl(EUCADEBUG, "Tried to convert null header to header struct. Returning empty struct.");
        return NULL;
    }

    int src_len = strlen(header_str);
    int i = strspn(header_str, " ");    //get any initial space padding

    //Get the header name
    int name_start = i;
    char delim_string[3] = { ' ', delimiter, '\0' };
    i += strcspn(&(header_str[i]), delim_string);   //find the next space or colon, denotes end of header name
    int name_len = i - name_start;
    char *name_str = (char *)calloc(name_len + 1, sizeof(char));
    if (name_str == NULL) {
        logprintfl(EUCAERROR, "deconstruct_header: failed to allocate memory for the header name string. Returning null");
        return NULL;
    }

    strncpy(name_str, &(header_str[name_start]), name_len); //copy the name into new buffer

    //convert header name to all lowercase
    for (int j = 0; j < name_len; j++) {
        name_str[j] = tolower(name_str[j]);
    }

    i += strspn(&(header_str[i]), " "); //skip any spaces after name but before ':'
    if (header_str[i] != ':') {
        //Format error, expected the ':' here, none found
        logprintfl(EUCAERROR, "deconstruct_header: malformed header did not find colon where expected in header= %s\n", header_str);
        EUCA_FREE(name_str);
        return NULL;
    } else {
        i++;                    //skip the colon itself
    }

    i += strspn(&(header_str[i]), " "); //skip remaining spaces after colon before value starts
    int value_start = i;
    //int value_len = strcspn(&(header_str[value_start])," "); //find the next space or end of string

    //find the last non-space char, skipping '\0'
    int j;
    for (j = src_len - 1; (header_str[j] == '\0' || header_str[j] == ' ') && j > value_start; j--) ;

    int value_len = j + 1 - value_start;
    char *value_str = (char *)calloc(value_len + 1, sizeof(char));
    if (value_str == NULL) {
        EUCA_FREE(name_str);
        logprintfl(EUCAERROR, "deconstruct_header: failed to allocate memory for the header value string. Returning null");
        return NULL;
    }
    strncpy(value_str, &(header_str[value_start]), value_len);

    struct key_value_pair *header = NULL;
    header = (struct key_value_pair *)malloc(sizeof(struct key_value_pair));
    if (header == NULL) {
        EUCA_FREE(name_str);
        EUCA_FREE(value_str);
        logprintfl(EUCAERROR, "deconstruct_header: failed to allocate memory for the header struct. Returning null");
        return NULL;
    }

    header->key = name_str;
    header->value = value_str;
    return header;
}

struct key_value_pair_array *convert_header_list_to_array(const struct curl_slist *header_list, char delimiter)
{
    if (header_list == NULL) {
        logprintfl(EUCADEBUG, "Tried to convert null header list to array. Returning empty array");
        return NULL;
    }

    int list_length = 0;
    const struct curl_slist *current_header;
    for (current_header = header_list; current_header != NULL; current_header = current_header->next) {
        list_length++;
    }
    struct key_value_pair_array *hdr_array = (struct key_value_pair_array *)malloc(sizeof(struct key_value_pair_array));
    hdr_array->size = list_length;
    hdr_array->data = (struct key_value_pair **)calloc(list_length, sizeof(struct key_value_pair *));
    if (hdr_array->data == NULL) {
        logprintfl(EUCAERROR, "convert_header_list_to_array: cannon allocate memory for header_array struct data. Returning null.");
        EUCA_FREE(hdr_array);
        hdr_array = NULL;
        return NULL;
    }

    int i = 0;
    for (current_header = header_list; current_header != NULL; current_header = current_header->next) {
        hdr_array->data[i] = deconstruct_header(current_header->data, delimiter);
        if (hdr_array->data[i] == NULL) {
            logprintfl(EUCAERROR, "convert_header_list_to_array: deconstruct_header failed, returned null. Cleaning up and returning null.");
            free_key_value_pair_array(hdr_array);
            hdr_array = NULL;
            return NULL;
        }
        i++;
    }
    qsort(hdr_array->data, hdr_array->size, sizeof(struct key_value_pair *), compare_keys);
    return hdr_array;
}

char *construct_canonical_headers(struct key_value_pair_array *hdr_array)
{
    if (hdr_array == NULL)
        return NULL;

    int i;
    int str_size = 0;
    //figure out size first
    for (i = 0; i < hdr_array->size; i++) {
        //one for colon and one for newline
        str_size += strlen(hdr_array->data[i]->key) + strlen(hdr_array->data[i]->value) + 2;
    }
    char *canonical_header_str = NULL;
    canonical_header_str = (char *)calloc(str_size + 1, sizeof(char));
    if (canonical_header_str == NULL) {
        logprintfl(EUCAERROR, "construct_canonical_headers: cannon allocate memory for canonical header string. Returning null.");
        return NULL;
    }
    //Create string
    int entry_start = 0;
    int name_length = 0;
    int value_length = 0;
    for (i = 0; i < hdr_array->size; i++) {
        name_length = strlen(hdr_array->data[i]->key);
        value_length = strlen(hdr_array->data[i]->value);
        //TODO: check the int output of snprintf to be sure we got the entire string written properly.
        snprintf(&(canonical_header_str[entry_start]), name_length + 2, "%s:", hdr_array->data[i]->key);
        entry_start += name_length + 1;
        //TODO: check the int output of snprintf to be sure we got the entire string written properly.
        snprintf(&(canonical_header_str[entry_start]), value_length + 2, "%s\n", hdr_array->data[i]->value);
        entry_start += value_length + 1;
    }
    if (entry_start > 0)
        canonical_header_str[entry_start - 1] = '\0';   //overwrite last newline with null-terminator
    return canonical_header_str;
}

char *construct_canonical_uri(const char *url)
{
    if (url == NULL)
        return NULL;

    char *canonical_uri = process_url(url, URL_PATH);
    if (canonical_uri != NULL && strlen(canonical_uri) == 0) {
        EUCA_FREE(canonical_uri);
        canonical_uri = NULL;
    }

    if (canonical_uri == NULL) {
        canonical_uri = (char *)malloc(2 * sizeof(char));
        if (canonical_uri == NULL) {
            logprintfl(EUCAERROR, "construct_canonical_uri: could not allocate memory for uri\n");
            return NULL;
        }

        canonical_uri[0] = '/';
        canonical_uri[1] = '\0';
    }

    return canonical_uri;
}

/*
 * Counts the query parameters in a query string.
 * i.e. count_query_params("acl&value=key") = 2
 */
static int count_query_params(const char *query_str)
{
    int param_count = 0;
    int i = 0;
    while (query_str[i] != '\0') {
        if (query_str[i++] == '&')
            param_count++;      //reached the end of a param, count it
    }

    //Correct since we probably didn't see a & as the last char, so the last param wasn't counted
    if (i > 0 && query_str[i - 1] != '&') {
        param_count++;
    }

    return param_count;
}

char *construct_canonical_query(const char *url)
{
    if (url == NULL)
        return NULL;
    char *querystring = process_url(url, URL_QUERY);
    if (querystring == NULL) {
        return NULL;            //return null if there is no query string.
    }

    if (strlen(querystring) == 0) {
        //empty string, just return it
        return querystring;
    }
    struct key_value_pair_array *params = (struct key_value_pair_array *)malloc(sizeof(struct key_value_pair_array));
    if (params == NULL) {
        EUCA_FREE(querystring);
        logprintfl(EUCAWARN, "construct_canonical_query: could not malloc memory for params struct\n");
        return NULL;
    }

    params->size = count_query_params(querystring);
    if (params->size == 0) {
        //No parameters to process
        EUCA_FREE(querystring);
        EUCA_FREE(params);
        logprintfl(EUCAWARN, "construct_canonical_query: non-empty querystring, but found 0 parameters. returning null");
        return NULL;
    }
    params->data = (struct key_value_pair **)malloc(params->size * sizeof(struct key_value_pair *));
    if (params->data == NULL) {
        EUCA_FREE(params);
        EUCA_FREE(querystring);
        logprintfl(EUCAWARN, "construct_canonical_query: could not malloc memory for params data array\n");
        return NULL;
    }

    int i = 0;
    char *k = NULL;
    char *v = NULL;
    char *save_ptr1 = NULL;
    int total_size = 0;         //the size to allocate for the canonical_query string later
    char *token = NULL;
    size_t subtoken_len;
    int param_len = 0;
    for (token = strtok_r(querystring, "&", &save_ptr1); token != NULL; token = strtok_r(NULL, "&", &save_ptr1)) {
        param_len = strlen(token);
        total_size += param_len + 2;

        //Get the key value pair
        subtoken_len = strcspn(token, "=");
        k = (char *)calloc(subtoken_len + 1, sizeof(char));
        if (k == NULL) {
            EUCA_FREE(querystring);
            free_key_value_pair_array(params);
            logprintfl(EUCAWARN, "construct_canonical_query: could not allocate memory for query string parsing\n");
            return NULL;
        }
        strncpy(k, token, subtoken_len);
        k[subtoken_len] = '\0';

        //Not done, Value needs to be parsed
        if (subtoken_len + 1 < param_len) {
            subtoken_len++;     //increment over the '='
            v = (char *)calloc(param_len - subtoken_len + 1, sizeof(char));
            if (v == NULL) {
                EUCA_FREE(querystring);
                free_key_value_pair_array(params);
                EUCA_FREE(k);
                logprintfl(EUCAWARN, "construct_canonical_query: could not allocate memory for query string parsing\n");
                return NULL;
            }
            strncpy(v, &(token[subtoken_len]), param_len - subtoken_len);
            v[param_len - subtoken_len] = '\0';
        }
        //Convert the pair of strings into a key_value_pair struct
        if (i < params->size) {
            params->data[i] = (struct key_value_pair *)malloc(sizeof(struct key_value_pair));
            if (params->data[i] == NULL) {
                EUCA_FREE(k);
                EUCA_FREE(v);
                free_key_value_pair_array(params);
                EUCA_FREE(querystring);
                logprintfl(EUCAWARN, "construct_canonical_query: could not allocate memory.\n");
                return NULL;
            }
            params->data[i]->key = k;
            params->data[i]->value = v;
            i++;
            k = NULL;
            v = NULL;
        } else {
            //Don't overrun the array, warn and clean up. Something weird happened between
            // tokenizer and strchr so they disagree
            EUCA_FREE(k);
            EUCA_FREE(v);
            free_key_value_pair_array(params);
            EUCA_FREE(querystring);
            logprintfl(EUCAWARN,
                       "construct_canonical_query: error constructing, found mismatch between expected and found number of query string parameters\n");
            return NULL;
        }
        subtoken_len = 0;
        param_len = 0;
    }

    //sort the key-values
    qsort(params->data, params->size, sizeof(struct key_value_pair *), compare_keys);
    char *canonical_query = (char *)calloc(total_size, sizeof(char));
    if (canonical_query == NULL) {
        free_key_value_pair_array(params);
        EUCA_FREE(querystring);
        return NULL;
    }
    //print_key_value_pair_array(params);
    int start_idx = 0;
    int write_size = 0;
    for (int i = 0; i < params->size; i++) {
        if (params->data[i] != NULL) {
            write_size = strlen(params->data[i]->key) + 2;
            if (params->data[i]->value != NULL) {
                write_size += strlen(params->data[i]->value);
                write_size = snprintf(&(canonical_query[start_idx]), write_size + 1, "%s=%s&", params->data[i]->key, params->data[i]->value);
            } else {
                write_size = snprintf(&(canonical_query[start_idx]), write_size + 1, "%s=&", params->data[i]->key);
            }
            start_idx += write_size;
        }
    }
    char *last_amp = strrchr(canonical_query, '&');
    if (last_amp != NULL) {
        (*last_amp) = '\0';     //set the last amp to null terminator
    }

    EUCA_FREE(querystring);
    free_key_value_pair_array(params);
    return canonical_query;
}

char *construct_signed_headers(struct key_value_pair_array *hdr_array)
{
    if (hdr_array == NULL)
        return NULL;

    int signed_size = 0;
    //figure out size first
    for (int i = 0; i < hdr_array->size; i++) {
        signed_size += strlen(hdr_array->data[i]->key) + 1; //add one for each semicolon to add
    }
    signed_size++;              //add one for null-terminated
    char *signed_header_str = (char *)calloc(signed_size, sizeof(char));
    if (signed_header_str == NULL) {
        logprintfl(EUCAERROR, "construct_signed_headers: Could not allocate memory for signed header string. Returning null");
        return NULL;
    }
    //Create string
    int name_start = 0;
    int name_length = 0;
    for (int i = 0; i < hdr_array->size; i++) {
        name_length = strlen(hdr_array->data[i]->key);
        strncpy(&(signed_header_str[name_start]), hdr_array->data[i]->key, name_length);
        name_start += name_length;
        if (i < hdr_array->size - 1)
            signed_header_str[name_start++] = ';';  //don't put semicolon on last
    }

    return signed_header_str;
}

char *eucav2_sign_request(const char *verb, const char *url, const struct curl_slist *headers)
{
    if (!initialized)
        euca_init_cert();

    RSA *rsa = NULL;
    FILE *fp = NULL;
    char *auth_header = NULL;

    if (verb == NULL || url == NULL || headers == NULL)
        return NULL;

    struct key_value_pair_array *hdr_array = convert_header_list_to_array(headers, ':');
    if (hdr_array == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: failed to create http header array from list.\n");
    }
    //Prepare the components of the canonical request
    char *canonical_uri = construct_canonical_uri(url);
    if (canonical_uri == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: Cannot sign request, got null canonical_uri, probably out of memory\n");
    }
    //logprintfl (EUCADEBUG, "eucav2_sign_request: canonical uri: %s\n",canonical_uri);
    char *canonical_query = construct_canonical_query(url);
    if (canonical_query == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: Cannot sign request, got null canonical_query, probably out of memory\n");
    }
    //logprintfl (EUCADEBUG, "eucav2_sign_request: canonical query: %s\n",canonical_query);
    char *canonical_headers = construct_canonical_headers(hdr_array);
    if (canonical_headers == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: Cannot sign request, got null canonical_headers, probably out of memory\n");
    }
    //logprintfl (EUCADEBUG, "eucav2_sign_request: canonical headers: %s\n", canonical_headers);
    char *signed_headers = construct_signed_headers(hdr_array);
    if (signed_headers == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: Cannot sign request, got null signed_headers, probably out of memory\n");
    }
    //logprintfl (EUCADEBUG, "eucav2_sign_request: signed headers: %s\n", signed_headers);
    if (hdr_array != NULL)
        free_key_value_pair_array(hdr_array);

#define BUFSIZE 2024
    char canonical_request[BUFSIZE];
    if (canonical_uri == NULL || canonical_query == NULL || canonical_headers == NULL || signed_headers == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: Cannot construct canonical request due to nulls in data\n");
        //Free any non-null strings.
        EUCA_FREE(canonical_uri);
        EUCA_FREE(canonical_query);
        EUCA_FREE(canonical_headers);
        EUCA_FREE(signed_headers);
        return NULL;
    }

    assert((strlen(verb) + strlen(canonical_uri) + strlen(canonical_query) + strlen(canonical_headers) + strlen(signed_headers) + 5) <= BUFSIZE);
    snprintf(canonical_request, BUFSIZE, "%s\n%s\n%s\n%s\n%s", verb, canonical_uri, canonical_query, canonical_headers, signed_headers);
    EUCA_FREE(canonical_uri);
    EUCA_FREE(canonical_query);
    EUCA_FREE(canonical_headers);
    //Don't free signed_headers... needed later for the auth header construction

    if ((rsa = RSA_new()) == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: error, RSA_new() failed\n");
    } else if ((fp = fopen(pk_file, "r")) == NULL) {
        logprintfl(EUCAERROR, "eucav2_sign_request: error, failed to open private key file %s\n", pk_file);
        RSA_free(rsa);
    } else {
        logprintfl(EUCADEBUG, "eucav2_sign_request: reading private key file %s\n", pk_file);
        PEM_read_RSAPrivateKey(fp, &rsa, NULL, NULL);   /* read the PEM-encoded file into rsa struct */
        if (rsa == NULL) {
            logprintfl(EUCAERROR, "eucav2_sign_request: error, failed to read private key file %s\n", pk_file);
        } else {
            char *cert_fingerprint = calc_fingerprint(cert_file);
            if (cert_fingerprint == NULL) {
                logprintfl(EUCAERROR, "eucav2_sign_request: error, failed to calculate certificate fingerprint for %s\n", cert_file);
            } else {
                unsigned char *sig;
                // RSA_print_fp (stdout, rsa, 0); /* (for debugging) */
                if ((sig = malloc(RSA_size(rsa))) == NULL) {
                    logprintfl(EUCAERROR, "eucav2_sign_request: out of memory (for RSA key)\n");
                } else {
                    unsigned int siglen = 0;
                    unsigned char sha256[SHA256_DIGEST_LENGTH];

                    /* finally, SHA256 and sign with PK */
                    logprintfl(EUCADEBUG, "eucav2_sign_request: signing input %s\n", get_string_stats(canonical_request));
                    SHA256((unsigned char *)canonical_request, strlen(canonical_request), sha256);

                    char *sig_str = NULL;
                    int ret = RSA_sign(NID_sha256, sha256, SHA256_DIGEST_LENGTH, sig, &siglen, rsa);
                    if (ret != 1) {
                        logprintfl(EUCADEBUG, "eucav2_sign_request: RSA_sign() failed\n");
                    } else {
                        logprintfl(EUCADEBUG, "eucav2_sign_request: signing output %d\n", sig[siglen - 1]);
                        sig_str = base64_enc(sig, siglen);
                        logprintfl(EUCADEBUG, "eucav2_sign_request: base64 signature %s\n", get_string_stats((char *)sig_str));
                    }
                    EUCA_FREE(sig);

                    //create full auth header string
                    auth_header = (char *)calloc(BUFSIZE + 1, sizeof(char));
                    if (auth_header == NULL) {
                        logprintfl(EUCAERROR, "eucav2_sign_request: Cannot sign walrus request, no memory for auth header string\n");
                    } else {
                        snprintf(auth_header, BUFSIZE, "Authorization: EUCA2-RSA-SHA256 %s %s %s", cert_fingerprint, signed_headers, sig_str);
                    }

                    EUCA_FREE(sig_str);

                }
                EUCA_FREE(cert_fingerprint);
            }
            RSA_free(rsa);
        }
        fclose(fp);
    }

    EUCA_FREE(signed_headers);
    return auth_header;
}

/* caller must free the returned string */
char *euca_sign_url(const char *verb, const char *date, const char *url)
{
    if (!initialized)
        euca_init_cert();

    char *sig_str = NULL;
    RSA *rsa = NULL;
    FILE *fp = NULL;

    if (verb == NULL || date == NULL || url == NULL)
        return NULL;

    if ((rsa = RSA_new()) == NULL) {
        logprintfl(EUCAERROR, "RSA_new() failed\n");
    } else if ((fp = fopen(pk_file, "r")) == NULL) {
        logprintfl(EUCAERROR, "failed to open private key file %s\n", pk_file);
        RSA_free(rsa);
    } else {
        logprintfl(EUCATRACE, "reading private key file %s\n", pk_file);
        PEM_read_RSAPrivateKey(fp, &rsa, NULL, NULL);   /* read the PEM-encoded file into rsa struct */
        if (rsa == NULL) {
            logprintfl(EUCAERROR, "failed to read private key file %s\n", pk_file);
        } else {
            unsigned char *sig;

            // RSA_print_fp (stdout, rsa, 0); /* (for debugging) */
            if ((sig = malloc(RSA_size(rsa))) == NULL) {
                logprintfl(EUCAERROR, "out of memory (for RSA key)\n");
            } else {
                unsigned char sha1[SHA_DIGEST_LENGTH];
#define BUFSIZE 2024
                char input[BUFSIZE];
                unsigned int siglen;
                int ret;

                /* finally, SHA1 and sign with PK */
                assert((strlen(verb) + strlen(date) + strlen(url) + 4) <= BUFSIZE);
                snprintf(input, BUFSIZE, "%s\n%s\n%s\n", verb, date, url);
                logprintfl(EUCAEXTREME, "signing input %s\n", get_string_stats(input));
                SHA1((unsigned char *)input, strlen(input), sha1);
                if ((ret = RSA_sign(NID_sha1, sha1, SHA_DIGEST_LENGTH, sig, &siglen, rsa)) != 1) {
                    logprintfl(EUCAERROR, "RSA_sign() failed\n");
                } else {
                    logprintfl(EUCAEXTREME, "signing output %d\n", sig[siglen - 1]);
                    sig_str = base64_enc(sig, siglen);
                    logprintfl(EUCAEXTREME, "base64 signature %s\n", get_string_stats((char *)sig_str));
                }
                EUCA_FREE(sig);
            }
            RSA_free(rsa);
        }
        fclose(fp);
    }

    return sig_str;
}

/*
 * Initializes and compiles the regular expression for url processing. This should be done only once.
 */
static void init_url_regex()
{
    if (uri_regex != NULL)
        return;                 //skip lock check if already set

    //Lock to enforce the singleton nature of this method, should only actually lock on first use
    pthread_mutex_lock(&regex_init_mutex);
    if (uri_regex != NULL) {
        //Previous holder of lock initialized, so this thread can skip
        pthread_mutex_unlock(&regex_init_mutex);
        return;
    }

    uri_regex = (regex_t *) malloc(sizeof(regex_t));
    int comp_result = regcomp(uri_regex, url_pattern, REG_EXTENDED);
    switch (comp_result) {
    case 0:
        //Successful regex compile an init.
        pthread_mutex_unlock(&regex_init_mutex);
        return;
    case REG_BADBR:
        logprintfl(EUCAERROR,
                   "init_url_regex: There was an invalid ‘\\{...\\}’ construct in the regular expression. A valid ‘\\{...\\}’ construct must contain either a single number, or two numbers in increasing order separated by a comma.\n");
        break;
    case REG_BADPAT:
        logprintfl(EUCAERROR, "init_url_regex: There was a syntax error in the regular expression.\n");
        break;
    case REG_BADRPT:
        logprintfl(EUCAERROR,
                   "init_url_regex: A repetition operator such as ‘?’ or ‘*’ appeared in a bad position (with no preceding subexpression to act on).\n");
        break;
    case REG_ECOLLATE:
        logprintfl(EUCAERROR,
                   "init_url_regex: The regular expression referred to an invalid collating element (one not defined in the current locale for string collation).\n");
        break;
    case REG_ECTYPE:
        logprintfl(EUCAERROR, "init_url_regex: The regular expression referred to an invalid character class name.\n");
        break;
    case REG_EESCAPE:
        logprintfl(EUCAERROR, "init_url_regex: The regular expression ended with ‘\\’.\n");
        break;
    case REG_ESUBREG:
        logprintfl(EUCAERROR, "init_url_regex: There was an invalid number in the ‘\\digit’ construct.\n");
        break;
    case REG_EBRACK:
        logprintfl(EUCAERROR, "init_url_regex: There were unbalanced square brackets in the regular expression.\n");
        break;
    case REG_EPAREN:
        logprintfl(EUCAERROR,
                   "init_url_regex: An extended regular expression had unbalanced parentheses, or a basic regular expression had unbalanced ‘\\(’ and ‘\\)’.\n");
        break;
    case REG_EBRACE:
        logprintfl(EUCAERROR, "init_url_regex: The regular expression had unbalanced ‘\\{’ and ‘\\}’.\n");
        break;
    case REG_ERANGE:
        logprintfl(EUCAERROR, "init_url_regex: One of the endpoints in a range expression was invalid.\n");
        break;
    case REG_ESPACE:
        logprintfl(EUCAERROR, "init_url_regex: regcomp ran out of memory.\n");
        break;
    default:
        logprintfl(EUCAERROR, "init_url_regex: Regex compile failed. Code = %d\n", comp_result);
        break;
    }

    EUCA_FREE(uri_regex);
    uri_regex = NULL;
    pthread_mutex_unlock(&regex_init_mutex);
    return;
}

char *process_url(const char *content, int url_component)
{
    init_url_regex();
    if (uri_regex == NULL) {
        logprintfl(EUCAERROR, "process_url: could not get initialized regex for urls\n");
        return NULL;
    }

    if (url_component < 0 || url_component > uri_regex->re_nsub - 1) {
        logprintfl(EUCAERROR, "process_url: Requested url component not available with regex.\n");
        return NULL;
    }
    regmatch_t *match_array = (regmatch_t *) malloc(uri_regex->re_nsub * sizeof(regmatch_t));
    if (regexec(uri_regex, content, uri_regex->re_nsub, match_array, 0) == 0) {
        char *substr = NULL;
        int substr_size = 0;
        for (int i = 0; i < uri_regex->re_nsub; i++) {
            substr_size = match_array[i].rm_eo - match_array[i].rm_so;
            if (substr_size > 0 && i == url_component) {
                substr = (char *)malloc(substr_size * sizeof(char) + 1);
                strncpy(substr, &(content[match_array[i].rm_so]), substr_size);
                substr[substr_size] = '\0';
                EUCA_FREE(match_array);
                return substr;
            }
        }
    }
    EUCA_FREE(match_array);
    char *empty_str = (char *)malloc(sizeof(char));
    empty_str[0] = '\0';
    return empty_str;
}

#ifdef _UNIT_TEST
void print_key_value_pair_array(const struct key_value_pair_array *kv_array)
{
    printf("Key-Value Pair array of length: %d\n", kv_array->size);
    int i;
    for (i = 0; i < kv_array->size; i++) {
        if (kv_array->data[i] != NULL)
            printf("\t%s:%s\n", kv_array->data[i]->key, kv_array->data[i]->value);
    }
    printf("Key-Value Pair array complete\n");
}

int main(int argc, char **argv)
{
#define TEST_COUNT 100000
#define REGEX_COUNT 5
    char *regex_test[REGEX_COUNT] = {
        "http://testserver.com:8773/services/Walrus/bucket/object?attribute=value",
        "walrus://192.168.51.1:8773/services/Walrus/bucket/object",
        "http://192.168.51.1:8773/services/Walrus/bucket/object123%2B?acl",
        "https://192.168.51.1:8773/services/Walrus/bucket/object?aclname=value&test123",
        "hargarble:?\?///blah123&"
    };

    int test_string_array_length = 4;
    char *test_strings[4] = {
        "host: 129.168.1.1",
        "host : walrus.eucalyptus",
        " host :  walrus.eucalyptus:8773    ",
        " hos t: 129.1.1"
    };
    int url_pieces[5] = { URL_PROTOCOL, URL_HOSTNAME, URL_PORT, URL_PATH, URL_QUERY };
    struct key_value_pair *h = NULL;
    char *url_component = NULL;
    char *fingerprint = NULL;
    struct curl_slist *list = NULL;
    struct key_value_pair_array *hdr_array = NULL;
    char *auth = NULL;
    char *canonical_query = NULL;
#define URL_COUNT 20
    char *test_urls[URL_COUNT] = {
        "http://myserver.com:8773/services/Walrus/bucket/cento2123_testkvm.img.manifest.xml?versionId=123&acl",
        "https://myserver.com:8773/services/Walrus/bucket/cento2123_testkvm.img._manifest_.xml?versionId=123&acl",
        "http://myserver.com:8773/services/Walrus/bucket/cento2123_test%20kvm.img.manifest.xml?versionId=123&acl",
        "https://myserver.com:8773/services/Walrus/bucket/xen_images/image123_test.manifest.xml",
        "http://myserver.com:8773/services/Walrus/bucket/centos/images/image1_testkvm.img.manifest.xml",
        "https://myserver.com:8773/services/Walrus/bucket/_testkvm.img.manifest.xml",
        "https://myserver.com:8773/services/Walrus/bucket/_---$123.img.manifest.xml",
        "http://myserver.com:8773/services/Walrus/bucket_temp/cento2123_testkvm.img.manifest.xml",
        "https://myserver.com/services/Walrus/bucket/cento2123_testkvm.img.manifest.xml",
        "http://myserver.com:8773/services/Walrus/bucket/cento2123_testkvm.img.manifest.xml",
        "myserver.com:8773/services/Walrus/bucket/object?versionId=123&acl",
        "myserver.com/services/Walrus/bucket/_A_?zoom&versionId=123&acl",
        "https://myserver.com/services/Walrus/bucket/object",
        "https://myserver.com/services/Walrus/bucket/",
        "http://myserver.com/",
        "http://myserver.com:8773/services/Walrus/bucket/object?versionId=123&zoom&acl",
        "myserver.com:8773/services/Walrus/bucket/object",
        "/services/Walrus/bucket",
        "wargarblr?/services/Walrus/bucket&?/object",
        "/services/Walrus/bucket/object?versionId=123&acl"
    };

    int test_count = 0;
    if (argc > 1) {
        test_count = strtol(argv[1], NULL, 0);
    } else {
        test_count = TEST_COUNT;
    }

    printf("Running auth tests for %d iterations.\n", test_count);

    for (; test_count > 0; test_count--) {
        printf("\nTesting regex:\n");
        init_url_regex();
        printf("Num groups: %d\n", (int)uri_regex->re_nsub);
        for (int i = 0; i < REGEX_COUNT; i++) {
            printf("Testing value: %s\n", regex_test[i]);
            for (int j = 0; j < 5; j++) {
                printf("Requesting component %d, ", j);
                url_component = process_url(regex_test[i], url_pieces[j]);
                if (url_component != NULL) {
                    printf("Got: %s\n", url_component);
                    EUCA_FREE(url_component);
                } else {
                    printf("No value returned\n");
                }
            }
        }
        printf("Regex test done\n");
        printf("\nRunning header test!\n");
        printf("Raw header deconstruction tests:\n");
        for (int i = 0; i < test_string_array_length; i++) {
            h = deconstruct_header(test_strings[i], ':');
            if (h != NULL) {
                printf("Tried: %s, got name = '%s', value = '%s'\n", test_strings[i], h->key, h->value);
                EUCA_FREE(h->key);
                EUCA_FREE(h->value);
                EUCA_FREE(h);
            } else {
                printf("Got null from header deconstruction\n");
            }
        }

        printf("\nCalcing cert fingerprint\n");
        euca_init_cert();
        fingerprint = calc_fingerprint(cert_file);
        printf("Fingerprint: %s\n", fingerprint);
        EUCA_FREE(fingerprint);

        printf("\nTesting converting slist to key_value_pair_array\n");
        list = curl_slist_append(list, "host: myserver0.com");
        list = curl_slist_append(list, "date: 20120910T101055Z");
        list = curl_slist_append(list, "x-amz-date: May 12, 2012 8:00pm EST");

        hdr_array = convert_header_list_to_array(list, ':');
        print_key_value_pair_array(hdr_array);
        free_key_value_pair_array(hdr_array);
        hdr_array = NULL;

        printf("\nDone tesing slist conversion\n");
        printf("\nTesting query string canonicalization\n");

        for (int i = 0; i < URL_COUNT; i++) {
            printf("Raw url string = %s\n", test_urls[i]);
            canonical_query = construct_canonical_query(test_urls[i]);
            printf("Canonical Query = %s\n\n", canonical_query);
            EUCA_FREE(canonical_query);
        }
        printf("Done testing query strings\n");

        for (int i = 0; i < URL_COUNT; i++) {
            printf("\nTesting signing for url: %s\n", test_urls[i]);
            auth = eucav2_sign_request("GET", test_urls[i], list);
            printf("Signed auth: %s\n\n", auth);
            EUCA_FREE(auth);
        }
        curl_slist_free_all(list);
        list = NULL;
    }
    return 0;
}
#endif
