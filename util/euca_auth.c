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
#include <openssl/ssl.h>
#include <openssl/x509.h>
#include <openssl/err.h>
#include <curl/curl.h>
#include <pthread.h>
#include <regex.h>
#include <arpa/inet.h>

#include "eucalyptus.h"
#include "misc.h"
#include "euca_auth.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define FILENAME                                 512    //!< Maximum filename length

#ifndef IV_LENGTH
#define IV_LENGTH                                 12
#endif /* ! IV_LENGTH */

#ifndef SYMMETRIC_KEY_LENGTH
#define SYMMETRIC_KEY_LENGTH                      32
#endif /* ! SYMMETRIC_KEY_LENGTH */

#ifndef MAX_ENCRYPTED_STRING_LEN
#define MAX_ENCRYPTED_STRING_LEN                8192
#endif /* ! MAX_ENCRYPTED_STRING_LEN */

#ifndef TAG_LENGTH
#define TAG_LENGTH                                16
#endif /* ! TAG_LENGTH */

#ifndef MAX_DECRYPTED_STRING_LEN
#define MAX_DECRYPTED_STRING_LEN                8192
#endif /* ! MAX_DECRYPTED_STRING_LEN */

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
static char sCertFileName[FILENAME] = "";   //!< Certificate file name
static char sPrivKeyFileName[FILENAME] = "";    //!< Private key file name
static char sCloudCertFileName[FILENAME] = "";  //!< Cloud public cert file name

static char hex_digits[16] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

static regex_t *uri_regex = NULL;

//! Mutex to guard initialization and compile of the uri_regex
static pthread_mutex_t regex_init_mutex = PTHREAD_MUTEX_INITIALIZER;

//! @TODO: this pattern does not exclude some invalid URLs, but does require a protocol section
static const char *url_pattern = "([^:?&]+://)([^:/?&]+)(:([0-9]+)?)?(/[^?&=]*)?(\\?(.*)?)?($)";

//! Mutex to guard certificate and ssl init to enforce the function as a singleton.
static pthread_mutex_t init_mutex = PTHREAD_MUTEX_INITIALIZER;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int compare_keys(const void *arg0, const void *arg1);
static int count_query_params(const char *query_str);
static void init_url_regex(void);

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
#define OK_MSG  "using file %s\n"
#define CHK_FILE(_n)                           \
{                                              \
	if ((fd = open((_n), O_RDONLY)) < 0) {     \
		LOGERROR(ERR_MSG, (_n));               \
		pthread_mutex_unlock(&init_mutex);     \
		return (EUCA_ERROR);                   \
	} else {                                   \
		close(fd);                             \
		LOGINFO(OK_MSG, (_n));                 \
	}                                          \
}

    int fd = -1;
    char root[] = "";
    char *euca_home = NULL;

    // Fast track so we don't lock unescessary
    if (initialized)
        return (EUCA_OK);

    //Lock to enforce the singleton nature of this method, should only actually lock on first use
    pthread_mutex_lock(&init_mutex);
    if (initialized) {
        //Previous holder of lock initialized, so this thread can skip
        pthread_mutex_unlock(&init_mutex);
        return (EUCA_OK);
    }

    if ((euca_home = getenv("EUCALYPTUS")) == NULL) {
        euca_home = root;
    }

    snprintf(sCertFileName, FILENAME, EUCALYPTUS_KEYS_DIR "/node-cert.pem", euca_home);
    snprintf(sPrivKeyFileName, FILENAME, EUCALYPTUS_KEYS_DIR "/node-pk.pem", euca_home);
    snprintf(sCloudCertFileName, FILENAME, EUCALYPTUS_KEYS_DIR "/cloud-cert.pem", euca_home);

    CHK_FILE(sCertFileName);
    CHK_FILE(sPrivKeyFileName);
    CHK_FILE(sCloudCertFileName);

    // initialize OpenSSL, must ONLY be called once. Not reentrant
    SSL_load_error_strings();

    //Not sure if these are necessary...likely not
    OpenSSL_add_all_algorithms();
    ERR_load_BIO_strings();
    ERR_load_crypto_strings();

    if (!SSL_library_init()) {
        LOGERROR("ssl library init failed\n");
        initialized = FALSE;
        pthread_mutex_unlock(&init_mutex);
        return (EUCA_ERROR);
    }

    initialized = TRUE;
    pthread_mutex_unlock(&init_mutex);
    return (EUCA_OK);

#undef ERR_MSG
#undef OK_MSG
#undef CHK_FILE
}

//! At first, decrypt the symmetric key in key_buffer using NC private key and use the symmetric key to decrypt the input string
//! Note the first 32 bytes of input string contains iv string (the remaining string is actual cipher text)
//!
//! @param[in] in_buffer - String that is base64 encoded and encrypted cipher-text
//! @param[in] key_buffer - String that contians symmetric key for decryption. Base-64 encoded
//! @param[in] out_buffer - Pointer to pointer where the result will be placed. Caller is responsible for freeing
//! @param[in] out_len - Length of the decrypted string
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
//! @pre in_buffer contains a valid null-terminated string that is a Base-64 encoded cipher-text from an symmetric encryption using the symmetric key in key_buffer
//!
//! @post *out_buffer points to the base64 decoded, decrypted string in plain text
int decrypt_string_with_node_and_symmetric_key(char *in_buffer, char *key_buffer, char **out_buffer, int *out_len)
{
    int cipher_len = -1;
    int ret = EUCA_ERROR;
    int len = -1;
    char *symm_key = NULL;
    char *dec64_in = NULL;
    char *enc64_key = NULL;
    char iv_buffer[IV_LENGTH + 1] = "";
    char *cipher_text = NULL;
    char *enc64_cipher_text = NULL;

    if (in_buffer == NULL || strlen(in_buffer) <= 0) {
        LOGERROR("No string to decrypt is given\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if (key_buffer == NULL || strlen(key_buffer) <= 0) {
        LOGERROR("No key string is given\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if (decrypt_string_with_node(key_buffer, &symm_key) != EUCA_OK) {
        LOGERROR("Failed to decrypt the symmetric key\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if ((enc64_key = base64_enc((unsigned char *)symm_key, SYMMETRIC_KEY_LENGTH)) == NULL) {
        LOGERROR("Failed to encode the key string\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    dec64_in = base64_dec2((unsigned char *)in_buffer, strlen(in_buffer), &len);
    if (dec64_in == NULL || len <= 0) {
        LOGERROR("Failed to decode the input string\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    // the first IV_LENGTH bytes are IV
    memcpy(iv_buffer, dec64_in, IV_LENGTH);
    cipher_len = len - IV_LENGTH;
    if (cipher_len <= 0) {
        LOGERROR("No ciphertext is found\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if ((cipher_text = EUCA_ZALLOC(cipher_len + 1, sizeof(char))) == NULL) {
        LOGERROR("Calloc failed \n");
        ret = EUCA_ERROR;
        goto cleanup;
    } else {
        bzero(cipher_text, cipher_len + 1);
    }

    memcpy(cipher_text, dec64_in + IV_LENGTH, cipher_len);
    enc64_cipher_text = base64_enc((unsigned char *)cipher_text, cipher_len);
    if (enc64_cipher_text == NULL) {
        LOGERROR("Failed to encode the cipher text\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if (decrypt_string_symmetric(enc64_cipher_text, enc64_key, iv_buffer, out_buffer, out_len) != EUCA_OK) {
        LOGERROR("Failed to decrypt the input string\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    ret = EUCA_OK;

cleanup:
    EUCA_FREE(symm_key);
    EUCA_FREE(dec64_in);
    EUCA_FREE(enc64_key);
    EUCA_FREE(cipher_text);
    EUCA_FREE(enc64_cipher_text);
    if (ret != EUCA_OK) {
        EUCA_FREE(*out_buffer);
    }
    return ret;
}

//! Encrypt the buffer using the symmetric key passed in key_buffer. in_buffer must be a null-terminated string.
//! Result is placed in *out_buffer on the heap and the caller is responsible for freeing.
//!
//! @param[in] in_buffer - Base64-encoded string that is to be encrypted
//! @param[in] key_buffer - Base64-encoded string that contains symmetric key
//! @param[in] iv_buffer - The string buffer that contains initialization vector (length defined by IV_LENGTH)
//! @param[out] out_buffer - Pointer to pointer where base64-encoded result will be placed. Null-terminated string. Caller is responsible for freeing
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
//! @pre in_buffer contains a valid null-terminated string that is a Base-64 encoded cipher-text
//!
//! @post *out_buffer points to the base64-encoded, encrypted string
//
//! WARNING: symmetric encryption is not fully tested with decrypt_string_symmetric(..). May need to consider tag stream
int encrypt_string_symmetric(char *in_buffer, char *key_buffer, char *iv_buffer, char **out_buffer, int *out_len)
{
    int len = -1;
    int key_len = -1;
    int in_len = -1;
    int ret = -1;
    char *dec64_key = NULL;
    char *dec64_in = NULL;
    char encrypted[MAX_ENCRYPTED_STRING_LEN] = "";
    EVP_CIPHER_CTX ctx = { 0 };

    dec64_key = base64_dec2((unsigned char *)key_buffer, strlen(key_buffer), &len);
    if (dec64_key == NULL || len <= 0) {
        LOGERROR("Base64 decode of key string failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    key_len = len;
    dec64_in = base64_dec2((unsigned char *)in_buffer, strlen(in_buffer), &len);
    if (dec64_in == NULL || len <= 0) {
        LOGERROR("Base64 decode of input string failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    in_len = len;

    EVP_CIPHER_CTX_init(&ctx);
    EVP_EncryptInit_ex(&ctx, EVP_aes_256_gcm(), NULL, (unsigned char *)dec64_key, (unsigned char *)iv_buffer);
    EVP_CIPHER_CTX_set_key_length(&ctx, key_len);
    EVP_CIPHER_CTX_ctrl(&ctx, EVP_CTRL_GCM_SET_IVLEN, IV_LENGTH, NULL);
    EVP_CIPHER_CTX_set_padding(&ctx, 0);

    if (!EVP_EncryptUpdate(&ctx, (unsigned char *)encrypted + IV_LENGTH, out_len, (unsigned char *)dec64_in, in_len)) {
        LOGERROR("Cipher update failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if (!EVP_EncryptFinal_ex(&ctx, (unsigned char *)encrypted + IV_LENGTH, &len)) {
        ERR_print_errors_fp(stderr);
        ret = EUCA_ERROR;
        LOGERROR("Cipher final failed\n");
        goto cleanup;
    }

    memcpy(encrypted, iv_buffer, IV_LENGTH);
    *out_len += len;
    *out_len += IV_LENGTH;
    *out_buffer = base64_enc((unsigned char *)encrypted, *out_len);

    ret = EUCA_OK;

cleanup:
    EUCA_FREE(dec64_key);
    EUCA_FREE(dec64_in);
    if (ret != EUCA_OK) {
        EUCA_FREE(*out_buffer);
    }
    EVP_CIPHER_CTX_cleanup(&ctx);
    return ret;
}

//! Decrypt the buffer using the symmetric key passed in key_buffer. in_buffer must be a null-terminated string.
//! Result is placed in *out_buffer on the heap and the caller is responsible for freeing.
//!
//! @param[in] in_buffer - String that is base64 encoded and encrypted cipher-text
//! @param[in] key_buffer - The string buffer that contains symmetric key for decryption. Base-64 encoded.
//! @param[in] iv_buffer - The string buffer that contains initialization vector
//! @param[out] out_buffer - Pointer to pointer where the result will be placed. Caller is responsible for freeing
//! @param[out] out_len - Pointer to integer number that is the length of decrypted string
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
//! @pre in_buffer contains a valid null-terminated string that is a Base-64 encoded cipher-text from an symmetric encryption using the symmetric key in key_buffer
//!
//! @post *out_buffer points to the base64 decoded, decrypted string in plain text
int decrypt_string_symmetric(char *in_buffer, char *key_buffer, char *iv_buffer, char **out_buffer, int *out_len)
{
    int ret = EUCA_ERROR;
    int len = -1;
    int in_len = -1;
    int key_len = -1;
    int cipher_init = FALSE;
    int cipher_len = -1;
    char *dec64_in = NULL;
    char *dec64_key = NULL;
    char decrypted_str[MAX_DECRYPTED_STRING_LEN] = "";  // MAX encrypted data length
    char *cipher_text = NULL;
    char *tag = NULL;
    EVP_CIPHER_CTX ctx = { 0 };

    if (in_buffer == NULL || strlen(in_buffer) <= 0) {
        LOGERROR("No input string to decrypt\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if (key_buffer == NULL || strlen(key_buffer) <= 0) {
        LOGERROR("No key string\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    //Base64 decode the string inbuffer, null terminator deducted from buffer size
    dec64_in = base64_dec2((unsigned char *)in_buffer, strlen(in_buffer), &in_len);
    if (dec64_in == NULL) {
        LOGERROR("Base64 decode of input string failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    cipher_len = in_len - TAG_LENGTH;
    if (cipher_len <= 0) {
        LOGERROR("No cipher text\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    cipher_text = dec64_in;
    tag = dec64_in + cipher_len;

    dec64_key = base64_dec2((unsigned char *)key_buffer, strlen(key_buffer), &key_len);
    if (dec64_key == NULL || key_len <= 0) {
        LOGERROR("Base64 decode of key string failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    EVP_CIPHER_CTX_init(&ctx);
    cipher_init = TRUE;
    EVP_DecryptInit_ex(&ctx, EVP_aes_256_gcm(), NULL, (unsigned char *)dec64_key, (unsigned char *)iv_buffer);
    EVP_CIPHER_CTX_set_key_length(&ctx, key_len);
    EVP_CIPHER_CTX_ctrl(&ctx, EVP_CTRL_GCM_SET_IVLEN, IV_LENGTH, NULL);
    EVP_CIPHER_CTX_set_padding(&ctx, 0);

    if (!EVP_DecryptUpdate(&ctx, (unsigned char *)decrypted_str, &len, (unsigned char *)cipher_text, cipher_len)) {
        ret = EUCA_ERROR;
        LOGERROR("Cipher update failed\n");
        goto cleanup;
    }

    if (!EVP_CIPHER_CTX_ctrl(&ctx, EVP_CTRL_GCM_SET_TAG, TAG_LENGTH, tag)) {
        ret = EUCA_ERROR;
        LOGERROR("Failed to set tag\n");
        goto cleanup;
    }

    *out_len = len;
    if (!EVP_DecryptFinal_ex(&ctx, (unsigned char *)decrypted_str + len, &len)) {
        ret = EUCA_ERROR;
        LOGERROR("Cipher final failed\n");
        goto cleanup;
    }

    *out_len += len;
    if (*out_len > MAX_DECRYPTED_STRING_LEN) {
        ret = EUCA_ERROR;
        LOGERROR("Decrypted string length exceeds max\n");
        goto cleanup;
    }

    if ((*out_buffer = EUCA_ZALLOC(*out_len + 1, sizeof(char))) == NULL) {
        LOGERROR("Calloc failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    } else {
        bzero(*out_buffer, *out_len + 1);
        strncpy(*out_buffer, decrypted_str, *out_len);
    }
    ret = EUCA_OK;

cleanup:
    EUCA_FREE(dec64_in);
    EUCA_FREE(dec64_key);
    if (ret != EUCA_OK) {
        EUCA_FREE(*out_buffer);
    }

    if (cipher_init)
        EVP_CIPHER_CTX_cleanup(&ctx);
    return ret;
}

//! Decrypt the buffer using the private key in the pk_file. in_buffer must be a null-terminated string
//! Result is placed in *out_buffer on the heap and caller is responsible for freeing.
//!
//! @param[in] in_buffer - String that is base64 encoded and encrypted cipher-text
//! @param[in] pk_file - The private key file that should be used to decrypt. Must be a RSA PKCS1 1024,2048, or 4096 bit key in PEM format
//! @param[out] out_buffer - Pointer to pointer where the result will be placed. Caller is responsible for freeing
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
//! @pre in_buffer contains a valid null-terminated string that is a Base-64 encoded cipher-text from an asymmetric encryption using the public key
//!     That corresponds to the private key in pk_file
//!
//! @post *out_buffer points to the base64 decoded, decrypted string in plain text
//!
int decrypt_string(char *in_buffer, char *pk_file, char **out_buffer)
{
    int ret = -1;
    int in_buffer_str_size = -1;
    char *dec64 = NULL;
    int out_size = 0;
    FILE *PKFP = NULL;
    RSA *pr = NULL;

    // Make sure we have valid parameters
    if ((in_buffer == NULL) || (pk_file == NULL) || (*pk_file == '\0') || (out_buffer == NULL)) {
        LOGERROR("Cannot decrypt buffer: invalid parameters\n");
        return (EUCA_ERROR);
    }

    in_buffer_str_size = (int)strlen(in_buffer);    //! the length of the string, not including the null-terminator

    // Open the private key file in read mode
    if ((PKFP = fopen(pk_file, "r")) == NULL) {
        LOGERROR("Cannot open key file: open failed on %s\n", pk_file);
        ret = EUCA_ERROR;
        goto cleanup;
    }
    // Get the key
    if (PEM_read_RSAPrivateKey(PKFP, &pr, NULL, NULL) == NULL) {
        LOGERROR("Private key file read failed\n");
        fclose(PKFP);
        PKFP = NULL;
        ret = EUCA_ERROR;
        goto cleanup;
    }
    // No longer need the file handler
    fclose(PKFP);
    PKFP = NULL;

    //Base64 decode the string, null terminator deducted from buffer size
    if ((dec64 = base64_dec((unsigned char *)in_buffer, in_buffer_str_size)) == NULL) {
        LOGERROR("Base64 decrypt failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    //Add the extra to ensure allocation for null-char
    if ((*out_buffer = EUCA_ZALLOC(in_buffer_str_size + 1, sizeof(char))) == NULL) {
        printf("Calloc failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    } else {
        bzero(*out_buffer, in_buffer_str_size + 1);
    }

    if ((out_size = RSA_private_decrypt(RSA_size(pr), ((unsigned char *)dec64), ((unsigned char *)*out_buffer), pr, RSA_PKCS1_PADDING)) == -1) {
        LOGERROR("private decrypt failed\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    ret = EUCA_OK;

cleanup:
    EUCA_FREE(dec64);
    if (ret == EUCA_ERROR) {
        EUCA_FREE(*out_buffer);
    }

    if (pr != NULL) {
        RSA_free(pr);
    }

    return ret;
}

//! Encrypts and base64 encodes the buffer using the public key in the cert_file certificate.
//! in_buffer must be a null-terminated string
//! Result is placed in *out_buffer on the heap and caller is responsible for freeing.
//!
//! @param[in] in_buffer - String that is null-terminated and <= block size used for key (typically 512 bytes)
//! @param[in] cert_file - The x590 cert file that should be used to decrypt. Must be a RSA PKCS1 1024,2048, or 4096 bit public key in PEM format cert
//! @param[out] out_buffer - Pointer to pointer where the result will be placed. Caller is responsible for freeing
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
int encrypt_string(char *in_buffer, char *cert_file, char **out_buffer)
{
    int rc = -1;
    int key_bits = -1;
    int ret = -1;
    int in_buffer_str_size = -1;
    int encrypt_size = -1;
    char *enc64 = NULL;
    RSA *pr = NULL;
    BIO *cert_bio = NULL;
    BIO *out_bio = NULL;
    X509 *cert = NULL;
    EVP_PKEY *pubkey = NULL;

    // Make sure we have valid parameters
    if ((in_buffer == NULL) || (cert_file == NULL) || (*cert_file == '\0') || (out_buffer == NULL)) {
        LOGERROR("Invalid input\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    in_buffer_str_size = (int)strlen(in_buffer);

    // create a BIO buffer for the cert contents
    cert_bio = BIO_new(BIO_s_file());
    out_bio = BIO_new(BIO_s_file());
    out_bio = BIO_new_fp(stdout, BIO_NOCLOSE);

    //Read cert
    rc = BIO_read_filename(cert_bio, cert_file);
    if (!rc) {
        LOGERROR("read file return error\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if (!(cert = PEM_read_bio_X509(cert_bio, NULL, 0, NULL))) {
        LOGERROR("Error loading cert into memory..\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    //Get the public key and populate the RSA struct
    if ((pubkey = X509_get_pubkey(cert)) == NULL) {
        LOGERROR("ERROR getting pub key\n");
        ret = EUCA_ERROR;
        goto cleanup;
    } else {
        switch (pubkey->type) {
        case EVP_PKEY_RSA:
            key_bits = EVP_PKEY_bits(pubkey);
            if (key_bits != 1024 && key_bits != 2048 && key_bits != 4096) {
                LOGERROR("Invalid RSA key length found in %s. Requires 1024, 2048, or 4096. Found %d\n", cert_file, key_bits);
                ret = EUCA_ERROR;
                goto cleanup;
            }
            break;
        case EVP_PKEY_DSA:
            LOGERROR("Invalid DSA key found. Only RSA supported.\n");
            ret = EUCA_ERROR;
            goto cleanup;
        default:
            LOGERROR("Unsupported %d bit non-RSA/DSA Key found", EVP_PKEY_bits(pubkey));
            ret = EUCA_ERROR;
            goto cleanup;
        }
    }

    if ((pr = EVP_PKEY_get1_RSA(pubkey)) == NULL) {
        LOGERROR("Could not get public RSA key for encrypting\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if ((encrypt_size = RSA_size(pr)) <= 0) {
        LOGERROR("Failed to read expected encryption size from RSA based on key\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if ((enc64 = EUCA_ZALLOC(encrypt_size + 1, sizeof(char))) == NULL) {
        LOGERROR("Failed mem alloc end64\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    rc = RSA_public_encrypt(in_buffer_str_size, ((unsigned char *)in_buffer), ((unsigned char *)enc64), pr, RSA_PKCS1_PADDING);
    if (rc == -1) {
        LOGERROR("Failed encrypt op\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    if ((*out_buffer = base64_enc((unsigned char *)enc64, encrypt_size)) == NULL) {
        LOGERROR("Failed b64 decode\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }

    ret = EUCA_OK;

cleanup:
    if (pr != NULL) {
        RSA_free(pr);
    }

    if (cert_bio != NULL) {
        BIO_free(cert_bio);
    }

    EUCA_FREE(enc64);

    if (cert != NULL) {
        X509_free(cert);
    }

    if (pr != NULL) {
        RSA_free(pr);
    }

    return ret;
}

//! Encrypts and base64 encodes the buffer using the public key in the cloud certificate.
//! Result is placed in *out_buffer on the heap and caller is responsible for freeing.
//!
//! @param[in] in_str - String that is null-terminated and <= block size used for key (typically 512 bytes)
//! @param[out] out_buffer - Pointer to pointer where the result will be placed. Caller is responsible for freeing
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
int encrypt_string_with_cloud(char *in_str, char **out_buffer)
{
    return encrypt_string(in_str, sCloudCertFileName, out_buffer);
}

//! Encrypts and base64 encodes the buffer using the public key in the node certificate
//! Result is placed in *out_buffer on the heap and caller is responsible for freeing.
//!
//! @param[in] in_str - String that is null-terminated and <= block size used for key (typically 512 bytes)
//! @param[out] out_buffer - Pointer to pointer where the result will be placed. Caller is responsible for freeing
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
int encrypt_string_with_node(char *in_str, char **out_buffer)
{
    return encrypt_string(in_str, sCertFileName, out_buffer);
}

//! Decrypt the buffer using the private key in the node private key file. in_buffer must be a null-terminated string
//! Result is placed in *out_buffer on the heap and caller is responsible for freeing.
//!
//! @param[in] in_str - String that is base64 encoded and encrypted cipher-text
//! @param[out] out_buffer - Pointer to pointer where the result will be placed. Caller is responsible for freeing
//!
//! @return EUCA_OK if operation was successful and *out_buffer has a value. EUCA_ERROR otherwise.
//!
//! @pre in_buffer contains a valid null-terminated string that is a Base-64 encoded cipher-text from an asymmetric encryption using the node cert
//!
//! @post *out_buffer points to the base64 decoded, decrypted string in plain text
//!
int decrypt_string_with_node(char *in_str, char **out_buffer)
{
    return decrypt_string(in_str, sPrivKeyFileName, out_buffer);
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
                if (options & INDENT_CERT) {    // indent lines 2 through N with TABs
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
char *base64_enc(u8 * sIn, int size)
{
    BIO *pBio64 = NULL;
    BIO *pBioMem = NULL;
    char *sEncVal = NULL;
    BUF_MEM *pBuffer = NULL;

    if ((sIn != NULL) && (size > 0)) {
        if ((pBio64 = BIO_new(BIO_f_base64())) == NULL) {
            LOGERROR("BIO_new(BIO_f_base64()) failed\n");
        } else {
            BIO_set_flags(pBio64, BIO_FLAGS_BASE64_NO_NL);  // no long-line wrapping
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

            BIO_free_all(pBio64);      // frees both bio64 and biomem
        }
    }
    return (sEncVal);
}

//!
//!
//!
//! @param[in] sIn
//! @param[in] size
//! @param[in] decoded_length
//!
//! @return
//!
char *base64_dec2(u8 * sIn, int size, int *decoded_length)
{
    BIO *pBio64 = NULL;
    BIO *pBioMem = NULL;
    char *sBuffer = NULL;

    if ((sIn != NULL) && (size > 0)) {
        if ((pBio64 = BIO_new(BIO_f_base64())) == NULL) {
            LOGERROR("BIO_new(BIO_f_base64()) failed\n");
        } else {
            BIO_set_flags(pBio64, BIO_FLAGS_BASE64_NO_NL);  /* no long-line wrapping */

            if ((pBioMem = BIO_new_mem_buf(sIn, size)) == NULL) {
                LOGERROR("BIO_new_mem_buf() failed\n");
            } else if ((sBuffer = EUCA_ZALLOC(size, sizeof(char))) == NULL) {
                LOGERROR("Memory allocation failure.\n");
            } else {
                pBioMem = BIO_push(pBio64, pBioMem);

                if ((*decoded_length = BIO_read(pBioMem, sBuffer, size)) <= 0) {
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
char *base64_dec(u8 * sIn, int size)
{
    BIO *pBio64 = NULL;
    BIO *pBioMem = NULL;
    char *sBuffer = NULL;

    if ((sIn != NULL) && (size > 0)) {
        if ((pBio64 = BIO_new(BIO_f_base64())) == NULL) {
            LOGERROR("BIO_new(BIO_f_base64()) failed\n");
        } else {
            BIO_set_flags(pBio64, BIO_FLAGS_BASE64_NO_NL);  /* no long-line wrapping */

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
//! Functions for EucalyptusV2 Internal Signing (EUCA2-SHA256-RSA)
//!
//! @param[in] data
//! @param[in] data_len
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *hexify(unsigned char *data, int data_len)
{
    int i = 0;
    char *hex_str = NULL;

    if (data == NULL)
        return (NULL);

    //2 hex digit chars for each byte plus one null
    if ((hex_str = (char *)EUCA_ALLOC((data_len * 2 + 1), sizeof(char))) == NULL) {
        LOGERROR("hexify: cannot allocate memory for the hex string. Returing null.");
        return (NULL);
    }

    for (i = 0; i < data_len; i++) {
        hex_str[i * 2] = hex_digits[(data[i] / 16)];
        hex_str[i * 2 + 1] = hex_digits[(data[i] % 16)];
    }

    hex_str[data_len * 2] = '\0';      //make sure it's null terminated
    return (hex_str);
}

//!
//!
//!
//! @param[in] cert_filename
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *calc_fingerprint(const char *cert_filename)
{
#define MD5_FINGERPRINT_SIZE        16

    u8 *cert_buffer = NULL;            // read buffer for certificate
    int fd = 0;                        // file descriptor for reading cert file
    int read_size = 0;
    int err;
    u32 n = 0;
    char errmsg[1024] = "";
    char *fingerprint_str = NULL;
    u8 fingerprint[EVP_MAX_MD_SIZE] = { 0 };
    ssize_t file_buffer_size = 0;      // size of read buffer
    BIO *bio = NULL;
    X509 *x509_cert = NULL;
    struct stat cert_file_stats = { 0 };    // file stat structure for getting the size of the file
    const EVP_MD *digest_function = NULL;   // digest of the cert

    if (cert_filename == NULL) {
        LOGERROR("got a null filename, returning null");
        return (NULL);
    }
    // Check the file and get the file size for allocation of buffers
    if (stat(cert_filename, &cert_file_stats) == -1) {
        LOGERROR("error in stat() of %s\n", cert_filename);
        return (NULL);
    }
    //! @TODO: can this be removed?
    // double the byte-count, for extra
    file_buffer_size = cert_file_stats.st_size * 2;
    if ((cert_buffer = EUCA_ZALLOC(file_buffer_size, sizeof(u8))) == NULL) {
        LOGERROR("could not allocate memory to read certificate file for fingerprint calculation\n");
        return (NULL);
    }

    if ((fd = open(cert_filename, O_RDONLY)) == -1) {
        LOGERROR("could not open %s to read certificate for fingerprint calculation\n", cert_filename);
        EUCA_FREE(cert_buffer);
        return (NULL);
    }
    //Read in the file, the whole thing
    if ((read_size = read(fd, cert_buffer, file_buffer_size)) == -1) {
        LOGERROR("error reading certificate file %s\n", sCertFileName);
        EUCA_FREE(cert_buffer);
        close(fd);
        return (NULL);
    }
    close(fd);

    // create a BIO buffer for the cert contents
    bio = BIO_new_mem_buf(cert_buffer, read_size);

    // decode buffer
    if ((x509_cert = PEM_read_bio_X509(bio, NULL, 0L, NULL)) == NULL) {
        // Construct an error message
        while ((err = ERR_get_error()) != 0) {
            errmsg[1023] = '\0';
            ERR_error_string_n(err, errmsg, 1023);
            LOGERROR("PEM_read_bio_x509 error message: %s\n", errmsg);
        }

        BIO_free(bio);
        X509_free(x509_cert);
        EUCA_FREE(cert_buffer);
        return (NULL);
    }
    // calculate fingerprint, first get digest
    digest_function = EVP_get_digestbyname("md5");

    // allocate the fingerprint buffer
    if (!X509_digest(x509_cert, digest_function, fingerprint, &n)) {
        LOGERROR("X509 digest failed.");
        BIO_free(bio);
        X509_free(x509_cert);
        EUCA_FREE(cert_buffer);
        return (NULL);
    }

    BIO_free(bio);
    X509_free(x509_cert);
    EUCA_FREE(cert_buffer);

    // Get the hex string of the digest
    if ((fingerprint_str = hexify(fingerprint, MD5_FINGERPRINT_SIZE)) == NULL) {
        // malloc failed or something broke
        LOGERROR("calc_fingerprint: hexify returned null. Cleaning up and returning null");
    }

    return (fingerprint_str);

#undef MD5_FINGERPRINT_SIZE
}

//!
//! Compares two key_value_pair structs for sorting. Uses the strcmp() value of the key strings.
//!
//! @param[in] arg0
//! @param[in] arg1
//!
//! @return
//!
//! @see
//!
//! @pre The true types of arg0 and arg1 must be: (struct key_value_pair**)
//!
//! @post
//!
//! @note
//!
static int compare_keys(const void *arg0, const void *arg1)
{
    char *arg0_name = (*(struct key_value_pair **)arg0)->key;
    char *arg1_name = (*(struct key_value_pair **)arg1)->key;
    int result = strcmp(arg0_name, arg1_name);
    return (result);
}

//!
//!
//!
//! @param[in] kv_array
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void free_key_value_pair_array(struct key_value_pair_array *kv_array)
{
    register int i = 0;
    if (kv_array != NULL) {
        for (i = 0; i < kv_array->size; i++) {
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

//!
//!
//!
//! @param[in] header_str
//! @param[in] delimiter
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
struct key_value_pair *deconstruct_header(const char *header_str, char delimiter)
{
    int j = 0;
    size_t i = 0;
    size_t value_start = 0;
    size_t src_len = 0;
    size_t name_start = 0;
    size_t name_len = 0;
    size_t value_len = 0;
    char *name_str = NULL;
    char *value_str = NULL;
    char delim_string[3] = { ' ', delimiter, '\0' };
    struct key_value_pair *header = NULL;

    if (header_str == NULL) {
        LOGDEBUG("Tried to convert null header to header struct. Returning empty struct.");
        return (NULL);
    }

    src_len = strlen(header_str);
    i = strspn(header_str, " ");       //get any initial space padding

    //Get the header name
    name_start = i;

    //find the next space or colon, denotes end of header name
    i += strcspn(&(header_str[i]), delim_string);
    name_len = i - name_start;
    if ((name_str = (char *)EUCA_ZALLOC((name_len + 1), sizeof(char))) == NULL) {
        LOGERROR("failed to allocate memory for the header name string. Returning null");
        return (NULL);
    }
    //copy the name into new buffer
    strncpy(name_str, &(header_str[name_start]), name_len);

    //convert header name to all lowercase
    for (j = 0; j < name_len; j++) {
        name_str[j] = tolower(name_str[j]);
    }

    // skip any spaces after name but before ':'
    i += strspn(&(header_str[i]), " ");
    if (header_str[i] != ':') {
        // Format error, expected the ':' here, none found
        LOGERROR("malformed header did not find colon where expected in header= %s\n", header_str);
        EUCA_FREE(name_str);
        return (NULL);
    }
    // skip the colon itself
    i++;

    i += strspn(&(header_str[i]), " "); //skip remaining spaces after colon before value starts
    value_start = i;

    //find the last non-space char, skipping '\0'
    for (j = src_len - 1; (((header_str[j] == '\0') || (header_str[j] == ' ')) && (j > value_start)); j--) ;

    value_len = j + 1 - value_start;
    if ((value_str = (char *)EUCA_ZALLOC((value_len + 1), sizeof(char))) == NULL) {
        EUCA_FREE(name_str);
        LOGERROR("failed to allocate memory for the header value string. Returning null");
        return (NULL);
    }
    strncpy(value_str, &(header_str[value_start]), value_len);

    if ((header = (struct key_value_pair *)EUCA_ZALLOC(1, sizeof(struct key_value_pair))) == NULL) {
        EUCA_FREE(name_str);
        EUCA_FREE(value_str);
        LOGERROR("failed to allocate memory for the header struct. Returning null");
        return (NULL);
    }

    header->key = name_str;
    header->value = value_str;
    return (header);
}

//!
//!
//!
//! @param[in] header_list
//! @param[in] delimiter
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
struct key_value_pair_array *convert_header_list_to_array(const struct curl_slist *header_list, char delimiter)
{
    int i = 0;
    int list_length = 0;
    const struct curl_slist *current_header = NULL;
    struct key_value_pair_array *hdr_array = NULL;

    if (header_list == NULL) {
        LOGDEBUG("Tried to convert null header list to array. Returning empty array");
        return (NULL);
    }

    for (current_header = header_list; current_header != NULL; current_header = current_header->next) {
        list_length++;
    }

    if ((hdr_array = (struct key_value_pair_array *)EUCA_ZALLOC(1, sizeof(struct key_value_pair_array))) == NULL) {
        LOGERROR("failed to allocate memory for the key/pair struct. Returning null");
        return (NULL);
    }

    hdr_array->size = list_length;
    if ((hdr_array->data = (struct key_value_pair **)EUCA_ZALLOC(list_length, sizeof(struct key_value_pair *))) == NULL) {
        LOGERROR("cannon allocate memory for header_array struct data. Returning null.");
        EUCA_FREE(hdr_array);
        hdr_array = NULL;
        return (NULL);
    }

    for (current_header = header_list; current_header != NULL; current_header = current_header->next) {
        hdr_array->data[i] = deconstruct_header(current_header->data, delimiter);
        if (hdr_array->data[i] == NULL) {
            LOGERROR("deconstruct_header failed, returned null. Cleaning up and returning null.");
            free_key_value_pair_array(hdr_array);
            hdr_array = NULL;
            return (NULL);
        }
        i++;
    }
    qsort(hdr_array->data, hdr_array->size, sizeof(struct key_value_pair *), compare_keys);
    return (hdr_array);
}

//!
//!
//!
//! @param[in] hdr_array
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *construct_canonical_headers(struct key_value_pair_array *hdr_array)
{
    int i = 0;
    size_t str_size = 0;
    size_t entry_start = 0;
    size_t name_length = 0;
    size_t value_length = 0;
    char *canonical_header_str = NULL;

    if (hdr_array == NULL)
        return (NULL);

    // figure out size first
    for (i = 0; i < hdr_array->size; i++) {
        //one for colon and one for newline
        str_size += strlen(hdr_array->data[i]->key) + strlen(hdr_array->data[i]->value) + 2;
    }

    if ((canonical_header_str = (char *)EUCA_ZALLOC((str_size + 1), sizeof(char))) == NULL) {
        LOGERROR("cannon allocate memory for canonical header string. Returning null.");
        return (NULL);
    }
    // Create string
    for (i = 0; i < hdr_array->size; i++) {
        name_length = strlen(hdr_array->data[i]->key);
        value_length = strlen(hdr_array->data[i]->value);

        //! @TODO: check the int output of snprintf to be sure we got the entire string written properly.
        snprintf(&(canonical_header_str[entry_start]), name_length + 2, "%s:", hdr_array->data[i]->key);
        entry_start += name_length + 1;

        //! @TODO: check the int output of snprintf to be sure we got the entire string written properly.
        snprintf(&(canonical_header_str[entry_start]), value_length + 2, "%s\n", hdr_array->data[i]->value);
        entry_start += value_length + 1;
    }

    if (entry_start > 0)
        canonical_header_str[entry_start - 1] = '\0';   //overwrite last newline with null-terminator
    return (canonical_header_str);
}

//!
//!
//!
//! @param[in] url
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *construct_canonical_uri(const char *url)
{
    char *canonical_uri = NULL;

    if (url == NULL)
        return (NULL);

    canonical_uri = process_url(url, URL_PATH);
    if ((canonical_uri != NULL) && (strlen(canonical_uri) == 0)) {
        EUCA_FREE(canonical_uri);
    }

    if (canonical_uri == NULL) {
        if ((canonical_uri = (char *)EUCA_ALLOC(2, sizeof(char))) == NULL) {
            LOGERROR("could not allocate memory for uri\n");
            return (NULL);
        }

        canonical_uri[0] = '/';
        canonical_uri[1] = '\0';
    }

    return (canonical_uri);
}

//!
//! Counts the query parameters in a query string. i.e. count_query_params("acl&value=key") = 2
//!
//! @param[in] query_str
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int count_query_params(const char *query_str)
{
    register int i = 0;
    register int param_count = 0;

    while (query_str[i] != '\0') {
        if (query_str[i++] == '&')
            param_count++;             //reached the end of a param, count it
    }

    // Correct since we probably didn't see a & as the last char, so the last param wasn't counted
    if ((i > 0) && (query_str[i - 1] != '&')) {
        param_count++;
    }

    return (param_count);
}

//!
//!
//!
//! @param[in] url
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *construct_canonical_query(const char *url)
{
    int i = 0;
    char *k = NULL;
    char *v = NULL;
    char *save_ptr1 = NULL;
    char *querystring = NULL;
    char *token = NULL;
    char *last_amp = NULL;
    char *canonical_query = NULL;
    size_t total_size = 0;             //the size to allocate for the canonical_query string later
    size_t param_len = 0;
    size_t subtoken_len;
    size_t write_size = 0;
    size_t start_idx = 0;
    struct key_value_pair_array *params = NULL;

    if (url == NULL)
        return (NULL);

    // return null if there is no query string.
    if ((querystring = process_url(url, URL_QUERY)) == NULL) {
        return (NULL);
    }

    if (strlen(querystring) == 0) {
        // empty string, just return it
        return (querystring);
    }

    if ((params = (struct key_value_pair_array *)EUCA_ZALLOC(1, sizeof(struct key_value_pair_array))) == NULL) {
        EUCA_FREE(querystring);
        LOGWARN("could not malloc memory for params struct\n");
        return (NULL);
    }

    if ((params->size = count_query_params(querystring)) == 0) {
        //No parameters to process
        EUCA_FREE(querystring);
        EUCA_FREE(params);
        LOGWARN("non-empty querystring, but found 0 parameters. returning null");
        return (NULL);
    }

    if ((params->data = (struct key_value_pair **)EUCA_ZALLOC(params->size, sizeof(struct key_value_pair *))) == NULL) {
        EUCA_FREE(params);
        EUCA_FREE(querystring);
        LOGWARN("could not malloc memory for params data array\n");
        return (NULL);
    }

    for (token = strtok_r(querystring, "&", &save_ptr1); token != NULL; token = strtok_r(NULL, "&", &save_ptr1)) {
        param_len = strlen(token);
        total_size += param_len + 2;

        // Get the key value pair
        subtoken_len = strcspn(token, "=");
        if ((k = (char *)EUCA_ZALLOC((subtoken_len + 1), sizeof(char))) == NULL) {
            EUCA_FREE(querystring);
            free_key_value_pair_array(params);
            LOGWARN("could not allocate memory for query string parsing\n");
            return (NULL);
        }

        strncpy(k, token, subtoken_len);
        k[subtoken_len] = '\0';

        // Not done, Value needs to be parsed
        if ((subtoken_len + 1) < param_len) {
            subtoken_len++;            //increment over the '='
            if ((v = (char *)EUCA_ZALLOC((param_len - subtoken_len + 1), sizeof(char))) == NULL) {
                EUCA_FREE(querystring);
                free_key_value_pair_array(params);
                EUCA_FREE(k);
                LOGWARN("could not allocate memory for query string parsing\n");
                return (NULL);
            }

            strncpy(v, &(token[subtoken_len]), param_len - subtoken_len);
            v[param_len - subtoken_len] = '\0';
        }
        // Convert the pair of strings into a key_value_pair struct
        if (i < params->size) {
            if ((params->data[i] = (struct key_value_pair *)EUCA_ZALLOC(1, sizeof(struct key_value_pair))) == NULL) {
                EUCA_FREE(k);
                EUCA_FREE(v);
                free_key_value_pair_array(params);
                EUCA_FREE(querystring);
                LOGWARN("could not allocate memory.\n");
                return (NULL);
            }

            params->data[i]->key = k;
            params->data[i]->value = v;
            i++;
            k = NULL;
            v = NULL;
        } else {
            // Don't overrun the array, warn and clean up. Something weird happened between
            // tokenizer and strchr so they disagree
            EUCA_FREE(k);
            EUCA_FREE(v);
            free_key_value_pair_array(params);
            EUCA_FREE(querystring);
            LOGWARN("error constructing, found mismatch between expected and found number of query string parameters\n");
            return (NULL);
        }

        subtoken_len = 0;
        param_len = 0;
    }

    // sort the key-values
    qsort(params->data, params->size, sizeof(struct key_value_pair *), compare_keys);

    if ((canonical_query = (char *)EUCA_ZALLOC(total_size, sizeof(char))) == NULL) {
        free_key_value_pair_array(params);
        EUCA_FREE(querystring);
        return (NULL);
    }

    for (i = 0; i < params->size; i++) {
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

    if ((last_amp = strrchr(canonical_query, '&')) != NULL) {
        (*last_amp) = '\0';            // set the last amp to null terminator
    }

    EUCA_FREE(querystring);
    free_key_value_pair_array(params);
    return (canonical_query);
}

//!
//!
//!
//! @param[in] hdr_array
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *construct_signed_headers(struct key_value_pair_array *hdr_array)
{
    int i = 0;
    int signed_size = 0;
    char *signed_header_str = NULL;
    size_t name_start = 0;
    size_t name_length = 0;

    if (hdr_array == NULL)
        return (NULL);

    // figure out size first
    for (i = 0; i < hdr_array->size; i++) {
        signed_size += strlen(hdr_array->data[i]->key) + 1; // add one for each semicolon to add
    }

    signed_size++;                     // add one for null-terminated
    if ((signed_header_str = (char *)EUCA_ZALLOC(signed_size, sizeof(char))) == NULL) {
        LOGERROR("construct_signed_headers: Could not allocate memory for signed header string. Returning null");
        return (NULL);
    }
    // Create string
    for (int i = 0; i < hdr_array->size; i++) {
        name_length = strlen(hdr_array->data[i]->key);
        strncpy(&(signed_header_str[name_start]), hdr_array->data[i]->key, name_length);
        name_start += name_length;
        if (i < (hdr_array->size - 1))
            signed_header_str[name_start++] = ';';  // don't put semicolon on last
    }

    return (signed_header_str);
}

//!
//!
//!
//! @param[in] verb
//! @param[in] url
//! @param[in] headers
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *eucav2_sign_request(const char *verb, const char *url, const struct curl_slist *headers)
{
#define BUFSIZE           2024

    u8 *sig = NULL;
    RSA *rsa = NULL;
    FILE *fp = NULL;
    u32 siglen = 0;
    int ret = 0;
    char *auth_header = NULL;
    char *canonical_uri = NULL;
    char *canonical_query = NULL;
    char *canonical_headers = NULL;
    char *signed_headers = NULL;
    char *cert_fingerprint = NULL;
    char *sig_str = NULL;
    char canonical_request[BUFSIZE] = "";
    u8 sha256[SHA256_DIGEST_LENGTH] = { 0 };
    struct key_value_pair_array *hdr_array = NULL;

    if (!initialized) {
        if (euca_init_cert() != EUCA_OK) {
            return (NULL);
        }
    }

    if ((verb == NULL) || (url == NULL) || (headers == NULL))
        return (NULL);

    if ((hdr_array = convert_header_list_to_array(headers, ':')) == NULL) {
        LOGERROR("failed to create http header array from list.\n");
    }
    // Prepare the components of the canonical request
    if ((canonical_uri = construct_canonical_uri(url)) == NULL) {
        LOGERROR("Cannot sign request, got null canonical_uri, probably out of memory\n");
    }

    if ((canonical_query = construct_canonical_query(url)) == NULL) {
        LOGERROR("Cannot sign request, got null canonical_query, probably out of memory\n");
    }

    if ((canonical_headers = construct_canonical_headers(hdr_array)) == NULL) {
        LOGERROR("Cannot sign request, got null canonical_headers, probably out of memory\n");
    }

    if ((signed_headers = construct_signed_headers(hdr_array)) == NULL) {
        LOGERROR("Cannot sign request, got null signed_headers, probably out of memory\n");
    }

    if (hdr_array != NULL)
        free_key_value_pair_array(hdr_array);

    if ((canonical_uri == NULL) || (canonical_query == NULL) || (canonical_headers == NULL) || (signed_headers == NULL)) {
        LOGERROR("Cannot construct canonical request due to nulls in data\n");

        // Free any non-null strings.
        EUCA_FREE(canonical_uri);
        EUCA_FREE(canonical_query);
        EUCA_FREE(canonical_headers);
        EUCA_FREE(signed_headers);
        return (NULL);
    }

    assert((strlen(verb) + strlen(canonical_uri) + strlen(canonical_query) + strlen(canonical_headers) + strlen(signed_headers) + 5) <= BUFSIZE);

    snprintf(canonical_request, BUFSIZE, "%s\n%s\n%s\n%s\n%s", verb, canonical_uri, canonical_query, canonical_headers, signed_headers);
    EUCA_FREE(canonical_uri);
    EUCA_FREE(canonical_query);
    EUCA_FREE(canonical_headers);
    // Don't free signed_headers... needed later for the auth header construction

    if ((rsa = RSA_new()) == NULL) {
        LOGERROR("error, RSA_new() failed\n");
    } else if ((fp = fopen(sPrivKeyFileName, "r")) == NULL) {
        LOGERROR("error, failed to open private key file %s\n", sPrivKeyFileName);
        RSA_free(rsa);
    } else {
        LOGTRACE("reading private key file %s\n", sPrivKeyFileName);
        PEM_read_RSAPrivateKey(fp, &rsa, NULL, NULL);   /* read the PEM-encoded file into rsa struct */
        if (rsa == NULL) {
            LOGERROR("error, failed to read private key file %s\n", sPrivKeyFileName);
        } else {
            if ((cert_fingerprint = calc_fingerprint(sCertFileName)) == NULL) {
                LOGERROR("error, failed to calculate certificate fingerprint for %s\n", sCertFileName);
            } else {
                // RSA_print_fp (stdout, rsa, 0); // (for debugging)
                if ((sig = EUCA_ZALLOC(RSA_size(rsa), sizeof(u8))) == NULL) {
                    LOGERROR("out of memory (for RSA key)\n");
                } else {
                    // finally, SHA256 and sign with PK
                    LOGTRACE("signing input %s\n", get_string_stats(canonical_request));
                    SHA256(((u8 *) canonical_request), strlen(canonical_request), sha256);

                    if ((ret = RSA_sign(NID_sha256, sha256, SHA256_DIGEST_LENGTH, sig, &siglen, rsa)) != 1) {
                        LOGDEBUG("RSA_sign() failed\n");
                    } else {
                        LOGTRACE("signing output %d\n", sig[siglen - 1]);
                        sig_str = base64_enc(sig, siglen);
                        LOGTRACE("base64 signature %s\n", get_string_stats((char *)sig_str));
                    }
                    EUCA_FREE(sig);

                    // create full auth header string
                    if ((auth_header = (char *)EUCA_ZALLOC((BUFSIZE + 1), sizeof(char))) == NULL) {
                        LOGERROR("Cannot sign object storage request, no memory for auth header string\n");
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
    return (auth_header);

#undef BUFSIZE
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
            if ((sSigBuffer = EUCA_ALLOC(RSA_size(pRSA), sizeof(u8))) == NULL) {
                LOGERROR("out of memory (for RSA key)\n");
            } else {
                // finally, SHA1 and sign with PK
                assert((strlen(sVerb) + strlen(sDate) + strlen(sURL) + 4) <= BUFSIZE);

                snprintf(sInput, BUFSIZE, "%s\n%s\n%s\n", sVerb, sDate, sURL);
                LOGEXTREME("signing input %s\n", get_string_stats(sInput));

                SHA1(((u8 *) sInput), strlen(sInput), sSHA1);
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

//!
//! Initializes and compiles the regular expression for url processing. This should be done only once.
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static void init_url_regex(void)
{
    int comp_result = 0;

    // skip lock check if already set
    if (uri_regex == NULL) {
        // Lock to enforce the singleton nature of this method, should only actually lock on first use
        pthread_mutex_lock(&regex_init_mutex);
        if (uri_regex != NULL) {
            // Previous holder of lock initialized, so this thread can skip
            pthread_mutex_unlock(&regex_init_mutex);
            return;
        }

        if ((uri_regex = (regex_t *) EUCA_ZALLOC(1, sizeof(regex_t))) == NULL) {
            pthread_mutex_unlock(&regex_init_mutex);
            return;
        }

        comp_result = regcomp(uri_regex, url_pattern, REG_EXTENDED);
        switch (comp_result) {
        case 0:
            //Successful regex compile an init.
            pthread_mutex_unlock(&regex_init_mutex);
            return;

        case REG_BADBR:
            LOGERROR
                ("There was an invalid ���������������������������\\{...\\}��������������������������� construct "
                 "in the regular expression. A valid ���������������������������\\{...\\}��������������������������� construct "
                 "must contain either a single number, or two numbers in increasing order separated by a comma.\n");
            break;

        case REG_BADPAT:
            LOGERROR("init_url_regex: There was a syntax error in the regular expression.\n");
            break;

        case REG_BADRPT:
            LOGERROR
                ("A repetition operator such as ���������������������������?��������������������������� or "
                 "���������������������������*��������������������������� appeared in a bad position (with no preceding "
                 "subexpression to act on).\n");
            break;

        case REG_ECOLLATE:
            LOGERROR("The regular expression referred to an invalid collating element (one not defined in the current locale for string collation).\n");
            break;

        case REG_ECTYPE:
            LOGERROR("The regular expression referred to an invalid character class name.\n");
            break;

        case REG_EESCAPE:
            LOGERROR
                ("The regular expression ended with ���������������������������\\���������������������������.\n");
            break;

        case REG_ESUBREG:
            LOGERROR
                ("There was an invalid number in the ���������������������������\\digit��������������������������� construct.\n");
            break;

        case REG_EBRACK:
            LOGERROR("There were unbalanced square brackets in the regular expression.\n");
            break;

        case REG_EPAREN:
            LOGERROR("An extended regular expression had unbalanced parentheses, or a basic regular expression had unbalanced "
                     "���������������������������\\(��������������������������� and ���������������������������\\)"
                     "���������������������������.\n");
            break;

        case REG_EBRACE:
            LOGERROR
                ("The regular expression had unbalanced ���������������������������\\{��������������������������� and "
                 "���������������������������\\}���������������������������.\n");
            break;

        case REG_ERANGE:
            LOGERROR("One of the endpoints in a range expression was invalid.\n");
            break;

        case REG_ESPACE:
            LOGERROR("regcomp ran out of memory.\n");
            break;

        default:
            LOGERROR("Regex compile failed. Code = %d\n", comp_result);
            break;
        }

        EUCA_FREE(uri_regex);
        pthread_mutex_unlock(&regex_init_mutex);
    }
}

//!
//!
//!
//! @param[in] content
//! @param[in] url_component
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
char *process_url(const char *content, int url_component)
{
    int i = 0;
    int substr_size = 0;
    char *substr = NULL;
    char *empty_str = NULL;
    regmatch_t *match_array = NULL;

    init_url_regex();

    if (uri_regex == NULL) {
        LOGERROR("process_url: could not get initialized regex for urls\n");
        return (NULL);
    }

    if ((url_component < 0) || (url_component > (uri_regex->re_nsub - 1))) {
        LOGERROR("process_url: Requested url component not available with regex.\n");
        return (NULL);
    }

    if ((match_array = (regmatch_t *) EUCA_ZALLOC(uri_regex->re_nsub, sizeof(regmatch_t))) == NULL) {
        LOGERROR("Failed to allocate memory.\n");
        return (NULL);
    }

    if (regexec(uri_regex, content, uri_regex->re_nsub, match_array, 0) == 0) {
        for (i = 0; i < uri_regex->re_nsub; i++) {
            substr_size = match_array[i].rm_eo - match_array[i].rm_so;
            if ((substr_size > 0) && (i == url_component)) {
                if ((substr = (char *)EUCA_ZALLOC(substr_size, sizeof(char) + 1)) != NULL) {
                    strncpy(substr, &(content[match_array[i].rm_so]), substr_size);
                    substr[substr_size] = '\0';
                    EUCA_FREE(match_array);
                    return (substr);
                }
            }
        }
    }

    EUCA_FREE(match_array);

    if ((empty_str = (char *)EUCA_ZALLOC(1, sizeof(char))) != NULL) {
        return (empty_str);
    }

    return (NULL);
}

#ifdef _UNIT_TEST
//!
//!
//!
//! @param[in] kv_array
//!
//! @return
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
void print_key_value_pair_array(const struct key_value_pair_array *kv_array)
{
    int i = 0;

    printf("Key-Value Pair array of length: %d\n", kv_array->size);
    for (i = 0; i < kv_array->size; i++) {
        if (kv_array->data[i] != NULL)
            printf("\t%s:%s\n", kv_array->data[i]->key, kv_array->data[i]->value);
    }
    printf("Key-Value Pair array complete\n");
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
#define TEST_COUNT          0
#define REGEX_COUNT         5
#define URL_COUNT          20

    int i = 0;
    int j = 0;
    int test_count = 0;
    int test_string_array_length = 4;
    int url_pieces[5] = { URL_PROTOCOL, URL_HOSTNAME, URL_PORT, URL_PATH, URL_QUERY };
    char *url_component = NULL;
    char *fingerprint = NULL;
    char *auth = NULL;
    char *canonical_query = NULL;
    struct key_value_pair *h = NULL;
    struct curl_slist *list = NULL;
    struct key_value_pair_array *hdr_array = NULL;

    static char *regex_test[REGEX_COUNT] = {
        "http://testserver.com:8773/services/objectstorage/bucket/object?attribute=value",
        "objectstorage://192.168.51.1:8773/services/objectstorage/bucket/object",
        "http://192.168.51.1:8773/services/objectstorage/bucket/object123%2B?acl",
        "https://192.168.51.1:8773/services/objectstorage/bucket/object?aclname=value&test123",
        "hargarble:?\?///blah123&"
    };

    static char *test_strings[4] = {
        "host: 129.168.1.1",
        "host : objectstorage.eucalyptus",
        " host :  objectstorage.eucalyptus:8773    ",
        " hos t: 129.1.1"
    };

    char *test_urls[URL_COUNT] = {
        "http://myserver.com:8773/services/objectstorage/bucket/cento2123_testkvm.img.manifest.xml?versionId=123&acl",
        "https://myserver.com:8773/services/objectstorage/bucket/cento2123_testkvm.img._manifest_.xml?versionId=123&acl",
        "http://myserver.com:8773/services/objectstorage/bucket/cento2123_test%20kvm.img.manifest.xml?versionId=123&acl",
        "https://myserver.com:8773/services/objectstorage/bucket/xen_images/image123_test.manifest.xml",
        "http://myserver.com:8773/services/objectstorage/bucket/centos/images/image1_testkvm.img.manifest.xml",
        "https://myserver.com:8773/services/objectstorage/bucket/_testkvm.img.manifest.xml",
        "https://myserver.com:8773/services/objectstorage/bucket/_---$123.img.manifest.xml",
        "http://myserver.com:8773/services/objectstorage/bucket_temp/cento2123_testkvm.img.manifest.xml",
        "https://myserver.com/services/objectstorage/bucket/cento2123_testkvm.img.manifest.xml",
        "http://myserver.com:8773/services/objectstorage/bucket/cento2123_testkvm.img.manifest.xml",
        "myserver.com:8773/services/objectstorage/bucket/object?versionId=123&acl",
        "myserver.com/services/objectstorage/bucket/_A_?zoom&versionId=123&acl",
        "https://myserver.com/services/objectstorage/bucket/object",
        "https://myserver.com/services/objectstorage/bucket/",
        "http://myserver.com/",
        "http://myserver.com:8773/services/objectstorage/bucket/object?versionId=123&zoom&acl",
        "myserver.com:8773/services/objectstorage/bucket/object",
        "/services/objectstorage/bucket",
        "wargarblr?/services/objectstorage/bucket&?/object",
        "/services/objectstorage/bucket/object?versionId=123&acl"
    };

    if (argc > 1) {
        test_count = strtol(argv[1], NULL, 0);
    } else {
        test_count = TEST_COUNT;
    }
    printf("Initializing certs\n");
    euca_init_cert();
    printf("Running auth tests for %d iterations.\n", test_count);

    for (; test_count > 0; test_count--) {
        printf("\nTesting regex:\n");
        init_url_regex();
        printf("Num groups: %d\n", (int)uri_regex->re_nsub);
        for (i = 0; i < REGEX_COUNT; i++) {
            printf("Testing value: %s\n", regex_test[i]);
            for (j = 0; j < 5; j++) {
                printf("Requesting component %d, ", j);
                if ((url_component = process_url(regex_test[i], url_pieces[j])) != NULL) {
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
        for (i = 0; i < test_string_array_length; i++) {
            if ((h = deconstruct_header(test_strings[i], ':')) != NULL) {
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
        fingerprint = calc_fingerprint(sCertFileName);
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

        for (i = 0; i < URL_COUNT; i++) {
            printf("Raw url string = %s\n", test_urls[i]);
            canonical_query = construct_canonical_query(test_urls[i]);
            printf("Canonical Query = %s\n\n", canonical_query);
            EUCA_FREE(canonical_query);
        }

        printf("Done testing query strings\n");

        for (i = 0; i < URL_COUNT; i++) {
            printf("\nTesting signing for url: %s\n", test_urls[i]);
            auth = eucav2_sign_request("GET", test_urls[i], list);
            printf("Signed auth: %s\n\n", auth);
            EUCA_FREE(auth);
        }
        curl_slist_free_all(list);
        list = NULL;
    }

    //Test encrypt/decrypt buffers!
    char *testbuffer = "testing123";
    //This is a good test token value because it with b64 decode to have nulls, so it tests any bad strlen() calls
    char *test_token =
        "fJJ5MnKbxOMItSLBdPlRUJQH6iJTgMQFXxf3/B9xvVfrNACNM9Fm41Ya1o5bdhLni+xG7Zj/z8kYcDp0Uwp2QS8e2qVPaeRoHQRf4PWvPuuT7SibmEVfKCH1Wh+L6Ja99HhgQtebVd4UEGLmaMVGshyANW0Rniz95iPwqxbXVpXbPV+2s865A1JjREQEnCm9LInJ/vpq5/CD+Em3jnASSQ97Nd0vR0G7AGTIEdTvA83jXVk6pSszl2FQjS2LGJ1+D3nSxZBTUMJxZ2ebL3v7E6eEsY7p8GqqWlxa27a/Ry5aWZttSPGWprBL1quK3dbo9MadQanVgwiW5VGyt1OifQ==";
    char *out_buffer = NULL;
    char *out_buffer2 = NULL;

    printf("Encryption test with %s cert\n", sCertFileName);
    printf("\tEncrypting test buffer: %s\n", testbuffer);
    int rc = encrypt_string(testbuffer, sCertFileName, &out_buffer);
    if (rc != EUCA_OK || out_buffer == NULL) {
        printf("\tEncrypt buffer failed!\n");
        return 1;
    } else {
        printf("\tEncrypting finished\n");
    }

    printf("\tPlaintext: %s\n\tCiphertext:%s\n", testbuffer, out_buffer);
    printf("Decrypting ciphertext wth private key %s\n", sPrivKeyFileName);
    rc = decrypt_string(out_buffer, sPrivKeyFileName, &out_buffer2);
    if (rc != EUCA_OK) {
        printf("\tDecryption failed!\n");
        return 1;
    } else {
        printf("\tDecrypting finished\n");
    }

    printf("\tSource plaintext: %s\n\tCiphertext: %s\n\tDecrypted plaintext: %s\n", testbuffer, out_buffer, out_buffer2);
    EUCA_FREE(out_buffer);
    EUCA_FREE(out_buffer2);

    printf("Testing token decryption with node PK using test token: %s\n", test_token);
    rc = decrypt_string_with_node(test_token, &out_buffer);
    if (rc != EUCA_OK) {
        printf("\tFailed decrypt of token\n");
    } else {
        printf("\tDecrypt token succeeded. Decrypted value: %s\n", out_buffer);
        EUCA_FREE(out_buffer);
    }

    printf("Testing cloud-cert encryption\n");
    rc = encrypt_string_with_cloud(testbuffer, &out_buffer);
    if (rc != EUCA_OK) {
        printf("\tFailed encrypt with cloud\n");
    } else {
        printf("\tEncrypt succeeded.\nEncrypted value of size %d: %s\n", (int)strlen(out_buffer), out_buffer);
    }

    printf("Testing decrypt with cloud pk if found %s\n", out_buffer);
    rc = decrypt_string(out_buffer, "/opt/eucalyptus/var/lib/eucalyptus/keys/cloud-pk.pem", &out_buffer2);
    if (rc != EUCA_OK) {
        printf("\tFailed decrypt with cloud pk\n");
    } else {
        printf("\tDecrypt succeeded.\n\tDecrypted value of size %d: %s\n", (int)strlen(out_buffer2), out_buffer2);
    }

    return (0);

#undef TEST_COUNT
#undef REGEX_COUNT
#undef URL_COUNT
}
#endif // _UNIT_TEST
