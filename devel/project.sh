CLASSPATH_HEADER='<?xml version="1.0" encoding="UTF-8"?><classpath>'
CLASSPATH_STANDARD='<classpathentry kind="con" path="GROOVY_SUPPORT"/><classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>'
CLASSPATH_FOOTER='<classpathentry kind="output" path="bin"/></classpath>'
SOURCES=""
LIBS=""

if [[ -e ${PWD}/clc/modules/ ]]; then
  SRC_DIR=$(readlink -f ${PWD}/)
else
  SRC_DIR=$(dirname $(dirname $(readlink -f ${BASH_SOURCE})))
fi
printf "%-40.40s %s\n" "Source Directory:" ${SRC_DIR} 

d=${SRC_DIR}
while [[ "${d}" != "/" ]]; do 
  if [[ -d ${d}/.metadata ]]; then
    WORKSPACE_DIR=$(echo $d/ | sed 's/\/+/\\\//g')
    break
  else
    d=$(dirname $d);
  fi
done
if [[ -z "${WORKSPACE_DIR}" ]]; then
  echo -e "ERROR Failed to find the eclipse workspace directory.\nERROR Is the branch in the right directory?\nERROR There should be a directory .metadata in one of the parent directories of ${SRC_DIR}" 1>&2
fi
printf "%-40.40s %s\n" "Eclipse Workspace Directory:" ${WORKSPACE_DIR} 

if [ -z "$1" ]; then
  NAME=${SRC_DIR//${WORKSPACE_DIR}/}
  NAME=${NAME//\//:}
else
  NAME=$1
fi
printf "\n%-40.40s %s\n" "-> New Project Name:" ${NAME} 
fixProjectName() {
  TARGET=$1
  SUFFIX=$2
  printf "%-40.40s %s in %s\n" "--> Setting project name:" ${NAME} ${TARGET}
  sed -i "s/<name>$(xpath -e projectDescription/name/text\(\) ${TARGET} 2>/dev/null)<\/name>/<name>${NAME}:${SUFFIX}<\/name>/g"   ${TARGET}
}


fixProjectName ${SRC_DIR}/clc/.project clc
fixProjectName ${SRC_DIR}/project/.project c
fixProjectName ${SRC_DIR}/clc/eucadmin/.project eucadmin
fixProjectName ${SRC_DIR}/devel/.project devel
  
for m in $(ls -1 ${SRC_DIR}/clc/modules/ | sort); do 
  f=$(basename $m)
  if [[ -d ${SRC_DIR}/clc/modules/$f ]]; then 
    if ls -1 ${SRC_DIR}/clc/modules/$f/src/main/java/* >/dev/null 2>&1; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" output=\"modules/$f/build\" path=\"modules/$f/src/main/java\"/>"
      printf "%-40.40s %s\n" "---> Adding directory to build path:" "${SRC_DIR}/clc/modules/$f/src/main/java"
    fi
    if ls -1 ${SRC_DIR}/clc/modules/$f/src/test/java/* >/dev/null 2>&1; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" output=\"modules/$f/build\" path=\"modules/$f/src/test/java\"/>"
      printf "%-40.40s %s\n" "---> Adding directory to build path:" "${SRC_DIR}/clc/modules/$f/src/test/java"
    fi
    if [[ -d ${SRC_DIR}/clc/modules/$f/conf ]]; then
      for CONF_SUBDIR in ${SRC_DIR}/clc/modules/$f/conf/*; do
        if [[ -d ${CONF_SUBDIR} ]] && ls -1 ${CONF_SUBDIR}/* >/dev/null 2>&1 && [[ "upgrade" != "$(basename ${CONF_SUBDIR})" ]]; then
          SOURCES="${SOURCES}<classpathentry kind=\"src\" path=\"modules/$f/conf/$(basename ${CONF_SUBDIR})\"/>"
          printf "%-40.40s %s\n" "---> Adding directory to build path:" "${SRC_DIR}/clc/modules/$f/conf/$(basename ${CONF_SUBDIR})"
        fi
      done 
    fi
  fi
done
for f in $(ls -1 ${SRC_DIR}/clc/lib/*.jar | sort); do 
  LIBS="${LIBS}<classpathentry kind=\"lib\" path=\"lib/$(basename $f)\"/>"
done
if [[ -e ${SRC_DIR}/clc/.classpath ]]; then 
  printf "%-40.40s %s\n" "--> Backing up old .classpath:" "$(cp -fv ${SRC_DIR}/clc/.classpath ${SRC_DIR}/clc/.classpath.bak)"
fi
printf "%-40.40s %s\n" "--> Generating new .classpath:" ${SRC_DIR}/clc/.classpath
echo "${CLASSPATH_HEADER}${SOURCES}${CLASSPATH_STANDARD}${LIBS}${CLASSPATH_FOOTER}" > ${SRC_DIR}/clc/.classpath
if which xmlindent >/dev/null 2>&1; then 
  xmlindent -f -w ${SRC_DIR}/clc/.classpath 2>/dev/null
fi
echo "FIN"

