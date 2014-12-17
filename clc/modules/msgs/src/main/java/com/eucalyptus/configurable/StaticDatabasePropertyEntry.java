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

package com.eucalyptus.configurable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@Entity
@PersistenceContext( name = "eucalyptus_config" )
@Table( name = "config_static_property" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StaticDatabasePropertyEntry extends AbstractPersistent {
  @Column( name = "config_static_field_name", nullable = false, unique = true )
  private String fieldName;
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  @Column( name = "config_static_field_value" )
  private String       value;
  @Column( name = "config_static_prop_name", nullable = false, unique = true )
  private String       propName;
  
  
  private StaticDatabasePropertyEntry( ) {
    super( );
    this.fieldName = null;
  }

  private StaticDatabasePropertyEntry( String fieldName, String propName, String value ) {
    super( );
    this.propName = propName;
    this.fieldName = fieldName;
    this.value = value;
  }
  
  static StaticDatabasePropertyEntry update( String fieldName, String propName, String newFieldValue ) throws Exception {
    final EntityTransaction db = Entities.get(StaticDatabasePropertyEntry.class);
    try {
      final StaticDatabasePropertyEntry dbEntry = Entities.uniqueResult(new StaticDatabasePropertyEntry( fieldName, propName, null ) );
      dbEntry.setValue( newFieldValue );
      Entities.persist(dbEntry);
      db.commit( );
      return dbEntry;
    } catch ( final NoSuchElementException ex ) {
      final StaticDatabasePropertyEntry dbEntry =  
          new StaticDatabasePropertyEntry( fieldName, propName, newFieldValue );
      try {
        Entities.persist( dbEntry );
        db.commit( );
      } catch ( final Exception ex1 ) {
        db.rollback();
        throw ex1;
      }
      return dbEntry;
    } catch(final Exception ex){
      throw ex;
    } finally{
      if(db.isActive())
        db.rollback();
    }
  }
  
  static StaticDatabasePropertyEntry lookup( String fieldName, String propName, String defaultFieldValue ) throws Exception {
    
    EntityTransaction db = Entities.get( StaticDatabasePropertyEntry.class );
    try {
      StaticDatabasePropertyEntry entity = Entities.uniqueResult( new StaticDatabasePropertyEntry( fieldName, propName, null ) );
      db.commit( );
      return entity;
    } catch ( Exception ex ) {
      try {
        StaticDatabasePropertyEntry entity = Entities.persist( new StaticDatabasePropertyEntry( fieldName, propName, defaultFieldValue ) );
        db.commit( );
        return entity;
      } catch ( Exception ex1 ) {
        Logs.extreme( ).error( "Failed to lookup static configuration property for: " + fieldName + " with property name: " + propName ); 
        db.rollback( );
        throw ex;
      }
    }
  }

  private void setValue( String value ) {
    this.value = value;
  }
  
  private String getFieldName( ) {
    return this.fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getValue( ) {
    return this.value;
  }

  public String getPropName( ) {
    return this.propName;
  }
  
  private void setPropName( String propName ) {
    this.propName = propName;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.propName == null )
      ? 0
      : this.propName.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( !super.equals( obj ) ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    StaticDatabasePropertyEntry other = ( StaticDatabasePropertyEntry ) obj;
    if ( this.propName == null ) {
      if ( other.propName != null ) {
        return false;
      }
    } else if ( !this.propName.equals( other.propName ) ) {
      return false;
    }
    return true;
  }
  
  @EntityUpgrade( entities = { StaticPropertyEntry.class }, since = Version.v3_2_0, value = Empyrean.class )
  public enum StaticPropertyEntryUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      EntityTransaction db = Entities.get( StaticDatabasePropertyEntry.class );
      try {
        List<StaticDatabasePropertyEntry> entities = Entities.query( new StaticDatabasePropertyEntry( ) );
        for ( StaticDatabasePropertyEntry entry : entities ) {
          LOG.debug( "Upgrading: " + entry.getPropName( ) + "=" + entry.getValue( ) );
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }

  @EntityUpgrade( entities = { StaticPropertyEntry.class }, since = Version.v3_3_0, value = Empyrean.class )
  public enum StaticPropertyEntryRenamePropertyUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryRenamePropertyUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      final String REPORTING_DEFAULT_POLL_INTERVAL_MINS_FIELD_NAME = "com.eucalyptus.reporting.modules.backend.DescribeSensorsListener.default_poll_interval_mins";
      final String REPORTING_DEFAULT_POLL_INTERVAL_MINS = "reporting.default_poll_interval_mins";
      final String CLOUD_MONITOR_DEFAULT_POLL_INTERVAL_MINS = "cloud.monitor.default_poll_interval_mins";
      EntityTransaction db = Entities.get( StaticDatabasePropertyEntry.class );
      try {
        List<StaticDatabasePropertyEntry> entities = Entities.query( new StaticDatabasePropertyEntry( ) );
        for ( StaticDatabasePropertyEntry entry : entities ) {
          if (REPORTING_DEFAULT_POLL_INTERVAL_MINS_FIELD_NAME.equals(entry.getFieldName()) && 
              REPORTING_DEFAULT_POLL_INTERVAL_MINS.equals(entry.getPropName())) {
            entry.setPropName(CLOUD_MONITOR_DEFAULT_POLL_INTERVAL_MINS);
            LOG.debug( "Upgrading: Changing property '"+REPORTING_DEFAULT_POLL_INTERVAL_MINS+"' to '"+CLOUD_MONITOR_DEFAULT_POLL_INTERVAL_MINS+"'");
          }
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }

  @EntityUpgrade( entities = { StaticPropertyEntry.class }, since = Version.v3_4_0, value = Empyrean.class )
  public enum StaticPropertyEntryRenameExpermentalDNSPropertyUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryRenameExpermentalDNSPropertyUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      final String EXPERIMENTAL_DNS_PREFIX = "experimental.dns.";
      final String DNS_PREFIX = "dns.";
      EntityTransaction db = Entities.get( StaticDatabasePropertyEntry.class );
      try {
        List<StaticDatabasePropertyEntry> entities = Entities.query( new StaticDatabasePropertyEntry( ) );
        for ( StaticDatabasePropertyEntry entry : entities ) {
          if (entry.getPropName() != null && entry.getPropName().startsWith(EXPERIMENTAL_DNS_PREFIX)) {
            String oldPropertyName = entry.getPropName();
            String newPropertyName = DNS_PREFIX + oldPropertyName.substring(EXPERIMENTAL_DNS_PREFIX.length());
            LOG.debug( "Upgrading: Changing property '"+oldPropertyName+"' to '"+newPropertyName+"'");
            entry.setPropName(newPropertyName);
          }
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      } finally {
        if (db.isActive())
          db.rollback();
      }
    }
  }

  @EntityUpgrade( entities = { StaticPropertyEntry.class }, since = Version.v4_0_1, value = Empyrean.class )
  public enum StaticPropertyEntryRenamePropertyCloudWatchUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryRenamePropertyUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_OLD_FIELD_NAME = "com.eucalyptus.cloudwatch.CloudWatchService.disable_cloudwatch_service";
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_NEW_FIELD_NAME = "com.eucalyptus.cloudwatch.backend.CloudWatchBackendService.disable_cloudwatch_service";
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME = "cloudwatch.disable_cloudwatch_service";
      EntityTransaction db = Entities.get( StaticDatabasePropertyEntry.class );
      try {
        List<StaticDatabasePropertyEntry> entities = Entities.query( new StaticDatabasePropertyEntry( ) );
        for ( StaticDatabasePropertyEntry entry : entities ) {
          if (CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_OLD_FIELD_NAME.equals(entry.getFieldName()) &&
            CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME.equals(entry.getPropName())) {
            entry.setFieldName(CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_NEW_FIELD_NAME);
            LOG.debug( "Upgrading: Changing property " + CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME + " field name'"+CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_OLD_FIELD_NAME+"' to '"+CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_NEW_FIELD_NAME+"'");
          }
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
  }

  @EntityUpgrade( entities = StaticPropertyEntry.class, since = Version.v4_0_0, value = Empyrean.class )
  public enum StaticPropertyEntryUpgrade40 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryUpgrade40.class );

    private void configureIdentifierCanonicalizer( ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        try {
          final StaticDatabasePropertyEntry property = Entities.uniqueResult( new StaticDatabasePropertyEntry( null, "cloud.identifier_canonicalizer", null ) );
          LOG.info( "Setting resource identifier canonicalizer property to 'upper' for upgraded system." );
          property.setValue( "upper" );
        } catch ( NoSuchElementException e ) {
          LOG.info( "Creating resource identifier canonicalizer property with value 'upper' for upgraded system." );
          Entities.persist( new StaticDatabasePropertyEntry(
              "com.eucalyptus.compute.identifier.ResourceIdentifiers.identifier_canonicalizer",
              "cloud.identifier_canonicalizer",
              "upper"
          ) );
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    private void deleteRemovedProperties( final Iterable<String> propertyNames ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        for ( final String propertyName : propertyNames ) try {
          final StaticDatabasePropertyEntry property = Entities.uniqueResult( new StaticDatabasePropertyEntry( null,propertyName, null ) );
          LOG.info( "Deleting cloud property: " + propertyName );
          Entities.delete( property );
        } catch ( NoSuchElementException e ) {
          LOG.info( "Property not found, skipped delete for: " + propertyName );
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    @Override
    public boolean apply( Class arg0 ) {
      configureIdentifierCanonicalizer( );
      deleteRemovedProperties( Lists.newArrayList( "authentication.websession_life_in_minutes" ) );
      return true;
    }
  }

  @EntityUpgrade( entities = { StaticPropertyEntry.class }, since = Version.v4_1_0, value = Empyrean.class )
  public enum StaticPropertyEntryRenameServiceVMPropertyUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryRenameServiceVMPropertyUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      ImmutableMap<String, String> changes = ImmutableMap.<String, String>builder()
          .put("imaging.imaging_worker_availability_zones", "services.imaging.worker.availability_zones")
          .put("imaging.imaging_worker_emi", "services.imaging.worker.image")
          .put("imaging.imaging_worker_enabled", "services.imaging.worker.configured")
          .put("imaging.imaging_worker_healthcheck", "services.imaging.worker.healthcheck")
          .put("imaging.imaging_worker_instance_type", "services.imaging.worker.instance_type")
          .put("imaging.imaging_worker_keyname", "services.imaging.worker.keyname")
          .put("imaging.imaging_worker_log_server", "services.imaging.worker.log_server")
          .put("imaging.imaging_worker_log_server_port", "services.imaging.worker.log_server_port")
          .put("imaging.imaging_worker_ntp_server", "services.imaging.worker.ntp_server")
          .put("imaging.import_task_expiration_hours", "services.imaging.import_task_expiration_hours")
          .put("imaging.import_task_timeout_minutes", "services.imaging.import_task_timeout_minutes")
          .put("loadbalancing.loadbalancer_app_cookie_duration", "services.loadbalancing.worker.app_cookie_duration")
          .put("loadbalancing.loadbalancer_dns_subdomain", "services.loadbalancing.dns_subdomain")
          .put("loadbalancing.loadbalancer_dns_ttl", "services.loadbalancing.dns_ttl")
          .put("loadbalancing.loadbalancer_emi", "services.loadbalancing.worker.image")
          .put("loadbalancing.loadbalancer_instance_type", "services.loadbalancing.worker.instance_type")
          .put("loadbalancing.loadbalancer_num_vm", "services.loadbalancing.vm_per_zone")
          .put("loadbalancing.loadbalancer_restricted_ports", "services.loadbalancing.restricted_ports")
          .put("loadbalancing.loadbalancer_vm_keyname", "services.loadbalancing.worker.keyname")
          .put("loadbalancing.loadbalancer_vm_ntp_server", "services.loadbalancing.worker.ntp_server")
          .build();
      EntityTransaction db = Entities.get( StaticDatabasePropertyEntry.class );
      LOG.info("Updating service VM properties");
      try {
        List<StaticDatabasePropertyEntry> entities = Entities.query( new StaticDatabasePropertyEntry( ) );
        for ( StaticDatabasePropertyEntry entry : entities ) {
          if (entry.getPropName() != null && changes.containsKey(entry.getPropName())) {
            String newPropertyName = changes.get(entry.getPropName());
            LOG.debug( "Upgrading: Copying property value of'" + entry.getPropName() + "' to '" + newPropertyName + "'");
            StaticDatabasePropertyEntry newEntry = findNewEntity(entities, newPropertyName);
            if (newEntry != null) {
              newEntry.setValue(entry.getValue());
            }
          }
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      } finally {
        if (db.isActive())
          db.rollback();
      }
    }

    private StaticDatabasePropertyEntry findNewEntity(List<StaticDatabasePropertyEntry> entities, String name) {
      for ( StaticDatabasePropertyEntry entry : entities ) {
        if (name.equals( entry.getPropName() ))
          return entry;
      }
      return null;
    }
  }
}

