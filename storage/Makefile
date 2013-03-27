# Makefile for eucalyptus/storage
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

EUCA_BLOBS_OBJS =                                     diskutil.o map.o                ../util/log.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o
WCLIENT_OBJS    =                     walrus.o http.o diskutil.o map.o                ../util/log.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o
TEST_BLOB_OBJS  =                                     diskutil.o map.o                ../util/log.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o
TEST_VBR_OBJS   = iscsi.o blobstore.o walrus.o http.o diskutil.o       ../util/hash.o ../util/log.o ../util/misc.o ../util/ipc.o ../util/euca_auth.o

STORAGE_LIBS    = $(LDFLAGS) -lcurl -lssl -lcrypto -pthread -lpthread
TESTS           = test_vbr test_blobstore
#EFENCE          = -lefence

all: vbr.o backing.o storage-windows.o walrus.o diskutil.o map.o Wclient euca-blobs $(TESTS)

build: all

euca-blobs: Makefile blobstore.c blobstore.h $(EUCA_BLOBS_OBJS)
	$(CC) $(CFLAGS) $(INCLUDES) -D_EUCA_BLOBS blobstore.c -o euca-blobs $(EUCA_BLOBS_OBJS) $(STORAGE_LIBS) $(EFENCE)

Wclient: Makefile Wclient.c $(WCLIENT_OBJS)
	$(CC) $(CFLAGS) $(INCLUDES) Wclient.c -o Wclient $(WCLIENT_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_blobstore: blobstore.o $(TEST_BLOB_OBJS)
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST blobstore.c -o test_blobstore $(TEST_BLOB_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_vbr: vbr.o $(TEST_VBR_OBJS)
	$(CC) -g -rdynamic $(CFLAGS) $(INCLUDES) -D_UNIT_TEST vbr.c -o test_vbr $(TEST_VBR_OBJS) $(STORAGE_LIBS) $(EFENCE)

test_url: http.c
	$(CC) -D_UNIT_TEST -o test_url http.c

%.o: %.c %.h
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

../util/ipc.o: ../util/ipc.c ../util/ipc.h ../util/eucalyptus.h
	make -C ../util

../util/misc.o: ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/hash.o: ../util/hash.c ../util/hash.h ../util/misc.c ../util/misc.h ../util/eucalyptus.h
	make -C ../util

../util/data.o: ../util/data.c ../util/data.h ../util/eucalyptus.h
	make -C ../util

../net/vnetwork.o: ../net/vnetwork.c
	make -C ../net

../util/euca_auth.o: ../util/euca_auth.c ../util/euca_auth.h
	make -C ../util

indent:
	@indent *.[ch] -nbad -bap -nbc -bbo -hnl -br -brs -c33 -cd33 -ncdb -ce -ci4 -cli0 -d0 -di1 -nfc1 -i4 -ut -ip0 -l200 -lp -npcs -nprs -npsl -sai -saf -saw -ncs -nsc -sob -nfca -ss -ts4 -il0 -nip
	@\rm *.*~

clean:
	rm -rf *~ *.o Wclient euca-blobs $(TESTS)

distclean:

install:

deploy:

uninstall:

