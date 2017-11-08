# Copyright 2013-2014 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the
# following conditions are met:
#
#   Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer
#   in the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

#hawaii:/git/projects/jibx/core/build>
#> ant -Dbindname=jibx-bind-1.2.5 \
# 	-Drunname=jibx-run-1.2.5 \
#	-Dextrasname=jibx-extras-1.2.5 \
#	-Dschemaname=jibx-schema-1.2.5 \
#	-Dtoolsname=jibx-tools-1.2.5 \
#	compile-tools jar-tools 
#
#> rsync -avP ../lib/jibx-tools-1.2.5.jar  10.111.1.2:euca_builder/eee/eucalyptus/devel/jibx-tools-1.2.5.jar
#
# 
# For generating the source code back from the XSDs use:
#	../devel/org.eclipse.equinox.common_3.6.0.v20110523.jar
#	../devel/org.eclipse.jdt.core_3.6.2.v_A76_R36x.jar
#	../devel/org.eclipse.text_3.5.200.v20120523-1310.jar
#> export CLASSPATH=$(ls -1 ../devel/qdox-1.5.jar ../devel/org.eclipse.* lib/*.jar | awk '{printf($1":")}')
#> java -classpath ../devel/jibx-tools-1.2.5.jar:${CLASSPATH} \
# 	org.jibx.schema.codegen.CodeGen \
#	-n schemas/autoscaling-2011-01-01-binding.xml \
#	-m test.xml \
# 	schemas/autoscaling-2011-01-01.xsd
DEVELDIR=$(cd $(dirname ${BASH_SOURCE:-$0}); pwd -P)
CLCDIR=$(dirname ${DEVELDIR})/clc
OUTDIR=${CLCDIR}/schemas
EUCADIR=${EUCALYPTUS:-/}
if [[ ! -e ${EUCADIR}/var/run/eucalyptus/classcache/msgs-binding.xml ]]; then
	echo '${EUCALYPTUS}/var/run/eucalyptus/classcache/ does not exist or is not populated with bound classes.'
	exit 1
fi
rm -rf ${CLCDIR}/genschema
mkdir -p ${CLCDIR}/genschema
cp -Rf ${EUCADIR}/var/run/eucalyptus/classcache/* ${CLCDIR}/genschema/
for f in \
	edu/ucsb/eucalyptus/msgs/BaseData.class \
	com/eucalyptus/util/HasSideEffect.class \
	com/eucalyptus/binding/Binding.class \
	edu/ucsb/eucalyptus/msgs/ModifyInstanceAttributeType\$Attr.class \
	com/eucalyptus/objectstorage/BucketLogData.class \
	com/eucalyptus/ws/util/SerializationUtils.class
do
	mkdir -p ${CLCDIR}/genschema/$(dirname $f)
	cp -f ${CLCDIR}/modules/*/build/$f ${CLCDIR}/genschema/$(dirname $f)/
done

#rm -fv ${CLCDIR}/genschema/msgs-binding.xml
ls -1 ${CLCDIR}/modules/*/src/main/resources/*.xml  | xargs -i grep -l '<binding' {} | xargs -i cp -f {}  ${CLCDIR}/genschema/
sed -i '
:a;N;$!ba
s/\n/ /g
s/[ \t][ \t]*/ /g
s/>/>\n/g
s/extends="[^"]*"//g
s/<structure map-as="[^"]*Message" \/>//g
s/<structure map-as="[^"]*Type" \/>//g
s/<structure map-as="[^"]*Message" usage="optional" \/>//g
s/<structure map-as="[^"]*Type" usage="optional" \/>//g
' ${CLCDIR}/genschema/*.xml

rm -rf ${OUTDIR}
CLASSPATH=\
$(readlink -f ${CLCDIR}/../devel/jibx-tools-1.2.5.jar):\
$(readlink -f ${CLCDIR}/../devel/qdox-1.5.jar):\
${CLCDIR}/genschema:\
$(ls -1 ${CLCDIR}/lib/*.jar | awk '{printf($1":")}')
# echo ${CLASSPATH}
for BINDING in ${CLCDIR}/genschema/*-binding.xml; do
	echo Processing ${BINDING}
	NAMESPACE=$(grep -- '<namespace uri' ${BINDING}  | sed 's/.*uri="\([^"]*\)".*/\1/g')
	FILENAME=$(echo ${NAMESPACE} | sed  's/\/$//g;s/http[s]*:\/\/\([^.]*\)[^/]*\//\1#/g;s/#.*\//-/g;s/http:\/\/\([^.]*\)\..*/internal-\1/g').xsd
	CMD="org.jibx.schema.generator.SchemaGen \
		-v \
		-p ${CLCDIR}/genschema/ \
		-n ${NAMESPACE}=${FILENAME},""=${FILENAME} \
		-t ${OUTDIR} \
		${BINDING}"
#	echo java -classpath '${CLASSPATH}' ${CMD} 
	java -classpath ${CLASSPATH} ${CMD} 
done
