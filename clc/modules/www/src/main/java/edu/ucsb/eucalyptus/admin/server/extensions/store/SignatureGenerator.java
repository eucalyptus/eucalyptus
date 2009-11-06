/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
package edu.ucsb.eucalyptus.admin.server.extensions.store;

import java.io.UnsupportedEncodingException;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;


public class SignatureGenerator {

    private final TreeMap<String,List<String>> parameters = new TreeMap<String,List<String>>();
    private final String method;
    private final String host;
    private final String path;

    private final String ALGORITHM = "HMacSHA256";

    public SignatureGenerator(String method, String host, int port, String path) {
        this.method = method.toUpperCase();
        if (port == 80) {
            this.host = host.toLowerCase();
        } else {
            this.host = host.toLowerCase() + ":" + port;
        }
        this.path = path;
    }

    public void addParameter(String name, String value) {
        List<String> values = parameters.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            parameters.put(name, values);
        }
        values.add(value);
    }

    public String getSignature(String secretKey) {
        Mac mac;
        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(), ALGORITHM));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        mac.update(method.getBytes());
        mac.update((byte)'\n');
        mac.update(host.getBytes());
        mac.update((byte)'\n');
        mac.update(path.getBytes());
        mac.update((byte)'\n');

        boolean addAmpersand = false;
        for (Map.Entry<String,List<String>> entry : parameters.entrySet()) {
            byte[] nameBytes = encodeString(entry.getKey());
            List<String> values = entry.getValue();
            Collections.sort(values);
            for (String value : values) {
                if (addAmpersand) {
                    mac.update((byte)'&');
                } else {
                    addAmpersand = true;
                }
                byte[] valueBytes = encodeString(value);
                mac.update(nameBytes);
                mac.update((byte)'=');
                mac.update(valueBytes);
            }
        }

        byte[] digest = mac.doFinal();
        return new String(Base64.encodeBase64(digest));
    }

    private final static String RFC3986_UNRESERVED =
                                           "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                           "abcdefghijklmnopqrstuvwxyz" +
                                           "01234567890-_.~";
    private final static char[] HEX_MAP = {'0','1','2','3','4','5','6','7',
                                           '8','9','A','B','C','D','E','F'};

    private byte[] encodeString(String value) {
        // Will be at most six times as large (U => %AB%CD,
        // where U is a unicode character).
        byte[] valueBytes;
        try {
            valueBytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        byte[] result = new byte[valueBytes.length * 6];
        int i = 0;
        for (byte c : valueBytes) {
            if (RFC3986_UNRESERVED.indexOf(c) != -1) {
                result[i++] = c;
            } else {
                result[i++] = (byte)'%';
                result[i++] = (byte)HEX_MAP[(c & 0xf0) >>> 4];
                result[i++] = (byte)HEX_MAP[(c & 0x0f)];
            }
        }
        byte[] trimmedResult = new byte[i];
        System.arraycopy(result, 0, trimmedResult, 0, i);
        return trimmedResult;
    }

}
