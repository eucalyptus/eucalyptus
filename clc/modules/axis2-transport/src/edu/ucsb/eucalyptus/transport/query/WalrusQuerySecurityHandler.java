/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.query;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.util.CaseInsensitiveMap;
import edu.ucsb.eucalyptus.keys.AbstractKeyStore;
import edu.ucsb.eucalyptus.keys.ServiceKeyStore;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.openssl.PEMReader;

import java.security.Signature;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.io.StringReader;

public class WalrusQuerySecurityHandler extends HMACQuerySecurityHandler {

    private static Logger LOG = Logger.getLogger( WalrusQuerySecurityHandler.class );

    public QuerySecurityHandler getInstance()
    {
        return new WalrusQuerySecurityHandler();
    }

    public String getName()
    {
        return "2";
    }

    public UserInfo authenticate( final HttpRequest httpRequest ) throws QuerySecurityException
    {
        String addr = httpRequest.getRequestURL();

        String verb = httpRequest.getHttpMethod();
        Map<String, String> httpParams = httpRequest.getParameters();
        Map<String, String> headers = httpRequest.getHeaders();
        return handle(addr, verb, httpParams, headers);
    }

    private String getStringStats (String s)
    {
        int len = s.length();
        String out = "length=" +  len
                + " buf[n-1]=" + s.getBytes()[len-1]
                + " hash=" + s.hashCode();
        return out;
    }

    public UserInfo handle(String addr, String verb, Map<String, String> parameters, Map<String, String> headers) throws QuerySecurityException
    {
        CaseInsensitiveMap hdrs = new CaseInsensitiveMap(headers);

        //this.checkParameters( hdrs );
        //:: check the signature :://

        if(hdrs.containsKey(StorageQuerySecurityHandler.StorageSecurityParameters.EucaSignature)) {
            //possible internal request -- perform authentication using internal credentials
            String date =  (String) hdrs.remove( SecurityParameter.Date);
            String eucaCert = (String) hdrs.remove(StorageQuerySecurityHandler.StorageSecurityParameters.EucaCert);
            String signature = (String) hdrs.remove(StorageQuerySecurityHandler.StorageSecurityParameters.EucaSignature);
            String data = verb + "\n" + date + "\n" + addr + "\n";

            Signature sig;
            boolean valid = false;
            try {
                byte[] bytes = Base64.decode(eucaCert);
                String certString = new String(bytes);
                PEMReader pemReader = new PEMReader(new StringReader(certString));
                X509Certificate cert = (X509Certificate) pemReader.readObject();
                AbstractKeyStore keyStore = ServiceKeyStore.getInstance();
                if (keyStore.getCertificateAlias(cert) != null) {
                    //cert found in keystore
                    PublicKey publicKey = cert.getPublicKey();
                    sig = Signature.getInstance("SHA1withRSA");

                    sig.initVerify(publicKey);
                    sig.update(data.getBytes());
                    valid = sig.verify(Base64.decode(signature));
                } else {
                    LOG.warn ("WalrusQuerySecurityHandler(): certificate not found in keystore");
                }
            } catch (Exception ex) {
                LOG.warn ("Authentication exception: " + ex.getMessage());
                ex.printStackTrace();
            }

            if(!valid) {
                throw new QuerySecurityException( "User authentication failed." );
            }
            //run as admin
            UserInfo admin = new UserInfo("admin");
            admin.setIsAdministrator(Boolean.TRUE);
            return admin;
        } else {
            //external user request
            String date =  (String) hdrs.remove( SecurityParameter.Date);
            date = date == null ? "" : date;
            String content_md5 = (String) hdrs.remove ( "Content-MD5");
            content_md5 = content_md5 == null ? "" : content_md5;
            String content_type = (String) hdrs.remove ( "Content-Type");
            content_type = content_type == null ? "" : content_type;

            String[] addrStrings = addr.split("\\?");
            String addrString = addrStrings[0];

            if(addrStrings.length > 1) {
                for(SubResource subResource : SubResource.values()) {
                    if(addr.contains(subResource.toString())) {
                        addrString += "?" + subResource.toString();
                        break;
                    }
                }
            }

            String data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" +  getCanonicalizedAmzHeaders(hdrs) + addrString;

            String auth_part = (String) hdrs.remove(SecurityParameter.Authorization);

            if(auth_part != null) {
                String sigString[] = getSigInfo(auth_part);
                String signature = sigString[1];
                signature = signature.replaceAll("=", "");

                String queryKey = findQueryKey( sigString[0] );

                String authSig = checkSignature( queryKey, data );

                if ( !authSig.equals(signature) )
                    throw new QuerySecurityException( "User authentication failed." );

                return findUserId( sigString[0] );
            } else {
                //anonymous request
                return null;
            }
        }
    }


    private void checkParameters( final CaseInsensitiveMap header ) throws QuerySecurityException
    {
        if ( !header.containsKey(SecurityParameter.Authorization.toString()))
            throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.Authorization );
        if ( !header.containsKey(SecurityParameter.Date.toString()))
            throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.Date );
    }

    private String[] getSigInfo (String auth_part) {
        int index = auth_part.lastIndexOf(" ");
        String sigString = auth_part.substring(index + 1);
        return sigString.split(":");
    }

    private String getCanonicalizedAmzHeaders(CaseInsensitiveMap headers) {
        String result = "";
        TreeMap amzHeaders = headers.removeSub("x-amz-");

        Iterator iterator = amzHeaders.keySet().iterator();
        while(iterator.hasNext()) {
            Object key = iterator.next();
            String trimmedKey = key.toString().trim();
            String value = (String) amzHeaders.get(key);
            String trimmedValue = value.trim();
            result += trimmedKey + ":" + trimmedValue + "\n";
        }
        return result;
    }
}
