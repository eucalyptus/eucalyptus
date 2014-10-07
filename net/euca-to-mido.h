#ifndef INCLUDE_EUCATOMIDO_H
#define INCLUDE_EUCATOMIDO_H

#include <midonet-api.h>

enum {VPCSG_INGRESS, VPCSG_EGRESS, VPCSG_IAGPRIV, VPCSG_IAGPUB, VPCSG_IAGALL, VPCSG_END};
typedef struct mido_vpc_secgroup_t {
  gni_secgroup *gniSecgroup;
  char name[16];
  midoname midos[VPCSG_END];
  int gnipresent;
  
} mido_vpc_secgroup;

enum {VPCBR_VMPORT, VPCBR_DHCPHOST, VMHOST, ELIP_PRE, ELIP_POST, ELIP_PRE_IPADDRGROUP, ELIP_POST_IPADDRGROUP, ELIP_ROUTE, INST_PRECHAIN, INST_POSTCHAIN, VPCINSTANCEEND};
typedef struct mido_vpc_instance_t {
  gni_instance *gniInst;
  char name[16];
  midoname midos[VPCINSTANCEEND];
  int gnipresent;
} mido_vpc_instance;

enum {VPCBR, VPCBR_RTPORT, VPCRT_BRPORT, VPCBR_DHCP, VPCBR_METAPORT, VPCBR_METAHOST, VPCSUBNETEND};
typedef struct mido_vpc_subnet_t {
  gni_vpcsubnet *gniSubnet;
  char name[16], vpcname[16];
  midoname midos[VPCSUBNETEND];
  midoname *brports, *dhcphosts;
  mido_vpc_instance *instances;
  int max_brports, max_dhcphosts, max_instances;
  int gnipresent;
} mido_vpc_subnet;

enum {VPCRT, EUCABR_DOWNLINK, VPCRT_UPLINK, VPCRT_PRECHAIN, VPCRT_POSTCHAIN, VPCRT_PREETHERCHAIN, VPCRT_PREMETACHAIN, VPCRT_PREVPCINTERNALCHAIN, VPCRT_PREELIPCHAIN, VPCRT_PREFWCHAIN, VPCEND};
typedef struct mido_vpc_t {
  char name[16];
  int rtid;
  midoname midos[VPCEND];
  midoname *rtports;
  mido_vpc_subnet *subnets;
  int max_rtports, max_subnets;
  int gnipresent;
} mido_vpc;

enum {EUCART, EUCABR, EUCART_BRPORT, EUCABR_RTPORT, EUCART_GWPORT, GWHOST, METADATA_IPADDRGROUP, MIDOCOREEND};
typedef struct mido_core_t { 
  midoname midos[MIDOCOREEND];
  midoname *brports, *rtports;
  int max_brports, max_rtports;
} mido_core;

typedef struct mido_config_t {
  char *ext_rthostname, *ext_rtaddr, *ext_rtiface, *ext_pubnw, *ext_pubgwip, *eucahome;
  u32 int_rtnw, int_rtaddr, enabledCLCIp;
  int int_rtsn;
  midoname *hosts, *routers, *bridges, *chains, *brports, *rtports, *ipaddrgroups;
  int max_hosts, max_routers, max_bridges, max_chains, max_brports, max_rtports, max_ipaddrgroups;
  mido_core *midocore;
  mido_vpc *vpcs;
  int max_vpcs;
  mido_vpc_secgroup *vpcsecgroups;
  int max_vpcsecgroups;
  int router_ids[4096];
} mido_config;

int get_next_router_id(mido_config *mido, int *nextid);
int set_router_id(mido_config *mido, int id);

int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw, char *outplustwo);

int initialize_mido(mido_config *mido, char *eucahome, char *ext_rthostname, char *ext_rtaddr, char *ext_rtiface, char *ext_pubnw, char *ext_pubgwip, char *int_rtnetwork, char *int_rtslashnet);
int discover_mido_resources(mido_config *mido);

int populate_mido_core(mido_config *mido, mido_core *midocore);
int create_mido_core(mido_config *mido, mido_core *midocore);
int delete_mido_core(mido_config *mido, mido_core *midocore);

int populate_mido_vpc(mido_config *mido, mido_core *midocore, mido_vpc *vpc);
int create_mido_vpc(mido_config *mido, mido_core *midocore, mido_vpc *vpc);
int delete_mido_vpc(mido_config *mido, mido_vpc *vpc);
int find_mido_vpc(mido_config *mido, char *vpcname, mido_vpc **outvpc);

int populate_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet);
int create_mido_vpc_subnet(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, char *subnet, char *slashnet, char *gw);
int delete_mido_vpc_subnet(mido_config *mido, mido_vpc_subnet *subnet);
int find_mido_vpc_subnet(mido_vpc *vpc, char *subnetname, mido_vpc_subnet **outvpcsubnet);

int populate_mido_vpc_instance(mido_config *mido, mido_core *midocore, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *vpcinstance);
int create_mido_vpc_instance(mido_config *mido, mido_vpc_instance *vpcinstance, char *nodehostname);
int delete_mido_vpc_instance(mido_vpc_instance *vpcinstance);
int find_mido_vpc_instance(mido_vpc_subnet *vpcsubnet, char *instancename, mido_vpc_instance **outvpcinstance);

int populate_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup);
int create_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup);
int delete_mido_vpc_secgroup(mido_vpc_secgroup *vpcsecgroup);
int find_mido_vpc_secgroup(mido_config *mido, char *secgroupname, mido_vpc_secgroup **outvpcsecgroup);

int connect_mido_vpc_instance(mido_vpc_subnet *vpcsubnet, mido_vpc_instance *inst, midoname *vmhost);

int connect_mido_vpc_instance_elip(mido_config *mido, mido_core *midocore, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *inst);
int disconnect_mido_vpc_instance_elip(mido_vpc_instance *vpcinstance);

int free_mido_config(mido_config *mido);
int free_mido_core(mido_core *midocore);
int free_mido_vpc(mido_vpc *vpc);
int free_mido_vpc_subnet(mido_vpc_subnet *vpcsubnet);
int free_mido_vpc_instance(mido_vpc_instance *vpcinstance);
int free_mido_vpc_secgroup(mido_vpc_secgroup *vpcsecgroup);

void print_mido_vpc(mido_vpc *vpc);
void print_mido_vpc_subnet(mido_vpc_subnet *vpcsubnet);
void print_mido_vpc_instance(mido_vpc_instance *vpcinstance);
void print_mido_vpc_secgroup(mido_vpc_secgroup *vpcsecgroup);

int do_midonet_update(globalNetworkInfo *gni, mido_config *mido);
int do_midonet_teardown(mido_config *mido);


int do_metaproxy_setup(mido_config *mido);
int do_metaproxy_teardown(mido_config *mido);
int do_metaproxy_maintain(mido_config *mido, int mode);


int create_mido_meta_core(mido_config *mido);
int create_mido_meta_vpc_namespace(mido_config *mido, mido_vpc *vpc);
int create_mido_meta_subnet_veth(mido_config *mido, mido_vpc *vpc, char *name, char *subnet, char *slashnet, char **tapiface);

int delete_mido_meta_core(mido_config *mido);
int delete_mido_meta_vpc_namespace(mido_config *mido, mido_vpc *vpc);
int delete_mido_meta_subnet_veth(mido_config *mido, char *name);


#endif
