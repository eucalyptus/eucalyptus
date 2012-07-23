#!/bin/bash -x

# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.

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
