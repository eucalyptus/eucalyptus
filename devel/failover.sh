#!/bin/bash

DESCRIBE_SERVICES=$(which euca-describe-services)
echo $DESCRIBE_SERVICES
EUCALYPTUS=${EUCALYPTUS:-/opt/eucalyptus}
if [[ -z "${DESCRIBE_SERVICES}" && -e ${EUCALYPTUS}/usr/sbin/euca-describe-services ]]; then
  DESCRIBE_SERVICES=${EUCALYPTUS}/usr/sbin/euca-describe-services
elif [[ -z "${DESCRIBE_SERVICES}" ]]; then
  echo Failed to find euca-describe-services command
  exit 1
fi
if [[ -z "${EC2_URL}" ]]; then
  echo Failed to find EC2_URL
fi
if ! euca-describe-services -T eucalyptus -F ENABLED >/dev/null; then
  echo Failed to find enabled cloud controller
else 
  export EC2_URL=$(euca-describe-services -T eucalyptus -F ENABLED | awk '{print $7}')
  echo Using EC2_URL=$EC2_URL
  if ! euca-describe-services -T eucalyptus -F ENABLED >/dev/null; then
    echo Failed to contact enabled cloud controller: ${EC2_URL}
  fi
fi
SERVICE_TYPE=cluster
SERVICE_INITD=eucalyptus-cc
FAILOVER_TRIES=100
RECOVER_TRIES=100
if [[ 2 -ne "$(${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} | wc -l)" ]]; then
  echo "Failed to find two registered services of type: ${SERVICE_TYPE}"
  exit 1
fi
TESTNAME=
ITER=
log() {
  FSTRING=$1
  shift
  printf "%10s:%04.4d ${FSTRING}\n" ${TESTNAME} ${ITER} ${@}
}

walrus_process() {
  echo "ps auxww | grep -v grep | grep us-cl"
}
walrus_check() {
  euca-describe-services -T walrus -H $1 | awk '{print $5}'
}
walrus_restore() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cloud start"
}
walrus_failure() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cloud stop"
}

storage_process() {
  echo "ps auxww | grep -v grep | grep us-cl"
}
storage_check() {
  euca-describe-services -T storage -H $1 | awk '{print $5}'
}
storage_restore() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cloud start"
}
storage_failure() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cloud stop"
}

eucalyptus_process() {
  echo "ps auxww | grep -v grep | grep us-cl"
}
eucalyptus_check() {
  euca-describe-services -T eucalyptus -H $1 | awk '{print $5}'
}
eucalyptus_restore() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cloud start"
}
eucalyptus_failure() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cloud stop"
}
cluster_process() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cc status"
}
cluster_check() {
  ssh root@$1 "awk '/localState=/{val=\$4\":\"\$7}END{print val}' /opt/eucalyptus/var/log/eucalyptus/cc.log | sed 's/:monitor.*locaState=//g'"
}
cluster_restore() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cc start"
}
cluster_failure() {
  echo "/opt/eucalyptus/etc/init.d/eucalyptus-cc stop"
}

doStateChange() {
  SERVICE_TYPE=$1
  shift
  TESTNAME=$1
  shift
  MASTER_START_STATE=$1
  shift
  SLAVE_START_STATE=$1
  shift
  MASTER_TARGET_STATE="NOTREADY"
  FAIL_OPERATION=$(${SERVICE_TYPE}_failure)
  RESTORE_OPERATION=$(${SERVICE_TYPE}_restore)
  CHECK_OPERATION=${SERVICE_TYPE}_check
  PROCESS_STATUS=$(${SERVICE_TYPE}_process)
  if [[ 2 -ne $(${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} | wc -l) ]]; then
    log FAILED to find two services of type ${SERVICE_TYPE}
    return
  fi
  for host in $(${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} | awk '{print $7}' | sed 's/.*\/\///g;s/:877.*//g'); do
    log "CHECKING ${host}"
    if ! ssh root@${host} ${PROCESS_STATUS} >/dev/null 2>&1; then
      log "RESTARTING ${host} $(ssh root@${host} ${RESTORE_OPERATION})"
    fi
  done
  for((a=0;a<10;a++));do 
    if ${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} -F ${MASTER_START_STATE} | awk '{print $5}' | sed 's/.*\/\///g;s/:877.*//g' | grep ${MASTER_START_STATE} >/dev/null 2>&1; then
      break;
    fi
    sleep 1
  done
  for((a=0;a<10;a++));do 
    if ${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} -F ${SLAVE_START_STATE} | awk '{print $5}' | sed 's/.*\/\///g;s/:877.*//g' | grep  ${SLAVE_START_STATE} >/dev/null 2>&1; then
      break;
    fi
    sleep 1
  done
  MASTER_HOST=$(${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} -F ${MASTER_START_STATE} | awk '{print $7}' | sed 's/.*\/\///g;s/:877.*//g')
  SLAVE_HOST=$(${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} -F ${SLAVE_START_STATE} | awk '{print $7}' | sed 's/.*\/\///g;s/:877.*//g')
  if [[ -z "${MASTER_HOST}" ]]; then
    log "FAILED No host has ${SERVICE_TYPE} which is ${MASTER_START_STATE}"
    ${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} -F ${MASTER_START_STATE}
    return
  fi
  if [[ -z "${SLAVE_HOST}" ]]; then
    log "FAILED No host has ${SERVICE_TYPE} which is ${SLAVE_START_STATE}"
    ${DESCRIBE_SERVICES} -T ${SERVICE_TYPE} -F ${SLAVE_START_STATE}
    return
  fi
  log "Found ${MASTER_START_STATE} ${SERVICE_TYPE}: ${MASTER_HOST}"
  log "Found ${SLAVE_START_STATE} ${SERVICE_TYPE}: ${SLAVE_HOST}"
  if euca-describe-services -T eucalyptus -H ${MASTER_HOST} | grep ENABLED >/dev/null 2>&1; then
    export EC2_URL=$(euca-describe-services -T eucalyptus -H ${SLAVE_HOST} | awk '{print $7}')
    log "Using EC2_URL=$EC2_URL"
  fi
  log "STOPPING ${SERVICE_TYPE} on ${MASTER_HOST} $(ssh root@${MASTER_HOST} ${FAIL_OPERATION})"

  log "FAILOVER Waiting for ${MASTER_HOST} to change state to ${MASTER_TARGET_STATE}"
  for((ITER=0;ITER<${FAILOVER_TRIES};ITER++)); do
    MASTER_HOST_STATE=$(${DESCRIBE_SERVICES} -H ${MASTER_HOST} -T ${SERVICE_TYPE} | awk '{print $5}' | sed 's/.*\/\///g;s/:877.*//g')
    SLAVE_HOST_STATE=$(${DESCRIBE_SERVICES} -H ${SLAVE_HOST} -T ${SERVICE_TYPE} | awk '{print $5}' | sed 's/.*\/\///g;s/:877.*//g')
    if [[ "${MASTER_TARGET_STATE}" == "NOTREADY" && "${MASTER_HOST_STATE}" == "${MASTER_TARGET_STATE}" && "${SLAVE_HOST_STATE}" == "ENABLED" ]]; then
      log "SUCCESS Switched services over from ${MASTER_HOST} to ${SLAVE_HOST}"
      ${DESCRIBE_SERVICES} -T ${SERVICE_TYPE}
      log "STARTING ${SERVICE_TYPE} on ${MASTER_HOST} $(ssh root@${MASTER_HOST} ${RESTORE_OPERATION})"
      MASTER_TARGET_STATE="DISABLED"
      TESTNAME="RESTORING"
    elif [[ "${MASTER_TARGET_STATE}" == "DISABLED" && "${MASTER_HOST_STATE}" == "${MASTER_TARGET_STATE}" && "${SLAVE_HOST_STATE}" == "ENABLED" ]]; then
      log "SUCCESS Switched services over from ${MASTER_HOST} to ${SLAVE_HOST}"
      ${DESCRIBE_SERVICES} -T ${SERVICE_TYPE}
      return
    else
      CHECK_MASTER=$("${CHECK_OPERATION}" ${MASTER_HOST})
      CHECK_SLAVE=$("${CHECK_OPERATION}" ${SLAVE_HOST})
      log "\n\t\t->SLAVE  %-15.15s %-10.10s %s\n\t\t->MASTER %-15.15s %-10.10s %s" ${MASTER_HOST} ${MASTER_HOST_STATE} ${CHECK_MASTER} ${SLAVE_HOST} ${SLAVE_HOST_STATE} ${CHECK_SLAVE}
    fi
    sleep 1
  done
}

for s in storage walrus; do
  for((a=0;a<5;a++)); do
    echo "Running ${s} trail ${a}"| tee -a failover.log
    doStateChange ${s} FAILOVER ENABLED DISABLED 2>&1 | tee -a failover.log
    sleep 1
  done
done
