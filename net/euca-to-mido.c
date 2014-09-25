#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>
#include <dirent.h>
#include <errno.h>
#include <ctype.h>
#include <curl/curl.h>
#include <json/json.h>

#include <eucalyptus.h>
#include <misc.h>
#include <vnetwork.h>
#include <euca_string.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <ipt_handler.h>
#include <atomic_file.h>

#include "globalnetwork.h"
#include "midonet-api.h"
#include "euca-to-mido.h"

int do_midonet_populate(mido_config * mido)
{
    int i = 0, j = 0, k = 0, rc = 0, rtid = 0, ret = 0;
    char subnetname[16], vpcname[16], chainname[16];
    char instanceId[16], deviceId[16];
    char *iface = NULL, *devid = NULL;
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc *vpc = NULL;

    // mido discovery
    rc = discover_mido_resources(mido);
    if (rc) {
        LOGERROR("could not discover resources from midonet: check midonet health\n");
        return (1);
    }
    // always populate the core from that which was discovered
    rc = populate_mido_core(mido, mido->midocore);
    if (rc) {
        LOGERROR("could not populate midonet core (eucabr, eucart): check midonet health\n");
        return (1);
    }
    // create the core (only the things that aren't already there)
    rc = create_mido_core(mido, mido->midocore);
    if (rc) {
        LOGERROR("cannot setup midonet core router/bridge: check midonet health\n");
        return (1);
    }
    // pattern 
    // - find all VPC routers (and populate VPCs)
    // - for each VPC, find all subnets (and populate subnets)
    // - for each VPC, for each subnet, find all instances (and populate instances)

    // VPCs
    for (i = 0; i < mido->max_routers; i++) {
        LOGDEBUG("inspecting mido router '%s'\n", mido->routers[i].name);

        bzero(vpcname, 16);

        sscanf(mido->routers[i].name, "vr_%12s_%d", vpcname, &rtid);
        if (strlen(vpcname)) {
            mido->vpcs = realloc(mido->vpcs, sizeof(mido_vpc) * (mido->max_vpcs + 1));
            vpc = &(mido->vpcs[mido->max_vpcs]);
            bzero(vpc, sizeof(mido_vpc));
            mido->max_vpcs++;
            LOGINFO("discovered VPC installed in midonet: %s\n", vpcname);

            snprintf(vpc->name, sizeof(vpc->name), "%s", vpcname);
            set_router_id(mido, rtid);
            vpc->rtid = rtid;
            rc = populate_mido_vpc(mido, mido->midocore, vpc);
            if (rc) {
                LOGERROR("cannot populate midonet VPC '%s': check midonet health\n", vpc->name);
                ret = 1;
            }
        }
    }

    // SUBNETS
    for (i = 0; i < mido->max_bridges; i++) {
        LOGDEBUG("inspecting bridge '%s'\n", mido->bridges[i].name);

        bzero(vpcname, 16);
        bzero(subnetname, 16);

        sscanf(mido->bridges[i].name, "vb_%12s_%15s", vpcname, subnetname);
        if (strlen(vpcname) && strlen(subnetname)) {
            LOGINFO("discovered VPC subnet installed in midonet: %s/%s\n", vpcname, subnetname);
            find_mido_vpc(mido, vpcname, &vpc);
            if (vpc) {
                LOGDEBUG("found VPC matching discovered subnet: '%s'/'%s'\n", vpc->name, subnetname);
                vpc->subnets = realloc(vpc->subnets, sizeof(mido_vpc_subnet) * (vpc->max_subnets + 1));
                vpcsubnet = &(vpc->subnets[vpc->max_subnets]);
                vpc->max_subnets++;
                bzero(vpcsubnet, sizeof(mido_vpc_subnet));
                snprintf(vpcsubnet->name, 16, "%s", subnetname);
                rc = populate_mido_vpc_subnet(mido, vpc, vpcsubnet);
                if (rc) {
                    LOGERROR("cannot populate midonet VPC '%s' subnet '%s': check midonet health\n", vpc->name, vpcsubnet->name);
                    ret = 1;
                }
            }
        }
    }

    // SECGROUPS
    for (i = 0; i < mido->max_chains; i++) {
        LOGDEBUG("inspecting chain '%s'\n", mido->chains[i].name);
        bzero(chainname, 16);

        sscanf(mido->chains[i].name, "sg_%11s_ingress", chainname);
        if (strlen(chainname)) {
            LOGINFO("discovered VPC security group installed in midonet: %s\n", chainname);
            mido->vpcsecgroups = realloc(mido->vpcsecgroups, sizeof(mido_vpc_secgroup) * (mido->max_vpcsecgroups + 1));
            vpcsecgroup = &(mido->vpcsecgroups[mido->max_vpcsecgroups]);
            mido->max_vpcsecgroups++;
            bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));
            snprintf(vpcsecgroup->name, 16, "%s", chainname);
            rc = populate_mido_vpc_secgroup(mido, vpcsecgroup);
            if (rc) {
                LOGERROR("cannot populate mido SG '%s': check midonet health\n", vpcsecgroup->name);
                ret = 1;
            }
        }
    }

    // INSTANCES
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            for (k = 0; k < vpcsubnet->max_brports; k++) {

                bzero(instanceId, 16);
                bzero(deviceId, 16);

                rc = mido_getel_midoname(&(vpcsubnet->brports[k]), "interfaceName", &iface);
                rc = mido_getel_midoname(&(vpcsubnet->brports[k]), "deviceId", &devid);

                if ((iface && devid) && !strcmp(devid, vpcsubnet->midos[VPCBR].uuid)) {
                    sscanf(iface, "vn_%s", instanceId);
                    snprintf(deviceId, 16, "%s", devid);

                    LOGINFO("discovered VPC subnet instance interface: %s/%s/%s\n", vpc->name, vpcsubnet->name, instanceId);

                    vpcsubnet->instances = realloc(vpcsubnet->instances, sizeof(mido_vpc_instance) * (vpcsubnet->max_instances + 1));
                    vpcinstance = &(vpcsubnet->instances[vpcsubnet->max_instances]);
                    bzero(vpcinstance, sizeof(mido_vpc_instance));
                    vpcsubnet->max_instances++;
                    snprintf(vpcinstance->name, 16, "%s", instanceId);

                    rc = populate_mido_vpc_instance(mido, mido->midocore, vpcsubnet, vpcinstance);
                    if (rc) {
                        LOGERROR("could not populate instance: check mido health\n");
                        ret = 1;
                    }
                }
                EUCA_FREE(iface);
                EUCA_FREE(devid);
            }
        }
    }

    // END population phase

    return (ret);
}

int do_midonet_teardown(mido_config * mido)
{
    int ret = 0, rc = 0, i = 0;

    rc = do_midonet_populate(mido);
    if (rc) {
        LOGERROR("cannot populate prior to teardown: check midonet health\n");
        return (1);
    }

    for (i = 0; i < mido->max_vpcs; i++) {
        delete_mido_vpc(&(mido->vpcs[i]));
    }

    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        delete_mido_vpc_secgroup(&(mido->vpcsecgroups[i]));
    }

    delete_mido_core(mido->midocore);

    free_mido_config(mido);

    return (ret);
}

int do_midonet_update(globalNetworkInfo * gni, mido_config * mido)
{
    int i = 0, j = 0, k = 0, rc = 0;
    char subnet_buf[24], slashnet_buf[8], gw_buf[24];
    mido_vpc_secgroup *vpcsecgroup = NULL;
    mido_vpc_instance *vpcinstance = NULL;
    mido_vpc_subnet *vpcsubnet = NULL;
    mido_vpc *vpc = NULL;

    rc = do_midonet_populate(mido);
    if (rc) {
        LOGERROR("could not populate prior to update: see above log entries for details\n");
        return (1);
    }
    // now, go through GNI and create new VPCs
    LOGINFO("initializing VPCs (%d)\n", gni->max_vpcs);

    for (i = 0; i < gni->max_vpcs; i++) {
        gni_vpc *gnivpc = NULL;
        gni_vpcsubnet *gnivpcsubnet = NULL;
        mido_vpc *vpc = NULL;
        mido_vpc_subnet *vpcsubnet = NULL;

        gnivpc = &(gni->vpcs[i]);
        LOGINFO("initializing VPC '%s' with '%d' subnets\n", gnivpc->name, gnivpc->max_subnets);

        rc = find_mido_vpc(mido, gnivpc->name, &vpc);
        if (vpc) {
            LOGINFO("found gni VPC '%s' already extant\n", gnivpc->name);
        } else {
            LOGINFO("creating new VPC '%s'\n", gnivpc->name);
            mido->vpcs = realloc(mido->vpcs, sizeof(mido_vpc) * (mido->max_vpcs + 1));
            vpc = &(mido->vpcs[mido->max_vpcs]);
            bzero(vpc, sizeof(mido_vpc));
            mido->max_vpcs++;

            snprintf(vpc->name, 16, "%s", gnivpc->name);
            get_next_router_id(mido, &(vpc->rtid));
        }
        rc = create_mido_vpc(mido, mido->midocore, vpc);
        if (rc) {
            LOGERROR("failed to create VPC '%s': check midonet health\n", gnivpc->name);
        }

        vpc->gnipresent = 1;

        // do subnets
        for (j = 0; j < gnivpc->max_subnets; j++) {
            gnivpcsubnet = &(gnivpc->subnets[j]);

            rc = find_mido_vpc_subnet(vpc, gnivpcsubnet->name, &vpcsubnet);
            if (vpcsubnet) {
                LOGINFO("found gni VPC '%s' subnet '%s' already extant\n", vpc->name, vpcsubnet->name);
            } else {
                LOGINFO("creating new VPC '%s' subnet '%s'\n", vpc->name, gnivpc->subnets[j].name);

                vpc->subnets = realloc(vpc->subnets, sizeof(mido_vpc_subnet) * (vpc->max_subnets + 1));
                vpcsubnet = &(vpc->subnets[vpc->max_subnets]);
                vpc->max_subnets++;
                bzero(vpcsubnet, sizeof(mido_vpc_subnet));
                snprintf(vpcsubnet->name, 16, "%s", gnivpc->subnets[j].name);
            }

            subnet_buf[0] = slashnet_buf[0] = gw_buf[0] = '\0';
            cidr_split(gnivpcsubnet->cidr, subnet_buf, slashnet_buf, gw_buf);

            rc = create_mido_vpc_subnet(mido, vpc, vpcsubnet, subnet_buf, slashnet_buf, gw_buf);
            if (rc) {
                LOGERROR("failed to create VPC '%s' subnet '%s': check midonet health\n", gnivpc->name, gnivpc->subnets[j].name);
            }
            vpcsubnet->gnipresent = 1;
        }
    }

    // should do sec. group populate/create loop

    // now do instance interface mappings
    for (i = 0; i < gni->max_instances; i++) {
        gni_instance *gniinstance = &(gni->instances[i]);

        LOGDEBUG("inspecting gni instance '%s'\n", gni->instances[i].name);

        // check that we can do something about this instance: 
        if (gniinstance->vpc && strlen(gniinstance->vpc) && gniinstance->nodehostname && strlen(gniinstance->nodehostname)) {
            rc = find_mido_vpc(mido, gniinstance->vpc, &vpc);
            if (vpc) {
                rc = find_mido_vpc_subnet(vpc, gniinstance->subnet, &vpcsubnet);
                if (vpcsubnet) {
                    rc = find_mido_vpc_instance(vpcsubnet, gniinstance->name, &vpcinstance);
                    if (vpcinstance) {
                        LOGDEBUG("found instance '%s' is in extant vpc '%s' subnet '%s' '%d'\n", vpcinstance->name, vpc->name, vpcsubnet->name, vpcinstance->midos[VMHOST].init);
                        vpcinstance->gniInst = gniinstance;
                    } else {
                        // create the instance
                        vpcsubnet->instances = realloc(vpcsubnet->instances, sizeof(mido_vpc_instance) * (vpcsubnet->max_instances + 1));
                        vpcinstance = &(vpcsubnet->instances[vpcsubnet->max_instances]);
                        bzero(vpcinstance, sizeof(mido_vpc_instance));
                        vpcsubnet->max_instances++;
                        snprintf(vpcinstance->name, 16, "%s", gniinstance->name);
                        vpcinstance->gniInst = gniinstance;
                    }

                    LOGDEBUG("ABOUT TO CREATE INSTANCE '%s' ON HOST '%s'\n", vpcinstance->name, gniinstance->nodehostname);
                    rc = create_mido_vpc_instance(mido, vpcinstance, gniinstance->nodehostname);
                    if (rc) {
                        // TODO
                    }

                    vpcinstance->gnipresent = 1;

                    // do instance<->port connection and elip
                    if (vpcinstance->midos[VMHOST].init) {
                        LOGINFO("connecting gni host '%s' with midonet host '%s' interface for instance '%s'\n", gniinstance->nodehostname, vpcinstance->midos[VMHOST].name,
                                gniinstance->name);

                        rc = connect_mido_vpc_instance(vpcsubnet, vpcinstance, &(vpcinstance->midos[VMHOST]));
                        if (rc) {
                            LOGERROR("cannot connect instance to midonet: check midonet health\n");
                        } else {
                            char *strptra = NULL;
                            strptra = hex2dot(gniinstance->publicIp);
                            LOGINFO("setting up floating IP '%s' for instance '%s'\n", strptra, vpcinstance->name);
                            EUCA_FREE(strptra);
                            rc = connect_mido_vpc_instance_elip(mido, mido->midocore, vpc, vpcinstance);
                            if (rc) {
                                LOGERROR("cannot setup midonet floating IP <-> instance mapping: check midonet health\n");
                            }
                        }
                    } else {
                        LOGERROR("could not find midonet host for instance '%s': check midonet/euca node/host mappings\n", vpcinstance->name);
                    }

                    // do sec. group rule application for instance
                    {
                        gni_secgroup *gnisecgroups = NULL;
                        int max_gnisecgroups, rulepos = 1;
                        char tmp_name1[32], tmp_name2[32], tmp_name3[32], tmp_name4[32];

                        // for egress
                        rulepos = 1;

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "accept", "matchReturnFlow", "true", NULL);
                        if (rc) {

                        } else {
                            rulepos++;
                        }

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "accept", "ipAddrGroupSrc",
                                              vpcinstance->midos[ELIP_POST_IPADDRGROUP].uuid, "matchForwardFlow", "true", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_PRECHAIN]), NULL, "position", tmp_name3, "type", "drop", "invDlType", "true", "dlType", "2054", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        // for ingress
                        rulepos = 1;

                        snprintf(tmp_name3, 32, "%d", rulepos);
                        rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "position", tmp_name3, "type", "accept", "matchReturnFlow", "true", NULL);
                        if (rc) {
                        } else {
                            rulepos++;
                        }

                        rc = gni_instance_get_secgroups(gni, gniinstance, NULL, 0, NULL, 0, &gnisecgroups, &max_gnisecgroups);
                        for (j = 0; j < max_gnisecgroups; j++) {
                            gni_secgroup *gnisecgroup = &(gnisecgroups[j]);
                            char *tmpstr = NULL;

                            // create the SG
                            rc = find_mido_vpc_secgroup(mido, gnisecgroup->name, &vpcsecgroup);
                            if (vpcsecgroup) {
                                // found one
                                vpcsecgroup->gniSecgroup = gnisecgroup;
                            } else {
                                mido->vpcsecgroups = realloc(mido->vpcsecgroups, sizeof(mido_vpc_secgroup) * (mido->max_vpcsecgroups + 1));
                                vpcsecgroup = &(mido->vpcsecgroups[mido->max_vpcsecgroups]);
                                bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));
                                mido->max_vpcsecgroups++;
                                snprintf(vpcsecgroup->name, 16, "%s", gnisecgroup->name);
                                vpcsecgroup->gniSecgroup = gnisecgroup;
                            }

                            LOGDEBUG("ABOUT TO CREATE SG '%s'\n", vpcsecgroup->name);
                            rc = create_mido_vpc_secgroup(mido, vpcsecgroup);
                            if (rc) {
                                // TODO
                            }

                            tmpstr = hex2dot(gniinstance->privateIp);
                            rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPRIV]), tmpstr, NULL);
                            rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGALL]), tmpstr, NULL);
                            EUCA_FREE(tmpstr);

                            tmpstr = hex2dot(gniinstance->publicIp);
                            rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGPUB]), tmpstr, NULL);
                            rc = mido_create_ipaddrgroup_ip(&(vpcsecgroup->midos[VPCSG_IAGALL]), tmpstr, NULL);
                            EUCA_FREE(tmpstr);

                            vpcsecgroup->gnipresent = 1;

                            // TODO make this better (not entire clear/reset each time
                            {
                                midoname *rules = NULL;
                                int max_rules = 0, r = 0;
                                rc = mido_get_rules(&(vpcsecgroup->midos[VPCSG_INGRESS]), &rules, &max_rules);
                                if (max_rules != gnisecgroup->max_ingress_rules) {
                                    for (r = 0; r < max_rules; r++) {
                                        mido_delete_rule(&(rules[r]));
                                    }
                                }
                                mido_free_midoname_list(rules, max_rules);
                                EUCA_FREE(rules);
                            }

                            snprintf(tmp_name3, 32, "%d", rulepos);
                            rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "position", tmp_name3, "type", "jump", "jumpChainId",
                                                  vpcsecgroup->midos[VPCSG_INGRESS].uuid, NULL);
                            if (rc) {
                            } else {
                                rulepos++;
                            }

                            snprintf(tmp_name3, 32, "%d", rulepos);
                            rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "type", "drop", "invDlType", "true", "position", tmp_name3, "dlType", "2054", NULL);
                            if (rc) {
                            } else {
                                rulepos++;
                            }

                            rulepos = 1;
                            for (k = 0; k < gnisecgroup->max_ingress_rules; k++) {
                                // TODO other protos?
                                // TODO add ingress from other SGs (set up IAGs and such)

                                snprintf(tmp_name4, 32, "%d", gnisecgroup->ingress_rules[k].protocol);
                                if (strlen(gnisecgroup->ingress_rules[k].groupId)) {
                                    // other group
                                    midoname *midos = NULL;
                                    int max_midos = 0, r;
                                    char name[32], *mname = NULL;
                                    int found = 0;
                                    rc = mido_get_ipaddrgroups("euca_tenant_1", &midos, &max_midos);
                                    for (r = 0; r < max_midos && !found; r++) {
                                        snprintf(name, 32, "sg_%11s_all", vpcsecgroup->name);
                                        rc = mido_getel_midoname(&(midos[r]), "name", &mname);
                                        if (mname && !strcmp(name, mname)) {
                                            LOGTRACE("FOUND: %s/%s\n", mname, midos[r].uuid);
                                            snprintf(tmp_name3, 32, "%d", rulepos);
                                            rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "ipAddrGroupSrc",
                                                                  midos[r].uuid, NULL);
                                            if (rc) {
                                            } else {
                                                rulepos++;
                                            }
                                            found++;
                                        }
                                        EUCA_FREE(mname);
                                    }
                                    mido_free_midoname_list(midos, max_midos);
                                    EUCA_FREE(midos)
                                } else if (gnisecgroup->ingress_rules[k].protocol == 6 || gnisecgroup->ingress_rules[k].protocol == 17) {
                                    // TCP/UDP

                                    snprintf(tmp_name1, 32, "%d", gnisecgroup->ingress_rules[k].fromPort);
                                    snprintf(tmp_name2, 32, "%d", gnisecgroup->ingress_rules[k].toPort);

                                    snprintf(tmp_name3, 32, "%d", rulepos);
                                    rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "tpDst", "jsonjson", "tpDst:start",
                                                          tmp_name1, "tpDst:end", tmp_name2, "tpDst:END", "END", "nwProto", tmp_name4, NULL);
                                    if (rc) {
                                    } else {
                                        rulepos++;
                                    }

                                } else if (gnisecgroup->ingress_rules[k].protocol == 1) {
                                    // ICMP

                                    snprintf(tmp_name3, 32, "%d", rulepos);

                                    if (gnisecgroup->ingress_rules[k].icmpCode >= 0) {
                                        snprintf(tmp_name1, 32, "%d", gnisecgroup->ingress_rules[k].icmpCode);
                                        //          rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "position", tmp_name3, "type", "accept", "tpDst", "jsonjson", "tpDst:start", tmp_name1, "tpDst:end", tmp_name1, "tpDst:END", "END", "nwProto", tmp_name4, NULL);
                                        rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "tpDst", "jsonjson",
                                                              "tpDst:start", tmp_name1, "tpDst:end", tmp_name1, "tpDst:END", "END", "nwProto", tmp_name4, NULL);
                                        if (rc) {
                                        } else {
                                            rulepos++;
                                        }
                                    } else {
                                        // its the all rule
                                        //          rc = mido_create_rule(&(vpcinstance->midos[INST_POSTCHAIN]), NULL, "position", tmp_name3, "type", "accept", "nwProto", tmp_name4, NULL);
                                        rc = mido_create_rule(&(vpcsecgroup->midos[VPCSG_INGRESS]), NULL, "position", tmp_name3, "type", "accept", "nwProto", tmp_name4, NULL);
                                        if (rc) {
                                        } else {
                                            rulepos++;
                                        }
                                    }

                                }
                            }

                        }
                        EUCA_FREE(gnisecgroups);

                    }

                    // done
                }
            }
        }
    }

    // temporary print
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        print_mido_vpc(vpc);
        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            print_mido_vpc_subnet(vpcsubnet);
            for (k = 0; k < vpcsubnet->max_instances; k++) {
                vpcinstance = &(vpcsubnet->instances[k]);
                print_mido_vpc_instance(vpcinstance);
            }
        }
    }

    // check and clear VPCs/subnets/instances
    // TODO clear sec. group chains
    LOGDEBUG("TOTAL VPCS: %d\n", mido->max_vpcs);
    for (i = 0; i < mido->max_vpcs; i++) {
        vpc = &(mido->vpcs[i]);
        LOGDEBUG("CHECK: %s/%d\n", vpc->name, vpc->gnipresent);

        for (j = 0; j < vpc->max_subnets; j++) {
            vpcsubnet = &(vpc->subnets[j]);
            LOGDEBUG("\tCHECK: %s/%d\n", vpcsubnet->name, vpcsubnet->gnipresent);

            for (k = 0; k < vpcsubnet->max_instances; k++) {
                vpcinstance = &(vpcsubnet->instances[k]);
                LOGDEBUG("\t\tCHECK: %s/%d\n", vpcinstance->name, vpcinstance->gnipresent);
                if (!vpc->gnipresent || !vpcsubnet->gnipresent || !vpcinstance->gnipresent) {
                    rc = delete_mido_vpc_instance(vpcinstance);
                }
            }
            if (!vpc->gnipresent || !vpcsubnet->gnipresent) {
                LOGINFO("tearing down VPC '%s' subnet '%s'\n", vpc->name, vpcsubnet->name);
                rc = delete_mido_vpc_subnet(vpcsubnet);
            }
        }
        if (!vpc->gnipresent) {
            LOGINFO("tearing down VPC '%s'\n", vpc->name);
            rc = delete_mido_vpc(vpc);
        }

    }

    if (0) {
        // if debugging
        for (i = 0; i < mido->max_vpcs; i++) {
            delete_mido_vpc(&(mido->vpcs[i]));
        }
        delete_mido_core(mido->midocore);

        gni_free(gni);
        free_mido_config(mido);

        exit(0);
    }
    return (0);
}

int get_next_router_id(mido_config * mido, int *nextid)
{
    int i;
    for (i = 2; i < 4096; i++) {
        if (!mido->router_ids[i]) {
            mido->router_ids[i] = 1;
            *nextid = i;
            return (0);
        }
    }
    return (1);
}

int set_router_id(mido_config * mido, int id)
{
    if (id < 4096) {
        mido->router_ids[id] = 1;
        return (0);
    }
    return (1);
}

void print_mido_vpc(mido_vpc * vpc)
{
    LOGDEBUG("PRINT VPC: name=%s max_rtports=%d max_subnets=%d gnipresent=%d\n", vpc->name, vpc->max_rtports, vpc->max_subnets, vpc->gnipresent);
    mido_print_midoname(&(vpc->midos[VPCRT]));
    mido_print_midoname(&(vpc->midos[EUCABR_DOWNLINK]));
    mido_print_midoname(&(vpc->midos[VPCRT_UPLINK]));
    mido_print_midoname(&(vpc->midos[VPCRT_PRECHAIN]));
    mido_print_midoname(&(vpc->midos[VPCRT_POSTCHAIN]));

}

void print_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet)
{
    LOGDEBUG("PRINT VPCSUBNET: name=%s vpcname=%s max_brports=%d max_dhcphosts=%d max_instances=%d gnipresent=%d\n", vpcsubnet->name, vpcsubnet->vpcname, vpcsubnet->max_brports,
             vpcsubnet->max_dhcphosts, vpcsubnet->max_instances, vpcsubnet->gnipresent);
    mido_print_midoname(&(vpcsubnet->midos[VPCBR]));
    mido_print_midoname(&(vpcsubnet->midos[VPCBR_RTPORT]));
    mido_print_midoname(&(vpcsubnet->midos[VPCRT_BRPORT]));
    mido_print_midoname(&(vpcsubnet->midos[VPCBR_DHCP]));
}

void print_mido_vpc_instance(mido_vpc_instance * vpcinstance)
{
    LOGDEBUG("PRINT VPCINSTANCE: name=%s gnipresent=%d\n", vpcinstance->name, vpcinstance->gnipresent);
    mido_print_midoname(&(vpcinstance->midos[VPCBR_VMPORT]));
    mido_print_midoname(&(vpcinstance->midos[VPCBR_DHCPHOST]));
    mido_print_midoname(&(vpcinstance->midos[VMHOST]));
}

int find_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, char *instancename, mido_vpc_instance ** outvpcinstance)
{
    int i;
    if (!vpcsubnet || !instancename || !outvpcinstance) {
        return (1);
    }

    *outvpcinstance = NULL;
    for (i = 0; i < vpcsubnet->max_instances; i++) {
        if (!strcmp(instancename, vpcsubnet->instances[i].name)) {
            *outvpcinstance = &(vpcsubnet->instances[i]);
            return (0);
        }
    }

    return (1);
}

int find_mido_vpc_subnet(mido_vpc * vpc, char *subnetname, mido_vpc_subnet ** outvpcsubnet)
{
    int i;

    if (!vpc || !subnetname || !outvpcsubnet) {
        return (1);
    }

    *outvpcsubnet = NULL;

    for (i = 0; i < vpc->max_subnets; i++) {
        if (!strcmp(subnetname, vpc->subnets[i].name)) {
            *outvpcsubnet = &(vpc->subnets[i]);
            return (0);
        }
    }
    return (1);
}

int find_mido_vpc(mido_config * mido, char *vpcname, mido_vpc ** outvpc)
{
    int i;

    if (!mido || !outvpc || !vpcname) {
        return (1);
    }

    *outvpc = NULL;
    for (i = 0; i < mido->max_vpcs; i++) {
        if (!strcmp(vpcname, mido->vpcs[i].name)) {
            *outvpc = &(mido->vpcs[i]);
            return (0);
        }
    }

    return (1);
}

int populate_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0, found = 0, foundcount = 0, rc = 0, i = 0;
    midoname *midos = NULL;
    int max_midos = 0;
    char name[64], *tmpstr = NULL;

    if (!mido || !vpcsecgroup) {
        return (1);
    }

    foundcount = found = 0;
    rc = mido_get_chains("euca_tenant_1", &midos, &max_midos);
    for (i = 0; i < max_midos && !found; i++) {
        snprintf(name, 64, "sg_%11s_ingress", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_INGRESS]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        snprintf(name, 64, "sg_%11s_egress", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_EGRESS]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        if (foundcount >= 2) {
            found++;
        }
    }
    mido_free_midoname_list(midos, max_midos);
    EUCA_FREE(midos);

    foundcount = found = 0;
    rc = mido_get_ipaddrgroups("euca_tenant_1", &midos, &max_midos);
    for (i = 0; i < max_midos && !found; i++) {
        snprintf(name, 64, "sg_%11s_priv", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_IAGPRIV]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        snprintf(name, 64, "sg_%11s_pub", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_IAGPUB]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        snprintf(name, 64, "sg_%11s_all", vpcsecgroup->name);
        tmpstr = NULL;
        rc = mido_getel_midoname(&(midos[i]), "name", &tmpstr);
        if (tmpstr && !strcmp(name, tmpstr)) {
            // found
            mido_copy_midoname(&(vpcsecgroup->midos[VPCSG_IAGALL]), &(midos[i]));
            foundcount++;
        }
        EUCA_FREE(tmpstr);

        if (foundcount >= 3) {
            found++;
        }
    }
    mido_free_midoname_list(midos, max_midos);
    EUCA_FREE(midos);

    return (ret);
}

int create_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0, rc = 0;
    char name[32];

    if (!mido || !vpcsecgroup) {
        return (1);
    }

    snprintf(name, 32, "sg_%11s_ingress", vpcsecgroup->name);
    rc = mido_create_chain("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_INGRESS]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_%11s_egress", vpcsecgroup->name);
    rc = mido_create_chain("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_EGRESS]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_%11s_priv", vpcsecgroup->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_IAGPRIV]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_%11s_pub", vpcsecgroup->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_IAGPUB]));
    if (rc) {
        // TODO
        ret = 1;
    }

    snprintf(name, 32, "sg_%11s_all", vpcsecgroup->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", name, &(vpcsecgroup->midos[VPCSG_IAGALL]));
    if (rc) {
        // TODO
        ret = 1;
    }

    return (ret);
}

int delete_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0, rc = 0;

    if (!vpcsecgroup) {
        return (1);
    }

    rc = mido_delete_chain(&(vpcsecgroup->midos[VPCSG_INGRESS]));
    rc = mido_delete_chain(&(vpcsecgroup->midos[VPCSG_EGRESS]));

    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGPRIV]));
    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGPUB]));
    rc = mido_delete_ipaddrgroup(&(vpcsecgroup->midos[VPCSG_IAGALL]));

    free_mido_vpc_secgroup(vpcsecgroup);

    bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));

    return (ret);
}

int find_mido_vpc_secgroup(mido_config * mido, char *secgroupname, mido_vpc_secgroup ** outvpcsecgroup)
{
    int i = 0;

    if (!mido || !secgroupname || !outvpcsecgroup) {
        return (1);
    }

    *outvpcsecgroup = NULL;
    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        if (mido->vpcsecgroups[i].name && !strcmp(mido->vpcsecgroups[i].name, secgroupname)) {
            *outvpcsecgroup = &(mido->vpcsecgroups[i]);
            return (0);
        }
    }

    return (1);
}

int free_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup)
{
    int ret = 0;

    if (!vpcsecgroup)
        return (0);

    mido_free_midoname_list(vpcsecgroup->midos, VPCSG_END);
    bzero(vpcsecgroup, sizeof(mido_vpc_secgroup));

    return (ret);
}

int populate_mido_vpc_instance(mido_config * mido, mido_core * midocore, mido_vpc_subnet * vpcsubnet, mido_vpc_instance * vpcinstance)
{
    int ret = 0, rc = 0, found = 0, i = 0, founda = 0, j = 0;
    char *tmpstr = NULL, fstr[64], tmp_name[32];

    if (vpcsubnet->midos[VPCBR].init) {
        for (i = 0; i < vpcsubnet->max_brports && !found; i++) {
            LOGDEBUG("VPC BRPORTS: %s/%s\n", SP(vpcsubnet->brports[i].name), vpcsubnet->brports[i].uuid);

            tmpstr = NULL;
            rc = mido_getel_midoname(&(vpcsubnet->brports[i]), "hostId", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr)) {
                founda = 0;
                for (j = 0; j < mido->max_hosts && !founda; j++) {
                    if (!strcmp(tmpstr, mido->hosts[j].uuid)) {
                        LOGDEBUG("matched host %s with instance %s\n", mido->hosts[j].name, vpcinstance->name);
                        mido_copy_midoname(&(vpcinstance->midos[VMHOST]), &(mido->hosts[j]));
                        founda = 1;
                    }
                }
            }
            EUCA_FREE(tmpstr);

            tmpstr = NULL;
            rc = mido_getel_midoname(&(vpcsubnet->brports[i]), "interfaceName", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr)) {
                LOGTRACE("PORT INTERFACE NAME: %s\n", SP(tmpstr));
                LOGTRACE("TRYING TO MATCH INSTANCE %s WITH PORT %s\n", vpcinstance->name, SP(tmpstr));
                if (strstr(tmpstr, vpcinstance->name)) {
                    LOGDEBUG("matched port %s with instance %s\n", vpcsubnet->brports[i].uuid, vpcinstance->name);
                    mido_copy_midoname(&(vpcinstance->midos[VPCBR_VMPORT]), &(vpcsubnet->brports[i]));
                }
            }
            EUCA_FREE(tmpstr);

            if (vpcinstance->midos[VPCBR_VMPORT].init && vpcinstance->midos[VMHOST].init) {
                found = 1;
            }
        }
    }

    if (vpcsubnet->midos[VPCBR_DHCP].init) {
        found = 0;
        for (i = 0; i < vpcsubnet->max_dhcphosts && !found; i++) {
            tmpstr = NULL;
            rc = mido_getel_midoname(&(vpcsubnet->dhcphosts[i]), "name", &tmpstr);
            if (!rc && tmpstr && strlen(tmpstr)) {
                LOGTRACE("HELLOFOO: %d\n", vpcinstance->midos[VPCBR_DHCPHOST].init);
                mido_copy_midoname(&(vpcinstance->midos[VPCBR_DHCPHOST]), &(vpcsubnet->dhcphosts[i]));
                found = 1;
            }
            EUCA_FREE(tmpstr);
        }
    }

    found = 0;
    for (i = 0; i < mido->max_ipaddrgroups && !found; i++) {
        snprintf(fstr, 64, "elip_pre_%s", vpcinstance->name);
        if (!strcmp(fstr, mido->ipaddrgroups[i].name)) {
            mido_copy_midoname(&vpcinstance->midos[ELIP_PRE_IPADDRGROUP], &(mido->ipaddrgroups[i]));
        }

        snprintf(fstr, 64, "elip_post_%s", vpcinstance->name);
        if (!strcmp(fstr, mido->ipaddrgroups[i].name)) {
            mido_copy_midoname(&vpcinstance->midos[ELIP_POST_IPADDRGROUP], &(mido->ipaddrgroups[i]));
        }
        if (vpcinstance->midos[ELIP_PRE_IPADDRGROUP].init && vpcinstance->midos[ELIP_POST_IPADDRGROUP].init) {
            found = 1;
        }
    }

    if (vpcinstance->midos[ELIP_PRE_IPADDRGROUP].init && midocore->midos[EUCART].init) {
        midoname *routes = NULL, *ips = NULL;
        int max_routes = 0, max_ips = 0;
        char *ip = NULL, *rdst = NULL;

        found = 0;

        rc = mido_get_ipaddrgroup_ips(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]), &ips, &max_ips);
        for (i = 0; i < max_ips && !found; i++) {
            rc = mido_getel_midoname(&(ips[i]), "addr", &ip);
            if (ip) {
                rc = mido_get_routes(&(midocore->midos[EUCART]), &routes, &max_routes);
                for (j = 0; j < max_routes && !found; j++) {
                    rc = mido_getel_midoname(&(routes[j]), "dstNetworkAddr", &rdst);
                    if (!strcmp(ip, rdst)) {
                        // found it!
                        mido_copy_midoname(&(vpcinstance->midos[ELIP_ROUTE]), &(routes[j]));
                        found = 1;
                    }
                    EUCA_FREE(rdst);
                }
                mido_free_midoname_list(routes, max_routes);
                EUCA_FREE(routes);
                EUCA_FREE(ip);
            }
        }
        mido_free_midoname_list(ips, max_ips);
        EUCA_FREE(ips);
    }

    for (i = 0; i < mido->max_chains; i++) {
        snprintf(tmp_name, 32, "ic_%s_prechain", vpcinstance->name);
        if (!strcmp(mido->chains[i].name, tmp_name)) {
            mido_copy_midoname(&(vpcinstance->midos[INST_PRECHAIN]), &(mido->chains[i]));
        }
        snprintf(tmp_name, 32, "ic_%s_postchain", vpcinstance->name);
        if (!strcmp(mido->chains[i].name, tmp_name)) {
            mido_copy_midoname(&(vpcinstance->midos[INST_POSTCHAIN]), &(mido->chains[i]));
        }
    }

    {
        LOGDEBUG("AFTER POPULATE: VPCBR_VMPORT: %d VPCBR_DHCPHOST: %d\n", vpcinstance->midos[VPCBR_VMPORT].init, vpcinstance->midos[VPCBR_DHCPHOST].init);
    }
    return (ret);
}

int create_mido_vpc_instance(mido_config * mido, mido_vpc_instance * vpcinstance, char *nodehostname)
{
    int ret = 0, found = 0, i = 0, rc = 0;
    char aigname[64], tmp_name[32];

    // find the interface mapping
    found = 0;
    for (i = 0; i < mido->max_hosts && !found; i++) {
        if (strstr(mido->hosts[i].name, nodehostname)) {
            mido_copy_midoname(&(vpcinstance->midos[VMHOST]), &(mido->hosts[i]));
            found = 1;
        }
    }

    // set up elip ipaddrgroups
    snprintf(aigname, 64, "elip_pre_%s", vpcinstance->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", aigname, &(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    snprintf(aigname, 64, "elip_post_%s", vpcinstance->name);
    rc = mido_create_ipaddrgroup("euca_tenant_1", aigname, &(vpcinstance->midos[ELIP_POST_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    snprintf(tmp_name, 32, "ic_%s_prechain", vpcinstance->name);
    rc = mido_create_chain("euca_tenant_1", tmp_name, &(vpcinstance->midos[INST_PRECHAIN]));
    if (rc) {
        ret = 1;
    }
    snprintf(tmp_name, 32, "ic_%s_postchain", vpcinstance->name);
    rc = mido_create_chain("euca_tenant_1", tmp_name, &(vpcinstance->midos[INST_POSTCHAIN]));
    if (rc) {
        ret = 1;
    }

    return (ret);
}

int delete_mido_vpc_instance(mido_vpc_instance * vpcinstance)
{
    int ret = 0, rc = 0;

    rc = disconnect_mido_vpc_instance_elip(vpcinstance);
    if (rc) {
        ret = 1;
    }
    // unlink port, delete port, delete dhcp entry
    rc = mido_unlink_host_port(&(vpcinstance->midos[VMHOST]), &(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_port(&(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_dhcphost(&(vpcinstance->midos[VPCBR_DHCPHOST]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_ipaddrgroup(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_ipaddrgroup(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_chain(&(vpcinstance->midos[INST_PRECHAIN]));
    if (rc) {
        ret = 1;
    }

    rc = mido_delete_chain(&(vpcinstance->midos[INST_POSTCHAIN]));
    if (rc) {
        ret = 1;
    }

    free_mido_vpc_instance(vpcinstance);

    bzero(vpcinstance, sizeof(mido_vpc_instance));

    return (ret);
}

int initialize_mido(mido_config * mido, char *ext_rthostname, char *ext_rtaddr, char *ext_rtiface, char *int_rtnetwork, char *int_rtslashnet)
{
    int ret = 0;

    if (!mido || !ext_rthostname || !ext_rtaddr || !ext_rtiface || !int_rtnetwork || !int_rtslashnet || !strlen(ext_rthostname) || !strlen(ext_rtaddr) || !strlen(ext_rtiface))
        return (1);

    bzero(mido, sizeof(mido_config));

    mido->ext_rthostname = strdup(ext_rthostname);
    mido->ext_rtaddr = strdup(ext_rtaddr);
    mido->ext_rtiface = strdup(ext_rtiface);
    mido->int_rtnw = dot2hex(int_rtnetwork);    // strdup(int_rtnetwork);
    mido->int_rtsn = atoi(int_rtslashnet);
    mido->int_rtaddr = mido->int_rtnw + 1;
    mido->midocore = calloc(1, sizeof(mido_core));
    LOGDEBUG("mido initialized: ext_rthostname=%s ext_rtaddr=%s\n", SP(mido->ext_rthostname), SP(mido->ext_rtaddr));

    return (ret);
}

int discover_mido_resources(mido_config * mido)
{
    int rc = 0, ret = 0, i = 0, max_ports = 0, j = 0, count = 0;
    midoname *ports = NULL;

    rc = mido_get_routers("euca_tenant_1", &(mido->routers), &(mido->max_routers));
    if (rc) {
        LOGWARN("no routers in midonet\n");
    }

    count = 0;
    for (i = 0; i < mido->max_routers; i++) {
        //      mido_get_ports(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->brports), &(vpcsubnet->max_brports));
        rc = mido_get_ports(&(mido->routers[i]), &ports, &max_ports);
        if (!rc && max_ports) {
            mido->rtports = realloc(mido->rtports, sizeof(midoname) * (mido->max_rtports + max_ports));
            for (j = 0; j < max_ports; j++) {
                bzero(&(mido->rtports[count]), sizeof(midoname));
                mido_copy_midoname(&(mido->rtports[count]), &(ports[j]));
                count++;
            }
            mido->max_rtports += max_ports;
        }

        if (ports && max_ports > 0) {
            mido_free_midoname_list(ports, max_ports);
            EUCA_FREE(ports);
        }

    }

    ports = NULL;
    max_ports = 0;

    rc = mido_get_bridges("euca_tenant_1", &(mido->bridges), &(mido->max_bridges));
    if (rc) {
        LOGWARN("no bridges in midonet\n");
    }
    count = 0;
    for (i = 0; i < mido->max_bridges; i++) {
        //      mido_get_ports(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->brports), &(vpcsubnet->max_brports));
        rc = mido_get_ports(&(mido->bridges[i]), &ports, &max_ports);
        if (!rc && max_ports) {
            mido->brports = realloc(mido->brports, sizeof(midoname) * (mido->max_brports + max_ports));
            for (j = 0; j < max_ports; j++) {
                bzero(&(mido->brports[count]), sizeof(midoname));
                mido_copy_midoname(&(mido->brports[count]), &(ports[j]));
                count++;
            }
            mido->max_brports += max_ports;
        }
        if (ports && max_ports > 0) {
            mido_free_midoname_list(ports, max_ports);
            EUCA_FREE(ports);
        }
    }

    rc = mido_get_chains("euca_tenant_1", &(mido->chains), &(mido->max_chains));
    if (rc) {
        LOGWARN("no chains in midonet\n");
    }

    /*
       for (i=0; i<mido->max_chains; i++) {
       LOGTRACE("MEHMEH: %s/%s\n", mido->chains[i].name, mido->chains[i].uuid);
       }
     */

    rc = mido_get_hosts(&(mido->hosts), &(mido->max_hosts));
    if (rc) {
        LOGERROR("cannot get hosts from midonet: check midonet health\n");
        return (1);
    }
    /*
       for (i=0; i<mido->max_hosts; i++) {
       printf("%s/%s\n", mido->hosts[i].name, mido->hosts[i].uuid);
       }
     */

    rc = mido_get_ipaddrgroups("euca_tenant_1", &(mido->ipaddrgroups), &(mido->max_ipaddrgroups));
    if (rc) {
        LOGWARN("no ip address groups in midonet\n");
    }

    /*
       for (i=0; i<mido->max_ipaddrgroups; i++) {
       LOGINFO("hello - %s/%s\n", mido->ipaddrgroups[i].name, mido->ipaddrgroups[i].uuid);
       }
     */

    return (ret);
}

int populate_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet)
{
    int rc = 0, ret = 0, i = 0, j = 0;
    char name[64];
    midoname *dhcps = NULL;
    int max_dhcps = 0;
    char *tmpstr = NULL;

    snprintf(name, 64, "vb_%s_%s", vpc->name, vpcsubnet->name);
    for (i = 0; i < mido->max_bridges; i++) {
        if (!strcmp(mido->bridges[i].name, name)) {
            mido_copy_midoname(&(vpcsubnet->midos[VPCBR]), &(mido->bridges[i]));
            mido_get_ports(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->brports), &(vpcsubnet->max_brports));
        }
    }

    if (vpcsubnet->midos[VPCBR].init) {
        mido_get_dhcps(&(vpcsubnet->midos[VPCBR]), &dhcps, &max_dhcps);
        if (max_dhcps) {
            LOGDEBUG("VPC DHCP SERVERS: %s/%s\n", dhcps[0].uuid, dhcps[0].jsonbuf);
            mido_copy_midoname(&(vpcsubnet->midos[VPCBR_DHCP]), &(dhcps[0]));
        }
    }
    mido_free_midoname_list(dhcps, max_dhcps);
    EUCA_FREE(dhcps);

    if (vpcsubnet->midos[VPCBR_DHCP].init) {
        rc = mido_get_dhcphosts(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->midos[VPCBR_DHCP]), &(vpcsubnet->dhcphosts), &(vpcsubnet->max_dhcphosts));
        if (rc) {
        }
    }

    if (vpcsubnet->midos[VPCBR].init) {
        for (i = 0; i < vpcsubnet->max_brports; i++) {
            LOGDEBUG("VPC BRPORTS: %s/%s\n", SP(vpcsubnet->brports[i].name), vpcsubnet->brports[i].uuid);

            for (j = 0; j < vpc->max_rtports; j++) {
                tmpstr = NULL;
                rc = mido_getel_midoname(&(vpc->rtports[j]), "peerId", &tmpstr);
                if (!rc && tmpstr && vpcsubnet->brports[i].uuid) {
                    if (!strcmp(tmpstr, vpcsubnet->brports[i].uuid)) {
                        mido_copy_midoname(&(vpcsubnet->midos[VPCBR_RTPORT]), &(vpcsubnet->brports[i]));
                        mido_copy_midoname(&(vpcsubnet->midos[VPCRT_BRPORT]), &(vpc->rtports[j]));
                    }
                }
                EUCA_FREE(tmpstr);
            }
        }
    }

    {
        // temporary
        LOGDEBUG("AFTER POPULATE: VPCBR: %d VPCBR_RTPORT: %d VPCRT_BRPORT: %d VPCBR_DHCP: %d\n", vpcsubnet->midos[VPCBR].init, vpcsubnet->midos[VPCBR_RTPORT].init,
                 vpcsubnet->midos[VPCRT_BRPORT].init, vpcsubnet->midos[VPCBR_DHCP].init);
    }

    return (ret);
}

int populate_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc)
{
    int rc = 0, ret = 0, i = 0, j = 0;
    char *url = NULL, vpcname[32];

    snprintf(vpcname, 32, "vr_%s", vpc->name);
    for (i = 0; i < mido->max_routers; i++) {
        if (strstr(mido->routers[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT]), &(mido->routers[i]));
            mido_get_ports(&(vpc->midos[VPCRT]), &(vpc->rtports), &(vpc->max_rtports));
        }
    }

    snprintf(vpcname, 32, "vc_%s_prechain", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PRECHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_preether", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREETHERCHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_premeta", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREMETACHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_prevpcinternal", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREVPCINTERNALCHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_preelip", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREELIPCHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_prefw", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_PREFWCHAIN]), &(mido->chains[i]));
        }
    }

    snprintf(vpcname, 32, "vc_%s_postchain", vpc->name);
    for (i = 0; i < mido->max_chains; i++) {
        if (!strcmp(mido->chains[i].name, vpcname)) {
            mido_copy_midoname(&(vpc->midos[VPCRT_POSTCHAIN]), &(mido->chains[i]));
        }
    }

    if (midocore->midos[EUCABR].init) {
        for (i = 0; i < midocore->max_brports; i++) {
            LOGDEBUG("MIDO BRPORTS: %s/%s\n", SP(midocore->brports[i].name), midocore->brports[i].uuid);
            for (j = 0; j < vpc->max_rtports; j++) {
                rc = mido_getel_midoname(&(vpc->rtports[j]), "peerId", &url);
                if (!rc && url && midocore->brports[i].uuid) {
                    if (!strcmp(url, midocore->brports[i].uuid)) {
                        mido_copy_midoname(&(vpc->midos[EUCABR_DOWNLINK]), &(midocore->brports[i]));
                        mido_copy_midoname(&(vpc->midos[VPCRT_UPLINK]), &(vpc->rtports[j]));
                    }
                    EUCA_FREE(url);
                }
            }
        }
    }

    {
        // temporary
        LOGDEBUG("AFTER POPULATE: VPCRT: %d VPCRT_PRECHAIN: %d VPCRT_POSTCHAIN: %d EUCABR_DOWNLINK: %d VPCRT_UPLINK: %d\n", vpc->midos[VPCRT].init, vpc->midos[VPCRT_PRECHAIN].init,
                 vpc->midos[VPCRT_POSTCHAIN].init, vpc->midos[EUCABR_DOWNLINK].init, vpc->midos[VPCRT_UPLINK].init);
    }

    return (ret);
}

int populate_mido_core(mido_config * mido, mido_core * midocore)
{
    int rc = 0, ret = 0, i = 0, j = 0;
    char *url = NULL;

    for (i = 0; i < mido->max_routers; i++) {
        if (!strcmp(mido->routers[i].name, "eucart")) {
            mido_copy_midoname(&(midocore->midos[EUCART]), &(mido->routers[i]));
            mido_get_ports(&(midocore->midos[EUCART]), &(midocore->rtports), &(midocore->max_rtports));
        }
    }

    for (i = 0; i < mido->max_bridges; i++) {
        if (!strcmp(mido->bridges[i].name, "eucabr")) {
            mido_copy_midoname(&(midocore->midos[EUCABR]), &(mido->bridges[i]));
            mido_get_ports(&(midocore->midos[EUCABR]), &(midocore->brports), &(midocore->max_brports));
        }
    }

    for (i = 0; i < midocore->max_brports; i++) {
        for (j = 0; j < midocore->max_rtports; j++) {
            rc = mido_getel_midoname(&(midocore->rtports[j]), "peerId", &url);
            if (!rc && url && midocore->brports[i].uuid) {
                if (!strcmp(url, midocore->brports[i].uuid)) {
                    mido_copy_midoname(&(midocore->midos[EUCABR_RTPORT]), &(midocore->brports[i]));
                    mido_copy_midoname(&(midocore->midos[EUCART_BRPORT]), &(midocore->rtports[j]));
                }
            }
            EUCA_FREE(url);
            url = NULL;
            rc = mido_getel_midoname(&(midocore->rtports[j]), "portAddress", &url);
            if (!rc && url && mido->ext_rtaddr) {
                if (!strcmp(mido->ext_rtaddr, url)) {
                    mido_copy_midoname(&(midocore->midos[EUCART_GWPORT]), &(midocore->rtports[j]));
                }
            }
            EUCA_FREE(url);
        }
    }

    for (i = 0; i < mido->max_hosts; i++) {
        if (strstr(mido->hosts[i].name, mido->ext_rthostname)) {
            mido_copy_midoname(&(midocore->midos[GWHOST]), &(mido->hosts[i]));
        }
    }

    {
        // temporary
        LOGDEBUG("AFTER POPULATE: EUCART: %d EUCABR: %d EUCART_BRPORT: %d EUCABR_RTPORT: %d EUCART_GWPORT: %d GWHOST: %d\n", midocore->midos[EUCART].init,
                 midocore->midos[EUCABR].init, midocore->midos[EUCART_BRPORT].init, midocore->midos[EUCABR_RTPORT].init, midocore->midos[EUCART_GWPORT].init,
                 midocore->midos[GWHOST].init);
    }

    return (ret);
}

int disconnect_mido_vpc_instance_elip(mido_vpc_instance * vpcinstance)
{
    int ret = 0, rc = 0;

    rc = mido_delete_rule(&(vpcinstance->midos[ELIP_PRE]));
    if (rc) {
        //TODO
        ret = 1;
    }

    rc = mido_delete_rule(&(vpcinstance->midos[ELIP_POST]));
    if (rc) {
        //TODO
        ret = 1;
    }

    rc = mido_delete_route(&(vpcinstance->midos[ELIP_ROUTE]));
    if (rc) {
        ret = 1;
    }

    return (ret);
}

int connect_mido_vpc_instance_elip(mido_config * mido, mido_core * midocore, mido_vpc * vpc, mido_vpc_instance * vpcinstance)
{
    int rc = 0, ret = 0;
    char *ipAddr_pub = NULL, *ipAddr_priv = NULL, *tmpstr = NULL;
    char ip[32];

    if (!vpcinstance->gniInst->publicIp || !vpcinstance->gniInst->privateIp) {
        LOGERROR("input ip is 0.0.0.0\n");
        return (1);
    }
    // TODO - check for a change, and do a DC if the pub/priv mapping has changed
    /*
       rc = disconnect_mido_vpc_instance_elip(vpcinstance);
       if (rc) {
       }
     */

    tmpstr = hex2dot(mido->int_rtnw + vpc->rtid);
    snprintf(ip, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    ipAddr_pub = hex2dot(vpcinstance->gniInst->publicIp);
    ipAddr_priv = hex2dot(vpcinstance->gniInst->privateIp);

    rc = mido_create_ipaddrgroup_ip(&(vpcinstance->midos[ELIP_PRE_IPADDRGROUP]), ipAddr_pub, NULL);
    if (rc) {
    }

    rc = mido_create_ipaddrgroup_ip(&(vpcinstance->midos[ELIP_POST_IPADDRGROUP]), ipAddr_priv, NULL);
    if (rc) {
    }

    rc = mido_create_rule(&(vpc->midos[VPCRT_PREELIPCHAIN]), &(vpcinstance->midos[ELIP_PRE]), "type", "dnat", "flowAction", "continue", "ipAddrGroupDst",
                          vpcinstance->midos[ELIP_PRE_IPADDRGROUP].uuid, "natTargets", "jsonlist", "natTargets:addressTo", ipAddr_priv, "natTargets:addressFrom", ipAddr_priv,
                          "natTargets:portFrom", "0", "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create midonet rule: check midonet health\n");
        ret = 1;
    }

    rc = mido_create_rule(&(vpc->midos[VPCRT_POSTCHAIN]), &(vpcinstance->midos[ELIP_POST]), "type", "snat", "flowAction", "continue", "ipAddrGroupSrc",
                          vpcinstance->midos[ELIP_POST_IPADDRGROUP].uuid, "natTargets", "jsonlist", "natTargets:addressTo", ipAddr_pub, "natTargets:addressFrom", ipAddr_pub,
                          "natTargets:portFrom", "0", "natTargets:portTo", "0", "natTargets:END", "END", NULL);
    if (rc) {
        LOGERROR("cannot create midonet rule: check midonet health\n");
        ret = 1;
    }

    if (!ret && strcmp(ipAddr_pub, "0.0.0.0")) {
        // create the EL ip route in main router
        rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_BRPORT]), "0.0.0.0", "0", ipAddr_pub, "32", ip, "100", &(vpcinstance->midos[ELIP_ROUTE]));
        if (rc) {
            LOGERROR("setup float IP router on midonet router: check midonet health\n");
            ret = 1;
        }
    }

    EUCA_FREE(ipAddr_pub);
    EUCA_FREE(ipAddr_priv);

    return (ret);
}

int connect_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, mido_vpc_instance * vpcinstance, midoname * vmhost)
{
    int ret = 0, rc = 0;
    char *macAddr = NULL, *ipAddr = NULL;
    char ifacename[16];

    snprintf(ifacename, 16, "vn_%s", vpcinstance->gniInst->name);

    // create the Exterior ports for VMs
    rc = mido_create_port(&(vpcsubnet->midos[VPCBR]), "ExteriorBridge", NULL, NULL, NULL, &(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }
    // link vm host port to vm bridge port
    rc = mido_link_host_port(vmhost, ifacename, &(vpcsubnet->midos[VPCBR]), &(vpcinstance->midos[VPCBR_VMPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port to vm interface link: check midonet health\n");
        return (1);
    }
    // set up dhcp host entry  
    hex2mac(vpcinstance->gniInst->macAddress, &macAddr);
    ipAddr = hex2dot(vpcinstance->gniInst->privateIp);
    for (int i = 0; i < strlen(macAddr); i++) {
        macAddr[i] = tolower(macAddr[i]);
    }

    LOGDEBUG("adding host %s/%s to dhcp server\n", SP(macAddr), SP(ipAddr));
    //  bzero(&(vpcinstance->midos[VPCBR_DHCPHOST]), sizeof(midoname));
    rc = mido_create_dhcphost(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->midos[VPCBR_DHCP]), vpcinstance->gniInst->name, macAddr, ipAddr, &(vpcinstance->midos[VPCBR_DHCPHOST]));
    if (rc) {
        LOGERROR("cannot create midonet dhcp host entry: check midonet health\n");
        return (1);
    }
    EUCA_FREE(ipAddr);
    EUCA_FREE(macAddr);

    // apply the chains to the instance port
    rc = mido_update_port(&(vpcinstance->midos[VPCBR_VMPORT]), "inboundFilterId", vpcinstance->midos[INST_PRECHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet port: check midonet health\n");
        return (1);
    }

    rc = mido_update_port(&(vpcinstance->midos[VPCBR_VMPORT]), "outboundFilterId", vpcinstance->midos[INST_POSTCHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet port: check midonet health\n");
        return (1);
    }

    return (ret);
}

int free_mido_config(mido_config * mido)
{
    int ret = 0, i = 0;

    if (!mido)
        return (0);

    EUCA_FREE(mido->ext_rthostname);
    EUCA_FREE(mido->ext_rtaddr);
    EUCA_FREE(mido->ext_rtiface);

    mido_free_midoname_list(mido->hosts, mido->max_hosts);
    EUCA_FREE(mido->hosts);

    mido_free_midoname_list(mido->routers, mido->max_routers);
    EUCA_FREE(mido->routers);

    mido_free_midoname_list(mido->bridges, mido->max_bridges);
    EUCA_FREE(mido->bridges);

    mido_free_midoname_list(mido->chains, mido->max_chains);
    EUCA_FREE(mido->chains);

    mido_free_midoname_list(mido->brports, mido->max_brports);
    EUCA_FREE(mido->brports);

    mido_free_midoname_list(mido->rtports, mido->max_rtports);
    EUCA_FREE(mido->rtports);

    mido_free_midoname_list(mido->ipaddrgroups, mido->max_ipaddrgroups);
    EUCA_FREE(mido->ipaddrgroups);

    free_mido_core(mido->midocore);
    EUCA_FREE(mido->midocore);

    for (i = 0; i < mido->max_vpcs; i++) {
        free_mido_vpc(&(mido->vpcs[i]));
    }
    EUCA_FREE(mido->vpcs);

    for (i = 0; i < mido->max_vpcsecgroups; i++) {
        free_mido_vpc_secgroup(&(mido->vpcsecgroups[i]));
    }
    EUCA_FREE(mido->vpcsecgroups);

    bzero(mido, sizeof(mido_config));

    return (ret);
}

int free_mido_core(mido_core * midocore)
{
    int ret = 0;

    if (!midocore)
        return (0);

    /*
       mido_free_midoname(&(midocore->midos[EUCART]));
       mido_free_midoname(&(midocore->midos[EUCART_BRPORT]));
       mido_free_midoname(&(midocore->midos[EUCABR_RTPORT]));
       mido_free_midoname(&(midocore->midos[EUCABR]));
       mido_free_midoname(&(midocore->midos[EUCART_GWPORT]));
       mido_free_midoname(&(midocore->midos[GWHOST]));
     */

    mido_free_midoname_list(midocore->midos, MIDOCOREEND);

    mido_free_midoname_list(midocore->brports, midocore->max_brports);
    EUCA_FREE(midocore->brports);

    mido_free_midoname_list(midocore->rtports, midocore->max_rtports);
    EUCA_FREE(midocore->rtports);

    bzero(midocore, sizeof(mido_core));

    return (ret);
}

int free_mido_vpc(mido_vpc * vpc)
{
    int ret = 0, i = 0;

    if (!vpc)
        return (0);

    mido_free_midoname_list(vpc->midos, VPCEND);

    mido_free_midoname_list(vpc->rtports, vpc->max_rtports);
    EUCA_FREE(vpc->rtports);

    for (i = 0; i < vpc->max_subnets; i++) {
        free_mido_vpc_subnet(&(vpc->subnets[i]));
    }
    EUCA_FREE(vpc->subnets);

    bzero(vpc, sizeof(mido_vpc));

    return (ret);
}

int free_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet)
{
    int ret = 0, i = 0;

    if (!vpcsubnet)
        return (0);

    mido_free_midoname_list(vpcsubnet->midos, VPCSUBNETEND);

    /*
       mido_free_midoname(&(vpcsubnet->midos[VPCBR]));
       mido_free_midoname(&(vpcsubnet->midos[VPCBR_RTPORT]));
       mido_free_midoname(&(vpcsubnet->midos[VPCRT_BRPORT]));
       mido_free_midoname(&(vpcsubnet->midos[VPCBR_DHCP]));
     */

    mido_free_midoname_list(vpcsubnet->brports, vpcsubnet->max_brports);
    EUCA_FREE(vpcsubnet->brports);

    mido_free_midoname_list(vpcsubnet->dhcphosts, vpcsubnet->max_dhcphosts);
    EUCA_FREE(vpcsubnet->dhcphosts);

    for (i = 0; i < vpcsubnet->max_instances; i++) {
        free_mido_vpc_instance(&(vpcsubnet->instances[i]));
    }
    EUCA_FREE(vpcsubnet->instances);

    bzero(vpcsubnet, sizeof(mido_vpc_subnet));

    return (ret);
}

int free_mido_vpc_instance(mido_vpc_instance * vpcinstance)
{
    int ret = 0;

    if (!vpcinstance)
        return (0);

    mido_free_midoname_list(vpcinstance->midos, VPCINSTANCEEND);

    bzero(vpcinstance, sizeof(mido_vpc_instance));

    return (ret);
}

int delete_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet)
{
    int rc = 0, ret = 0, i = 0;

    LOGDEBUG("DELETING SUBNET '%s'\n", vpcsubnet->name);

    // delete all instances on this subnet
    for (i = 0; i < vpcsubnet->max_instances; i++) {
        rc = delete_mido_vpc_instance(&(vpcsubnet->instances[i]));
    }

    //  rc = mido_delete_route(&(vpcsubnet->vpcrt_route));

    rc = mido_delete_port(&(vpcsubnet->midos[VPCRT_BRPORT]));

    rc = mido_delete_dhcp(&(vpcsubnet->midos[VPCBR]), &(vpcsubnet->midos[VPCBR_DHCP]));

    rc = mido_delete_bridge(&(vpcsubnet->midos[VPCBR]));

    free_mido_vpc_subnet(vpcsubnet);

    return (ret);
}

int delete_mido_vpc(mido_vpc * vpc)
{
    int rc = 0, ret = 0, i = 0;

    LOGDEBUG("DELETING VPC: %s, %s\n", vpc->name, vpc->midos[VPCRT].name);

    rc = mido_delete_port(&(vpc->midos[EUCABR_DOWNLINK]));
    rc = mido_delete_router(&(vpc->midos[VPCRT]));

    rc = mido_delete_chain(&(vpc->midos[VPCRT_PREETHERCHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_PREMETACHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_PREVPCINTERNALCHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_PREELIPCHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_PREFWCHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_PRECHAIN]));
    rc = mido_delete_chain(&(vpc->midos[VPCRT_POSTCHAIN]));

    for (i = 0; i < vpc->max_subnets; i++) {
        rc = delete_mido_vpc_subnet(&(vpc->subnets[i]));
    }

    free_mido_vpc(vpc);

    return (ret);
}

int create_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, char *subnet, char *slashnet, char *gw)
{
    int rc = 0, ret = 0;
    char name_buf[32];

    rc = mido_create_port(&(vpc->midos[VPCRT]), "InteriorRouter", gw, subnet, slashnet, &(vpcsubnet->midos[VPCRT_BRPORT]));
    if (rc) {
        LOGERROR("cannot create midonet router port: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(vpc->midos[VPCRT]), &(vpcsubnet->midos[VPCRT_BRPORT]), "0.0.0.0", "0", subnet, slashnet, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vb_%s_%s", vpc->name, vpcsubnet->name);
    rc = mido_create_bridge("euca_tenant_1", name_buf, &(vpcsubnet->midos[VPCBR]));
    if (rc) {
        LOGERROR("cannot create midonet bridge: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(vpcsubnet->midos[VPCBR]), "InteriorBridge", NULL, NULL, NULL, &(vpcsubnet->midos[VPCBR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }

    rc = mido_link_ports(&(vpcsubnet->midos[VPCRT_BRPORT]), &(vpcsubnet->midos[VPCBR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create midonet router <-> bridge link: check midonet health\n");
        return (1);
    }
    // setup DHCP on the bridge for this subnet
    rc = mido_create_dhcp(&(vpcsubnet->midos[VPCBR]), subnet, slashnet, gw, "8.8.8.8", &(vpcsubnet->midos[VPCBR_DHCP]));
    if (rc) {
        LOGERROR("cannot create midonet dhcp server: check midonet health\n");
        return (1);
    }

    return (ret);
}

int create_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc)
{
    int rc = 0;
    char name_buf[32], nw[32], sn[32], ip[32], gw[32], *tmpstr = NULL;

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(nw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(sn, 32, "%d", mido->int_rtsn);

    tmpstr = hex2dot(mido->int_rtnw + vpc->rtid);
    snprintf(ip, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(gw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    //  snprintf(vpc->name, 16, "%s", name);
    snprintf(name_buf, 32, "vr_%s_%d", vpc->name, vpc->rtid);
    rc = mido_create_router("euca_tenant_1", name_buf, &(vpc->midos[VPCRT]));
    if (rc) {
        LOGERROR("cannot create midonet router: check midonet health\n");
        return (1);
    }
    // link the vpc network and euca network
    rc = mido_create_port(&(midocore->midos[EUCABR]), "InteriorBridge", NULL, NULL, NULL, &(vpc->midos[EUCABR_DOWNLINK]));
    if (rc) {
        LOGERROR("cannot create midonet bridge port: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(vpc->midos[VPCRT]), "InteriorRouter", ip, nw, sn, &(vpc->midos[VPCRT_UPLINK]));
    if (rc) {
        LOGERROR("cannot create midonet router port: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(vpc->midos[VPCRT]), &(vpc->midos[VPCRT_UPLINK]), "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(vpc->midos[VPCRT]), &(vpc->midos[VPCRT_UPLINK]), "0.0.0.0", "0", "0.0.0.0", "0", gw, "0", NULL);
    if (rc) {
        LOGERROR("cannot create midonet router route: check midonet health\n");
        return (1);
    }

    rc = mido_link_ports(&(vpc->midos[EUCABR_DOWNLINK]), &(vpc->midos[VPCRT_UPLINK]));
    if (rc) {
        LOGERROR("cannot create midonet bridge <-> router link: check midonet health\n");
        return (1);
    }
    // create the chains
    snprintf(name_buf, 32, "vc_%s_prechain", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PRECHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_preether", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREETHERCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_premeta", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREMETACHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_prevpcinternal", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREVPCINTERNALCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_preelip", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREELIPCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_prefw", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_PREFWCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }

    snprintf(name_buf, 32, "vc_%s_postchain", vpc->name);
    rc = mido_create_chain("euca_tenant_1", name_buf, &(vpc->midos[VPCRT_POSTCHAIN]));
    if (rc) {
        LOGERROR("cannot create midonet chain: check midonet health\n");
        return (1);
    }
    // add the jump chains
    rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "1", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREETHERCHAIN].uuid, NULL);
    rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "2", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREMETACHAIN].uuid, NULL);
    rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "3", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREVPCINTERNALCHAIN].uuid, NULL);
    rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "4", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREELIPCHAIN].uuid, NULL);
    rc = mido_create_rule(&(vpc->midos[VPCRT_PRECHAIN]), NULL, "position", "5", "type", "jump", "jumpChainId", vpc->midos[VPCRT_PREFWCHAIN].uuid, NULL);

    // meta
    rc = mido_create_rule(&(vpc->midos[VPCRT_PREMETACHAIN]), NULL, "type", "dnat", "flowAction", "continue", "ipAddrGroupDst", midocore->midos[METADATA_IPADDRGROUP].uuid,
                          "natTargets", "jsonlist", "natTargets:addressTo", "10.111.5.34", "natTargets:addressFrom", "10.111.5.34", "natTargets:portFrom", "8773",
                          "natTargets:portTo", "8773", "natTargets:END", "END", NULL);
    rc = mido_create_rule(&(vpc->midos[VPCRT_POSTCHAIN]), NULL, "type", "rev_dnat", "flowAction", "accept", NULL);

    // apply the chains to the vpc router
    rc = mido_update_router(&(vpc->midos[VPCRT]), "inboundFilterId", vpc->midos[VPCRT_PRECHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet router: check midonet health\n");
        return (1);
    }

    rc = mido_update_router(&(vpc->midos[VPCRT]), "outboundFilterId", vpc->midos[VPCRT_POSTCHAIN].uuid, NULL);
    if (rc) {
        LOGERROR("cannot attach midonet chain to midonet router: check midonet health\n");
        return (1);
    }

    return (0);
}

int delete_mido_core(mido_core * midocore)
{
    int rc;
    //  rc = mido_unlink_host_port(&(midocore->midos[GWHOST]), &(midocore->midos[EUCART_GWPORT]));
    rc = mido_unlink_host_port(&(midocore->midos[GWHOST]), &(midocore->midos[EUCART_GWPORT]));
    sleep(1);
    //  rc = mido_delete_bridge(&(midocore->midos[EUCABR]));
    rc = mido_delete_bridge(&(midocore->midos[EUCABR]));
    sleep(1);
    rc = mido_delete_router(&(midocore->midos[EUCART]));
    return (0);
}

int create_mido_core(mido_config * mido, mido_core * midocore)
{
    int ret = 0, rc = 0;
    char nw[32], sn[32], gw[32], *tmpstr = NULL;

    tmpstr = hex2dot(mido->int_rtnw);
    snprintf(nw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    tmpstr = hex2dot(mido->int_rtaddr);
    snprintf(gw, 32, "%s", tmpstr);
    EUCA_FREE(tmpstr);

    snprintf(sn, 32, "%d", mido->int_rtsn);

    LOGINFO("creating mido core router\n");
    //  rc = mido_create_router("euca_tenant_1", "eucart", &(midocore->midos[EUCART]));
    rc = mido_create_router("euca_tenant_1", "eucart", &(midocore->midos[EUCART]));
    if (rc) {
        LOGERROR("cannot create router: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(midocore->midos[EUCART]), "InteriorRouter", gw, nw, sn, &(midocore->midos[EUCART_BRPORT]));
    if (rc) {
        LOGERROR("cannot create router port: check midonet health\n");
        return (1);
    }

    rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_BRPORT]), "0.0.0.0", "0", nw, sn, "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        return (1);
    }
    //  rc = mido_create_port(&(midocore->midos[EUCART]), "ExteriorRouter", "10.111.5.57", "10.111.0.0", "16", &(midocore->midos[EUCART_GWPORT]));
    rc = mido_create_port(&(midocore->midos[EUCART]), "ExteriorRouter", "10.111.5.57", "10.111.0.0", "16", &(midocore->midos[EUCART_GWPORT]));
    if (rc) {
        LOGERROR("cannot create router port: check midonet health\n");
        return (1);
    }
    //  rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", "10.111.0.0", "16", NULL, "0", NULL);
    rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", "10.111.0.0", "16", "UNSET", "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        return (1);
    }
    //  rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", "0.0.0.0", "0", "10.111.5.34", "0", NULL);
    rc = mido_create_route(&(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]), "0.0.0.0", "0", "0.0.0.0", "0", "10.111.5.34", "0", NULL);
    if (rc) {
        LOGERROR("cannot create router route: check midonet health\n");
        return (1);
    }

    rc = mido_link_host_port(&(midocore->midos[GWHOST]), mido->ext_rtiface, &(midocore->midos[EUCART]), &(midocore->midos[EUCART_GWPORT]));
    if (rc) {
        LOGERROR("cannot link router port to host interface: check midonet health\n");
        return (1);
    }

    rc = mido_create_bridge("euca_tenant_1", "eucabr", &(midocore->midos[EUCABR]));
    if (rc) {
        LOGERROR("cannot create bridge: check midonet health\n");
        return (1);
    }

    rc = mido_create_port(&(midocore->midos[EUCABR]), "InteriorBridge", NULL, NULL, NULL, &(midocore->midos[EUCABR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create bridge port: check midonet health\n");
        return (1);
    }

    rc = mido_link_ports(&(midocore->midos[EUCART_BRPORT]), &(midocore->midos[EUCABR_RTPORT]));
    if (rc) {
        LOGERROR("cannot create router <-> bridge link: check midonet health\n");
        return (1);
    }

    rc = mido_create_ipaddrgroup("euca_tenant_1", "metadata_ip", &(midocore->midos[METADATA_IPADDRGROUP]));
    if (rc) {
        LOGERROR("cannot create metadata ipaddrgroup: check midonet health\n");
        ret = 1;
    }

    rc = mido_create_ipaddrgroup_ip(&(midocore->midos[METADATA_IPADDRGROUP]), "169.254.169.254", NULL);
    if (rc) {
        // TODO
        ret = 1;
    }
    return (ret);

}

int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw)
{
    char *tok = NULL;
    char *cpy = NULL;
    u32 nw = 0, gw = 0;

    cpy = strdup(cidr);
    tok = strchr(cpy, '/');
    if (tok) {
        *tok = '\0';
        snprintf(outnet, strlen(cpy) + 1, "%s", cpy);
        tok++;
        snprintf(outslashnet, strlen(tok) + 1, "%s", tok);
    }
    EUCA_FREE(cpy);

    nw = dot2hex(outnet);
    gw = nw + 1;
    tok = hex2dot(gw);
    snprintf(outgw, strlen(tok) + 1, "%s", tok);
    EUCA_FREE(tok);

    return (0);
}
