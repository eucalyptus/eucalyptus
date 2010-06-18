#ifndef _ADB_HELPERS_H
#define _ADB_HELPERS_H

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
