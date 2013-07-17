#ifndef INCLUDE_SEQUENCE_EXECUTOR_H
#define INCLUDE_SEQUENCE_EXECUTOR_H

#define MAX_SE_COMMANDS 1024

typedef int (*func_ptr)(int,char*,char*);
typedef struct sequence_executor_t {
  char *commands[MAX_SE_COMMANDS];
  int commands_timers[MAX_SE_COMMANDS];
  
  char *cleanup_commands[MAX_SE_COMMANDS];
  int cleanup_commands_timers[MAX_SE_COMMANDS];
  
  func_ptr checkers[MAX_SE_COMMANDS];
  
  int max_commands, init, clean_only_on_fail;
} sequence_executor;

// interface functions
int se_init(sequence_executor *se, int clean_only_on_fail);
int se_add(sequence_executor *se, char *command, char *cleanup_command, void *checker);
int se_print(sequence_executor *se);
int se_execute(sequence_executor *se);
int se_free(sequence_executor *se);

// some pre-defined command exit checkers
int ignore_exit(int rc, char *stdoutbuf, char *stderrbuf);
int ignore_exit2(int rc, char *stdoutbuf, char *stderrbuf);


#endif
