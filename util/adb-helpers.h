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
  int virtualBootRecordSize = adb_virtualMachineType_sizeof_virtualBootRecord(vm_type, env);
  for (i = 0; i<EUCA_MAX_VBRS && i<virtualBootRecordSize; i++) {
    adb_virtualBootRecordType_t * vbr_type = adb_virtualMachineType_get_virtualBootRecord_at (vm_type, env, i);
    strncpy (params->virtualBootRecord[i].resourceLocation, adb_virtualBootRecordType_get_resourceLocation(vbr_type, env), CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG, "resource location: %s\n", params->virtualBootRecord[i].resourceLocation);
    strncpy (params->virtualBootRecord[i].guestDeviceName, adb_virtualBootRecordType_get_guestDeviceName(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG, "   guest dev name: %s\n", params->virtualBootRecord[i].guestDeviceName);
    params->virtualBootRecord[i].size = adb_virtualBootRecordType_get_size(vbr_type, env);
    logprintfl (EUCADEBUG, "             size: %d\n", params->virtualBootRecord[i].size);
    strncpy (params->virtualBootRecord[i].format, adb_virtualBootRecordType_get_format(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG, "           format: %s\n", params->virtualBootRecord[i].format);
    strncpy (params->virtualBootRecord[i].id, adb_virtualBootRecordType_get_id(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG, "               id: %s\n", params->virtualBootRecord[i].id);
    strncpy (params->virtualBootRecord[i].type, adb_virtualBootRecordType_get_type(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG, "             type: %s\n", params->virtualBootRecord[i].type);
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
  for (i=0; i<sizeof(params->virtualBootRecord)/sizeof(virtualBootRecord); i++) {
    virtualBootRecord * vbr = & params->virtualBootRecord [i];
    if (strlen(vbr->resourceLocation)>0) {
      adb_virtualBootRecordType_t * vbr_type = adb_virtualBootRecordType_create(env);
      adb_virtualBootRecordType_set_resourceLocation(vbr_type, env, vbr->resourceLocation);
      adb_virtualBootRecordType_set_guestDeviceName(vbr_type, env, vbr->guestDeviceName);
      adb_virtualBootRecordType_set_size(vbr_type, env, vbr->size);
      adb_virtualBootRecordType_set_format(vbr_type, env, vbr->format);
      adb_virtualBootRecordType_set_id(vbr_type, env, vbr->id);
      adb_virtualBootRecordType_set_type(vbr_type, env, vbr->type);
      adb_virtualMachineType_add_virtualBootRecord(vm_type, env, vbr_type);
    }
  }

  return vm_type;
}

#endif // _ADB_HELPERS_H
