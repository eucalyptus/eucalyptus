# Makefile for eucalyptus/net
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

# Standard Libraries, Dependencies and Includes
STDLIBS      := -lpthread -lm -lssl -lxml2 -lcurl -lcrypto -ljson
STDDEPS      := ../util/sequence_executor.o ../util/atomic_file.o ../util/log.o ../util/ipc.o ../util/misc.o  
STDDEPS      += ../util/euca_string.o ../util/euca_file.o ../util/hash.o ../util/fault.o ../util/wc.o ../util/utf8.o  
STDDEPS      += ../util/euca_auth.o ../storage/diskutil.o ../storage/http.o ../util/config.o ../util/euca_network.o
STDINC       +=
 
# The Eucalyptus Network Library
LIBNET       := euca_lni euca_gni ipt_handler ips_handler ebt_handler dev_handler
LIBNETOBJS   := $(LIBNET:=.o)
LIBNETDEPS   := $(LIBNETOBJS) $(STDDEPS)
LIBNETNAME   := libeucanet.a

# The EUCANETD Cloud Component
EUCANETD     := eucanetd eucanetd_util eucanetd_edge eucanetd_managed eucanetd_managednv 
EUCANETD     += eucanetd_system eucanetd_static eucanetd_vpc midonet-api euca-to-mido
EUCANETDOBJS := $(EUCANETD:=.o)
EUCANETDDEPS := $(EUCANETDOBJS) $(LIBNETNAME) $(STDDEPS)
EUCANETDNAME := eucanetd

# Various global Makefile parameters
TARGETS      := $(LIBNETNAME) $(EUCANETDNAME)  
MAJOR        := 0
AR           := /usr/bin/ar
ARFLAGS      := -cq
CFLAGS       += `xslt-config --cflags`

all: $(TARGETS)

build: all

$(LIBNETNAME): $(LIBNETDEPS)
	$(AR) $(ARFLAGS) $@ $(LIBNETOBJS)

$(EUCANETDNAME): $(EUCANETDDEPS)
	$(CC) -o $@ $(EUCANETDDEPS) $(LIBNETNAME) $(STDLIBS)

.c.o:
	$(CC) -c $(CPPFLAGS) $(CFLAGS) $(INCLUDES) $<

clean:
	@rm -rf *~ *.o *.a $(LIBNETNAME) $(EUCANETDNAME)

distclean: clean

install:
	@$(INSTALL) -d $(DESTDIR)$(sbindir)
	@$(INSTALL) -m 755 $(EUCANETDNAME) $(DESTDIR)$(sbindir)

deploy:
	@$(INSTALL) -d $(DESTDIR)$(sbindir)
	@$(INSTALL) -m 755 eucanetd $(DESTDIR)$(sbindir)

uninstall:
	@$(RM) -f $(DESTDIR)$(sbindir)/$(EUCANETDNAME)

indent:
	@for idfile in *.[ch]; do \
		echo $(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
		$(INDENTTOOLS) $$idfile $(INDENTFLAGS) -o $$idfile ; \
	done

