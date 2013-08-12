#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include <log.h>
#include <http.h>
#include <hash.h>
#include "atomic_file.h"

int atomic_file_init(atomic_file *file, char *source, char *dest) {
    if (!file) {
      return(1);
    }

    atomic_file_free(file);
    
    snprintf(file->dest, MAX_PATH, "%s", dest);
    snprintf(file->source, MAX_PATH, "%s", source);
    snprintf(file->tmpfilebase, MAX_PATH, "%s-XXXXXX", dest);
    snprintf(file->tmpfile, MAX_PATH, "%s-XXXXXX", dest);
    file->lasthash = strdup("UNSET");
    file->currhash = strdup("UNSET");
    return(0);
}

int atomic_file_get(atomic_file *file, int *file_updated) {
    char type[32], hostname[512], path[MAX_PATH], tmpsource[MAX_PATH], tmppath[MAX_PATH];
    int port, fd, ret, rc;
    
    if (!file || !file_updated) {
      return(1);
    }

    ret=0;
    *file_updated = 0;

    snprintf(file->tmpfile, MAX_PATH, "%s", file->tmpfilebase);
    fd = safe_mkstemp(file->tmpfile);
    if (fd < 0) {
      LOGERROR("cannot open tmpfile '%s'\n", file->tmpfile);
      return (1);
    }
    chmod(file->tmpfile, 0644);
    close(fd);

    snprintf(tmpsource, MAX_PATH, "%s", file->source);
    type[0] = tmppath[0] = path[0] = hostname[0] = '\0';
    port = 0;
    
    tokenize_uri(tmpsource, type, hostname, &port, tmppath);
    snprintf(path, MAX_PATH, "/%s", tmppath);

    if (!strcmp(type, "http")) {
      rc = http_get_timeout(file->source, file->tmpfile, 0, 0, 10, 15);
      if (rc) {
        LOGERROR("http client failed to fetch file URL=%s\n", file->source);
        ret=1;
      }
    } else if (!strcmp(type, "file")) {
      if (!strlen(path) || copy_file(path, file->tmpfile)) {
        LOGERROR("could not rename source file (%s) to dest file (%s)\n", path, file->tmpfile);
        ret=1;
      }
    } else {
      LOGWARN("incompatible URI type (only support http, file): (%s)\n", type);
      ret=1;
    }
    
    if (!ret) {
      char *hash=NULL;
      // do checksum - only copy if file has changed
      hash = file2md5str(file->tmpfile);
      if (!hash) {
        LOGERROR("could not compute hash of tmpfile (%s)\n", file->tmpfile);
        ret = 1;
      } else {
        if (file->currhash) EUCA_FREE(file->currhash);
        file->currhash = hash;
        if (check_file(file->dest) || strcmp(file->currhash, file->lasthash)) {
          // hashes are different, put new file in place
            LOGDEBUG("renaming file %s -> %s\n", file->tmpfile, file->dest);
            if (rename(file->tmpfile, file->dest)) {
              LOGERROR("could not rename local copy to dest (%s -> %s)\n", file->tmpfile, file->dest);
              ret=1;
            } else {
              EUCA_FREE(file->lasthash);
              file->lasthash = strdup(file->currhash);
              *file_updated = 1;
            }
        }
      }
    }
    
    unlink(file->tmpfile);
    return(ret);
}

int atomic_file_free(atomic_file *file) {
    if (!file) return(1);
    if (file->lasthash) EUCA_FREE(file->lasthash);
    if (file->currhash) EUCA_FREE(file->currhash);
    bzero(file, sizeof(atomic_file));
    return(0);
}
