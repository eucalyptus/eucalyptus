#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stdarg.h>
#include <linux/limits.h>

#include <globalnetwork.h>

int evaluate_xpath_property (xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results) {
  int i;
  xmlXPathObjectPtr objptr;
  char **retresults;
  
  *max_results = 0;

  objptr = xmlXPathEvalExpression((unsigned char *)expression, ctxptr);
  if(objptr == NULL) {
    LOGERROR("unable to evaluate xpath expression: %s\n", expression);
    return(1);
  } else {
    if (objptr->nodesetval) {
      *max_results = (int)objptr->nodesetval->nodeNr;
      *results = malloc(sizeof(char *) * *max_results);
      retresults = *results;
      for (i=0; i<*max_results; i++) {
        retresults[i] = strdup((char *)objptr->nodesetval->nodeTab[i]->children->content);
      }
      LOGDEBUG("%d results after evaluated expression %s\n", *max_results, expression);
      for (i=0; i<*max_results; i++) {
        LOGTRACE("\tRESULT %d: %s\n", i, retresults[i]);
      }
    }
  }
  xmlXPathFreeObject(objptr);
  return(0);
}

int evaluate_xpath_element (xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results) {
  int i;
  xmlXPathObjectPtr objptr;
  char **retresults;

  *max_results = 0;

  objptr = xmlXPathEvalExpression((unsigned char *)expression, ctxptr);
  if(objptr == NULL) {
    LOGERROR("unable to evaluate xpath expression: %s\n", expression);
    return(1);
  } else {
    if (objptr->nodesetval) {
      *max_results = (int)objptr->nodesetval->nodeNr;
      *results = malloc(sizeof(char *) * *max_results);
      retresults = *results;
      for (i=0; i<*max_results; i++) {
        retresults[i] = strdup((char *)objptr->nodesetval->nodeTab[i]->properties->children->content);
      }
      LOGDEBUG("%d results after evaluated expression %s\n", *max_results, expression);
      for (i=0; i<*max_results; i++) {
        LOGTRACE("\tRESULT %d: %s\n", i, retresults[i]);
      }
    }
  }
  xmlXPathFreeObject(objptr);
  return(0);
}

int gni_init(globalNetworkInfo *gni, char *xmlpath) {
  int rc;
  xmlDocPtr docptr;
  xmlXPathContextPtr ctxptr;  
  char expression[2048];
  char **results=NULL;
  int max_results=0, i, j, k;

  xmlInitParser();
  LIBXML_TEST_VERSION
  

  docptr = xmlParseFile(xmlpath);
  if (docptr == NULL) {
    LOGERROR("unable to parse XML file (%s)\n", xmlpath);
    return(1);
  }
  
  ctxptr = xmlXPathNewContext(docptr);
  if(ctxptr == NULL) {
    LOGERROR("unable to get new xml context\n");
    xmlFreeDoc(docptr);
    return(1);
  }
  
  snprintf(expression, 2048, "/network-data/configuration/property[@name='enabledCLCIp']/value");
  rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
  for (i=0; i<max_results; i++) {
    LOGTRACE("after function: %d: %s\n", i, results[i]);
    gni->enabledCLCIp = dot2hex(results[i]);
    free(results[i]);
  }
  free(results);

  snprintf(expression, 2048, "/network-data/configuration/property[@name='instanceDNSDomain']/value");
  rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
  for (i=0; i<max_results; i++) {
    LOGTRACE("after function: %d: %s\n", i, results[i]);
    snprintf(gni->instanceDNSDomain, HOSTNAME_SIZE, "%s", results[i]);
    free(results[i]);
  }
  free(results);

  snprintf(expression, 2048, "/network-data/configuration/property[@name='publicIps']/value");
  rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
  for (i=0; i<max_results; i++) {
    LOGTRACE("after function: %d: %s\n", i, results[i]);
    gni->public_ips[i] = dot2hex(results[i]);
    free(results[i]);
  }
  gni->max_public_ips = max_results;
  free(results);
  
  snprintf(expression, 2048, "/network-data/configuration/property[@name='subnets']/subnet");
  rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
  for (i=0; i<max_results; i++) {
    LOGTRACE("after function: %d: %s\n", i, results[i]);
    gni->subnets[i].subnet = dot2hex(results[i]);
    free(results[i]);
  }
  gni->max_subnets = max_results;
  free(results);
  
  for (j=0; j<gni->max_subnets; j++) {
    snprintf(expression, 2048, "/network-data/configuration/property[@name='subnets']/subnet[@name='%s']/property[@name='netmask']/value", euca_hex2dot(gni->subnets[j].subnet));
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      gni->subnets[j].netmask = dot2hex(results[i]);
      free(results[i]);
    }
    free(results);
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='subnets']/subnet[@name='%s']/property[@name='gateway']/value", euca_hex2dot(gni->subnets[j].subnet));
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      gni->subnets[j].gateway = dot2hex(results[i]);
      free(results[i]);
    }
    free(results);
  }
  
  snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster");
  rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
  for (i=0; i<max_results; i++) {
    LOGTRACE("after function: %d: %s\n", i, results[i]);
    snprintf(gni->clusters[i].name, HOSTNAME_SIZE, "%s", results[i]);
    free(results[i]);
  }
  gni->max_clusters = max_results;
  free(results);
  
  for (j=0; j<gni->max_clusters; j++) {
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='enabledCCIp']/value", gni->clusters[j].name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      gni->clusters[j].enabledCCIp = dot2hex(results[i]);
      free(results[i]);
    }
    free(results);
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='macPrefix']/value", gni->clusters[j].name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      snprintf(gni->clusters[j].macPrefix, 8, "%s", results[i]);
      free(results[i]);
    }
    free(results);
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='privateIps']/value", gni->clusters[j].name);
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      gni->clusters[j].private_ips[i] = dot2hex(results[i]);
      free(results[i]);
    }
    gni->clusters[j].max_private_ips = max_results;
    free(results);
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/subnet", gni->clusters[j].name);
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      gni->clusters[j].private_subnet.subnet = dot2hex(results[i]);
      free(results[i]);
    }
    free(results);
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/subnet[@name='%s']/property[@name='netmask']/value", gni->clusters[j].name, hex2dot(gni->clusters[j].private_subnet.subnet));
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      gni->clusters[j].private_subnet.netmask = dot2hex(results[i]);
      free(results[i]);
    }
    free(results);
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/subnet[@name='%s']/property[@name='gateway']/value", gni->clusters[j].name, hex2dot(gni->clusters[j].private_subnet.subnet));
    rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      gni->clusters[j].private_subnet.gateway = dot2hex(results[i]);
      free(results[i]);
    }
    free(results);
    
    snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='nodes']/node", gni->clusters[j].name);
    rc = evaluate_xpath_element(ctxptr, expression, &results, &max_results);
    for (i=0; i<max_results; i++) {
      LOGTRACE("after function: %d: %s\n", i, results[i]);
      snprintf(gni->clusters[j].nodes[i].name, HOSTNAME_SIZE, "%s", results[i]);
      free(results[i]);
    }
    gni->clusters[j].max_nodes = max_results;
    free(results);
    
    for (k=0; k<gni->clusters[j].max_nodes; k++) {
      snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='nodes']/node[@name='%s']/property[@name='dhcpdPath']/value", gni->clusters[j].name, gni->clusters[j].nodes[k].name);
      rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
      for (i=0; i<max_results; i++) {
	LOGTRACE("after function: %d: %s\n", i, results[i]);
	snprintf(gni->clusters[j].nodes[k].dhcpdPath, MAX_PATH, "%s", results[i]);
	free(results[i]);
      }
      free(results);
      
      snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='nodes']/node[@name='%s']/property[@name='bridgeInterface']/value", gni->clusters[j].name, gni->clusters[j].nodes[k].name);
      rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
      for (i=0; i<max_results; i++) {
	LOGTRACE("after function: %d: %s\n", i, results[i]);
	snprintf(gni->clusters[j].nodes[k].bridgeInterface, 32, "%s", results[i]);
	free(results[i]);
      }
      free(results);
      
      snprintf(expression, 2048, "/network-data/configuration/property[@name='clusters']/cluster[@name='%s']/property[@name='nodes']/node[@name='%s']/property[@name='publicInterface']/value", gni->clusters[j].name, gni->clusters[j].nodes[k].name);
      rc = evaluate_xpath_property(ctxptr, expression, &results, &max_results);
      for (i=0; i<max_results; i++) {
	LOGTRACE("after function: %d: %s\n", i, results[i]);
	snprintf(gni->clusters[j].nodes[k].publicInterface, 32, "%s", results[i]);
	free(results[i]);
      }
      free(results);
      
    }  
  }
  
  xmlXPathFreeContext(ctxptr);
  xmlFreeDoc(docptr);
  xmlCleanupParser();

  return(0);
}
int gni_print(globalNetworkInfo *gni) {
  int i, j, k;

  LOGTRACE("enabledCLCIp: %s\n", hex2dot(gni->enabledCLCIp));
  LOGTRACE("instanceDNSDomain: %s\n", gni->instanceDNSDomain);
  LOGTRACE("publicIps: \n");
  for (i=0; i<gni->max_public_ips; i++) {
    LOGTRACE("\tip %d: %s\n", i, hex2dot(gni->public_ips[i]));
  }
  LOGTRACE("subnets: \n");
  for (i=0; i<gni->max_subnets; i++) {
    LOGTRACE("\tsubnet %d: %s\n", i, hex2dot(gni->subnets[i].subnet));
    LOGTRACE("\t\tnetmask: %s\n", hex2dot(gni->subnets[i].netmask));
    LOGTRACE("\t\tgateway: %s\n", hex2dot(gni->subnets[i].gateway));
  }
  LOGTRACE("clusters: \n");
  for (i=0; i<gni->max_clusters; i++) {
    LOGTRACE("\tcluster %d: %s\n", i, gni->clusters[i].name);
    LOGTRACE("\t\tenabledCCIp: %s\n", hex2dot(gni->clusters[i].enabledCCIp));
    LOGTRACE("\t\tmacPrefix: %s\n", gni->clusters[i].macPrefix);
    LOGTRACE("\t\tsubnet: %s\n", hex2dot(gni->clusters[i].private_subnet.subnet));
    LOGTRACE("\t\t\tnetmask: %s\n", hex2dot(gni->clusters[i].private_subnet.netmask));
    LOGTRACE("\t\t\tgateway: %s\n", hex2dot(gni->clusters[i].private_subnet.gateway));
    LOGTRACE("\t\tprivate_ips \n");
    for (j=0; j<gni->clusters[i].max_private_ips; j++) {
      LOGTRACE("\t\t\tip %d: %s\n", j, hex2dot(gni->clusters[i].private_ips[j]));
    }
    LOGTRACE("\t\tnodes \n");
    for (j=0; j<gni->clusters[i].max_nodes; j++) {
      LOGTRACE("\t\t\tnode %d: %s\n", j, gni->clusters[i].nodes[j].name);
      LOGTRACE("\t\t\t\tdhcpdPath: %s\n", gni->clusters[i].nodes[j].dhcpdPath);
      LOGTRACE("\t\t\t\tbridgeInterface: %s\n", gni->clusters[i].nodes[j].bridgeInterface);
      LOGTRACE("\t\t\t\tpublicInterface: %s\n", gni->clusters[i].nodes[j].publicInterface);
    }
  }

  return(0);
}
int gni_free(globalNetworkInfo *gni) {
  EUCA_FREE(gni);
  return(0);
}
