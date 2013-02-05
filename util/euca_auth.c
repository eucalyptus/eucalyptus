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
//! @file util/euca_auth.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS                        64 //!< so large-file support works on 32-bit systems

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

#include "eucalyptus.h"
#include "euca_auth.h"
#include "misc.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define FILENAME                                 512    //!< Maximum filename length

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

static boolean initialized = FALSE;    //!< Boolean to make sure we have initialized this module
static char sCertFileName[FILENAME] = { 0 };    //!< Certificate file name
static char sPrivKeyFileName[FILENAME] = { 0 }; //!< Private key file name

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int euca_init_cert(void);

char *euca_get_cert(u8 options);

char *base64_enc(u8 *in, int size);
char *base64_dec(u8 *in, int size);

char *euca_sign_url(const char *verb, const char *date, const char *url);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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
//! Initialize the certificate authentication module.
//!
//! @pre The certification module should not be initialized
//!
//! @post If the certification module is not initialized, uppon success, it will be
//!       initialized (ie. local initialized field set to TRUE).
//!
int euca_init_cert(void)
{
#define ERR_MSG "Error: required file %s not found by euca_init_cert(). Is $EUCALYPTUS set?\n"
#define OK_MSG  "euca_init_cert(): using file %s\n"
#define CHK_FILE(_n)                           \
{                                              \
	if ((fd = open((_n), O_RDONLY)) < 0) {     \
		LOGERROR(ERR_MSG, (_n));               \
		return (EUCA_ERROR);                   \
	} else {                                   \
		close(fd);                             \
		LOGINFO(OK_MSG, (_n));                 \
	}                                          \
}

    int fd = -1;
    char root[] = "";
    char *euca_home = getenv("EUCALYPTUS");

    if (initialized)
        return (EUCA_OK);

    if (!euca_home) {
        euca_home = root;
    }

    snprintf(sCertFileName, FILENAME, EUCALYPTUS_KEYS_DIR "/node-cert.pem", euca_home);
    snprintf(sPrivKeyFileName, FILENAME, EUCALYPTUS_KEYS_DIR "/node-pk.pem", euca_home);

    CHK_FILE(sCertFileName);
    CHK_FILE(sPrivKeyFileName);

    initialized = TRUE;
    return (EUCA_OK);

#undef ERR_MSG
#undef OK_MSG
#undef CHK_FILE
}

//!
//! Retrieves the certificate data from the certificate file
//!
//! @param[in] options bitfield providing certificate manipulation options (CONCATENATE_CERT, INDENT_CERT, TRIM_CERT)
//!
//! @return a new string containing the certificate information or NULL if any error occured.
//!
//! @pre
//!
//! @post
//!
//! @note caller must free the returned string
//!
char *euca_get_cert(u8 options)
{
    int s = 0;
    int fd = -1;
    int got = 0;
    char *sCert = NULL;
    ssize_t ret = -1;
    struct stat st = { 0 };

    if (!initialized) {
        if (euca_init_cert() != EUCA_OK) {
            return (NULL);
        }
    }

    if (stat(sCertFileName, &st) != 0) {
        LOGERROR("cannot stat the certificate file %s\n", sCertFileName);
    } else if ((s = st.st_size * 2) < 1) {  // *2 because we'll add characters
        LOGERROR("certificate file %s is too small\n", sCertFileName);
    } else if ((sCert = EUCA_ALLOC((s + 1), sizeof(char))) == NULL) {
        LOGERROR("out of memory\n");
    } else if ((fd = open(sCertFileName, O_RDONLY)) < 0) {
        LOGERROR("failed to open certificate file %s\n", sCertFileName);
        EUCA_FREE(sCert);
        sCert = NULL;
    } else {
        while ((got < s) && ((ret = read(fd, sCert + got, 1)) == 1)) {
            if (options & CONCATENATE_CERT) {   // omit all newlines
                if (sCert[got] == '\n')
                    continue;
            } else {
                if (options & INDENT_CERT) { // indent lines 2 through N with TABs
                    if (sCert[got] == '\n')
                        sCert[++got] = '\t';
                }
            }
            got++;
        }

        if (ret != 0) {
            LOGERROR("failed to read whole certificate file %s\n", sCertFileName);
            EUCA_FREE(sCert);
            sCert = NULL;
        } else {
            if (options & TRIM_CERT) {
                if ((sCert[got - 1] == '\t') || (sCert[got - 1] == '\n'))
                    got--;

                if (sCert[got - 1] == '\n')
                    got--;             // because of indenting
            }

            sCert[got] = '\0';
        }

        close(fd);
    }
    return (sCert);
}

//!
//! Encode a given buffer
//!
//! @param[in] sIn a pointer to the string buffer to encode
//! @param[in] size the length of the string buffer
//!
//! @return a pointer to the encoded string buffer.
//!
//! @pre
//!
//! @post
//!
//! @note caller must free the returned string
//!
char *base64_enc(u8 *sIn, int size)
{
    BIO *pBio64 = NULL;
    BIO *pBioMem = NULL;
    char *sEncVal = NULL;
    BUF_MEM *pBuffer = NULL;

    if((sIn != NULL) && (size > 0)) {
        if ((pBio64 = BIO_new(BIO_f_base64())) == NULL) {
            LOGERROR("BIO_new(BIO_f_base64()) failed\n");
        } else {
            BIO_set_flags(pBio64, BIO_FLAGS_BASE64_NO_NL);   // no long-line wrapping
            if ((pBioMem = BIO_new(BIO_s_mem())) == NULL) {
                LOGERROR("BIO_new(BIO_s_mem()) failed\n");
            } else {
                pBio64 = BIO_push(pBio64, pBioMem);
                if (BIO_write(pBio64, sIn, size) != size) {
                    LOGERROR("BIO_write() failed\n");
                } else {
                    (void)BIO_flush(pBio64);
                    BIO_get_mem_ptr(pBio64, &pBuffer);
                    if ((sEncVal = EUCA_ALLOC((pBuffer->length + 1), sizeof(char))) == NULL) {
                        LOGERROR("out of memory for Base64 buf\n");
                    } else {
                        memcpy(sEncVal, pBuffer->data, pBuffer->length);
                        sEncVal[pBuffer->length] = '\0';
                    }
                }
            }

            BIO_free_all(pBio64);           // frees both bio64 and biomem
        }
    }
    return (sEncVal);
}

//!
//! Decode a given buffer
//!
//! @param[in] sIn a pointer to the string buffer to decode
//! @param[in] size the length of the string buffer
//!
//! @return a pointer to the decoded string buffer.
//!
//! @pre
//!
//! @post
//!
//! @note caller must free the returned string
//!
char *base64_dec(u8 *sIn, int size)
{
    BIO *pBio64 = NULL;
    BIO *pBioMem = NULL;
    char *sBuffer = NULL;

    if ((sIn != NULL) && (size > 0)) {
        if ((pBio64 = BIO_new(BIO_f_base64())) == NULL) {
            LOGERROR("BIO_new(BIO_f_base64()) failed\n");
        } else {
            BIO_set_flags(pBio64, BIO_FLAGS_BASE64_NO_NL);   /* no long-line wrapping */

            if ((pBioMem = BIO_new_mem_buf(sIn, size)) == NULL) {
                LOGERROR("BIO_new_mem_buf() failed\n");
            } else if ((sBuffer = EUCA_ZALLOC(size, sizeof(char))) == NULL) {
                LOGERROR("Memory allocation failure.\n");
            } else {
                pBioMem = BIO_push(pBio64, pBioMem);

                if ((BIO_read(pBioMem, sBuffer, size)) <= 0) {
                    LOGERROR("BIO_read() read failed\n");
                    EUCA_FREE(sBuffer);
                }
            }

            BIO_free_all(pBio64);
        }
    }

    return (sBuffer);
}

//!
//! Signs an URL address
//!
//! @param[in] sVerb the verb for the signing algorithm
//! @param[in] sDate the date
//! @param[in] sURL a pointer to the URL to sign
//!
//! @return a pointer to the signed URL string buffer
//!
//! @pre
//!
//! @post
//!
//! @note caller must free the returned string
//!
char *euca_sign_url(const char *sVerb, const char *sDate, const char *sURL)
{
#define BUFSIZE                        2024

    int ret = 0;
    char sInput[BUFSIZE] = "";
    char *sSignature = NULL;
    RSA *pRSA = NULL;
    FILE *pFile = NULL;
    u32 siglen = 0;
    u8 *sSigBuffer = NULL;
    u8 sSHA1[SHA_DIGEST_LENGTH] = "";

    if (!initialized) {
        if (euca_init_cert() != EUCA_OK) {
            return (NULL);
        }
    }

    if ((sVerb == NULL) || (sDate == NULL) || (sURL == NULL))
        return (NULL);

    if ((pRSA = RSA_new()) == NULL) {
        LOGERROR("RSA_new() failed\n");
    } else if ((pFile = fopen(sPrivKeyFileName, "r")) == NULL) {
        LOGERROR("failed to open private key file %s\n", sPrivKeyFileName);
        RSA_free(pRSA);
    } else {
        LOGTRACE("reading private key file %s\n", sPrivKeyFileName);
        PEM_read_RSAPrivateKey(pFile, &pRSA, NULL, NULL);   // read the PEM-encoded file into rsa struct
        if (pRSA == NULL) {
            LOGERROR("failed to read private key file %s\n", sPrivKeyFileName);
        } else {
#if 0
            RSA_print_fp (stdout, rsa, 0); // (for debugging)
#endif /* 0 */
            if ((sSigBuffer = EUCA_ALLOC(RSA_size(pRSA), sizeof(u8))) == NULL) {
                LOGERROR("out of memory (for RSA key)\n");
            } else {
                // finally, SHA1 and sign with PK
                assert((strlen(sVerb) + strlen(sDate) + strlen(sURL) + 4) <= BUFSIZE);

                snprintf(sInput, BUFSIZE, "%s\n%s\n%s\n", sVerb, sDate, sURL);
                LOGEXTREME("signing input %s\n", get_string_stats(sInput));

                SHA1(((u8 *)sInput), strlen(sInput), sSHA1);
                if ((ret = RSA_sign(NID_sha1, sSHA1, SHA_DIGEST_LENGTH, sSigBuffer, &siglen, pRSA)) != 1) {
                    LOGERROR("RSA_sign() failed\n");
                } else {
                    LOGEXTREME("signing output %d\n", sSigBuffer[siglen - 1]);
                    sSignature = base64_enc(sSigBuffer, siglen);
                    LOGEXTREME("base64 signature %s\n", get_string_stats((char *)sSignature));
                }

                EUCA_FREE(sSigBuffer);
            }

            RSA_free(pRSA);
        }

        fclose(pFile);
    }

    return (sSignature);

#undef BUFSIZE
}
