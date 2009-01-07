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
#define USAGE { fprintf (stderr, "Usage: Wclient [GetDecryptedImage|GetObject] -h [host:port] -m [manifest] -f [output file]\n"); exit (1); }
char debug = 1;

int main (int argc, char * argv[])
{
	char * command = DEFAULT_COMMAND;
	char * hostport = NULL;
	char * manifest = NULL;
	char * file_name = NULL;
	int ch;

	while ((ch = getopt(argc, argv, "dh:m:f:")) != -1) {
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
            result = walrus_object_by_url (request, tmp_name);
        } else {
            result = walrus_image_by_manifest_url (request, tmp_name);
        }
    } else {
        strncpy (request, manifest, STRSIZE);
        if ( strcmp(command, "GetObject")==0 ) {
            result = walrus_object_by_path (request, tmp_name);
        } else {
            result = walrus_image_by_manifest_path (request, tmp_name);
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
