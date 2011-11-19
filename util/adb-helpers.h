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
    themeta->disabledServicesLen = adb_##thefunc##_sizeof_disabledServices(theadb, env); \
    for (i=0; i<themeta->disabledServicesLen && i < 16; i++) {			\
      sit = adb_##thefunc##_get_disabledServices_at(theadb, env, i);		\
      snprintf(themeta->disabledServices[i].type,32,"%s",adb_serviceInfoType_get_type(sit, env)); \
      snprintf(themeta->disabledServices[i].name,32,"%s",adb_serviceInfoType_get_name(sit, env)); \
      themeta->disabledServices[i].urisLen = adb_serviceInfoType_sizeof_uris(sit, env);	\
      for (j=0; j<themeta->disabledServices[i].urisLen && j < 8; j++) {		\
	snprintf(themeta->disabledServices[i].uris[j], 512, "%s",adb_serviceInfoType_get_uris_at(sit, env, j)); \
      }									\
    }									\
    themeta->notreadyServicesLen = adb_##thefunc##_sizeof_notreadyServices(theadb, env); \
    for (i=0; i<themeta->notreadyServicesLen && i < 16; i++) {			\
      sit = adb_##thefunc##_get_notreadyServices_at(theadb, env, i);		\
      snprintf(themeta->notreadyServices[i].type,32,"%s",adb_serviceInfoType_get_type(sit, env)); \
      snprintf(themeta->notreadyServices[i].name,32,"%s",adb_serviceInfoType_get_name(sit, env)); \
      themeta->notreadyServices[i].urisLen = adb_serviceInfoType_sizeof_uris(sit, env);	\
      for (j=0; j<themeta->notreadyServices[i].urisLen && j < 8; j++) {		\
	snprintf(themeta->notreadyServices[i].uris[j], 512, "%s",adb_serviceInfoType_get_uris_at(sit, env, j)); \
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
  }

//    logprintfl(EUCADEBUG, "eucalyptusMessageMarshal: excerpt: userId=%s correlationId=%s epoch=%d services[0].name=%s services[0].type=%s services[0].uris[0]=%s\n", SP(themeta->userId), SP(themeta->correlationId), themeta->epoch, SP(themeta->services[0].name), SP(themeta->services[0].type), SP(themeta->services[0].uris[0]));


static inline int datetime_to_unix (axutil_date_time_t *dt, const axutil_env_t *env)
{
    time_t tsu, ts, tsdelta, tsdelta_min;
    struct tm *tmu;
    
    if (!dt || !env) {
      return(0);
    }

    ts = time(NULL);
    tmu = gmtime(&ts);
    tsu = mktime(tmu);
    tsdelta = (tsu - ts) / 3600;
    tsdelta_min = ((tsu - ts) - (tsdelta * 3600)) / 60;
    
    struct tm t = {
        axutil_date_time_get_second(dt, env),
        axutil_date_time_get_minute(dt, env) - tsdelta_min,
        axutil_date_time_get_hour(dt, env) - tsdelta,
        axutil_date_time_get_date(dt, env),
        axutil_date_time_get_month(dt, env)-1,
        axutil_date_time_get_year(dt, env)-1900,
        0,
        0,
        0
    };
    
    return (int) mktime(&t);
}

static inline void copy_vm_type_from_adb (virtualMachine * params, adb_virtualMachineType_t * vm_type, const axutil_env_t *env)
{
  int i;

  if (vm_type==NULL) return;
  bzero(params, sizeof(virtualMachine));
  params->mem = adb_virtualMachineType_get_memory(vm_type, env);
  params->cores = adb_virtualMachineType_get_cores(vm_type, env);
  params->disk = adb_virtualMachineType_get_disk(vm_type, env);
  safe_strncpy(params->name, adb_virtualMachineType_get_name(vm_type, env), sizeof(params->name));
  params->virtualBootRecordLen = adb_virtualMachineType_sizeof_virtualBootRecord(vm_type, env);
  for (i = 0; i<EUCA_MAX_VBRS && i<params->virtualBootRecordLen; i++) {
    adb_virtualBootRecordType_t * vbr_type = adb_virtualMachineType_get_virtualBootRecord_at (vm_type, env, i);
    safe_strncpy (params->virtualBootRecord[i].resourceLocation, adb_virtualBootRecordType_get_resourceLocation(vbr_type, env), CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG2, "resource location: %s\n", params->virtualBootRecord[i].resourceLocation);
    safe_strncpy (params->virtualBootRecord[i].guestDeviceName, adb_virtualBootRecordType_get_guestDeviceName(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG2, "   guest dev name: %s\n", params->virtualBootRecord[i].guestDeviceName);
    params->virtualBootRecord[i].size = adb_virtualBootRecordType_get_size(vbr_type, env);
    logprintfl (EUCADEBUG2, "             size: %d\n", params->virtualBootRecord[i].size);
    safe_strncpy (params->virtualBootRecord[i].formatName, adb_virtualBootRecordType_get_format(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG2, "           format: %s\n", params->virtualBootRecord[i].formatName);
    safe_strncpy (params->virtualBootRecord[i].id, adb_virtualBootRecordType_get_id(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG2, "               id: %s\n", params->virtualBootRecord[i].id);
    safe_strncpy (params->virtualBootRecord[i].typeName, adb_virtualBootRecordType_get_type(vbr_type, env), SMALL_CHAR_BUFFER_SIZE);
    logprintfl (EUCADEBUG2, "             type: %s\n", params->virtualBootRecord[i].typeName);
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
  //  for (i=0; i<sizeof(params->virtualBootRecord)/sizeof(virtualBootRecord); i++) { // TODO: dan ask dmitrii
  for (i=0; i<EUCA_MAX_VBRS && i<params->virtualBootRecordLen; i++) {
    virtualBootRecord * vbr = & params->virtualBootRecord [i];
    if (strlen(vbr->resourceLocation)>0) {
      adb_virtualBootRecordType_t * vbr_type = adb_virtualBootRecordType_create(env);
      adb_virtualBootRecordType_set_resourceLocation(vbr_type, env, vbr->resourceLocation);
      adb_virtualBootRecordType_set_guestDeviceName(vbr_type, env, vbr->guestDeviceName);
      adb_virtualBootRecordType_set_size(vbr_type, env, vbr->size);
      adb_virtualBootRecordType_set_format(vbr_type, env, vbr->formatName);
      adb_virtualBootRecordType_set_id(vbr_type, env, vbr->id);
      adb_virtualBootRecordType_set_type(vbr_type, env, vbr->typeName);
      adb_virtualMachineType_add_virtualBootRecord(vm_type, env, vbr_type);
    }
  }

  return vm_type;
}

static inline adb_serviceInfoType_t * copy_service_info_type_to_adb(const axutil_env_t *env, serviceInfoType * input) {
  int i;
  adb_serviceInfoType_t *sit = adb_serviceInfoType_create(env);

  adb_serviceInfoType_set_type(sit, env, input->type);
  adb_serviceInfoType_set_name(sit, env, input->name);
  for (i=0; i<input->urisLen; i++) {
    adb_serviceInfoType_add_uris(sit, env, input->uris[i]);
  }
  
  return (sit);
}

static inline void copy_service_info_type_from_adb(serviceInfoType * input, adb_serviceInfoType_t * sit, const axutil_env_t *env) {
  int i;

  snprintf(input->type, 32, "%s", adb_serviceInfoType_get_type(sit, env));
  snprintf(input->name, 32, "%s", adb_serviceInfoType_get_name(sit, env));
  input->urisLen = adb_serviceInfoType_sizeof_uris(sit, env);
  for (i=0; i<input->urisLen && i<8; i++) {
    snprintf(input->uris[i], 512, "%s", adb_serviceInfoType_get_uris_at(sit, env, i));
  }
}

#endif // _ADB_HELPERS_H
