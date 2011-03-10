CLASSPATH_HEADER='<?xml version="1.0" encoding="UTF-8"?><classpath>'
CLASSPATH_STANDARD='<classpathentry kind="con" path="GROOVY_SUPPORT"/><classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>'
CLASSPATH_FOOTER='<classpathentry kind="output" path="bin"/></classpath>'
SOURCES=""
LIBS=""
if [[ -e ${PWD}/modules/ ]]; then
  SRC_DIR=${PWD}
else
  SRC_DIR=$(dirname $(dirname $(readlink -f ${BASH_SOURCE})))
fi
echo "Using ${SRC_DIR} as clc directory" >&2

if bzr nick >/dev/null 2>&1 ; then
  NAME=$(bzr nick 2>/dev/null)
  NAME=$(bzr info | awk '/checkout root/{print $3}' | sed 's/^\///g;s/\([^\/]\)\//\1:/g')
  echo "Using ${NAME} as Eclipse project name" >&2
  sed -i "s/<name>.*<\/name>/<name>${NAME}:clc<\/name>/g" ${SRC_DIR}/.project
  sed -i "s/<name>.*<\/name>/<name>${NAME}:c-code<\/name>/g" ${SRC_DIR}/../project/.project
fi
  
for m in $(ls -1 ${SRC_DIR}/modules/ | sort); do 
  f=$(basename $m)
  if [[ -d ${SRC_DIR}/modules/$f ]]; then 
    if [[ -d ${SRC_DIR}/modules/$f/src/main/java ]]; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" output=\"modules/$f/build\" path=\"modules/$f/src/main/java\"/>"
    fi
    if [[ -d ${SRC_DIR}/modules/$f/src/test/java ]]; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" output=\"modules/$f/build\" path=\"modules/$f/src/test/java\"/>"
    fi
    if [[ -d ${SRC_DIR}/modules/$f/conf/scripts ]]; then
      SOURCES="${SOURCES}<classpathentry kind=\"src\" path=\"modules/$f/conf/scripts\"/>"
    fi
  fi
done
for f in $(ls -1 ${SRC_DIR}/lib/*.jar | sort); do 
  LIBS="${LIBS}<classpathentry kind=\"lib\" path=\"lib/$(basename $f)\"/>"
done
if [[ -e ${SRC_DIR}/.classpath ]]; then 
  echo "Backing up old .classpath file"
  cp -fv ${SRC_DIR}/.classpath ${SRC_DIR}/.classpath.bak
fi
echo "Generating .classpath file"
echo "${CLASSPATH_HEADER}${SOURCES}${CLASSPATH_STANDARD}${LIBS}${CLASSPATH_FOOTER}" > ${SRC_DIR}/.classpath
if which xmlindent; then 
  xmlindent -f -w ${SRC_DIR}/.classpath
fi
echo "Project ready to import"


