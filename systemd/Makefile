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

install: deploy
ifeq ($(USE_SYSTEMD), yes)
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/modules-load.d
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/systemd/system
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/sysctl.d
	mkdir -p $(DESTDIR)/$(prefix)/usr/lib/tmpfiles.d
	install -m 0644 modules-load.d/*.conf $(DESTDIR)/$(prefix)/usr/lib/modules-load.d
	install -m 0644 sysctl.d/*.conf $(DESTDIR)/$(prefix)/usr/lib/sysctl.d
	install -m 0644 tmpfiles.d/*.conf $(DESTDIR)/$(prefix)/usr/lib/tmpfiles.d
	install -m 0644 units/*.service $(DESTDIR)/$(prefix)/usr/lib/systemd/system
endif
