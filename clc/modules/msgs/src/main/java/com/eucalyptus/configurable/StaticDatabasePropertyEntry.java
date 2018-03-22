/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.configurable;

import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import javaslang.Tuple;
import javaslang.Tuple3;

@Entity
@PersistenceContext( name = "eucalyptus_config" )
@Table( name = "config_static_property" )
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

  public static StaticDatabasePropertyEntry update(
      final String fieldName,
      final String propName,
      final String newFieldValue ) throws Exception {
    final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class );
    try {
      final StaticDatabasePropertyEntry dbEntry =
          Entities.uniqueResult( new StaticDatabasePropertyEntry( fieldName, propName, null ) );
      dbEntry.setValue( newFieldValue );
      db.commit( );
      return dbEntry;
    } catch ( final NoSuchElementException ex ) {
      final StaticDatabasePropertyEntry dbEntry = new StaticDatabasePropertyEntry( fieldName, propName, newFieldValue );
      Entities.persist( dbEntry );
      db.commit( );
      return dbEntry;
    } finally {
      db.close( );
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

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v3_2_0, value = Empyrean.class )
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

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v3_3_0, value = Empyrean.class )
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

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v3_4_0, value = Empyrean.class )
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

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_0_1, value = Empyrean.class )
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


  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_4_0, value = Empyrean.class )
  public enum StaticPropertyEntryInvertPropertyCloudWatchUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryRenamePropertyUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_FIELD_NAME = "com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties.disable_cloudwatch_service";
      final String CLOUDWATCH_ENABLE_CLOUDWATCH_SERVICE_FIELD_NAME = "com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties.enable_cloudwatch_service";
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME = "cloudwatch.disable_cloudwatch_service";
      final String CLOUDWATCH_ENABLE_CLOUDWATCH_SERVICE_PROP_NAME = "cloudwatch.enable_cloudwatch_service";
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        List<StaticDatabasePropertyEntry> entities = Entities.criteriaQuery(StaticDatabasePropertyEntry.class)
          .whereEqual(StaticDatabasePropertyEntry_.fieldName, CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_FIELD_NAME)
          .whereEqual(StaticDatabasePropertyEntry_.propName, CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME)
          .list();
        for ( StaticDatabasePropertyEntry entry : entities ) {
          entry.setFieldName(CLOUDWATCH_ENABLE_CLOUDWATCH_SERVICE_FIELD_NAME);
          entry.setPropName(CLOUDWATCH_ENABLE_CLOUDWATCH_SERVICE_PROP_NAME);
          entry.setValue(Boolean.toString(!Boolean.valueOf(entry.getValue())));
          LOG.debug( "Upgrading: Changing property " + CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME + " field name'"+CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_FIELD_NAME+"' to '"+CLOUDWATCH_ENABLE_CLOUDWATCH_SERVICE_FIELD_NAME+"'");
        }
        db.commit( );
        return true;
      }
    }
  }

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_0_0, value = Empyrean.class )
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

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_1_0, value = Empyrean.class )
  public enum StaticPropertyEntryRenameServiceVMPropertyUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryRenameServiceVMPropertyUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      ImmutableMap<String, String[]> changes = ImmutableMap.<String, String[]>builder()
          .put("imaging.imaging_worker_availability_zones", new String[] {"services.imaging.worker.availability_zones",
              "com.eucalyptus.imaging.ImagingServiceProperties.availability_zones"})
          .put("imaging.imaging_worker_emi", new String[] {"services.imaging.worker.image",
              "com.eucalyptus.imaging.ImagingServiceProperties.image"})
          .put("imaging.imaging_worker_enabled", new String[] {"services.imaging.worker.configured",
              "com.eucalyptus.imaging.worker.ImagingServiceLaunchers.configured"})
          .put("imaging.imaging_worker_healthcheck", new String[] {"services.imaging.worker.healthcheck",
              "com.eucalyptus.imaging.ImagingServiceProperties.healthcheck"})
          .put("imaging.imaging_worker_instance_type", new String[] {"services.imaging.worker.instance_type",
              "com.eucalyptus.imaging.ImagingServiceProperties.instance_type"})
          .put("imaging.imaging_worker_keyname", new String[] {"services.imaging.worker.keyname",
              "com.eucalyptus.imaging.ImagingServiceProperties.keyname"})
          .put("imaging.imaging_worker_log_server", new String[] {"services.imaging.worker.log_server",
              "com.eucalyptus.imaging.ImagingServiceProperties.log_server"})
          .put("imaging.imaging_worker_log_server_port", new String[] {"services.imaging.worker.log_server_port",
              "com.eucalyptus.imaging.ImagingServiceProperties.log_server_port"})
          .put("imaging.imaging_worker_ntp_server", new String[] {"services.imaging.worker.ntp_server",
              "com.eucalyptus.imaging.ImagingServiceProperties.ntp_server"})
          .put("imaging.import_task_expiration_hours", new String[] {"services.imaging.import_task_expiration_hours",
              "com.eucalyptus.imaging.ImportTaskProperties.import_task_expiration_hours"})
          .put("imaging.import_task_timeout_minutes", new String[] {"services.imaging.import_task_timeout_minutes",
              "com.eucalyptus.imaging.ImportTaskProperties.import_task_timeout_minutes"})
          .put("loadbalancing.loadbalancer_app_cookie_duration", new String[] {"services.loadbalancing.worker.app_cookie_duration",
              "com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.app_cookie_duration"})
          .put("loadbalancing.loadbalancer_dns_subdomain", new String[] {"services.loadbalancing.dns_subdomain",
              "com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.dns_subdomain"})
          .put("loadbalancing.loadbalancer_dns_ttl", new String[] {"services.loadbalancing.dns_ttl",
              "com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.dns_ttl"})
          .put("loadbalancing.loadbalancer_emi", new String[] {"services.loadbalancing.worker.image",
              "com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.image"})
          .put("loadbalancing.loadbalancer_instance_type", new String[] {"services.loadbalancing.worker.instance_type",
              "com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.instance_type"})
          .put("loadbalancing.loadbalancer_num_vm", new String[] {"services.loadbalancing.vm_per_zone",
              "com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.vm_per_zone"})
          .put("loadbalancing.loadbalancer_restricted_ports", new String[] {"services.loadbalancing.restricted_ports",
              "com.eucalyptus.loadbalancing.LoadBalancerListener.restricted_ports"})
          .put("loadbalancing.loadbalancer_vm_keyname", new String[] {"services.loadbalancing.worker.keyname",
              "com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.keyname"})
          .put("loadbalancing.loadbalancer_vm_ntp_server", new String[] {"services.loadbalancing.worker.ntp_server",
              "com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.ntp_server"})
          .build();
      EntityTransaction db = Entities.get( StaticDatabasePropertyEntry.class );
      LOG.info("Updating service VM properties");
      try {
        List<StaticDatabasePropertyEntry> entities = Entities.query( new StaticDatabasePropertyEntry( ) );
        for ( StaticDatabasePropertyEntry entry : entities ) {
          if (entry.getPropName() != null && changes.containsKey(entry.getPropName())) {
            String[] newProperty = changes.get(entry.getPropName());
            LOG.info( "Upgrading: Copying property value of'" + entry.getPropName() + "' to '" + newProperty[0] + "'" );
            StaticDatabasePropertyEntry.update(newProperty[1], newProperty[0], entry.getValue());
            LOG.info( "Deleting old property from DB'" + entry.getPropName() + "'" );
            Entities.delete(entry);
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

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_2_0, value = Empyrean.class )
  public enum StaticPropertyEntryUpgrade420 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryUpgrade420.class );

    private void updateLdapConfiguration( ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        try {
          final StaticDatabasePropertyEntry property = Entities.uniqueResult( new StaticDatabasePropertyEntry( null, "authentication.ldap_integration_configuration", null ) );
          LOG.info( "Updating field and default value for authentication.ldap_integration_configuration" );
          if ( "{ 'sync': { 'enable':'false' } }".equals( property.getValue( ) ) ) {
            property.setValue( "{ \"sync\": { \"enable\":\"false\" } }" );
          }
          property.setFieldName( "com.eucalyptus.auth.euare.ldap.LdapProperties.ldap_integration_configuration" );
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

    private void updateStackConfiguration ( ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        try {
          final StaticDatabasePropertyEntry pipeline_max_query_property = Entities.uniqueResult( new StaticDatabasePropertyEntry( null, "bootstrap.webservices.pipeline_max_query_request_size", null ) );
          final StaticDatabasePropertyEntry max_chunk_property = Entities.uniqueResult( new StaticDatabasePropertyEntry( null, "bootstrap.webservices.http_max_chunk_bytes", null ) );
          LOG.info( " Checking to make sure default pipeline_max_query_request_size is not smaller than http_max_chunk_bytes." );
          if ( Integer.parseInt(max_chunk_property.getValue()) > Integer.parseInt(pipeline_max_query_property.getValue())) {
            pipeline_max_query_property.setValue(max_chunk_property.getValue());
          }
        } catch ( NoSuchElementException e ) {
          LOG.info( "Property not found, skipped size check for : bootstrap.webservices.pipeline_max_query_request_size" );
        }
        try {
          final String expect =
              "RSA:DSS:ECDSA:+RC4:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES";
          final String update =
              "RSA:DSS:ECDSA:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4";
          for ( final String propName : Lists.newArrayList(
              "bootstrap.webservices.ssl.user_ssl_ciphers",
              "www.https_ciphers",
              "bootstrap.webservices.ssl.server_ssl_ciphers"
          ) ) {
            final StaticDatabasePropertyEntry ciphersProperty =
                Entities.uniqueResult( new StaticDatabasePropertyEntry( null, propName, null ) );
            if ( expect.equals( ciphersProperty.getValue( ) ) ) {
              LOG.info( "Updating ciphers property " + propName + " with value " + update );
              ciphersProperty.setValue( update );
            }
          }
        } catch ( Exception e ) {
          LOG.error( "Error updating cipher suite configuration to remove RC4", e );
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    private void migrateEc2ClassicProtocolExtensions() {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        try {
          final StaticDatabasePropertyEntry property = Entities.uniqueResult( new StaticDatabasePropertyEntry( null, "cloud.network.ec2_classic_additional_protocols_allowed", null ) );
          LOG.info( "Updating field and default value for cloud.network.ec2_classic_additional_protocols_allowed" );
          property.setFieldName( "com.eucalyptus.compute.common.config.ExtendedNetworkingConfiguration.ec2_classic_additional_protocols_allowed" );
        } catch ( NoSuchElementException e ) {
          //Nothing to do.
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    private void migrateDisableCloudWatchProperty() {
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_OLD_FIELD_NAME = "com.eucalyptus.cloudwatch.backend.CloudWatchBackendService.disable_cloudwatch_service";
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_NEW_FIELD_NAME = "com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties.disable_cloudwatch_service";
      final String CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME = "cloudwatch.disable_cloudwatch_service";
      try (final TransactionResource db = Entities.transactionFor(StaticDatabasePropertyEntry.class)) {
        List<StaticDatabasePropertyEntry> entities = Entities.query(new StaticDatabasePropertyEntry());
        for (StaticDatabasePropertyEntry entry : entities) {
          if (CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_OLD_FIELD_NAME.equals(entry.getFieldName()) &&
            CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME.equals(entry.getPropName())) {
            entry.setFieldName(CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_NEW_FIELD_NAME);
            LOG.debug("Upgrading: Changing property " + CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_PROP_NAME + " field name'" + CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_OLD_FIELD_NAME + "' to '" + CLOUDWATCH_DISABLE_CLOUDWATCH_SERVICE_NEW_FIELD_NAME + "'");
          }
        }
        db.commit();
      } catch (Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }

      @Override
    public boolean apply( Class arg0 ) {
      updateStackConfiguration( );
      updateLdapConfiguration( );
      deleteRemovedProperties( Lists.newArrayList( "www.httpProxyHost" , "www.httpProxyPort" ) );

      //move ec2_classic network config from cluster-manager to compute-common property
      migrateEc2ClassicProtocolExtensions();

      migrateDisableCloudWatchProperty();
      return true;
    }
  }

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_2_0, value = Empyrean.class )
  public enum DropServiceKeypairPropertyUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( DropServiceKeypairPropertyUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
        try {
          StaticDatabasePropertyEntry.update( "com.eucalyptus.imaging.ImagingServiceProperties.keyname",
              "services.imaging.worker.keyname", "" );
          LOG.info("Resetting services.imaging.worker.keyname property to ''. Please set it to a keypair from "
              + AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT + " account if needed.");
          StaticDatabasePropertyEntry.update( "com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.keyname",
              "services.loadbalancing.worker.keyname", "" );
          LOG.info("Resetting services.loadbalancing.worker.keyname to '' . Please set it to a keypair from "
              + AccountIdentifiers.ELB_SYSTEM_ACCOUNT + " account if needed.");
          return true;
        } catch (Exception e) {
          throw Exceptions.toUndeclared( e );
        }
    }
  }

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_2_1, value = Empyrean.class )
  public enum StaticPropertyEntryUpgrade421 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryUpgrade421.class );

    private void migrateIdentifierCanonicalizerProperty() {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        try {
          final StaticDatabasePropertyEntry property = Entities.uniqueResult( new StaticDatabasePropertyEntry( null, "cloud.identifier_canonicalizer", null ) );
          LOG.info( "Updating field for cloud.identifier_canonicalizer property" );
          property.setFieldName( "com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers.identifier_canonicalizer" );
        } catch ( NoSuchElementException e ) {
          //Nothing to do.
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    private void enableInstanceDns( ) {
      setPropertyValue(
          "dns.split_horizon.enabled",
          String.valueOf( true ),
          "Setting property dns.split_horizon.enabled property to true to enable instance dns" );
    }

    private void enableServiceDns( ) {
      setPropertyValue(
          "dns.services.enabled",
          String.valueOf( true ),
          "Setting property dns.services.enabled property to true to enable service dns" );
    }

    private void setPropertyValue( final String name, final String value, final String message ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        try {
          final StaticDatabasePropertyEntry property =
              Entities.uniqueResult( new StaticDatabasePropertyEntry( null, name, null ) );
          if ( !value.equals( property.getValue( ) ) ) {
            LOG.info( message );
            property.setValue( value );
          }
        } catch ( NoSuchElementException e ) {
          //Nothing to do.
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    @Override
    public boolean apply( final Class entity ) {
      migrateIdentifierCanonicalizerProperty( );
      enableInstanceDns( );
      enableServiceDns( );
      return true;
    }
  }

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_3_0, value = Empyrean.class )
  public enum StaticPropertyEntryUpgrade430 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryUpgrade430.class );

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
      deleteRemovedProperties( Lists.newArrayList(
          "www.https_ciphers",
          "www.https_port",
          "www.https_protocols",
          "services.database.worker.availability_zones",
          "services.database.worker.configured",
          "services.database.worker.expiration_days",
          "services.database.worker.image",
          "services.database.worker.init_script",
          "services.database.worker.instance_type",
          "services.database.worker.keyname",
          "services.database.worker.ntp_server",
          "services.database.worker.volume",
          "cloud.perm_gen_memory_check_poll_time",
          "cloud.perm_gen_memory_check_ratio"
      ) );

      return true;
    }
  }

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_4_0, value = Empyrean.class )
  public enum StaticPropertyEntryUpgrade440 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryUpgrade440.class );

    private void deleteRemovedProperties( final Iterable<String> propertyNames ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        for ( final String propertyName : propertyNames ) try {
          final StaticDatabasePropertyEntry property =
              Entities.criteriaQuery( StaticDatabasePropertyEntry.class )
                  .whereEqual( StaticDatabasePropertyEntry_.propName, propertyName )
                  .uniqueResult( );
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

    private void updateMovedProperties( final Iterable<Tuple3<String,String,String>> movedProperties ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        for ( final Tuple3<String,String,String> movedProperty : movedProperties ) {
          final String propName = movedProperty._1( );
          final String oldFieldName = movedProperty._2( );
          final String newFieldName = movedProperty._3( );
          Entities.criteriaQuery( StaticDatabasePropertyEntry.class )
              .whereEqual( StaticDatabasePropertyEntry_.propName, propName )
              .uniqueResultOption( ).transform( property -> {
            if ( oldFieldName.equals( property.getFieldName( ) ) ) {
              LOG.info( "Updating field for "+propName+" property" );
              property.setFieldName( newFieldName );
            }
            return property;
          } );
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    private void updatePropertyValues( final Iterable<Tuple3<String,String,String>> updatedProperties ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        for ( final Tuple3<String,String,String> updatedProperty : updatedProperties ) {
          final String propName = updatedProperty._1( );
          final String oldValue = updatedProperty._2( );
          final String newValue = updatedProperty._3( );
          Entities.criteriaQuery( StaticDatabasePropertyEntry.class )
              .whereEqual( StaticDatabasePropertyEntry_.propName, propName )
              .uniqueResultOption( ).transform( property -> {
            if ( oldValue.equals( property.getValue( ) ) ) {
              LOG.info( "Updating value for "+propName+" property to " + newValue );
              property.setValue( newValue );
            }
            return property;
          } );
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    private void configureCloudformationStrictResourcePropertyEnforcement( ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        try {
          final StaticDatabasePropertyEntry property = Entities.criteriaQuery(StaticDatabasePropertyEntry.class).
            whereEqual(StaticDatabasePropertyEntry_.propName, "cloudformation.enforce_strict_resource_properties")
            .uniqueResult();
          LOG.info( "Found existing 'cloudformation.enforce_strict_resource_properties' property, leaving alone.");
        } catch ( NoSuchElementException e ) {
          LOG.info( "Creating property 'cloudformation.enforce_strict_resource_properties' with value 'false')");
          Entities.persist( new StaticDatabasePropertyEntry(
            "com.eucalyptus.cloudformation.config.CloudFormationProperties.enforce_strict_resource_properties",
            "cloudformation.enforce_strict_resource_properties",
            "false"
          ) );
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    @Override
    public boolean apply( final Class arg0 ) {
      deleteRemovedProperties( ImmutableList.of(
          "bootstrap.servicebus.max_outstanding_messages",
          "bootstrap.servicebus.min_scheduler_core_size",
          "bootstrap.servicebus.workers_per_stage",
          "cloud.network.global_min_network_tag",
          "cloud.network.global_max_network_tag",
          "cloud.network.global_min_network_index",
          "cloud.network.global_max_network_index",
          "cloud.network.network_tag_pending_timeout",
          "services.loadbalancing.worker.backend_instance_update_interval",
          "services.loadbalancing.worker.cache_duration",
          "services.loadbalancing.worker.cw_put_interval",
          "services.loadbalancing.worker.lb_poll_interval"
      ) );

      updateMovedProperties( ImmutableList.of(
          Tuple.of( "dns.recursive.enabled",
              "com.eucalyptus.dns.resolvers.RecursiveDnsResolver.enabled",
              "com.eucalyptus.vm.dns.RecursiveDnsResolver.enabled" )
      ) );

      updatePropertyValues( ImmutableList.of(
          Tuple.of( "cloudformation.swf_activity_worker_config",
              "{\"PollThreadCount\": 8, \"TaskExecutorThreadPoolSize\": 16, \"MaximumPollRateIntervalMilliseconds\": 50 }",
              "{\"PollThreadCount\": 8, \"TaskExecutorThreadPoolSize\": 16, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }" ),
          Tuple.of( "cloudformation.swf_workflow_worker_config",
              "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8, \"MaximumPollRateIntervalMilliseconds\": 50 }",
              "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }" ),
          Tuple.of( "services.loadbalancing.swf_activity_worker_config",
              "{\"PollThreadCount\": 4, \"TaskExecutorThreadPoolSize\": 32, \"MaximumPollRateIntervalMilliseconds\": 50 }",
              "{\"PollThreadCount\": 4, \"TaskExecutorThreadPoolSize\": 32, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }" ),
          Tuple.of( "services.loadbalancing.swf_workflow_worker_config",
              "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 4, \"MaximumPollRateIntervalMilliseconds\": 50 }",
              "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 4, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }" ),
          Tuple.of( "bootstrap.webservices.ssl.server_ssl_ciphers",
              "RSA:DSS:ECDSA:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4",
              "RSA:DSS:ECDSA:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4:!ECDHE" ),
          Tuple.of( "bootstrap.webservices.ssl.user_ssl_ciphers",
              "RSA:DSS:ECDSA:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4",
              "RSA:DSS:ECDSA:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4:!ECDHE" ),
          Tuple.of( "region.region_ssl_ciphers",
              "RSA:DSS:ECDSA:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4",
              "RSA:DSS:ECDSA:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4:!ECDHE" )
      ) );

      configureCloudformationStrictResourcePropertyEnforcement();
      return true;
    }
  }

  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = Version.v4_4_3, value = Empyrean.class )
  public enum StaticPropertyEntryUpgrade443 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( StaticPropertyEntryUpgrade443.class );

    private void updatePropertyValues( final Iterable<Tuple3<String,String,String>> updatedProperties ) {
      try ( final TransactionResource db = Entities.transactionFor( StaticDatabasePropertyEntry.class ) ) {
        for ( final Tuple3<String,String,String> updatedProperty : updatedProperties ) {
          final String propName = updatedProperty._1( );
          final String oldValue = updatedProperty._2( );
          final String newValue = updatedProperty._3( );
          Entities.criteriaQuery( StaticDatabasePropertyEntry.class )
              .whereEqual( StaticDatabasePropertyEntry_.propName, propName )
              .uniqueResultOption( ).transform( property -> {
            if ( oldValue.equals( property.getValue( ) ) ) {
              LOG.info( "Updating value for "+propName+" property to " + newValue );
              property.setValue( newValue );
            }
            return property;
          } );
        }
        db.commit( );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    @Override
    public boolean apply( final Class arg0 ) {
      updatePropertyValues( ImmutableList.of(
          Tuple.of( "bootstrap.webservices.server_pool_max_threads", "32", "128" ),
          Tuple.of( "bootstrap.webservices.async_internal_operations", "false", "true" )
      ) );

      return true;
    }
  }
}

