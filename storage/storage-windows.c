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
//! @file storage/storage-windows.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU                      /* strnlen */
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>                    /* open|read|close dir */
#include <time.h>                      /* time() */
#include <stdint.h>
#include <arpa/inet.h>

#include <openssl/sha.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/err.h>

#include <eucalyptus.h>

#include <storage-windows.h>
#include <euca_auth.h>
#include <misc.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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
//!
//!
//! @param[in]  encpass
//! @param[in]  encsize
//! @param[in]  pkfile
//! @param[out] out
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_IO_ERROR and EUCA_MEMORY_ERROR.
//!
int decryptWindowsPassword(char *encpass, int encsize, char *pkfile, char **out)
{
    FILE *PKFP = NULL;
    RSA *pr = NULL;
    char *dec64 = NULL;
    int rc = -1;

    // Make sure we have valid parameters
    if ((encpass == NULL) || (encsize <= 0) || (pkfile == NULL) || (*pkfile == '\0') || (out == NULL)) {
        return (EUCA_ERROR);
    }
    // Open the private key file in read mode
    if ((PKFP = fopen(pkfile, "r")) == NULL) {
        return (EUCA_IO_ERROR);
    }
    // Give is to SSL
    if (PEM_read_RSAPrivateKey(PKFP, &pr, NULL, NULL) == NULL) {
        fclose(PKFP);
        PKFP = NULL;
        return (EUCA_ERROR);
    }
    // No longer need the file handler
    fclose(PKFP);
    PKFP = NULL;

    if ((dec64 = base64_dec(((unsigned char *)encpass), strlen(encpass))) == NULL) {
        return (EUCA_ERROR);
    }

    if ((*out = EUCA_ZALLOC(512, sizeof(char))) == NULL) {
        EUCA_FREE(dec64);
        return (EUCA_MEMORY_ERROR);
    }

    rc = RSA_private_decrypt(encsize, ((unsigned char *)dec64), ((unsigned char *)*out), pr, RSA_PKCS1_PADDING);
    EUCA_FREE(dec64);

    if (rc) {
        EUCA_FREE(*out);
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in]  pass
//! @param[in]  key
//! @param[out] out
//! @param[out] outsize
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         and EUCA_MEMORY_ERROR
//!
int encryptWindowsPassword(char *pass, char *key, char **out, int *outsize)
{
    char *sshkey_dec = NULL;
    char *modbuf = NULL;
    char *exponentbuf = NULL;
    char *ptr = NULL;
    char *tmp = NULL;
    char hexstr[4] = { 0 };
    char encpassword[512] = { 0 };
    uint32_t len = 0;
    uint32_t exponent = 0;
    int size = 0;
    int ilen = 0;
    int i = 0;
    int encsize = 0;
    RSA *r = NULL;

    if (!pass || !key || !out || !outsize) {
        return (EUCA_ERROR);
    }

    size = strlen(key);
    if ((sshkey_dec = base64_dec(((unsigned char *)key), size)) == NULL) {
        return (EUCA_ERROR);
    }

    ptr = sshkey_dec;
    memcpy(&len, ptr, 4);
    len = htonl(len);
    ptr += 4 + len;

    memcpy(&len, ptr, 4);
    len = htonl(len);
    ptr += 4;

    // read public exponent
    if ((exponentbuf = EUCA_ZALLOC(32768, sizeof(char))) == NULL) {
        EUCA_FREE(sshkey_dec);
        return (EUCA_MEMORY_ERROR);
    }

    exponent = 0;
    memcpy(&exponent, ptr, len);
    exponent = htonl(exponent);
    exponent = htonl(exponent);
    snprintf(exponentbuf, 128, "%08X", exponent);
    ptr += len;

    memcpy(&len, ptr, 4);
    len = htonl(len);
    ptr += 4;

    // read modulus material
    if ((modbuf = EUCA_ZALLOC(32768, sizeof(char))) == NULL) {
        EUCA_FREE(sshkey_dec);
        EUCA_FREE(exponentbuf);
        return (EUCA_MEMORY_ERROR);
    }

    ilen = ((int)len);
    for (i = 0; i < ilen; i++) {
        if ((tmp = strndup(ptr, 1)) != NULL) {
            len = *tmp;
            bzero(hexstr, sizeof(char) * 4);
            snprintf(hexstr, 3, "%02X", (len << 24) >> 24);
            strcat(modbuf, hexstr);
            ptr += 1;
            EUCA_FREE(tmp);
        }
    }
    //printf("MOD: |%s|\n", modbuf);
    //printf("EXPONENT: |%s|\n", exponentbuf);

    if ((r = RSA_new()) == NULL) {
        EUCA_FREE(sshkey_dec);
        EUCA_FREE(exponentbuf);
        EUCA_FREE(modbuf);
        return (EUCA_MEMORY_ERROR);
    }

    if (!BN_hex2bn(&(r->e), exponentbuf) || !BN_hex2bn(&(r->n), modbuf)) {
        EUCA_FREE(sshkey_dec);
        EUCA_FREE(exponentbuf);
        EUCA_FREE(modbuf);
        return (EUCA_ERROR);
    }

    bzero(encpassword, 512);
    if ((encsize = RSA_public_encrypt(strlen(pass), (unsigned char *)pass, (unsigned char *)encpassword, r, RSA_PKCS1_PADDING)) <= 0) {
        EUCA_FREE(sshkey_dec);
        EUCA_FREE(exponentbuf);
        EUCA_FREE(modbuf);
        return (EUCA_ERROR);
    }

    *out = base64_enc((unsigned char *)encpassword, encsize);
    *outsize = encsize;
    if (!*out || *outsize <= 0) {
        EUCA_FREE(sshkey_dec);
        EUCA_FREE(exponentbuf);
        EUCA_FREE(modbuf);
        return (EUCA_ERROR);
    }

    EUCA_FREE(sshkey_dec);
    EUCA_FREE(exponentbuf);
    EUCA_FREE(modbuf);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] euca_home
//! @param[in] rundir_path
//! @param[in] keyName
//! @param[in] instName
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_IO_ERROR and EUCA_MEMORY_ERROR.
//!
int makeWindowsFloppy(char *euca_home, char *rundir_path, char *keyName, char *instName)
{
    int i = 0;
    int fd = 0;
    int rc = 0;
    int rbytes = 0;
    int count = 0;
    int encsize = 0;
    char *buf = NULL;
    char *ptr = NULL;
    char *tmp = NULL;
    char *newpass = NULL;
    char dest_path[1024] = "";
    char source_path[1024] = "";
    char password[16] = "";
    char *encpassword = NULL;
    char *newInstName = NULL;
    char c[4] = "";
    char enckey[2048] = "";
    char keyNameHolder1[512] = "";
    char keyNameHolder2[512] = "";
    FILE *FH = NULL;

    if (!euca_home || !rundir_path || !strlen(euca_home) || !strlen(rundir_path)) {
        return (EUCA_ERROR);
    }

    snprintf(source_path, 1024, EUCALYPTUS_HELPER_DIR "/floppy", euca_home);
    snprintf(dest_path, 1024, "%s/floppy", rundir_path);
    if (!keyName || !strlen(keyName) || !strlen(instName)) {
        return (euca_execlp(NULL, "cp", "-a", source_path, dest_path, NULL));
    }

    bzero(password, sizeof(char) * 16);
    for (i = 0; i < 8; i++) {
        c[0] = '0';
        while (c[0] == '0' || c[0] == 'O')
            snprintf(c, 2, "%c", RANDALPHANUM());
        strcat(password, c);
    }
    //  snprintf(source_path, 1024, "%s/usr/share/eucalyptus/floppy", euca_home);
    //  snprintf(dest_path, 1024, "%s/floppy", rundir_path);

    if ((buf = EUCA_ALLOC(1024 * 2048, sizeof(char))) == NULL) {
        return (EUCA_MEMORY_ERROR);
    }

    if ((fd = open(source_path, O_RDONLY)) < 0) {
        EUCA_FREE(buf);
        return (EUCA_IO_ERROR);
    }

    rbytes = read(fd, buf, 1024 * 2048);
    close(fd);
    if (rbytes < 0) {
        EUCA_FREE(buf);
        return (EUCA_IO_ERROR);
    }

    ptr = buf;
    count = 0;
    tmp = EUCA_ZALLOC(strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER") + 1, sizeof(char));
    newpass = EUCA_ZALLOC(strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER") + 1, sizeof(char));
    newInstName = EUCA_ZALLOC(strlen("MAGICEUCALYPTUSHOSTNAMEPLACEHOLDER") + 1, sizeof(char));

    if (!tmp || !newpass || !newInstName) {
        EUCA_FREE(tmp);
        EUCA_FREE(newpass);
        EUCA_FREE(newInstName);
        EUCA_FREE(buf);
        return (EUCA_MEMORY_ERROR);
    }

    snprintf(newpass, strlen(password) + 1, "%s", password);
    snprintf(newInstName, strlen(instName) + 1, "%s", instName);

    while (count < rbytes) {
        memcpy(tmp, ptr, strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER"));
        if (!strcmp(tmp, "MAGICEUCALYPTUSPASSWORDPLACEHOLDER")) {
            memcpy(ptr, newpass, strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER"));
        }

        if (!strcmp(tmp, "MAGICEUCALYPTUSHOSTNAMEPLACEHOLDER")) {
            memcpy(ptr, newInstName, strlen("MAGICEUCALYPTUSHOSTNAMEPLACEHOLDER"));
        }

        ptr++;
        count++;
    }

    if ((fd = open(dest_path, O_CREAT | O_TRUNC | O_RDWR, 0700)) < 0) {
        EUCA_FREE(buf);
        EUCA_FREE(tmp);
        EUCA_FREE(newpass);
        EUCA_FREE(newInstName);
        return (EUCA_IO_ERROR);
    }

    rc = write(fd, buf, rbytes);
    close(fd);

    if (rc != rbytes) {
        EUCA_FREE(buf);
        EUCA_FREE(tmp);
        EUCA_FREE(newpass);
        EUCA_FREE(newInstName);
        return (EUCA_IO_ERROR);
    }

    EUCA_FREE(buf);

    // encrypt password and write to console log for later retrieval
    sscanf(keyName, "%s %s %s", keyNameHolder1, enckey, keyNameHolder2);
    rc = encryptWindowsPassword(password, enckey, &encpassword, &encsize);
    if (rc) {
        EUCA_FREE(tmp);
        EUCA_FREE(newpass);
        EUCA_FREE(newInstName);
        return (EUCA_ERROR);
    }

    snprintf(dest_path, 1024, "%s/console.append.log", rundir_path);
    if ((FH = fopen(dest_path, "w")) != NULL) {
        fprintf(FH, "<Password>\r\n%s\r\n</Password>\r\n", encpassword);
        fclose(FH);
        EUCA_FREE(encpassword);
        EUCA_FREE(tmp);
        EUCA_FREE(newpass);
        EUCA_FREE(newInstName);
        return (EUCA_OK);
    }

    EUCA_FREE(encpassword);
    EUCA_FREE(tmp);
    EUCA_FREE(newpass);
    EUCA_FREE(newInstName);
    return (EUCA_ERROR);
}
