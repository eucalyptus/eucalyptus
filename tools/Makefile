# Makefile for eucalyptus/tools
#
# $Id: Makefile,v 1.6 2008-12-24 23:55:15 nurmi Exp $
#

include ../Makedefs

all:
	@echo nothing to do here

build: all

deploy:

clean:
	rm -f *~ *.o

distclean:
	@rm -rf eucalyptus

install: deploy
	@$(INSTALL) -m 755 eucalyptus $(etcdir)/init.d
	@$(INSTALL) -m 644 httpd.conf $(etcdir)/eucalyptus
	@if test -e $(etcdir)/eucalyptus/eucalyptus.conf ; then \
		cp $(etcdir)/eucalyptus/eucalyptus.conf $(etcdir)/eucalyptus/eucalyptus.conf.old ; fi
	@$(INSTALL) -m 644 eucalyptus.conf $(etcdir)/eucalyptus
	@$(INSTALL) -m 755 add_key.pl $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_libvirt_xml $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_kvm_libvirt_xml $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 partition2disk $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 get_xen_info $(datarootdir)/eucalyptus/get_xen_info
	@$(INSTALL) -m 755 get_sys_info $(datarootdir)/eucalyptus/get_sys_info
	@$(INSTALL) -m 755 euca_ipt $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 populate_arp.pl $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 euca_conf $(sbindir)
	@$(INSTALL) -m 755 euca_sync_key $(sbindir)

