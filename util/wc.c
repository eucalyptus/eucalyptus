// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <ctype.h>				// isspace
#include <assert.h>
#include <stdarg.h>
#include <errno.h>				// errno
#include <locale.h>				// setlocale
#include <string.h>

#include "misc.h"				// boolean
#include "wc.h"

#define VAR_PREFIX L"${"		// this sequence of chars indicates beginning of a variable name
#define VAR_SUFFIX L"}"			// this sequence of chars, when following VAR_PREFIX, means the end

#define C_VAR_PREFIX "${"		// this sequence of chars indicates beginning of a variable name
#define C_VAR_SUFFIX "}"		// this sequence of chars, when following VAR_PREFIX, means the end

// searches the 'vars' map (NULL-terminated array of wchar_map * pointers)
// for an entry with 'key' equaling 'name' with only the first 'name_len'
// characters considered; returns pointer to the 'val' in the map
static wchar_t *find_valn(const wchar_map * vars[], const wchar_t * name, size_t name_len)
{
	for (int i = 0; vars[i] != NULL; i++) {
		const wchar_map *v = vars[i];
		if (wcsncmp(v->key, name, name_len) == 0)
			return v->val;
	}
	return NULL;
}

// searches the 'vars' map (NULL-terminated array of wchar_map * pointers)
// for an entry with 'key' equaling 'name' with only the first 'name_len'
// characters considered; returns pointer to the 'val' in the map
static char *c_find_valn(const char_map * vars[], const char *name, size_t name_len)
{
	for (int i = 0; vars[i] != NULL; i++) {
		const char_map *v = vars[i];
		if (strncmp(v->key, name, name_len) == 0)
			return v->val;
	}
	return NULL;
}

// appends string 'src' to 'dst', up to 'src_len' characters (unless set
// to 0, in which case to the end of 'src'), enlarging the 'dst' as necessary
// returns the concatenated string or NULL if memory could not be allocated
// if 'src' is an empty string, 'dst' is returned.
static wchar_t *wcappendn(wchar_t * dst, const wchar_t * src, size_t src_limit)
{
	size_t src_len = wcslen(src);
	if (src_len < 1)
		return dst;
	if (src_len > src_limit && src_limit > 0)
		src_len = src_limit;
	size_t dst_len = 0;
	if (dst) {
		dst_len = wcslen(dst);
		dst = (wchar_t *) realloc(dst, (dst_len + src_len + 1) * sizeof(wchar_t));
	} else {
		dst = (wchar_t *) malloc((dst_len + src_len + 1) * sizeof(wchar_t));
		*dst = L'\0';
	}
	if (dst == NULL)
		return dst;
	return wcsncat(dst, src, src_len);
}

// appends string 'src' to 'dst', up to 'src_len' characters (unless set
// to 0, in which case to the end of 'src'), enlarging the 'dst' as necessary
// returns the concatenated string or NULL if memory could not be allocated
// if 'src' is an empty string, 'dst' is returned.
static char *c_wcappendn(char *dst, const char *src, size_t src_limit)
{
	size_t src_len = strlen(src);
	if (src_len < 1)
		return dst;
	if (src_len > src_limit && src_limit > 0)
		src_len = src_limit;
	size_t dst_len = 0;
	if (dst) {
		dst_len = strlen(dst);
		dst = (char *)realloc(dst, (dst_len + src_len + 1) * sizeof(char));
	} else {
		dst = (char *)malloc((dst_len + src_len + 1) * sizeof(char));
		*dst = '\0';
	}
	if (dst == NULL)
		return dst;
	return strncat(dst, src, src_len);
}

// substitutes in 's' all occurence of variables '${var}'
// based on the 'vars' map (NULL-terminated array of wchar_map * pointers)
// returns a new string with all variables substituted or returns NULL
// (and logs an error with logprintfl()) if some variables were not
// found in the map or if the map is empty
//
// FIXME: This currently will not sub any variables if it can't sub *all*
//        variables. This is unfriendly: it should sub what it can.
wchar_t *varsub(const wchar_t * s, const wchar_map * vars[])
{
	if (s == NULL) {
		return NULL;
	}
	if (vars == NULL) {
		return (wchar_t *) wcsdup(s);
	}
	size_t pref_len = wcslen(VAR_PREFIX);
	size_t suff_len = wcslen(VAR_SUFFIX);

	int vars_subbed = 0;
	boolean malformed = FALSE;
	wchar_t *result = NULL;
	const wchar_t *remainder = s;

	wchar_t *var_start;
	while ((var_start = wcsstr(remainder, VAR_PREFIX)) != NULL) {	// we have a beginning of a variable
		if (wcslen(var_start) <= (pref_len + suff_len)) {	// nothing past the prefix
			malformed = TRUE;
			break;
		}
		wchar_t *var_end = wcsstr(var_start + pref_len, VAR_SUFFIX);
		if (var_end == NULL) {	// not suffix after prefix
			malformed = TRUE;
			break;
		}
		size_t var_len = var_end - var_start - pref_len;	// length of the variable
		if (var_len < 1) {		// empty var name
			remainder = var_end + suff_len;	// move the pointer past the empty variable (skip it)
			malformed = TRUE;
			continue;
		}
		wchar_t *val = find_valn(vars, var_start + pref_len, var_len);
		if (val == NULL) {
			logprintfl(EUCAWARN, "failed to substitute variable\n");	// TODO: print variable name
			if (result != NULL)
				free(result);
			return NULL;
		}
		if (var_start > remainder) {	// there is text prior to the variable
			result = wcappendn(result, remainder, var_start - remainder);
			if (result == NULL) {
				logprintfl(EUCAERROR, "failed to append during variable substitution");	// TODO: more specific error
				break;
			}
		}
		result = wcappendn(result, val, 0);
		remainder = var_end + suff_len;
	}
	result = wcappendn(result, remainder, 0);

	if (malformed) {
		logprintfl(EUCAWARN, "malformed string used for substitution\n");	// TODO: print the string
	}

	return result;
}

wchar_map **varmap_alloc(wchar_map ** map, const wchar_t * key, const wchar_t * val)
{
	int i = 0;

	if (map == NULL) {
		PRINTF1(("malloc(): %d\n", sizeof(wchar_map *) + sizeof(wchar_t *)));
		map = malloc(sizeof(wchar_map *) + sizeof(wchar_t *));
	} else {
		while (map[i]) {
			i++;
		}
		PRINTF1(("relloc(): %d\n", (i + 1) * sizeof(wchar_map *) + sizeof(wchar_t *)));
		map = realloc(map, (i + 1) * sizeof(wchar_map *) + sizeof(wchar_t *));
	}
	map[i] = malloc(sizeof(wchar_map));
	map[i]->key = wcsdup(key);
	map[i]->val = wcsdup(val);
	map[i + 1] = NULL;			/* NULL terminator */

	return map;
}

void varmap_free(wchar_map ** map)
{
	int i = 0;

	if (map == NULL) {
		PRINTF(("varmap_free() called on NULL map.\n"));
		return;
	}
	while (map[i]) {
		free(map[i]->key);
		free(map[i]->val);
		free(map[i]);
		i++;
	}
	free(map[i]);				/* NULL terminator */
	free(map);
}

// substitutes in 's' all occurence of variables '${var}'
// based on the 'vars' map (NULL-terminated array of wchar_map * pointers)
// returns a new string with all variables substituted or returns NULL
// (and logs an error with logprintfl()) if some variables were not
// found in the map or if the map is empty
char *c_varsub(const char *s, const char_map * vars[])
{
	if (s == NULL) {
		return NULL;
	}
	if (vars == NULL) {
		return (char *)strdup(s);
	}
	size_t pref_len = strlen(C_VAR_PREFIX);
	size_t suff_len = strlen(C_VAR_SUFFIX);

	int vars_subbed = 0;
	boolean malformed = FALSE;
	char *result = NULL;
	const char *remainder = s;

	char *var_start;
	while ((var_start = strstr(remainder, C_VAR_PREFIX)) != NULL) {	// we have a beginning of a variable
		if (strlen(var_start) <= (pref_len + suff_len)) {	// nothing past the prefix
			malformed = TRUE;
			break;
		}
		char *var_end = strstr(var_start + pref_len, C_VAR_SUFFIX);
		if (var_end == NULL) {	// not suffix after prefix
			malformed = TRUE;
			break;
		}
		size_t var_len = var_end - var_start - pref_len;	// length of the variable
		if (var_len < 1) {		// empty var name
			remainder = var_end + suff_len;	// move the pointer past the empty variable (skip it)
			malformed = TRUE;
			continue;
		}
		char *val = c_find_valn(vars, var_start + pref_len, var_len);
		if (val == NULL) {
			char *missed_var = NULL;
			char *vartok = NULL;
			if ((missed_var = strndup(var_start + pref_len, var_len)) == NULL) {
				logprintfl(EUCAWARN, "failed to substitute variable\n");
				continue;
			} else {

				logprintfl(EUCAWARN, "substituted variable: %s%s%s\n", C_VAR_PREFIX, missed_var, C_VAR_SUFFIX);
			}

			if ((vartok = (char *)malloc(strlen(C_VAR_PREFIX) + strlen(C_VAR_SUFFIX) + strlen(missed_var) + 1)) == NULL) {
				if (result != NULL) {
					free(result);
				}
				free(missed_var);
				return NULL;
			}
			sprintf(vartok, "%s%s%s", C_VAR_PREFIX, missed_var, C_VAR_SUFFIX);
			if (var_start > remainder) {	// there is text prior to the variable
				result = c_wcappendn(result, remainder, var_start - remainder);
				if (result == NULL) {
					logprintfl(EUCAERROR, "failed to append during variable substitution");	// TODO: more specific error
					free(vartok);
					free(missed_var);
					break;
				}
			}
			result = c_wcappendn(result, vartok, 0);
			remainder = var_end + suff_len;	// move the pointer past the empty variable (skip it)
			if (missed_var != NULL) {
				free(missed_var);
			}
			free(vartok);
			continue;
		}
		if (var_start > remainder) {	// there is text prior to the variable
			result = c_wcappendn(result, remainder, var_start - remainder);
			if (result == NULL) {
				logprintfl(EUCAERROR, "failed to append during variable substitution");	// TODO: more specific error
				break;
			}
		}
		result = c_wcappendn(result, val, 0);
		remainder = var_end + suff_len;
	}
	result = c_wcappendn(result, remainder, 0);

	if (malformed) {
		logprintfl(EUCAWARN, "malformed string used for substitution\n");	// TODO: print the string
	}

	return result;
}

char_map **c_varmap_alloc(char_map ** map, const char *key, const char *val)
{
	int i = 0;

	if (map == NULL) {
		PRINTF1(("malloc(): %d\n", sizeof(char_map *) + sizeof(char *)));
		map = malloc(sizeof(char_map *) + sizeof(char *));
	} else {
		while (map[i]) {
			i++;
		}
		PRINTF1(("relloc(): %d\n", (i + 1) * sizeof(char_map *) + sizeof(char *)));
		map = realloc(map, (i + 1) * sizeof(char_map *) + sizeof(char *));
	}
	map[i] = malloc(sizeof(char_map));
	map[i]->key = strdup(key);
	map[i]->val = strdup(val);
	map[i + 1] = NULL;			/* NULL terminator */

	return map;
}

void c_varmap_free(char_map ** map)
{
	int i = 0;

	if (map == NULL) {
		PRINTF(("c_varmap_free() called on NULL map.\n"));
		return;
	}
	while (map[i]) {
		free(map[i]->key);
		free(map[i]->val);
		free(map[i]);
		i++;
	}
	free(map[i]);				/* NULL terminator */
	free(map);
}

/////////////////////////////////////////////// unit testing code ///////////////////////////////////////////////////

#ifdef _UNIT_TEST

const wchar_t *s1 = L"The quick ${color} ${subject} jümps øver the låzy ${øbject}";
const wchar_t *s2 = L"${}A m${}alformed ${color} string${}${}";
const wchar_t *s3 = L"An undefined ${variable}";
wchar_map **m = NULL;

const char *c_s1 = "The quick ${color} ${subject} jumps over the lazy ${object}";
const char *c_s2 = "${}A m${}alformed ${color} string${}${}";
const char *c_s3 = "An undefined ${variable}";
char_map **c_m = NULL;

int main(int argc, char **argv)
{
	setlocale(LC_ALL, "en_US.UTF-8");

	m = varmap_alloc(NULL, L"color", L"brown");
	m = varmap_alloc(m, L"subject", L"føx");
	m = varmap_alloc(m, L"øbject", L"dog");

	printf("       nice string: %ls\n", s1);
	wchar_t *s1_sub = varsub(s1, (const wchar_map **)m);
	assert(s1_sub != NULL);
	printf("nice string subbed: %ls\n", s1_sub);
	free(s1_sub);
	printf("       ugly string: %ls\n", s2);
	wchar_t *s2_sub = varsub(s2, (const wchar_map **)m);
	assert(s2_sub != NULL);
	printf("ugly string subbed: %ls\n", s2_sub);
	free(s2_sub);
	printf(" unsubbable string: %ls\n", s3);
	assert(varsub(s3, (const wchar_map **)m) == NULL);

	varmap_free(m);

	printf("  sending null map: %ls\n", s3);	// Reuse s3
	wchar_t *s3_sub = varsub(s3, NULL);
	printf("returned from null: %ls\n", s3_sub);
	free(s3_sub);

	// Now do it again, this time non-widechar
	c_m = c_varmap_alloc(NULL, "colorxxxxxx", "brown");	// FIXME: This matches
	c_m = c_varmap_alloc(c_m, "subject", "fox");
	c_m = c_varmap_alloc(c_m, "object", "dog");

	printf("       nice string: %s\n", c_s1);
	char *c_s1_sub = c_varsub(c_s1, (const char_map **)c_m);

	printf("nice string subbed: %s\n", c_s1_sub);

	assert(c_s1_sub != NULL);
	free(c_s1_sub);
	printf("       ugly string: %s\n", c_s2);
	char *c_s2_sub = c_varsub(c_s2, (const char_map **)c_m);
	assert(c_s2_sub != NULL);
	printf("ugly string subbed: %s\n", c_s2_sub);
	free(c_s2_sub);

	printf(" unsubbable string: %s\n", c_s3);
	char *c_s3_sub = c_varsub(c_s3, (const char_map **)c_m);
	printf("   unsubbed string: %s\n", c_s3_sub);
	assert(!strcmp(c_s3, c_s3_sub));
	free(c_s3_sub);

	c_varmap_free(c_m);

	printf("  sending null map: %s\n", c_s3);	// Reuse s3
	c_s3_sub = c_varsub(c_s3, NULL);
	printf("returned from null: %s\n", c_s3_sub);
	assert(!strcmp(c_s3, c_s3_sub));
	free(c_s3_sub);
}

#endif
