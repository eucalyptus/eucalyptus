# Makefile for eucalyptus/util
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

EFENCE=-lefence
#DEBUGS = -DDEBUG # -DDEBUG1
all: euca_string.o euca_file.o utf8.o log.o config.o fault.o misc.o wc.o hash.o data.o sensor.o euca_auth.o euca_axis.o ipc.o sequence_executor.o atomic_file.o euca_rootwrap euca_mountwrap euca-generate-fault 

build: all

euca_rootwrap: euca_rootwrap.c euca_string.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -o euca_rootwrap euca_rootwrap.c euca_string.o

euca_mountwrap: euca_mountwrap.c euca_string.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -o euca_mountwrap euca_mountwrap.c euca_string.o

test: test.c ipc.o log.o misc.o ../storage/diskutil.o euca_string.o euca_file.o data.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -o test test.c ipc.o log.o misc.o ../storage/diskutil.o euca_string.o euca_file.o data.o -lpthread $(LIBS) $(LDFLAGS)

test_misc: misc.c euca_string.o euca_file.o log.o ../storage/diskutil.o ipc.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) -D_UNIT_TEST -o test_misc misc.c euca_string.o euca_file.o log.o ../storage/diskutil.o ipc.o $(LIBS) $(LDFLAGS)

test_wc: wc.c misc.o euca_string.o euca_file.o log.o ../storage/diskutil.o ipc.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUGS) -D_UNIT_TEST -o test_wc wc.c misc.o euca_string.o euca_file.o log.o ../storage/diskutil.o ipc.o $(LIBS) $(LDFLAGS)

test_fault: fault.c misc.o euca_string.o euca_file.o log.o wc.o ../storage/diskutil.o ipc.o utf8.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) `xslt-config --cflags` $(DEBUGS) -D_UNIT_TEST -o test_fault fault.c misc.o euca_string.o euca_file.o log.o wc.o ../storage/diskutil.o ipc.o utf8.o -lpthread -lxml2 $(LDFLAGS)

euca-generate-fault: fault.c misc.o euca_string.o euca_file.o log.o wc.o ../storage/diskutil.o ipc.o utf8.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) `xslt-config --cflags` $(DEBUGS) -DEUCA_GENERATE_FAULT -o euca-generate-fault fault.c misc.o euca_string.o euca_file.o log.o wc.o ../storage/diskutil.o ipc.o utf8.o -lpthread -lxml2 $(LDFLAGS)

test_sensor: sensor.c sensor.h misc.o euca_string.o euca_file.o log.o ipc.o ../storage/diskutil.o
	$(CC) $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUG) -D_UNIT_TEST -o test_sensor sensor.c misc.o euca_string.o euca_file.o log.o ../storage/diskutil.o ipc.o $(LIBS) $(LDFLAGS) $(EFENCE)

../storage/diskutil.o:
	make -C ../storage

test_auth: euca_auth.c euca_string.o euca_file.o log.o misc.o ipc.o ../storage/diskutil.o
	$(CC) $(CFLAGS) $(INCLUDES) $(DEBUGS) -trigraphs -D_UNIT_TEST -o test_auth euca_auth.c euca_string.o euca_file.o log.o misc.o ../storage/diskutil.o ipc.o $(LIBS) $(LDFLAGS) $(EFENCE) -lcurl

%.o: %.c %.h
	$(CC) -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $(DEBUGS) -trigraphs `xslt-config --cflags` $<

indent:
	@for idfile in *.[ch]; do \
		echo $(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
		$(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
	done

clean:
	rm -rf *~ *.o test test_fault euca-generate-fault test_misc test_wc euca_rootwrap euca_mountwrap test_sensor

distclean:
	rm -rf eucalyptus-config.h

install: all
	$(INSTALL) -m 0755 euca_rootwrap $(DESTDIR)$(usrdir)/lib/eucalyptus/
	$(INSTALL) -m 0755 euca_mountwrap $(DESTDIR)$(usrdir)/lib/eucalyptus/
	$(INSTALL) -m 0755 euca-generate-fault $(DESTDIR)$(usrdir)/sbin/
	$(INSTALL) -d $(DESTDIR)$(usrdir)/share/eucalyptus/faults/en_US/
	$(INSTALL) -m 0644 faults/en_US/common.xml $(DESTDIR)$(usrdir)/share/eucalyptus/faults/en_US/
	$(INSTALL) -m 0644 -v faults/en_US/????.xml $(DESTDIR)$(usrdir)/share/eucalyptus/faults/en_US/
	$(INSTALL) -d $(DESTDIR)$(etcdir)/eucalyptus/faults/en_US/

deploy:

uninstall:
	$(RM) -f $(DESTDIR)$(usrdir)/lib/eucalyptus/euca_rootwrap
	$(RM) -f $(DESTDIR)$(usrdir)/lib/eucalyptus/euca_mountwrap
	$(RM) -f $(DESTDIR)$(usrdir)/share/eucalyptus/faults/en_US/*.xml
