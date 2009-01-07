#include <stdio.h>
#include <stdlib.h>
#include <string.h> /* strlen, strcpy */
#include <handlers.h>
#include <storage.h>
#include <eucalyptus.h>

bunchOfInstances * global_instances = NULL; /* will be initiated upon first call */

int doRunInstance (ncMetadata *meta, char *instanceId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, ncNetConf *conf, ncInstance **outInst)
{
    ncInstance * instance = NULL;
    * outInst = NULL;
    int error;

    printf ("doRunInstance() invoked (id=%s image=%s)\n", instanceId, imageId);
    if (!(instance = allocate_instance (instanceId, params, imageId, "groovy", NEW, "l33t"))) return 1; /* TODO: return out-of-memory error */
    error = add_instance (&global_instances, instance);
    if ( error ) return error; 

    /* this is a dummy, so it doesn't actually run the instance */

    change_state (instance, BOOTING);
    
    * outInst = instance;
    return 0;
}

int doTerminateInstance (ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState)
{
    ncInstance * instance;

    printf ("doTerminateInstance() invoked (id=%s)\n", instanceId);
    instance = find_instance(&global_instances, instanceId);
    if ( instance == NULL ) return NOT_FOUND;
    * previousState = instance->stateCode;
    change_state (instance, TERMINATING);
    * shutdownState = instance->stateCode;
    
    /* this is a dummy, so it doesn't actually terminate the instance */

    remove_instance (&global_instances, instance);

    /* TODO: fix free_instance (&instance); */

    return 0;
}

int doDescribeResource (ncMetadata *meta, char *resourceType, ncResource **outRes)
{
    printf ("doDescribeResource() invoked\n");

   /* this is a dummy, so it doesn't actually know anything about resources */

    return 0;
}

int doDescribeInstances (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    printf ("doDescribeInstances() invoked\n");
    if (instIdsLen == 0) { /* describe all instances */
        ncInstance * instance;
        int i;

        * outInstsLen = total_instances (&global_instances);
        if ( * outInstsLen ) {
            * outInsts = malloc (sizeof(ncInstance *)*(*outInstsLen));
            if ( (* outInsts) == NULL ) return OUT_OF_MEMORY;
            
            for (i=0; (instance = get_instance (&global_instances))!=NULL; i++) {
                (* outInsts)[i] = instance;
            }
        }
        
    } else {
        printf ("specific doDescribeInstances() not implemented\n");
        return 1;
    }
    return 0;
}
