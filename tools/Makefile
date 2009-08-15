# Makefile for eucalyptus/tools
#
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
	@if test -e /etc/init.d/eucalyptus ; then \
		mv /etc/init.d/eucalyptus /etc/init.d/eucalyptus.old ; \
		echo "Moving away old init.d script"; fi
	@if test -e $(etcdir)/init.d/eucalyptus ; then \
		mv $(etcdir)/init.d/eucalyptus $(etcdir)/init.d/eucalyptus.old ; fi
	@$(INSTALL) -m 755 eucalyptus-cloud $(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-walrus $(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-sc $(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-nc $(etcdir)/init.d
	@$(INSTALL) -m 755 eucalyptus-cc $(etcdir)/init.d
	@$(INSTALL) -m 644 httpd.conf $(etcdir)/eucalyptus
	@if test -e $(etcdir)/eucalyptus/eucalyptus.conf ; then \
		cp $(etcdir)/eucalyptus/eucalyptus.conf $(etcdir)/eucalyptus/eucalyptus.conf.old ; fi
	@$(INSTALL) -m 644 eucalyptus.conf $(etcdir)/eucalyptus
	@$(INSTALL) -m 644 vtunall.conf.template $(etcdir)/eucalyptus
	@$(INSTALL) -m 755 add_key.pl $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_libvirt_xml $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 gen_kvm_libvirt_xml $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 detach.pl $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 partition2disk $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 get_xen_info $(datarootdir)/eucalyptus/get_xen_info
	@$(INSTALL) -m 755 get_sys_info $(datarootdir)/eucalyptus/get_sys_info
	@$(INSTALL) -m 755 euca_ipt $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 populate_arp.pl $(datarootdir)/eucalyptus
	@$(INSTALL) -m 755 euca_conf $(sbindir)
	@$(INSTALL) -m 755 euca_sync_key $(sbindir)

