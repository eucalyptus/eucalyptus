#ifndef INCLUDE_IPT_HANDLER_H
#define INCLUDE_IPT_HANDLER_H

typedef struct ipt_rule_t {
  char iptrule[1024];
} ipt_rule;

typedef struct ipt_chain_t {
  char name[64], policyname[64], counters[64];
  ipt_rule *rules;
  int max_rules;
} ipt_chain;

typedef struct ipt_table_t {
  char name[64];
  ipt_chain *chains;
  int max_chains;
} ipt_table;

typedef struct ipt_handler_t {
  ipt_table *tables;
  int max_tables;
  int init;
  char ipt_file[MAX_PATH];
} ipt_handler;

int ipt_handler_init(ipt_handler *ipth);
int ipt_handler_free(ipt_handler *ipth);

int ipt_system_save(ipt_handler *ipth);
int ipt_system_restore(ipt_handler *ipth);

int ipt_handler_repopulate(ipt_handler *ipth);
int ipt_handler_deploy(ipt_handler *ipth);

int ipt_handler_add_table(ipt_handler *ipth, char *tablename);
int ipt_table_add_chain(ipt_handler *ipth, char *tablename, char *chainname, char *policyname, char *counters);
int ipt_chain_add_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule);

int ipt_chain_flush(ipt_handler *ipth, char *tablename, char *chainname);

int ipt_handler_print(ipt_handler *ipth);

#endif
