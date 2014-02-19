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

#ifndef _INCLUDE_EUCA_AUTH_H_
#define _INCLUDE_EUCA_AUTH_H_

//!
//! @file util/euca_auth.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <curl/curl.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name options for euca_get_cert()
//! @see euca_get_cert()

#define TRIM_CERT                     0x01  //!< remove the last newline
#define CONCATENATE_CERT              0x02  //!< remove all newlines
#define INDENT_CERT                   0x04  //!< indent lines 2-N

//! @}

//! @{
//! @name Components of URL

#define URL_COMPLETE	0
#define URL_PROTOCOL	1
#define URL_HOSTNAME	2
#define URL_PORT		4
#define URL_PATH		5
#define URL_QUERY		7

//! @}

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

//! A struct for holding a single header for sorting etc
struct key_value_pair {
    char *key;
    char *value;
};

//! An array of key_value_pairs, including the length
struct key_value_pair_array {
    int size;
    struct key_value_pair **data;
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int euca_init_cert(void);
char *euca_get_cert(u8 options);
char *base64_enc(u8 * sIn, int size);
char *base64_dec(u8 * sIn, int size);
char *base64_dec2(u8 * sIn, int size, int *decoded_length);
char *hexify(unsigned char *data, int data_len);
char *calc_fingerprint(const char *cert_filename);
void free_key_value_pair_array(struct key_value_pair_array *kv_array);
struct key_value_pair *deconstruct_header(const char *header_str, char delimiter);
struct key_value_pair_array *convert_header_list_to_array(const struct curl_slist *header_list, char delimiter);
char *construct_canonical_headers(struct key_value_pair_array *hdr_array);
char *construct_canonical_uri(const char *url);
char *construct_canonical_query(const char *url);
char *construct_signed_headers(struct key_value_pair_array *hdr_array);
char *eucav2_sign_request(const char *verb, const char *url, const struct curl_slist *headers);
char *euca_sign_url(const char *sVerb, const char *sDate, const char *sURL);
char *process_url(const char *content, int url_component);
int decrypt_string(char *in_buffer, char *pk_file, char **out_buffer);
int encrypt_string(char *in_buffer, char *cert_file, char **out_buffer);
int encrypt_string_with_cloud(char *in_str, char **out_buffer);
int encrypt_string_with_node(char *in_str, char **out_buffer);
int decrypt_string_with_node(char *in_str, char **out_buffer);
int decrypt_string_with_node_and_symmetric_key(char *in_buffer, char *key_buffer, char **out_buffer, int *out_len);
int encrypt_string_symmetric(char *in_buffer, char *key_buffer, char *iv_buffer, char **out_buffer, int *out_len);
int decrypt_string_symmetric(char *in_buffer, char *key_buffer, char *iv_buffer, char **out_buffer, int *out_len);

#ifdef _UNIT_TEST
void print_key_value_pair_array(const struct key_value_pair_array *kv_array);
#endif // _UNIT_TEST

#if 0
int euca_init_cert(void);

char *euca_get_cert(u8 options);

char *base64_enc(u8 * in, int size);
char *base64_dec(u8 * in, int size);

char *euca_sign_url(const char *verb, const char *date, const char *url);

void print_key_value_pair_array(const struct key_value_pair_array *kv_array);

/*
 * Returns a newly allocated string with the desired url component.
 * URL components are:
 * 0 = complete url, just does a check to make sure it is valid, returns whole string if valid
 * 1 = protocol
 * 2 = hostname
 * 3 = port
 * 4 = path
 * 5 = query string
 *
 * If no such component is found then an empty string is returned. NULL is only returned as an error
 *
 * Caller must free the returned string.
 */
char *process_url(const char *content, int url_component);

/*
 * Caller must free the string returned.
 * Converts the byte array to the hex string representing it.
 */
char *hexify(unsigned char *data, int data_len);

/*
 * Calculates the md5 fingerprint of the certificate in the specified file.
 * Fingerprint string must be freed by caller
 */
char *calc_fingerprint(const char *cert_filename);

/*
 * Frees an entire key_value_pair_array struct and all key_value_pair structs it contains.
 * Will free all name and value strings in the headers contained in the array.
 */
void free_header_array(struct key_value_pair_array *hdr_array);

/*
 * Get the header name from the full string value (expects string of format 'name: value')
 * Will handle spaces around name, colon, and value. Does trim.
 *
 * If header_str is NULL, returns NULL
 */
struct key_value_pair *deconstruct_header(const char *header_str, char delimiter);

/*
 * Converts a linked-list of string-represented headers into an array of header structs for ease of sorting later
 * The returned pointer points to an array
 */
struct key_value_pair_array *convert_header_list_to_array(const struct curl_slist *header_list, char delimiter);

/*
 * Constructs a string that is the Authorization header for the request to Walrus. This should be the *last* header
 * added before the request is sent as it requires all the other headers for signing.
 *
 * This method calculates the message signature by RSA_SHA256(NC_private_key,SHA256(CanonicalRequest)) where the CanonicalRequest is:
 *
 * CanonicalRequest =
 * HTTP Verb + '\n' +
 * CanonicalURI + '\n' +
 * CanonicalQuery + '\n' +
 * CanonicalHeaders + '\n' +
 * SignedHeaders
 *
 * Signature is added to message as the Authorization header, of the form:
 *  Authorization: EUCA2-RSA-SHA256 <md5 fingerprint of signing certificate> <signedheaders list> <signature>
 *
 * The caller must free the returned string.
 */
char *eucav2_sign_request(const char *verb, const char *url, const struct curl_slist *headers);

/*
 * Caller must free returned string.
 *
 * Constructs and returns the CanonicalURI string from the passed-in url.
 *
 * CanonicalURI: the URI-encoded version of the absolute path component of the URI
 * (this is everything between the HTTP host header to the question mark character (?) that
 * begins the query string parameters) followed by a newline character. Normalize URI Paths
 * according to RFC 3986 rules by removing redundant and relative path components. Each path
 * segment must be URI-encoded. If the absolute path is empty, use a forward slash (/).
 *
 * E.G. construct_canonical_uri(http://192.168.1.1:8773/services/Walrus/bucket?acl) = /services/Walrus/bucket
 *
 */
char *construct_canonical_uri(const char *url);

/*
 * Constructs a string that is the CanonicalQueryString for EUCAV2 internal signing.
 * The canonical query string is the sorted list of query parameters and values in url-encoded form.
 * e.g. construct_canonical_query(http://192.168.1.1:8773/services/Walrus/bucket?acl&versionId=123455) = acl=&versionid=123455

 * The specific algorithm:
 * 	1. URI-encode each parameter according to the following rules:
 * 		a. Do not URL encode any of the unreserved characters that RFC 3986 defines. These unreserved
 * 		characters are A-Z, a-z, 0-9, hyphen (-), underscore (_), period (.), and tilde (~).
 *		b. Percent encode all other characters with %XY, where X and Y are hex characters 0-9 and uppercase A-F.
 *		c. Percent encode extended UTF-8 characters in the form %XY%ZA….
 *		d. Percent encode the space character as %20 (and not +, as common encoding schemes do).
 *	2. Sort the encoded parameters by character code.
 *		a. For example, a parameter name that begins with the upper-case letter F (ASCII code 70) would
 *		precede a parameter name that begins with a lower-case letter b (ASCII code 98).
 *	3. Build the CanonicalQueryString by starting with the first parameter in the sorted list.
 *		a. For each parameter, append the URI-encoded parameter name, followed by the character = (ASCII code 61),
 *		followed by the URI-encoded parameter value.
 *		b. Use an empty string for parameters without a value.
 *		c. Append the character & (ASCII code 38) after each parameter value except the last value in the list.
 *
 *	Caller must free the returned string.
 */
char *construct_canonical_query(const char *url);

/*
 * Constructs the string that is the CanonicalHeaders, which is a list of all the HTTP headers that you plan to send as part of the request.
 * To create a canonical headers list:
 * Convert all header names to lowercase and trim all header values of excess whitespace characters.
 * The following pseudo-code (python-ish) describes how to contruct the canonical list of headers.
 * 		tmp_headers = []
 *		for (header, value in request.headers.items())
 *		{
 *			tmp_headers.append(header).lower() + ':' + value.strip())
 *		}
 *		CanonicalHeaders = '\n'.join(sorted(canonical_headers))
 *
 *	For each header:
 *	1. Append the lower case header name, followed by a colon.
 *	2. Append a comma separated list of values that reflect the values for that header (where there were duplicate headers
 *		the values are comma separated).
 *	3. Append a newline (\n).
 *		a. Do not include the Authorization header.
 *		b. You must include a valid host header.
 *		c. You must also include the date as part of the date or x-amz-date header, or as part of an x-amz-date query parameter.
 *
 * Caller must free the returned string.
 */
char *construct_canonical_headers(struct key_value_pair_array *hdr_array);

/*
 * Constructs the SignedHeaders, which is the list of HTTP headers that you included in
 * the CanonicalHeaders.
 *
 * The following python code describes how to construct a list of signed headers:
 * SignedHeaders = ';'.join(sorted(header.lower() for header in request_headers))
 * 	a. Build the SignedHeaders list by iterating through the collection of header names, sorted by lower case character code.
 * 	b.For each header name except the last, append a semicolon (;) to the header name to separate it from the following header name.
 *
 * 	The caller must free the returned string
 */
char *construct_signed_headers(struct key_value_pair_array *hdr_array);
#endif // 0

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCA_AUTH_H_ */
