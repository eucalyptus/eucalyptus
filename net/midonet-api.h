#ifndef INCLUDE_MIDONET_API_H
#define INCLUDE_MIDONET_API_H


typedef struct midoname_t {
  char *tenant;
  char *name;
  char *uuid;
  char *jsonbuf;
  char *resource_type;
  char *content_type;
} midoname;

void mido_free_midoname(midoname *name);
int mido_update_midoname(midoname *name);

int mido_create_tenant(char *name, midoname *outname);
int mido_read_tenant(midoname *name);
int mido_update_tenant(midoname *name);
int mido_delete_tenant(midoname *name);

int mido_create_bridge(char *tenant, char *name, midoname *outname);
int mido_read_bridge(midoname *name);
int mido_update_bridge(midoname *name, ...);
int mido_print_bridge(midoname *name);
int mido_delete_bridge(midoname *name);

int mido_create_router(char *tenant, char *name, midoname *outname);
int mido_read_router(midoname *name);
int mido_update_router(midoname *name, ...);
int mido_print_router(midoname *name);
int mido_delete_router(midoname *name);

int mido_router_create_route(midoname *router, midoname *rport, char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip);

int mido_create_port(midoname *devname, char *port_type, char *ip, char *nw, char *slashnet, midoname *outname);
int mido_read_port(midoname *name);
int mido_update_port(midoname *name, ...);
int mido_print_port(midoname *name);
int mido_delete_port(midoname *name);

int mido_link_ports(midoname *a, midoname *b);

int mido_create_resource(midoname *parentname, midoname *newname, midoname *outname, ...);
int mido_read_resource(char *resource_type, midoname *name);
int mido_update_resource(char *resource_type, char *content_type, midoname *name, va_list *al);
int mido_print_resource(char *resource_type, midoname *name);
int mido_delete_resource(char *resource_type, midoname *name);


int midonet_http_get(char *url, char **out_payload);
int midonet_http_put(char *url, char *resource_type, char *payload);
int midonet_http_post(char *url, char *resource_type, char *payload, char **out_payload);
int midonet_http_delete(char *url);


#endif
