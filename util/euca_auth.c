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

#include <sys/types.h>
#define _FILE_OFFSET_BITS 64
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

#include "euca_auth.h"
#include "misc.h" /* get_string_stats, logprintf */

static int initialized = 0;

#define FILENAME 512
static char cert_file [FILENAME];
static char pk_file   [FILENAME];

int euca_init_cert (void)
{	
    if (initialized) return 0;
    
    char root [] = "";
    char * euca_home = getenv("EUCALYPTUS");
    if (!euca_home) {
        euca_home = root;
    }
    snprintf (cert_file, FILENAME, "%s/var/lib/eucalyptus/keys/node-cert.pem", euca_home);
    snprintf (pk_file,   FILENAME, "%s/var/lib/eucalyptus/keys/node-pk.pem", euca_home);

	#define ERR "Error: required file %s not found by euca_init_cert(). Is $EUCALYPTUS set?\n"
	#define OK  "euca_init_cert(): using file %s\n"
    #define CHK_FILE(n) \
        if ((fd=open(n, O_RDONLY))<0) {\
                logprintfl (EUCAERROR, ERR, n); return 1; \
        } else { \
                close (fd); logprintfl (EUCAINFO, OK, n); \
        }

    int fd; 
	CHK_FILE(cert_file)
	CHK_FILE(pk_file)
	
	initialized = 1;
	return 0;
}

/* caller must free the returned string */
char * euca_get_cert (unsigned char options) 
{
	if (!initialized) euca_init_cert ();
		
    char * cert_str = NULL;
    int s, fp;

    struct stat st;
    if (stat (cert_file, &st) != 0) {
        logprintfl (EUCAERROR, "error: cannot stat the certificate file %s\n", cert_file); 

    } else if ( (s = st.st_size*2) < 1) { /* *2 because we'll add characters */
        logprintfl (EUCAERROR, "error: certificate file %s is too small\n", cert_file); 

    } else if ( (cert_str = malloc (s+1)) == NULL ) { 
        logprintfl (EUCAERROR, "error: out of memory\n");

    } else if ( (fp = open (cert_file, O_RDONLY)) < 0 ) {
        logprintfl (EUCAERROR, "error: failed to open certificate file %s\n", cert_file);
        free (cert_str);
        cert_str = NULL;

    } else {
        ssize_t ret = -1;
        int got = 0; 

        while ( got < s && (ret = read (fp, cert_str + got, 1) ) == 1 ) {
            if ( options & CONCATENATE_CERT ) { /* omit all newlines */
                if ( cert_str [got] == '\n' ) 
                    continue;
            } else {
                if ( options & INDENT_CERT ) /* indent lines 2 through N with TABs */
                    if ( cert_str [got] == '\n' )
                        cert_str [++got] = '\t'; 
            }
            got++;
        }
        
        if (ret != 0) {
            logprintfl (EUCAERROR, "error: failed to read whole certificate file %s\n", cert_file);
            free (cert_str);
            cert_str = NULL;

        } else {
            if ( options & TRIM_CERT ) {
                if ( cert_str [got-1] == '\t' || 
                     cert_str [got-1] == '\n' ) got--;
                if ( cert_str [got-1] == '\n' ) got--; /* because of indenting */ 
            }
            cert_str [got] = '\0';
        }
        close (fp);
    }
    return cert_str;
}

/* caller must free the returned string */
char * base64_enc (unsigned char * in, int size)
{
  char * out_str = NULL;
  BIO * biomem, * bio64;
  
  if ( (bio64 = BIO_new (BIO_f_base64 ())) == NULL) {
    logprintfl (EUCAERROR, "error: BIO_new(BIO_f_base64()) failed\n");
  } else {
    BIO_set_flags (bio64, BIO_FLAGS_BASE64_NO_NL); /* no long-line wrapping */
    if ( (biomem = BIO_new (BIO_s_mem ())) == NULL) {
      logprintfl (EUCAERROR, "error: BIO_new(BIO_s_mem()) failed\n");
    } else {
      bio64 = BIO_push (bio64, biomem);
      if ( BIO_write (bio64, in, size)!=size) {
	logprintfl (EUCAERROR, "error: BIO_write() failed\n");
      } else {
	BUF_MEM * buf;
	(void) BIO_flush (bio64);
	BIO_get_mem_ptr (bio64, &buf);
	if ( (out_str = malloc(buf->length+1)) == NULL ) {
	  logprintfl (EUCAERROR, "error: out of memory for Base64 buf\n");
	} else {
	  memcpy (out_str, buf->data, buf->length);
	  out_str [buf->length] = '\0';
	}
      }
    }
    BIO_free_all (bio64); /* frees both bio64 and biomem */
  }
  return out_str;
}

/* caller must free the returned string */
char *base64_dec(unsigned char *in, int size)
{
  BIO *bio64, *biomem;
  char *buf=NULL;

  buf = malloc(sizeof(char) * size);
  bzero(buf, size);

  if ((bio64 = BIO_new(BIO_f_base64())) == NULL) {
    logprintfl(EUCAERROR, "BIO_new(BIO_f_base64()) failed\n");
  } else {
    BIO_set_flags (bio64, BIO_FLAGS_BASE64_NO_NL); /* no long-line wrapping */

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


/* caller must free the returned string */
char * euca_sign_url (const char * verb, const char * date, const char * url)
{
	if (!initialized) euca_init_cert ();
		
    char * sig_str = NULL;
    RSA * rsa = NULL;
    FILE * fp = NULL;

    if ( verb==NULL || date==NULL || url==NULL ) return NULL;

    if ( ( rsa = RSA_new() ) == NULL ) {
      logprintfl (EUCAERROR, "error: RSA_new() failed\n");
    } else if ( ( fp = fopen (pk_file, "r") ) == NULL) {
      logprintfl (EUCAERROR, "error: failed to open private key file %s\n", pk_file);
      RSA_free (rsa);
    } else {
      logprintfl (EUCADEBUG2, "euca_sign_url(): reading private key file %s\n", pk_file);
      PEM_read_RSAPrivateKey(fp, &rsa, NULL, NULL); /* read the PEM-encoded file into rsa struct */
      if ( rsa==NULL ) {
	logprintfl (EUCAERROR, "error: failed to read private key file %s\n", pk_file);
      } else {
	unsigned char * sig;
        
	// RSA_print_fp (stdout, rsa, 0); /* (for debugging) */
	if ( (sig = malloc(RSA_size(rsa))) == NULL) {
	  logprintfl (EUCAERROR, "error: out of memory (for RSA key)\n");
	} else {
	  unsigned char sha1 [SHA_DIGEST_LENGTH];
#define BUFSIZE 2024
	  char input [BUFSIZE];
	  unsigned int siglen;
	  int ret;
	  
	  /* finally, SHA1 and sign with PK */
	  assert ((strlen(verb)+strlen(date)+strlen(url)+4)<=BUFSIZE);
	  snprintf (input, BUFSIZE, "%s\n%s\n%s\n", verb, date, url);
	  logprintfl (EUCADEBUG2, "euca_sign_url(): signing input %s\n", get_string_stats(input));	
	  SHA1 ((unsigned char *)input, strlen(input), sha1);
	  if ((ret = RSA_sign (NID_sha1, sha1, SHA_DIGEST_LENGTH, sig, &siglen, rsa))!=1) {
	    logprintfl (EUCAERROR, "error: RSA_sign() failed\n");
	  } else {
	    logprintfl (EUCADEBUG2, "euca_sign_url(): signing output %d\n", sig[siglen-1]);	
	    sig_str = base64_enc (sig, siglen);
	    logprintfl (EUCADEBUG2, "euca_sign_url(): base64 signature %s\n", get_string_stats((char *)sig_str));	
	  }
	  free (sig);
	}
	RSA_free (rsa);
      }            
      fclose(fp);
    }
    
    return sig_str;
}
