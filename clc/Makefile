# Makefile wrapper around ant
#
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
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

include ../Makedefs

theme_dir	= ${TOP}/clc/modules/www/src/main/java/com/eucalyptus/webui/public/themes
web_prop_dir	= ${TOP}/clc/modules/www/conf

.PHONY: all build update_properties update_theme deps extras install

all: deps build
test: deps build-test

lib/bcprov.jar:
	@mkdir -p ${TOP}/clc/lib
	@git clone --depth=1 git://github.com/eucalyptus/eucalyptus-cloud-libs.git lib

deps: lib/bcprov.jar

update_properties: ${web_prop_dir}/eucalyptus-web.properties

${web_prop_dir}/eucalyptus-web.properties: ${web_prop_dir}/eucalyptus-web-default.properties ${TOP}/Makedefs
	@sed "s/\(^extensions =\).*/\1 ${extensions}/" < ${web_prop_dir}/eucalyptus-web-default.properties > ${web_prop_dir}/eucalyptus-web.properties
	@echo "Copied defaults to ${web_prop_dir}/eucalyptus-web.properties"

update_theme:
	@if test -d ${theme_dir}/${euca_theme} ; then \
		if [ "`readlink ${theme_dir}/active`" != "${euca_theme}" ] ; then \
			echo "Making '${euca_theme}' the active theme" ; \
			rm -f ${theme_dir}/active ; \
			ln -sf ${euca_theme} ${theme_dir}/active ; \
		fi \
	else \
		echo "Theme ${theme_dir}/${euca_theme} is not found!"; exit 1; \
	fi \

extras: update_properties
	@if test -n "${euca_theme}" ; then \
		$(MAKE) update_theme; \
	else \
		if [ "`readlink ${theme_dir}/active`" != "eucalyptus" ] ; then \
			echo "Making 'eucalyptus' the active theme" ; \
			rm -f ${theme_dir}/active ; \
			ln -sf eucalyptus ${theme_dir}/active ; \
		fi \
	fi

build: deps extras
	@# we need JAVA_HOME
	@if test -z "${JAVA_HOME}" ; then \
		echo "You need to define JAVA_HOME!"; exit 1; fi
	@ANT_OPTS="-Xmx1g -XX:MaxPermSize=512m" $(ANT) build-all
	@ANT_OPTS="-Xmx1g -XX:MaxPermSize=512m" $(ANT) jar
	@make -C eucadmin

build-test: deps extras
	@# we need JAVA_HOME
	@if test -z "${JAVA_HOME}" ; then \
		echo "You need to define JAVA_HOME!"; exit 1; fi
	@ANT_OPTS="-Xmx1g -XX:MaxPermSize=512m" $(ANT) build-test
	@ANT_OPTS="-Xmx1g -XX:MaxPermSize=512m" $(ANT) jar
	@make -C eucadmin


clean: deps
	$(ANT) clean
	make -C eucadmin clean

distclean: clean
	rm -rf ${TOP}/deps ${TOP}/lib
	$(ANT) distclean
	make -C eucadmin distclean

install: build
	@ANT_OPTS="-Xmx1g -XX:MaxPermSize=512m" $(ANT) install
	@make -C eucadmin install

install-test: build-test
	@ANT_OPTS="-Xmx1g -XX:MaxPermSize=256m" $(ANT) install
	@make -C eucadmin install


deploy: install

uninstall:
	make -C eucadmin uninstall
	$(RM) -rf $(DESTDIR)$(etcdir)/eucalyptus/cloud.d
	$(RM) -rf $(DESTDIR)$(datarootdir)/eucalyptus/*jar
	$(RM) -rf $(DESTDIR)$(vardir)/eucalyptus/webapps

# cleaning up after run-test currently requires a few iterations of:
#   dmsetup table | cut -d':' -f 1 | sort | uniq | xargs -L 1 dmsetup remove ; losetup -a | cut -d':' -f 1 | xargs -L 1 losetup -d; losetup -a
