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

NAME=${SRC_DIR//${WORKSPACE_DIR}/}
NAME=${NAME//\//:}
printf "\n%-40.40s %s\n" "-> New Project Name:" ${NAME} 
if bzr nick >/dev/null 2>&1 ; then
  printf "%-40.40s %s in %s\n" "--> Setting project name:" ${NAME} ${SRC_DIR}/clc/.project 
  sed -i "s/<name>.*<\/name>/<name>${NAME}:clc<\/name>/g" ${SRC_DIR}/clc/.project
  printf "%-40.40s %s in %s\n" "--> Setting project name:" ${NAME} ${SRC_DIR}/tools/.project 
  sed -i "s/<name>.*<\/name>/<name>${NAME}:python<\/name>/g" ${SRC_DIR}/clc/tools/.project
  printf "%-40.40s %s in %s\n" "--> Setting project name:" ${NAME} ${SRC_DIR}/project/.project 
  sed -i "s/<name>.*<\/name>/<name>${NAME}:c<\/name>/g" ${SRC_DIR}/project/.project
fi
  
for m in $(ls -1 ${SRC_DIR}/clc/modules/ | sort); do 
  f=$(basename $m)
  if [[ -d ${SRC_DIR}/clc/modules/$f ]]; then 
    if [[ -d ${SRC_DIR}/clc/modules/$f/src/main/java ]]; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" output=\"modules/$f/build\" path=\"modules/$f/src/main/java\"/>"
    fi
    if [[ -d ${SRC_DIR}/clc/modules/$f/src/test/java ]]; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" output=\"modules/$f/build\" path=\"modules/$f/src/test/java\"/>"
    fi
    if [[ -d ${SRC_DIR}/clc/modules/$f/conf/scripts ]]; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" path=\"modules/$f/conf/scripts\"/>"
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
  xmlindent -f -w ${SRC_DIR}/.classpath
fi
echo "FIN"

