/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.images;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Intervals;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "cloud_image_configuration" )
@ConfigurableClass( root = "cloud.images", description = "Configuration options controlling the handling of registered images (EMI/EKI/ERI)." )
public class ImageConfiguration extends AbstractPersistent {
  @Transient
  private static Logger LOG = Logger.getLogger( ImageConfiguration.class );

  private static final int DEFAULT_MAX_IMAGE_SIZE_GB = 30;

  private static final int DEFAULT_MAX_MANIFEST_SIZE_IN_BYTES = 1024 * 1024 * 5; // 5MiB

  @ConfigurableField( displayName = "default_visibility",
      description = "The default value used to determine whether or not images are marked 'public' when first registered.",
      initial = "false" )
  @Column( name = "config_image_is_public", nullable = false, columnDefinition = "boolean default false" )
  private Boolean       defaultVisibility;

  @ConfigurableField( displayName = "cleanup_period", description = "The period between runs for clean up of deregistered images.", initial = "10m" )
  @Column( name = "config_image_cleanup_period" )
  private String        cleanupPeriod;
  
  @ConfigurableField( displayName = "max_image_size_gb",
      description = "The maximum registerable image size in GB",
      initialInt = DEFAULT_MAX_IMAGE_SIZE_GB )
  @Column( name = "max_image_size_gb")
  private Integer       maxImageSizeGb;

  @ConfigurableField( displayName = "max_manifest_size",
      description = "The maximum allowed image manifest size in bytes",
      initialInt = DEFAULT_MAX_MANIFEST_SIZE_IN_BYTES )
  @Column( name = "max_manifest_size_b")
  private Integer       maxManifestSizeBytes;

  public ImageConfiguration( ) {
    super( );
  }
  
  public static void modify( Callback<ImageConfiguration> callback ) throws ExecutionException {
    Transactions.one( new ImageConfiguration( ), callback );
  }
  
  public static ImageConfiguration getInstance( ) {
    ImageConfiguration ret = null;
    try {
      ret = Transactions.find( new ImageConfiguration( ) );
    } catch ( Exception ex1 ) {
      try {
        ret = Transactions.save( new ImageConfiguration( ) );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        ret = new ImageConfiguration( );
      }
    }
    return ret;
  }
  
  @PrePersist
  protected void initialize( ) {
    if ( this.defaultVisibility == null ) {
      this.defaultVisibility = Boolean.FALSE;
    }
    if ( this.cleanupPeriod == null ) {
      this.cleanupPeriod = "10m";
    }
    
    if ( this.maxImageSizeGb == null) {
      this.maxImageSizeGb = DEFAULT_MAX_IMAGE_SIZE_GB;
    }

    if ( this.maxManifestSizeBytes == null ) {
      this.maxManifestSizeBytes = DEFAULT_MAX_MANIFEST_SIZE_IN_BYTES;
    }
  }
  
  public Integer getMaxImageSizeGb() {
	  return maxImageSizeGb;
  }
  
  public void setMaxImageSizeGb(Integer maxImageSize) {
	  this.maxImageSizeGb = maxImageSize;
  }

  public Integer getMaxManifestSizeBytes( ) {
    return Objects.firstNonNull( maxManifestSizeBytes, DEFAULT_MAX_MANIFEST_SIZE_IN_BYTES );
  }

  public void setMaxManifestSizeBytes( final Integer maxManifestSizeBytes ) {
    this.maxManifestSizeBytes = maxManifestSizeBytes;
  }

  public Boolean getDefaultVisibility( ) {
    return this.defaultVisibility;
  }
  
  public void setDefaultVisibility( Boolean defaultVisibility ) {
    this.defaultVisibility = defaultVisibility;
  }
  
  public String getCleanupPeriod() {
    return cleanupPeriod;
  }

  public void setCleanupPeriod( final String cleanupPeriod ) {
    this.cleanupPeriod = cleanupPeriod;
  }

  /**
   * Get the image cleanup period in milliseconds.
   * 
   * @return The period, or zero for no cleanup.
   */
  public long getCleanupPeriodMillis( ) {
    return Intervals.parse( getCleanupPeriod( ), 0 ); 
  }
  
  @EntityUpgrade( entities = { ImageConfiguration.class }, since = Upgrades.Version.v3_4_0, value = Eucalyptus.class)
  public enum ImageConfigurationEntityUpgrade implements Predicate<Class> {
      INSTANCE;
      private static Logger LOG = Logger.getLogger(ImageConfiguration.ImageConfigurationEntityUpgrade.class);

      @Override
      public boolean apply(@Nullable Class aClass) {
          EntityTransaction tran = Entities.get(ImageConfiguration.class);
          try {
              List<ImageConfiguration> configs = Entities.query(new ImageConfiguration());
              if (configs != null && configs.size() > 0) {
                  for (ImageConfiguration config : configs) {
                      if (config.getMaxImageSizeGb() == null) {
                          config.setMaxImageSizeGb(0); //None, set to 'unlimited' on upgrade if not already present.
                          LOG.debug("Putting max image size as zero('unlimted') for image configuration entity: " + config.getId());
                      }
                  }
              }
              tran.commit();
          }
          catch (Exception ex) {
              tran.rollback();
              LOG.error("caught exception during upgrade, while attempting to create max image size");
              Exceptions.toUndeclared(ex);
          }
          return true;
      }
  }
}
