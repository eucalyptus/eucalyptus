/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.providers;

import com.amazonaws.auth.AWSCredentials;
import com.eucalyptus.objectstorage.client.OsgInternalS3Client;
import com.eucalyptus.storage.config.ConfigurationCache;
import com.eucalyptus.objectstorage.entities.S3ProviderConfiguration;
import org.apache.commons.pool.PoolableObjectFactory;

import java.util.Date;

/**
 * see @org.apache.commons.pool.PoolableObjectFactory&lt;T&gt;
 */
public class PoolableProviderClientFactory implements PoolableObjectFactory<OsgInternalS3Client> {

    private AWSCredentials requestUser;
    private String upstreamEndpoint;
    private boolean usePathStyle;

    private long checkIntervalInMs = 15l; //use constant for now, if we start using this again should make a config value

    public PoolableProviderClientFactory(AWSCredentials requestUser, String upstreamEndpoint, boolean usePathStyle) {
        this.requestUser = requestUser;
        this.upstreamEndpoint = upstreamEndpoint;
        this.usePathStyle = usePathStyle;
    }

    @Override
    public OsgInternalS3Client makeObject() throws Exception {
        boolean useHttps = false;
        S3ProviderConfiguration providerConfig = ConfigurationCache.getConfiguration(S3ProviderConfiguration.class);
        if(providerConfig != null && providerConfig.getS3UseHttps() != null && providerConfig.getS3UseHttps()) {
            useHttps = true;
        }

        OsgInternalS3Client osgInternalS3Client = new OsgInternalS3Client(requestUser, upstreamEndpoint, useHttps, providerConfig.getS3UseBackendDns());
        osgInternalS3Client.setUsePathStyle(usePathStyle);
        return osgInternalS3Client;
    }

    @Override
    public void destroyObject(OsgInternalS3Client amazonS3Client) throws Exception {
        amazonS3Client.getS3Client().shutdown();
    }

    @Override
    public boolean validateObject(OsgInternalS3Client amazonS3Client) {
        long now = new Date().getTime();
        long instantiated = amazonS3Client.getInstantiated().getTime();
        if (now - instantiated > checkIntervalInMs) {
            if (ConfigurationCache.getConfiguration(S3ProviderConfiguration.class).getLastUpdateTimestamp().getTime() - instantiated > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void activateObject(OsgInternalS3Client amazonS3Client) throws Exception {
        // no-op
    }

    @Override
    public void passivateObject(OsgInternalS3Client amazonS3Client) throws Exception {
        // no-op
    }
}
