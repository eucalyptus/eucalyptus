/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.msgs;

import java.nio.ByteBuffer;


public class ObjectStorageDataMessage {
    private Header header;
    private byte[] payload;
    private static final String DELIMITER = "/";

    public enum Header {
        START, DATA, INTERRUPT, EOF
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public ObjectStorageDataMessage(Header header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    public ObjectStorageDataMessage() {
    }

    public static ObjectStorageDataMessage EOF() {
        return new ObjectStorageDataMessage(Header.EOF, String.valueOf(System.currentTimeMillis()).getBytes());
    }

    public static ObjectStorageDataMessage InterruptTransaction() {
        return new ObjectStorageDataMessage(Header.INTERRUPT, new byte[0]);
    }

    public static ObjectStorageDataMessage StartOfData(long size) {
        return new ObjectStorageDataMessage(Header.START, String.valueOf(size).getBytes());
    }

    public static ObjectStorageDataMessage DataMessage(byte[] data) {
        return new ObjectStorageDataMessage(Header.DATA, data);
    }

    public static ObjectStorageDataMessage DataMessage(byte[] data, int length) {
        byte[] bytes = new byte[length];
        copyBytes(data, bytes, 0, length);
        return new ObjectStorageDataMessage(Header.DATA, bytes);
    }

    public static ObjectStorageDataMessage DataMessage(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes, 0, length);
        return new ObjectStorageDataMessage(Header.DATA, bytes);
    }


    public static boolean isStart(ObjectStorageDataMessage message) {
        if (Header.START.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static boolean isEOF(ObjectStorageDataMessage message) {
        if (Header.EOF.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static boolean isInterrupted(ObjectStorageDataMessage message) {
        if (Header.INTERRUPT.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static boolean isData(ObjectStorageDataMessage message) {
        if (Header.DATA.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static void copyBytes(byte[] sourceBytes, byte[] destBytes, int offset, int length) {
        System.arraycopy(sourceBytes, 0, destBytes, offset, length);
    }
}
