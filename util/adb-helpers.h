#ifndef _ADB_HELPERS_H
#define _ADB_HELPERS_H

#define EUCA_MESSAGE_UNMARSHAL(thefunc, theadb, themeta)		\
  {									\
    int i, j;								\
    adb_serviceInfoType_t *sit=NULL;					\
    bzero(themeta, sizeof(ncMetadata));					\
    themeta->correlationId = adb_##thefunc##_get_correlationId(theadb, env); \
    themeta->userId = adb_##thefunc##_get_userId(theadb, env);		\
    themeta->epoch = adb_##thefunc##_get_epoch(theadb, env);			\
    themeta->servicesLen = adb_##thefunc##_sizeof_services(theadb, env); \
    for (i=0; i<themeta->servicesLen && i < 16; i++) {			\
      sit = adb_##thefunc##_get_services_at(theadb, env, i);		\
      snprintf(themeta->services[i].type,32,"%s",adb_serviceInfoType_get_type(sit, env)); \
      snprintf(themeta->services[i].name,32,"%s",adb_serviceInfoType_get_name(sit, env)); \
      themeta->services[i].urisLen = adb_serviceInfoType_sizeof_uris(sit, env);	\
      for (j=0; j<themeta->services[i].urisLen && j < 8; j++) {		\
	snprintf(themeta->services[i].uris[j], 512, "%s",adb_serviceInfoType_get_uris_at(sit, env, j)); \
      }									\
    }									\
  }


#define EUCA_MESSAGE_MARSHAL(thefunc, theadb, themeta)		\
  {									\
    int i, j;								\
    adb_serviceInfoType_t *sit=NULL;					\
    adb_##thefunc##_set_correlationId(theadb, env, themeta->correlationId); \
    adb_##thefunc##_set_userId(theadb, env, themeta->userId);		\
    adb_##thefunc##_set_epoch(theadb, env,  themeta->epoch);		\
    for (i=0; i<themeta->servicesLen && i < 16; i++) {			\
      sit = adb_serviceInfoType_create(env);				\
      adb_serviceInfoType_set_type(sit, env, themeta->services[i].type); \
      adb_serviceInfoType_set_name(sit, env, themeta->services[i].name); \
      for (j=0; j<themeta->services[i].urisLen && j < 8; j++) {	\
	adb_serviceInfoType_add_uris(sit, env, themeta->services[i].uris[j]); \
      }									\
      adb_##thefunc##_add_services(theadb, env, sit);			\
    }									\
    logprintfl(EUCADEBUG, "eucalyptusMessageMarshal: excerpt: userId=%s correlationId=%s epoch=%d services[0].name=%s services[0].type=%s services[0].uris[0]=%s\n", SP(themeta->userId), SP(themeta->correlationId), themeta->epoch, SP(themeta->services[0].name), SP(themeta->services[0].type), SP(themeta->services[0].uris[0])); \
  }


static inline void copy_vm_type_from_adb (virtualMachine * params, adb_virtualMachineType_t * vm_type, const axutil_env_t *env)
{
  int i;

  if (vm_type==NULL) return;
  bzero(params, sizeof(virtualMachine));
  params->mem = adb_virtualMachineType_get_memory(vm_type, env);
  params->cores = adb_virtualMachineType_get_cores(vm_type, env);
  params->disk = adb_virtualMachineType_get_disk(vm_type, env);
  strncpy(params->name, adb_virtualMachineType_get_name(vm_type, env), sizeof(params->name));
  int deviceMappingSize = adb_virtualMachineType_sizeof_deviceMapping(vm_type, env);
  for (i = 0; i<EUCA_MAX_DEVMAPS && i<deviceMappingSize; i++) {
    adb_deviceMappingType_t * dm_type = adb_virtualMachineType_get_deviceMapping_at (vm_type, env, i);
    strncpy (params->deviceMapping[i].deviceName, adb_deviceMappingType_get_deviceName(dm_type, env), sizeof(params->deviceMapping[i].deviceName));
    strncpy (params->deviceMapping[i].virtualName, adb_deviceMappingType_get_virtualName(dm_type, env), sizeof(params->deviceMapping[i].virtualName));
    params->deviceMapping[i].size = adb_deviceMappingType_get_size(dm_type, env);
    strncpy (params->deviceMapping[i].format, adb_deviceMappingType_get_format(dm_type, env), sizeof(params->deviceMapping[i].format));
  }
}

static inline adb_virtualMachineType_t * copy_vm_type_to_adb (const axutil_env_t *env, virtualMachine * params)
{
  int i;

  adb_virtualMachineType_t * vm_type = adb_virtualMachineType_create(env);
  adb_virtualMachineType_set_memory(vm_type, env, params->mem);
  adb_virtualMachineType_set_cores(vm_type, env, params->cores);
  adb_virtualMachineType_set_disk(vm_type, env, params->disk);
  adb_virtualMachineType_set_name(vm_type, env, params->name);
  for (i=0; i<sizeof(params->deviceMapping)/sizeof(deviceMapping); i++) {
    deviceMapping * dm = & params->deviceMapping [i];
    if (strlen(dm->deviceName)>0) {
      adb_deviceMappingType_t * dm_type = adb_deviceMappingType_create(env);
      adb_deviceMappingType_set_deviceName(dm_type, env, dm->deviceName);
      adb_deviceMappingType_set_virtualName(dm_type, env, dm->virtualName);
      adb_deviceMappingType_set_size(dm_type, env, dm->size);
      adb_deviceMappingType_set_format(dm_type, env, dm->format);
      adb_virtualMachineType_add_deviceMapping(vm_type, env, dm_type);
    }
  }

  return vm_type;
}

#endif // _ADB_HELPERS_H
