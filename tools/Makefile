# Makefile for eucalyptus/tools
#
# Copyright 2009-2013 Eucalyptus Systems, Inc.
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

all:
	@make -C imaging

build: all

deploy:

clean:
	$(RM) -f *~ *.o
	@make -C imaging clean

distclean:
	@echo "removing generated files scripts ..."
	@rm -rf eucalyptus eucalyptus-cloud eucalyptus-walrus eucalyptus-sc eucalyptus-nc
	@make -C imaging distclean

install: build
	@if test -e /etc/init.d/eucalyptus ; then \
		mv /etc/init.d/eucalyptus /etc/init.d/eucalyptus.old ; \
		echo "Moving away old init.d script"; fi
	@if test -e $(DESTDIR)$(etcdir)/init.d/eucalyptus ; then \
		mv $(DESTDIR)$(etcdir)/init.d/eucalyptus $(DESTDIR)$(etcdir)/init.d/eucalyptus.old ; fi
	@$(INSTALL) -m 755 eucalyptus-cloud $(DESTDIR)$(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-nc $(DESTDIR)$(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-cc $(DESTDIR)$(etcdir)/init.d
	@$(INSTALL) -m 644 eucalyptus-logrotate.conf $(DESTDIR)$(etcdir)/logrotate.d/eucalyptus
	@$(INSTALL) -m 755 eucanetd $(DESTDIR)$(etcdir)/init.d
	@$(INSTALL) -m 644 httpd.conf $(DESTDIR)$(etcdir)/eucalyptus
	@if test -e $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf ; then \
		cp $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf.old ; fi
	@$(INSTALL) -m 644 eucalyptus.conf $(DESTDIR)$(etcdir)/eucalyptus
	@$(INSTALL) -m 644 vtunall.conf.template $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_libvirt_xml $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_kvm_libvirt_xml $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 detach.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 partition2disk $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 get_xen_info $(DESTDIR)$(datarootdir)/eucalyptus/get_xen_info
	@$(INSTALL) -m 755 get_sys_info $(DESTDIR)$(datarootdir)/eucalyptus/get_sys_info
	@$(INSTALL) -m 755 euca_ipt $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 populate_arp.pl $(DESTDIR)$(datarootdir)/eucalyptus
#	@$(INSTALL) -m 755 euca_conf $(DESTDIR)$(sbindir)
#	-$(INSTALL) -m 755 euca_conf.complete $(DESTDIR)$(etcdir)/bash_completion.d/euca_conf
#	@$(INSTALL) -m 755 eucanetd $(DESTDIR)$(sbindir)
	@$(INSTALL) -m 755 connect_iscsitarget.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 connect_iscsitarget_sc.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 connect_iscsitarget_main.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 disconnect_iscsitarget.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 disconnect_iscsitarget_sc.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 disconnect_iscsitarget_main.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 get_iscsitarget.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 iscsitarget_common.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 644 floppy $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 dynserv.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 644 libvirt-kvm-windows-example.xml $(DESTDIR)$(datarootdir)/eucalyptus/doc/
	@$(INSTALL) -m 644 libvirt-xen-windows-example.xml $(DESTDIR)$(datarootdir)/eucalyptus/doc/
	@$(INSTALL) -m 755 getstats.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 getstats_net.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 get_bundle $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 euca-lictool $(DESTDIR)$(sbindir)
	@$(INSTALL) -m 755 lic_template $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 lic_default $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 644 libvirt.xsl $(DESTDIR)$(etcdir)/eucalyptus
	@$(INSTALL) -m 755 create-loop-devices $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 644 nc-hooks/example.sh $(DESTDIR)$(etcdir)/eucalyptus/nc-hooks/
	@$(INSTALL) -m 755 generate-migration-keys.sh $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 authorize-migration-keys.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 conntrack_kernel_params $(DESTDIR)$(libexecdir)/eucalyptus
	@$(INSTALL) -m 755 euca-upgrade $(DESTDIR)$(libexecdir)/eucalyptus
	@$(INSTALL) -m 644 iptables-preload.template $(DESTDIR)$(datarootdir)/eucalyptus/doc/
	@make -C imaging install

uninstall:
	@$(RM) -f $(DESTDIR)$(etcdir)/init.d/eucalyptus-cloud
	@$(RM) -f $(DESTDIR)$(etcdir)/init.d/eucalyptus-nc
	@$(RM) -f $(DESTDIR)$(etcdir)/init.d/eucalyptus-cc
	@$(RM) -f $(DESTDIR)$(etcdir)/init.d/eucanetd
	@$(RM) -f $(DESTDIR)$(etcdir)/logrotate.d/eucalyptus
	@$(RM) -f $(DESTDIR)$(etcdir)/eucalyptus/httpd*
	@$(RM) -f $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/vtunall.conf.template
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/gen_libvirt_xml
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/gen_kvm_libvirt_xml
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/detach.pl
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/partition2disk
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/get_xen_info
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/get_sys_info
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/euca_ipt
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/populate_arp.pl
	@$(RM) -f $(DESTDIR)$(sbindir)/euca_conf
	@$(RM) -f $(DESTDIR)$(etcdir)/bash_completion.d/euca_conf
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/lic_template
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/lic_default
	@$(RM) -f $(DESTDIR)$(sbindir)/euca-lictool
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/create-loop-devices
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/generate-migration-keys.sh
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/authorize-migration-keys.pl
	@$(RM) -f $(DESTDIR)$(libexecdir)/eucalyptus/conntrack_kernel_params
	@$(RM) -f $(DESTDIR)$(libexecdir)/eucalyptus/euca-upgrade
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/doc/iptables-preload.template
	@make -C imaging uninstall
