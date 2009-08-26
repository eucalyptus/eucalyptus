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
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include <unistd.h> /* getopt */
#include <fcntl.h> /* open */
#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"

#define BUFSIZE 4096 /* should be big enough for CERT and the signature */
#define STRSIZE 245 /* for short strings: files, hosts, URLs */
#define WALRUS_ENDPOINT "/services/Walrus"
#define DEFAULT_HOST_PORT "localhost:8773"
#define DEFAULT_COMMAND "GetObject"
#define USAGE { fprintf (stderr, "Usage: Wclient [GetDecryptedImage|GetObject] -h [host:port] -m [manifest] -f [output file] [-z]\n"); exit (1); }
char debug = 1;

int main (int argc, char * argv[])
{
	char * command = DEFAULT_COMMAND;
	char * hostport = NULL;
	char * manifest = NULL;
	char * file_name = NULL;
    int do_compress = 0;
	int ch;

	while ((ch = getopt(argc, argv, "dh:m:f:z")) != -1) {
		switch (ch) {
			case 'h':
				hostport = optarg; 
				break;
			case 'm':
				manifest = optarg;
				break;
			case 'd':
				debug = 1;
				break;
			case 'f':
				file_name = optarg;
				break;
            case 'z':
                do_compress = 1;
                break;
			case '?':
			default:
				USAGE;
		}
	}
	argc -= optind;
	argv += optind;

	if (argc>0) {
		command = argv[0];
	}

	if ( strcmp (command, "GetDecryptedImage")==0 
         || strcmp (command, "GetObject")==0 ) {
		if (manifest==NULL) {
			fprintf (stderr, "Error: manifest must be specified\n"); 
			USAGE;
		}
	} else {
		fprintf (stderr, "Error: unknown command [%s]\n", command);
		USAGE;
	}

    /* use a temporary file for network data */
    char * tmp_name = strdup ("walrus-download-XXXXXX");
    int tmp_fd = mkstemp (tmp_name);
    if (tmp_fd<0) {
        fprintf (stderr, "Error: failed to create a temporary file\n"); 
        USAGE;
    }
    close (tmp_fd);
        
    int result;
    char request [STRSIZE];
    if (hostport) {
        snprintf (request, STRSIZE, "http://%s%s/%s", hostport, WALRUS_ENDPOINT, manifest);
        if ( strcmp(command, "GetObject")==0 ) {
            result = walrus_object_by_url (request, tmp_name, do_compress);
        } else {
            result = walrus_image_by_manifest_url (request, tmp_name, do_compress);
        }
    } else {
        strncpy (request, manifest, STRSIZE);
        if ( strcmp(command, "GetObject")==0 ) {
            result = walrus_object_by_path (request, tmp_name, do_compress);
        } else {
            result = walrus_image_by_manifest_path (request, tmp_name, do_compress);
        }
    }
    
    if (result) { 
        /* error has occured */
        cat (tmp_name);
        fprintf (stderr, "\n"); /* in case error doesn't end with a newline */
        remove (tmp_name);
    } else {
        /* all's well */
        if (file_name) {
            rename (tmp_name, file_name);
        } else {
            fprintf (stderr, "Saved output in %s\n", tmp_name);
        }
    }
    
    free (tmp_name);		
	
	return 0;
}
