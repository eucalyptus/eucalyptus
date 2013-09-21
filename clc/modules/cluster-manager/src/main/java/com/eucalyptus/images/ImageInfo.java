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

package com.eucalyptus.images;

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.util.Parameters.checkParam;
import java.lang.Object;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.Mappable;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_images" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "metadata_image_discriminator", discriminatorType = DiscriminatorType.STRING )
@DiscriminatorValue( value = "metadata_kernel_or_ramdisk" )
public class ImageInfo extends UserMetadata<ImageMetadata.State> implements ImageMetadata {
  @Transient
  private static final long          serialVersionUID = 1L;
  
  @Transient
  private static Logger              LOG              = Logger.getLogger( ImageInfo.class );
  
  /**
   * Name of the registered image info:
   * Constraints: 3-128 alphanumeric characters, parenthesis (()), commas (,), slashes (/), dashes
   * (-), or underscores(_)
   */
  @Column( name = "metadata_image_name", nullable = false )
  private String                     imageName;
  
  @Column( name = "metadata_image_description" )
  private String                     description;
  
  @Column( name = "metadata_image_arch", nullable = false )
  @Enumerated( EnumType.STRING )
  private ImageMetadata.Architecture architecture;
  
  @Column( name = "metadata_image_is_public", columnDefinition = "boolean default false" )
  private Boolean                    imagePublic;
  
  @Column( name = "metadata_image_platform", nullable = false )
  @Enumerated( EnumType.STRING )
  private ImageMetadata.Platform     platform;
  
  @Column( name = "metadata_image_type" )
  @Enumerated( EnumType.STRING )
  private ImageMetadata.Type         imageType;
  
  @ElementCollection
  @CollectionTable( name = "metadata_images_permissions" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<String>                permissions      = new HashSet<String>( );
  
  @ElementCollection
  @CollectionTable( name = "metadata_images_pcodes" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<String>                productCodes     = new HashSet<String>( );
  
  @OneToMany( cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, orphanRemoval = true )
  @JoinColumn( name = "metadata_image_dev_map_fk" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<DeviceMapping>         deviceMappings   = new ArrayList<DeviceMapping>( );
  
  @Column( name = "metadata_image_size_bytes", nullable = false )
  private Long                       imageSizeBytes;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "image" )
  private Collection<ImageInfoTag> tags;

  @Transient
  private FullName                   fullName;
  
  public ImageInfo( ) {}
  
  public ImageInfo( final ImageMetadata.Type imageType ) {
    this.imageType = imageType;
  }
  
  ImageInfo( final String imageId ) {
    this( );
    this.setDisplayName( imageId.substring( 0, 4 ).toLowerCase( ) + imageId.substring( 4 ).toUpperCase( ) );
  }
  
  ImageInfo( final ImageMetadata.Type imageType, final String imageId ) {
    this( imageId );
    this.imageType = imageType;
  }
  
  protected ImageInfo( final OwnerFullName ownerFullName, final String imageId,
                       final ImageMetadata.Type imageType, final String imageName, final String imageDescription, final Long imageSizeBytes,
                       final ImageMetadata.Architecture arch, final ImageMetadata.Platform platform) {
    this( ownerFullName, imageId.substring( 0, 4 ).toLowerCase( ) + imageId.substring( 4 ).toUpperCase( ) );
    checkParam( imageName, notNullValue() );
    checkParam( imageType, notNullValue() );
    checkParam( imageSizeBytes, notNullValue() );
    checkParam( arch, notNullValue() );
    checkParam( platform, notNullValue() );
    this.setState( ImageMetadata.State.pending );
    this.imageType = imageType;
    this.imageName = imageName;
    this.description = imageDescription;
    this.imageSizeBytes = imageSizeBytes;
    this.architecture = arch;
    this.platform = platform;
    this.imagePublic = ImageConfiguration.getInstance( ).getDefaultVisibility( );
  }
  
  ImageInfo( final OwnerFullName ownerFullName, final String imageId ) {
    super( ownerFullName, imageId );
  }
  
  static ImageInfo self( final ImageInfo image ) {
    return new ImageInfo( image.getDisplayName( ) );
  }
  
  public static ImageInfo named( final String imageId ) {
    return new ImageInfo( imageId );
  }
  
  public ImageMetadata.Type getImageType( ) {
    return this.imageType;
  }
  
  public ImageMetadata.Platform getPlatform( ) {
    return this.platform;
  }
  
  protected void setPlatform( final ImageMetadata.Platform platform ) {
    this.platform = platform;
  }
  
  public Architecture getArchitecture( ) {
    return this.architecture;
  }
  
  protected void setArchitecture( final Architecture architecture ) {
    this.architecture = architecture;
  }
  
  public Boolean getImagePublic( ) {
    return this.imagePublic;
  }
  
  public void setImagePublic( final Boolean aPublic ) {
    this.imagePublic = aPublic;
  }
  
  protected Set<String> getPermissions( ) {
    return this.permissions;
  }
  
  private void setPermissions( final Set<String> permissions ) {
    this.permissions = permissions;
  }
  
  @SuppressWarnings( "unchecked" )
  public ImageInfo revokePermission( final Account account ) {
    EntityTransaction db = Entities.get( ImageInfo.class );
    try {
      ImageInfo entity = Entities.merge( this );
      entity.getPermissions( ).remove( account.getAccountNumber( ) );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
    }
    return this;
  }
  
  @SuppressWarnings( "unchecked" )
  public ImageInfo grantPermission( final Account account ) {
    EntityTransaction db = Entities.get( ImageInfo.class );
    try {
      ImageInfo entity = Entities.merge( this );
      entity.getPermissions( ).add( account.getAccountNumber( ) );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
    }
    return this;
  }
  
  /** GRZE:REMOVE: /not/ OK. cross-module reference to private component data type. **/
  public boolean checkPermission( final String accountId ) {
    EntityTransaction db = Entities.get( ImageInfo.class );
    try {
      ImageInfo entity = Entities.merge( this );
      boolean ret = this.getPermissions( ).contains( accountId ) || this.getOwner( ).isOwner( accountId );
      db.commit( );
      return ret;
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      return false;
    }
  }
  
  public ImageInfo resetPermission( ) {
    try {
      Transactions.one( new ImageInfo( this.displayName ), new Callback<ImageInfo>( ) {
        @Override
        public void fire( final ImageInfo t ) {
          t.getPermissions( ).clear( );
          t.getPermissions( ).add( t.getOwnerAccountNumber( ) );
          t.setImagePublic( ImageConfiguration.getInstance( ).getDefaultVisibility( ) );
        }
      } );
    } catch ( final ExecutionException e ) {
      LOG.debug( e, e );
    }
    return this;
  }
  
  public ImageInfo resetProductCodes( ) {
    try {
      Transactions.one( ImageInfo.self( this ), new Callback<ImageInfo>( ) {
        @Override
        public void fire( final ImageInfo t ) {
          t.getProductCodes( ).clear( );
        }
      } );
    } catch ( final ExecutionException e ) {
      LOG.debug( e, e );
    }
    return this;
  }
  
  public Set<String> getProductCodes( ) {
    return this.productCodes;
  }
  
  /**
   * @see Mappable#getName()
   * @see UserMetadata#equals(Object)
   * @param o
   * @return
   */
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( ( o == null ) || ( this.getClass( ) != o.getClass( ) ) ) return false;
    
    final ImageInfo imageInfo = ( ImageInfo ) o;
    
    if ( !this.getDisplayName( ).equals( imageInfo.getDisplayName( ) ) ) return false;
    
    return true;
  }
  
  /**
   * @see UserMetadata#hashCode()
   * @see Mappable#hashCode()
   * @return
   */
  @Override
  public int hashCode( ) {
    return this.getDisplayName( ).hashCode( );
  }
  
  /**
   * @see Mappable#toString()
   * @return
   */
  @Override
  public String toString( ) {
    return this.getFullName( ).toString( );
  }
  
  @Override
  public String getPartition( ) {
    return ComponentIds.lookup( Eucalyptus.class ).name( );
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( this.getOwnerAccountNumber( ) )
                          .relativeId( "image", this.getDisplayName( ) );
  }
  
  protected void setImageType( final Type imageType ) {
    this.imageType = imageType;
  }
  
  public Long getImageSizeBytes( ) {
    return this.imageSizeBytes;
  }
  
  protected void setImageSizeBytes( final Long imageSizeBytes ) {
    this.imageSizeBytes = imageSizeBytes;
  }
  
  public boolean addProductCode( final String prodCode ) {
    
    EntityTransaction db = Entities.get( ImageInfo.class );
    try {
      ImageInfo entity = Entities.merge( this );
      entity.getProductCodes( ).add( prodCode );
      db.commit( );
      return true;
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      return false;
    }
  }
  
  public String getDescription( ) {
    return this.description;
  }
  
  protected void setDescription( final String description ) {
    this.description = description;
  }
  
  public List<DeviceMapping> getDeviceMappings( ) {
    return this.deviceMappings;
  }
  
  protected void setDeviceMappings( final List<DeviceMapping> deviceMappings ) {
    this.deviceMappings = deviceMappings;
  }
  
  public String getImageName( ) {
    return this.imageName;
  }
  
  public void setImageName( final String imageName ) {
    this.imageName = imageName;
  }
  
  /**
   * @param accountId
   * @return true if the accountId has an explicit launch permission.
   */
  public boolean hasPermission( final String... accountIds ) {
    return !Sets.intersection( this.getPermissions( ), Sets.newHashSet( accountIds ) ).isEmpty( );
  }
  
  /**
   * Add launch permissions.
   * 
   * @param accountIds
   */
  public void addPermissions( final List<String> accountIds ) {
    final EntityTransaction db = Entities.get( ImageInfo.class );
    try {
      final ImageInfo entity = Entities.merge( this );
      Iterables.all( accountIds, new Predicate<String>( ) {
        
        @Override
        public boolean apply( final String input ) {
          try {
            final Account account = Accounts.lookupAccountById( input );
            ImageInfo.this.getPermissions( ).add( input );
          } catch ( final Exception e ) {
            try {
              final User user = Accounts.lookupUserById( input );
              ImageInfo.this.getPermissions( ).add( user.getAccount( ).getAccountNumber( ) );
            } catch ( AuthException ex ) {
              try {
                final User user = Accounts.lookupUserByAccessKeyId( input );
                ImageInfo.this.getPermissions( ).add( user.getAccount( ).getAccountNumber( ) );
              } catch ( AuthException ex1 ) {
                LOG.error( ex1, ex1 );
              }
            }
          }
          return true;
        }
      } );
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
    }
  }
  
  /**
   * Remove launch permissions.
   * 
   * @param accountIds
   */
  public void removePermissions( final List<String> accountIds ) {
    
    final EntityTransaction db = Entities.get( ImageInfo.class );
    try {
      final ImageInfo entity = Entities.merge( this );
      Iterables.all( accountIds, new Predicate<String>( ) {
        
        @Override
        public boolean apply( final String input ) {
          ImageInfo.this.getPermissions( ).remove( input );
          return true;
        }
      } );
      
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
    }
  }
  
  public static ImageInfo named( @Nullable final OwnerFullName input,
                                 @Nullable final String imageId ) {
    return new ImageInfo( input, imageId );
  }
  
}
