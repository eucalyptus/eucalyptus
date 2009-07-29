/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2009, Eucalyptus Systems, Inc.
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
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.util;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import java.util.UUID;
import java.util.List;
import java.util.Collections;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet6Address;

public class DNSProperties {

    private static Logger LOG = Logger.getLogger( DNSProperties.class );
    public static String ADDRESS = "0.0.0.0";
    public static int PORT = 53;
    public static int MAX_MESSAGE_SIZE = 1024;
    public static String DNS_REF = "vm://DNSControlInternal";
    public static String DOMAIN = "localdomain";
    public static String NS_HOST = "nshost." + DOMAIN;
    public static String NS_IP = "127.0.0.1";

    static {
        updateHost();
    }

    public static void update() {
        try {
            SystemConfiguration systemConfiguration = EucalyptusProperties.getSystemConfiguration();
            DOMAIN = systemConfiguration.getDnsDomain();
            NS_HOST = systemConfiguration.getNameserver();
            NS_IP = systemConfiguration.getNameserverAddress();
        } catch(Exception ex) {
            LOG.warn(ex.getMessage());
        }
    }

    private static void updateHost () {
        InetAddress ipAddr = null;
        String localAddr = "127.0.0.1";

        List<NetworkInterface> ifaces = null;
        try {
            ifaces = Collections.list( NetworkInterface.getNetworkInterfaces() );
        }
        catch ( SocketException e1 ) {}

        for ( NetworkInterface iface : ifaces )
            try {
                if ( !iface.isLoopback() && !iface.isVirtual() && iface.isUp() ) {
                    for ( InetAddress iaddr : Collections.list( iface.getInetAddresses() ) ) {
                        if ( !iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address) ) {
                            ipAddr = iaddr;
                        } else if ( iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address ) ) {
                            ipAddr = iaddr;
                        }
                    }
                }
            }
            catch ( SocketException e1 ) {}

        if(ipAddr != null) {
            DNSProperties.NS_IP = ipAddr.getHostAddress();
            DNSProperties.NS_HOST = ipAddr.getCanonicalHostName();
        }
    }

}