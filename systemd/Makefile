# Copyright 2016 Ent. Services Development Corporation LP
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

include ../Makedefs

.PHONY: build deploy install

# Appending /usr to $prefix is a problem common to all eucalyptus makefiles

build:

deploy:

clean:

distclean: clean

install: deploy
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/modules-load.d
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/sysctl.d
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/tmpfiles.d
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/systemd/system
	install -m 0644 modules-load.d/*.conf $(DESTDIR)/$(prefix)/usr/lib/modules-load.d
	install -m 0644 sysctl.d/*.conf $(DESTDIR)/$(prefix)/usr/lib/sysctl.d
	install -m 0644 tmpfiles.d/*.conf $(DESTDIR)/$(prefix)/usr/lib/tmpfiles.d
	install -m 0644 units/*.service $(DESTDIR)/$(prefix)/usr/lib/systemd/system
	# For backwards compatibility with ``service eucalyptus-cc/nc start''
	ln -fs eucalyptus-cluster.service $(DESTDIR)/$(prefix)/usr/lib/systemd/system/eucalyptus-cc.service
	ln -fs eucalyptus-node.service $(DESTDIR)/$(prefix)/usr/lib/systemd/system/eucalyptus-nc.service

uninstall:
# Extra "if" checks prevent e.g. "rm -f /..../system/*.service" removing files we didn't install.
	@if [ -n "$$(/bin/ls modules-load.d/*.conf)" ]; then \
		for conf_file in modules-load.d/*.conf; do \
			rm -f $(DESTDIR)/$(prefix)/usr/lib/$$conf_file; \
		done; \
	fi
	@if [ -n "$$(/bin/ls sysctl.d/*.conf)" ]; then \
		for conf_file in sysctl.d/*.conf; do \
			rm -f $(DESTDIR)/$(prefix)/usr/lib/$$conf_file; \
		done; \
	fi
	@if [ -n "$$(/bin/ls tmpfiles.d/*.conf)" ]; then \
		for conf_file in tmpfiles.d/*.conf; do \
			rm -f $(DESTDIR)/$(prefix)/usr/lib/$$conf_file; \
		done; \
	fi
	@if [ -n "$$(/bin/ls units/*.service)" ]; then \
		for unit_file in units/*.service; do \
			rm -f $(DESTDIR)/$(prefix)/usr/lib/systemd/system/$$(basename $$unit_file); \
		done; \
	fi
	@$(RM) -f $(DESTDIR)/$(prefix)/usr/lib/systemd/system/eucalyptus-cc.service
	@$(RM) -f $(DESTDIR)/$(prefix)/usr/lib/systemd/system/eucalyptus-nc.service
