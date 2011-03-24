#!/bin/bash -x
TARGET=SystemBootstrapper
if [ -n "${1}" ]; then
  TARGET=$1
fi
REPO=$(dirname $(dirname $(readlink -f ${BASH_SOURCE})))
DEPFIND_HOME=/opt/DependencyFinder-1.2.1-beta4
export PATH=${DEPFIND_HOME}/bin:${PATH}
DEPS_DIR=$(mktemp -d /tmp/deps-XXXXXXXX)
DEPS=${DEPS_DIR}/dependencies.xml
DEPS_C2C=${DEPS_DIR}/dependencies-classes.xml
DEPS_CLOSURE=${DEPS_DIR}/dependencies-closure.xml
DEPS_CLOSURE_GRAPH=${DEPS_DIR}/dependencies-closure.graphml
touch ${DEPS} ${DEPS_C2C} ${DEPS_CLOSURE}
DependencyExtractor -xml -out ${DEPS} -time ${REPO}/clc/target/*.jar
c2c -xml -out ${DEPS_C2C} ${DEPS}
DependencyClosure -xml -verbose -maximum-outbound-depth 0 -start-includes /${TARGET}/ \
  -package-stop-includes com.google \
  -package-stop-includes java \
  -package-stop-includes javax \
  -package-stop-includes org \
  -package-stop-includes edu.emory \
  -package-stop-includes groovy \
  -out ${DEPS_CLOSURE} ${DEPS_C2C}
sed -i '/<outbound/d' ${DEPS_CLOSURE}
DependencyGraphToyEd -in ${DEPS_CLOSURE} -out ${DEPS_CLOSURE//xml/graphml}
echo See ${DEPS_DIR}
