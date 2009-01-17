#include <stdio.h>
#include <stdlib.h>
#include <string.h> 
#include "data.h"

/* metadata is present in every type of nc request */
ncMetadata * allocate_metadata (char *correlationId, char *userId)
{
    ncMetadata * meta;

    if (!(meta = malloc(sizeof(ncMetadata)))) return NULL;
    if (correlationId) meta->correlationId = strdup(correlationId);
    if (userId)        meta->userId        = strdup(userId);

    return meta;
}

void free_metadata (ncMetadata ** metap)
{
    ncMetadata * meta = * metap;
    if (meta->correlationId) free(meta->correlationId);
    if (meta->userId)        free(meta->userId);
    * metap = NULL;
}

/* instances are present in instance-related requests */
ncInstance * allocate_instance (char *instanceId, char *reservationId, 
                                ncInstParams *params, 
                                char *imageId, char *imageURL, 
                                char *kernelId, char *kernelURL, 
                                char *ramdiskId, char *ramdiskURL,
                                char *stateName, int stateCode, char *userId, 
                                ncNetConf *ncnet, char *keyName,
                                char *userData, char *launchIndex, char **groupNames, int groupNamesSize)
{
    ncInstance * inst;

    /* zeroed out for cleaner-looking checkpoints and
     * strings that are empty unless set */
    if (!(inst = calloc(1, sizeof(ncInstance)))) return NULL; 

    if (userData) {
        strncpy(inst->userData, userData, CHAR_BUFFER_SIZE*10);
    }

    if (launchIndex) {
        strncpy(inst->launchIndex, launchIndex, CHAR_BUFFER_SIZE);
    }

    inst->groupNamesSize = groupNamesSize;
    if (groupNames && groupNamesSize) {
        int i;
        for (i=0; groupNames[i] && i<groupNamesSize; i++) {
            strncpy(inst->groupNames[i], groupNames[i], CHAR_BUFFER_SIZE);
        }
    }
    inst->volumesSize = 0;
    
    if (ncnet != NULL) {
      memcpy(&(inst->ncnet), ncnet, sizeof(ncNetConf));
    }

    if (instanceId) {
      strncpy(inst->instanceId, instanceId, CHAR_BUFFER_SIZE);
    }

    if (keyName) {
      strncpy(inst->keyName, keyName, CHAR_BUFFER_SIZE*4);
    }

    if (reservationId) {
      strncpy(inst->reservationId, reservationId, CHAR_BUFFER_SIZE);
    }

    if (imageId) {
      strncpy(inst->imageId, imageId, CHAR_BUFFER_SIZE);
    }
    if (imageURL) {
      strncpy(inst->imageURL, imageURL, CHAR_BUFFER_SIZE);
    }
    if (kernelId) {
      strncpy(inst->kernelId, kernelId, CHAR_BUFFER_SIZE);
    }
    if (kernelURL) {
      strncpy(inst->kernelURL, kernelURL, CHAR_BUFFER_SIZE);
    }
    if (ramdiskId) {
      strncpy(inst->ramdiskId, ramdiskId, CHAR_BUFFER_SIZE);
    }
    if (ramdiskURL) {
      strncpy(inst->ramdiskURL, ramdiskURL, CHAR_BUFFER_SIZE);
    }

    if (stateName) {
      strncpy(inst->stateName, stateName, CHAR_BUFFER_SIZE);
    }
    if (userId) {
      strncpy(inst->userId, userId, CHAR_BUFFER_SIZE);
    }
    if (params) {
        inst->params.memorySize = params->memorySize;
        inst->params.diskSize = params->diskSize;
        inst->params.numberOfCores = params->numberOfCores;
    }
    inst->stateCode = stateCode;
    
    return inst;
}

void free_instance (ncInstance ** instp) 
{
    ncInstance * inst;

    if (!instp) return;
    inst = * instp;
    * instp = NULL;
    if (!inst) return;

    free (inst);
}

/* resource is used to return information about resources */
ncResource * allocate_resource (char *nodeStatus,
                                int memorySizeMax, int memorySizeAvailable, 
                                int diskSizeMax, int diskSizeAvailable,
                                int numberOfCoresMax, int numberOfCoresAvailable,
                                char *publicSubnets)
{
    ncResource * res;
    
    if (!(res = malloc(sizeof(ncResource)))) return NULL;
    if (nodeStatus) {
      strncpy(res->nodeStatus, nodeStatus, CHAR_BUFFER_SIZE);
    }
    if (publicSubnets) {
      strncpy(res->publicSubnets, publicSubnets, CHAR_BUFFER_SIZE);
    }
    res->memorySizeMax = memorySizeMax;
    res->memorySizeAvailable = memorySizeAvailable;
    res->diskSizeMax = diskSizeMax;
    res->diskSizeAvailable = diskSizeAvailable;
    res->numberOfCoresMax = numberOfCoresMax;
    res->numberOfCoresAvailable = numberOfCoresAvailable;
    
    if (!res->nodeStatus ) free_resource(&res);
    return res;
}

void free_resource (ncResource ** resp)
{
    ncResource * res;

    if (!resp) return;
    res = * resp;
    * resp = NULL;
    if (!res) return;

    free (res);
}

instance_error_codes add_instance (bunchOfInstances ** headp, ncInstance * instance)
{
    bunchOfInstances * new = malloc (sizeof(bunchOfInstances));

    if ( new == NULL ) return OUT_OF_MEMORY;
    new->instance = instance;
    new->next = NULL;

    if ( * headp == NULL) {
        * headp = new;
        (* headp)->count = 1;

    } else {
        bunchOfInstances * last = * headp;
        do {
            if ( !strcmp(last->instance->instanceId, instance->instanceId) ) {
                free (new);
                return DUPLICATE;
            }
        } while ( last->next && (last = last->next) );
        last->next = new;
        (* headp)->count++;
    }        

    return OK;
}

instance_error_codes remove_instance (bunchOfInstances ** headp, ncInstance * instance)
{
    bunchOfInstances * head, * prev = NULL;

    for ( head = * headp; head; ) {
        int count = (* headp)->count;

        if ( !strcmp(head->instance->instanceId, instance->instanceId) ) {
            if ( prev ) {
                prev->next = head->next;
            } else {
                * headp = head->next;
            }
            if (* headp) {
                (* headp)->count = count - 1;
            }
            free (head);
            return OK;
        }
        prev = head;
        head = head->next;
    }
    return NOT_FOUND;
}

instance_error_codes for_each_instance (bunchOfInstances ** headp, void (* function)(bunchOfInstances **, ncInstance *, void *), void *param) 
{
    bunchOfInstances * head;
    
    for ( head = * headp; head; head = head->next ) {
        function ( headp, head->instance, param );
    }
    
    return OK;
}

ncInstance * find_instance (bunchOfInstances **headp, char * instanceId)
{
    bunchOfInstances * head;
    
    for ( head = * headp; head; head = head->next ) {
        if (!strcmp(head->instance->instanceId, instanceId)) {
            return head->instance;
        }
    }
    
    return NULL;
}

ncInstance * get_instance (bunchOfInstances **headp)
{
    static bunchOfInstances * current = NULL;
    
    /* advance static variable, wrapping to head if at the end */
    if ( current == NULL ) current = * headp;
    else current = current->next;
    
    /* return the new value, if any */
    if ( current == NULL ) return NULL;
    else return current->instance;
}

int total_instances (bunchOfInstances **headp)
{
    if ( * headp ) return (* headp)->count;
    else return 0;
}

/* 
 * finds a matching volume 
 * OR returns a pointer to the next empty volume slot 
 * OR if full, returns NULL
 */
static ncVolume * find_volume (ncInstance * instance, char *volumeId) 
{
    ncVolume * v = instance->volumes;

    int i;
    for (i=0; i<EUCA_MAX_VOLUMES; i++,v++) {
        if ( ! strncmp (v->volumeId, volumeId, CHAR_BUFFER_SIZE) )
            break;
        if ( ! strnlen (v->volumeId, CHAR_BUFFER_SIZE) )
            break;
    }
    if (i==EUCA_MAX_VOLUMES)
        v = NULL;

    return v;
}

ncVolume * add_volume (ncInstance * instance, char *volumeId, char *remoteDev, char *localDev)
{
    ncVolume * v = find_volume (instance, volumeId);

    if ( v == NULL) {
        return NULL; /* out of room */
    }

    if ( ! strncmp (v->volumeId, volumeId, CHAR_BUFFER_SIZE) ) {
        return NULL; /* already there */
    } else {
        strncpy (v->volumeId, volumeId, CHAR_BUFFER_SIZE);
        strncpy (v->remoteDev, remoteDev, CHAR_BUFFER_SIZE);
        strncpy (v->localDev, localDev , CHAR_BUFFER_SIZE);
        instance->volumesSize++;
    }

    return v;
}

ncVolume * free_volume (ncInstance * instance, char *volumeId, char *remoteDev, char *localDev)
{
    ncVolume * v = find_volume (instance, volumeId);
    
    if ( v == NULL) {
        return NULL; /* not there (and out of room) */
    }

    if ( strncmp (v->volumeId, volumeId, CHAR_BUFFER_SIZE) ) {
        return NULL; /* not there */
	
    } else {
        ncVolume * last_v = instance->volumes+(EUCA_MAX_VOLUMES-1);
        int slots_left = last_v - v;
        /* shift the remaining entries up, empty or not */
        if (slots_left)
            memmove (v, v+1, slots_left*sizeof(ncVolume));
        
        /* empty the last one */
        bzero (last_v, sizeof(ncVolume));
        instance->volumesSize--;
    }
    
    return v;
}
