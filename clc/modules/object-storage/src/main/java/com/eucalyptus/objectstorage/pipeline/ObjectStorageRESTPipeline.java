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

package com.eucalyptus.objectstorage.pipeline;

import com.eucalyptus.component.ComponentIds;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.xbill.DNS.Name;

import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Base class for all ObjectStorageGateway REST pipelines.
 * @author zhill
 *
 */
public abstract class ObjectStorageRESTPipeline extends FilteredPipeline {
	private static Logger LOG = Logger.getLogger( ObjectStorageRESTPipeline.class );
	private static final Splitter hostSplitter = Splitter.on( ':' ).limit( 2 );

    private static final Set<String> servicePaths = Sets.newHashSet(ComponentIds.lookup(ObjectStorage.class).getServicePath(), "/services/Walrus");

    public static Set<String> getServicePaths() {
        return servicePaths;
    }

    /**
	 * Does not accept any SOAP requests
	 * 
	 * Two options: path-style or virtual-hosted.
	 * 
	 * Path Style: objectstorage.hostname.com/bucket if using DNS, 192.168.1.1/services/ObjectStorage/bucket if not DNS
	 * 
	 * Virtual-hosted: objectstorage.walrus.hostname.com/ if using DNS, not-applicable if not using DNS
	 */
	@Override
	public boolean checkAccepts( HttpRequest message ) {
		String uriPath = message.getUri();
		uriPath = (uriPath == null ? "" : uriPath);
		String hostHeader = message.getHeader(HttpHeaders.Names.HOST);
		hostHeader = (hostHeader == null ? "" : hostHeader);
		
		return (!isSoapRequest(message) && 
				(isObjectStorageHostName(hostHeader) || isObjectStorageServicePathRequest(uriPath, hostHeader)));
	}
	
	private static boolean isSoapRequest(HttpRequest message) {
		return message.getHeaderNames().contains( "SOAPAction" );
	}

	/**
	 * The service path is the prefix for the URI and DNS is not used (if DNS used then it could be a bucket/key name)
	 */
	private boolean isObjectStorageServicePathRequest(final String uriPath, final String hostHeader) {
        final Predicate<String> matchUriPredicate = new Predicate<String>() {

            @Override
            public boolean apply(@Nullable String s) {
                return checkServicePathUri(uriPath, s);
            }
        };
        return !isObjectStorageHostName(hostHeader) && Iterators.any(servicePaths.iterator(), matchUriPredicate);


	}

    private boolean checkServicePathUri(final String uriPath, final String servicePath) {
        String tmpUri = uriPath;
        if(!tmpUri.endsWith("/")) {
            tmpUri += "/"; //Ensure a trailing / for check
        }
        return tmpUri.startsWith(servicePath + "/");
    }
	
	/**
	 * Returns whether or not the host header resolves to ObjectStorage
	 * @param hostHeader
	 * @return
	 */
	private boolean isObjectStorageHostName(String hostHeader) {
		//Try both the raw name as well as a bucket-stripped version in case using virtual-hosting style.
		return this.resolvesByHost(hostHeader) || this.maybeBucketHostedStyle(hostHeader);
	}
	
	/**
	 * Is ObjectSTorage domainname a subdomain of the host header host. If so, then it is likely a bucket prefix.
     * Includes checksd for Walrus domain (walrus.myserver.com) for legacy support
	 * But, since S3 buckets can include '.' can't just parse on '.'s
	 * @param fullHostHeader
	 * @return
	 */
    private boolean maybeBucketHostedStyle(String fullHostHeader) {
    	try {
    		return DomainNames.absolute(Name.fromString( Iterables.getFirst( hostSplitter.split( fullHostHeader ), fullHostHeader ) )).subdomain(DomainNames.externalSubdomain(ObjectStorage.class)) ||
                    DomainNames.absolute(Name.fromString( Iterables.getFirst( hostSplitter.split( fullHostHeader ), fullHostHeader ) )).subdomain(Name.fromString("walrus." + DomainNames.externalSubdomain().toString()));
    	} catch(Exception e) {
    		LOG.error("Error parsing domain name from hostname: " + fullHostHeader,e);
    		return false;
    	}
    }
}
