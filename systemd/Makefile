# (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

include ../Makedefs

.PHONY: build deploy install

# Appending /usr to $prefix is a problem common to all eucalyptus makefiles

build:

deploy:

clean:

distclean: clean

install: deploy
ifeq ($(USE_SYSTEMD), yes)
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
endif

uninstall:
ifeq ($(USE_SYSTEMD), yes)
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
endif
