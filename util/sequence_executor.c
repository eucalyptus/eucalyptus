#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <misc.h>

#include "sequence_executor.h"


int se_init(sequence_executor *se, int clean_only_on_fail) {
  if (!se) {
    return(1);
  }
  if (!memset(se, 0, sizeof(sequence_executor))) {
    return(1);
  }
  se->init = 1;
  se->clean_only_on_fail = clean_only_on_fail;
  return(0);
}

int se_add(sequence_executor *se, char *command, char *cleanup_command, void *checker) {
  if (!se || !se->init) {
    return(1);
  }

  if (command) {
    se->commands[se->max_commands] = strdup(command);
  } else {
    se->commands[se->max_commands] = NULL;
  }
  if (cleanup_command) {
    se->cleanup_commands[se->max_commands] = strdup(cleanup_command);
  } else {
    se->cleanup_commands[se->max_commands] = NULL;
  }
  if (checker) {
    se->checkers[se->max_commands] = checker;
  } else {
    se->checkers[se->max_commands] = NULL;
  }
  se->max_commands++;
  return(0);
}

int se_print(sequence_executor *se) {
  int i;
  if (!se || !se->init) {
    return(1);
  }
  for (i=0; i<se->max_commands; i++) {
    LOGDEBUG("COMMAND: %s CLEANUP_COMMAND: %s\n", se->commands[i], se->cleanup_commands[i]);
  }
  return(0);
}

int se_execute(sequence_executor *se) {
  int i, rc, failed, lastran=0, ret;
  char out[1024], err[1024];
  if (!se || !se->init || se->max_commands <= 0) {
    return(1);
  }

  ret=0; failed=0;

  for (i=0; i<se->max_commands; i++) {
    LOGDEBUG("RUNNING COMMAND: %s\n", se->commands[i]);
    rc = timeshell(se->commands[i], out, err, 1024, se->commands_timers[i] ? se->commands_timers[i] : 1);
    lastran=i;
    
    if (se->checkers[i]) {
      rc = se->checkers[i](rc, out, err);
    }
    if (rc) {
      LOGERROR("COMMAND FAILED with %d: %s (stdout=%s, stderr=%s)\n", rc, se->commands[i], out, err);
      failed=1;
      break;
    }
  }

  if (se->clean_only_on_fail && failed) {
    for (i=lastran; i>=0; i--) {
      if (se->cleanup_commands[i]) {
	LOGDEBUG("RUNNING CLEANUP_COMMAND: %s\n", se->cleanup_commands[i]);
	rc = system(se->cleanup_commands[i]);
	rc = rc>>8;
      }
    }
  }

  if (failed) {
    ret=1;
  }
  
  return(ret);
}

int se_free(sequence_executor *se) {
  int i;
  if (!se || !se->init) {
    return(1);
  }
  for (i=0; i<se->max_commands; i++) {
    if (se->commands[i]) free(se->commands[i]);
    if (se->cleanup_commands[i]) free(se->cleanup_commands[i]);
  }
  return(se_init(se, se->clean_only_on_fail));
}


int ignore_exit(int rc, char *stdoutbuf, char *stderrbuf) {
  return(0);
}

int ignore_exit2(int rc, char *stdoutbuf, char *stderrbuf) {
  if (rc && (rc != 2)) {
    return(rc);
  }
  return(0);
}
