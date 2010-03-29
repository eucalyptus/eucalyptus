# Makefile for eucalyptus/tools
#
#

include ../Makedefs

all:

build: all

deploy:

clean:
	$(RM) -f *~ *.o

distclean:
	@echo "removing generated files scripts ..."
	@rm -rf eucalyptus eucalyptus-cloud eucalyptus-walrus eucalyptus-sc

install: build
	@if test -e /etc/init.d/eucalyptus ; then \
		mv /etc/init.d/eucalyptus /etc/init.d/eucalyptus.old ; \
		echo "Moving away old init.d script"; fi
	@if test -e $(DESTDIR)$(etcdir)/init.d/eucalyptus ; then \
		mv $(DESTDIR)$(etcdir)/init.d/eucalyptus $(DESTDIR)$(etcdir)/init.d/eucalyptus.old ; fi
	@$(INSTALL) -m 755 eucalyptus-cloud $(DESTDIR)$(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-nc $(DESTDIR)$(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-cc $(DESTDIR)$(etcdir)/init.d
	@$(INSTALL) -m 644 httpd.conf $(DESTDIR)$(etcdir)/eucalyptus
	@if test -e $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf ; then \
		cp $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf.old ; fi
	@$(INSTALL) -m 644 eucalyptus.conf $(DESTDIR)$(etcdir)/eucalyptus
	@$(INSTALL) -m 644 vtunall.conf.template $(DESTDIR)$(etcdir)/eucalyptus
	@$(INSTALL) -m 755 add_key.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_libvirt_xml $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_kvm_libvirt_xml $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 detach.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 partition2disk $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 get_xen_info $(DESTDIR)$(datarootdir)/eucalyptus/get_xen_info
	@$(INSTALL) -m 755 get_sys_info $(DESTDIR)$(datarootdir)/eucalyptus/get_sys_info
	@$(INSTALL) -m 755 euca_ipt $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 euca_upgrade $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 populate_arp.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 euca_conf $(DESTDIR)$(sbindir)
	-$(INSTALL) -m 755 euca_conf.complete $(DESTDIR)/etc/bash_completion.d/euca_conf
	@$(INSTALL) -m 755 euca_sync_key $(DESTDIR)$(sbindir)
	@$(INSTALL) -m 755 connect_iscsitarget.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 disconnect_iscsitarget.pl $(DESTDIR)$(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 get_iscsitarget.pl $(DESTDIR)$(datarootdir)/eucalyptus

uninstall: 
	@$(RM) -f $(DESTDIR)$(etcdir)/init.d/eucalyptus-cloud
	@$(RM) -f $(DESTDIR)$(etcdir)/init.d/eucalyptus-nc
	@$(RM) -f $(DESTDIR)$(etcdir)/init.d/eucalyptus-cc
	@$(RM) -f $(DESTDIR)$(etcdir)/eucalyptus/httpd*
	@$(RM) -f $(DESTDIR)$(etcdir)/eucalyptus/eucalyptus.conf
	@$(RM) -f $(DESTDIR)$(etcdir)/eucalyptus/vtunall.conf.template
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/add_key.pl
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/gen_libvirt_xml
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/gen_kvm_libvirt_xml
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/detach.pl
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/partition2disk
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/get_xen_info
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/get_sys_info
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/euca_ipt
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/euca_upgrade
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/populate_arp.pl
	@$(RM) -f $(DESTDIR)$(sbindir)/euca_conf
	@$(RM) -f $(DESTDIR)/etc/bash_completion.d/euca_conf
	@$(RM) -f $(DESTDIR)$(sbindir)/euca_sync_key

