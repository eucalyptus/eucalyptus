#
# Makefile for vmware-broker
#

include ../Makedefs

all:
	$(MAKE) -C imager all

clean:
	$(MAKE) -C imager clean

distclean:
	$(MAKE) -C imager distclean

install:
	$(MAKE) -C imager install
	@$(INSTALL) -m 755 euca_vmware $(DESTDIR)$(datarootdir)/eucalyptus

deploy:
	$(MAKE) -C imager deploy

uninstall:
	$(MAKE) -C imager uninstall
	@$(RM) -f $(DESTDIR)$(datarootdir)/eucalyptus/euca_vmware

build: all
