# top-level Eucalyptus makefile
#
#

include Makedefs

# notes: storage has to preceed node and node has to preceed cluster
SUBDIRS			=	tools \
				util \
				net \
				storage	 \
				gatherlog \
				node  \
				cluster \
				clc

.PHONY: all clean distclean build 

all: build

help:
	@echo; echo "Available targets:"
	@echo "   all          this is the default target: it builds eucalyptus"
	@echo "   install      install eucalyptus"
	@echo "   clean        remove objects file and compile by-products"
	@echo "   distclean    restore the source tree to a pristine state"
	@echo 


tags:
	@echo making tags for emacs and vi
	find cluster net node storage tools util -name "*.[chCH]" -print | ctags -L -
	find cluster net node storage tools util -name "*.[chCH]" -print | etags -L -

build: Makedefs 
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) $@) || exit $$? ; done

deploy: build
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) $@) || exit $$? ; done

install: deploy
	@$(INSTALL) -d $(prefix)
	@$(INSTALL) -d $(etcdir)/eucalyptus/cloud.d
	@$(INSTALL) -m 0644 VERSION $(etcdir)/eucalyptus/eucalyptus-version
	@$(INSTALL) -d $(etcdir)/init.d
	@$(INSTALL) -d $(vardir)/run/eucalyptus/net
	@$(INSTALL) -d $(vardir)/lib/eucalyptus/keys
	@$(INSTALL) -d $(vardir)/lib/eucalyptus/CC
	@$(INSTALL) -d $(vardir)/log/eucalyptus
	@$(INSTALL) -d $(datarootdir)/eucalyptus
	@$(INSTALL) -d $(usrdir)/sbin
	@$(INSTALL) -d $(usrdir)/lib/eucalyptus
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) $@) || exit $$? ; done

clean:
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) $@) || exit $$? ; done

distclean: clean
	@for subdir in $(SUBDIRS); do \
		(cd $$subdir && $(MAKE) $@) || exit $$? ; done
	@rm -f config.cache config.log config.status Makedefs tags TAGS
	@# they where part of CLEAN
	@rm -rf lib 

# the following target is used to remove eucalyptuys from your system
uninstall:
	@echo something to do here


Makedefs: Makedefs.in config.status
	./config.status

config.status: configure
	@if test ! -x ./config.status; then \
		echo "you have to run ./configure!"; exit 1; fi
	./config.status --recheck

# DO NOT DELETE
