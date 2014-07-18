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
void mido_copy_midoname(midoname *dst, midoname *src);

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

int mido_router_create_route(midoname *router, midoname *rport, char *src, char *src_slashnet, char *dst, char *dst_slashnet, char *next_hop_ip, char *weight);

int mido_create_dhcp(midoname *devname, char *subnet, char *slashnet, char *gw, char *dns, midoname *outname);
int mido_read_dhcp(midoname *name);
int mido_update_dhcp(midoname *name, ...);
int mido_print_dhcp(midoname *name);
int mido_delete_dhcp(midoname *devname, midoname *name);

int mido_create_dhcphost(midoname *devname, midoname *dhcp, char *mac, char *ip, midoname *outname);
int mido_delete_dhcphost(midoname *name);

int mido_create_port(midoname *devname, char *port_type, char *ip, char *nw, char *slashnet, midoname *outname);
int mido_read_port(midoname *name);
int mido_update_port(midoname *name, ...);
int mido_print_port(midoname *name);
int mido_delete_port(midoname *name);

int mido_link_ports(midoname *a, midoname *b);

int mido_link_host_port(midoname *host, char *interface, midoname *device, midoname *port);
int mido_unlink_host_port(midoname *host, midoname *port);

int mido_get_hosts(midoname **outnames, int *outnames_max);
int mido_get_interfaces(midoname *host, midoname **outports, int *outports_max);

int mido_create_chain(char *tenant, char *name, midoname *outname);
int mido_read_chain(midoname *name);
int mido_update_chain(midoname *name, ...);
int mido_print_chain(midoname *name);
int mido_delete_chain(midoname *name);

int mido_create_rule(midoname *chain, char *type, char *src, char *src_slashnet, char *src_ports, char *dst, char *dst_slashnet, char *dst_ports, char *action, char *nat_target, char *nat_port_min, char *nat_port_max, midoname *outname);
int mido_read_rule(midoname *name);
int mido_update_rule(midoname *name, ...);
int mido_print_rule(midoname *name);
int mido_delete_rule(midoname *name);

int mido_create_resource(midoname *parents, int max_parents, midoname *newname, midoname *outname, ...);
int mido_read_resource(char *resource_type, midoname *name);
int mido_update_resource(char *resource_type, char *content_type, midoname *name, va_list *al);
int mido_print_resource(char *resource_type, midoname *name);
int mido_delete_resource(midoname *parentname, midoname *name);


int midonet_http_get(char *url, char **out_payload);
int midonet_http_put(char *url, char *resource_type, char *payload);
int midonet_http_post(char *url, char *resource_type, char *payload, char **out_payload);
int midonet_http_delete(char *url);


#endif
