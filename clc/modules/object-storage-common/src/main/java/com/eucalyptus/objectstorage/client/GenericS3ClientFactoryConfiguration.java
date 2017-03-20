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
package com.eucalyptus.objectstorage.client;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableInit;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Transactions;

/**
 * Set of configurable values that are used by GenericS3ClientFactory
 */
@Entity
@PersistenceContext(name = "eucalyptus_osg")
@Table(name = "s3client_config")
@ConfigurableClass(root = "objectstorage.s3client", description = "Configuration for internal S3 clients.", singleton = true)
public class GenericS3ClientFactoryConfiguration extends AbstractPersistent {

  private static final Logger LOG = Logger.getLogger(GenericS3ClientFactoryConfiguration.class);

  @ConfigurableField(description = "Internal S3 client connection timeout in ms", displayName = "connection_timeout", initial = "10000")
  @Column(name = "connection_timeout")
  private Integer connection_timeout_ms;
  private static final Integer DEFAULT_TIMEOUT_MS = new Integer(10 * 1000);

  @ConfigurableField(description = "Internal S3 client maximum connections", displayName = "max_connections", initial = "64")
  @Column(name = "max_connections")
  private Integer max_connections;
  private static final Integer DEFAULT_MAX_CONNECTIONS = new Integer(64);

  @ConfigurableField(description = "Internal S3 client socket read timeout in ms", displayName = "socket_read_timeout", initial = "30000")
  @Column(name = "socket_read_timeout_ms")
  private Integer socket_read_timeout_ms;
  private static final Integer DEFAULT_SOCKET_READ_TIMEOUT_MS = new Integer(30 * 1000);

  @ConfigurableField(description = "Internal S3 client maximum retries on error", displayName = "max_retries", initial = "3")
  @Column(name = "max_error_retries")
  private Integer max_error_retries;
  private static final Integer DEFAULT_MAX_ERROR_RETRIES = new Integer(3);

  @ConfigurableField(description = "Internal S3 client buffer size", displayName = "buffer_size", initialInt = DEFAULT_BUFFER_SIZE)
  @Column(name = "buffer_size")
  private Integer buffer_size;
  private static final int DEFAULT_BUFFER_SIZE = 512 * 1024;

  //TODO: make configurable?
  private String signer_type = "S3SignerType";

  @ConfigurableInit
  @PrePersist
  protected void initialize() {
    if (connection_timeout_ms == null) {
      connection_timeout_ms = DEFAULT_TIMEOUT_MS;
    }
    if (max_connections == null) {
      max_connections = DEFAULT_MAX_CONNECTIONS;
    }
    if (socket_read_timeout_ms == null) {
      socket_read_timeout_ms = DEFAULT_SOCKET_READ_TIMEOUT_MS;
    }
    if (max_error_retries == null) {
      max_error_retries = DEFAULT_MAX_ERROR_RETRIES;
    }
    if (buffer_size == null) {
      buffer_size = DEFAULT_BUFFER_SIZE;
    }
  }

  public static GenericS3ClientFactoryConfiguration getInstance() {
    GenericS3ClientFactoryConfiguration config = null;
    try {
      config = Transactions.find(new GenericS3ClientFactoryConfiguration());
    } catch (Exception e) {
      try {
        config = Transactions.save(new GenericS3ClientFactoryConfiguration());
      } catch (Exception ex) {
        LOG.warn("failed to load and save configuration for internal S3 clients");
        config = new GenericS3ClientFactoryConfiguration();
        config.initialize();
      }
    }
    return config;
  }

  public Integer getConnection_timeout_ms() {
    return connection_timeout_ms;
  }

  public void setConnection_timeout_ms(Integer connection_timeout_ms) {
    this.connection_timeout_ms = connection_timeout_ms;
  }

  public Integer getMax_connections() {
    return max_connections;
  }

  public void setMax_connections(Integer max_connections) {
    this.max_connections = max_connections;
  }

  public Integer getSocket_read_timeout_ms() {
    return socket_read_timeout_ms;
  }

  public void setSocket_read_timeout_ms(Integer socket_read_timeout_ms) {
    this.socket_read_timeout_ms = socket_read_timeout_ms;
  }

  public Integer getMax_error_retries() {
    return max_error_retries;
  }

  public void setMax_error_retries(Integer max_error_retries) {
    this.max_error_retries = max_error_retries;
  }

  public Integer getBuffer_size() {
    return buffer_size;
  }

  public void setBuffer_size(Integer buffer_size) {
    this.buffer_size = buffer_size;
  }

  public String getSigner_type() {
    return signer_type;
  }

  public void setSigner_type( final String signer_type ) {
    this.signer_type = signer_type;
  }
}
