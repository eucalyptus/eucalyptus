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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.Image;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.JoinTx;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_images" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "metadata_image_discriminator", discriminatorType = DiscriminatorType.STRING )
@DiscriminatorValue( value = "metadata_kernel_or_ramdisk" )
public class ImageInfo extends UserMetadata<Image.State> implements Image {
  
  @Transient
  private static Logger         LOG          = Logger.getLogger( ImageInfo.class );
  
  @Column( name = "metadata_image_path" )
  private String                imageLocation;
  
  @Column( name = "metadata_image_arch" )
  @Enumerated( EnumType.STRING )
  private Image.Architecture    architecture;
  
  @Column( name = "metadata_image_is_public", nullable = false, columnDefinition = "boolean default true" )
  private Boolean               imagePublic  = Boolean.FALSE;
  
  @Column( name = "metadata_image_platform" )
  @Enumerated( EnumType.STRING )
  private Image.Platform        platform;
  
  @Column( name = "metadata_image_type" )
  @Enumerated( EnumType.STRING )
  private Type                  imageType;
  
  @Lob
  @Column( name = "metadata_image_signature" )
  private String                signature;
  
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "metadata_image_auth_for_image_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<LaunchPermission> permissions  = new HashSet<LaunchPermission>( );
  
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "metadata_image_productcode_for_image_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<ProductCode>      productCodes = new HashSet<ProductCode>( );
  
  @Transient
  private FullName              fullName;
  
  public ImageInfo( ) {}
  
  public ImageInfo( final String imageId ) {
    this( );
    this.setDisplayName( imageId.substring( 0, 4 ).toLowerCase( ) + imageId.substring( 4 ).toUpperCase( ) );
  }
  
  public ImageInfo( final UserFullName userFullName, final String imageId, final String imageLocation, final Image.Architecture arch,
                    final Image.Platform platform ) {
    super( userFullName, imageId.substring( 0, 4 ).toLowerCase( ) + imageId.substring( 4 ).toUpperCase( ) );
    Assertions.assertNotNull( arch );
    Assertions.assertNotNull( arch );
    Assertions.assertNotNull( imageLocation );
    Assertions.assertNotNull( platform );
    this.setState( Image.State.pending );
    this.imageLocation = imageLocation;
    this.imagePublic = ImageConfiguration.getInstance( ).getDefaultVisibility( );
    this.architecture = arch;
    this.platform = platform;
  }
  
  public Image.Type getImageType( ) {
    return this.imageType;
  }
  
  public Image.Platform getPlatform( ) {
    return this.platform;
  }
  
  public void setPlatform( Image.Platform platform ) {
    this.platform = platform;
  }
  
  public Architecture getArchitecture( ) {
    return this.architecture;
  }
  
  public void setArchitecture( Architecture architecture ) {
    this.architecture = architecture;
  }
  
  public String getImageLocation( ) {
    return imageLocation;
  }
  
  public void setImageLocation( String imageLocation ) {
    this.imageLocation = imageLocation;
  }
  
  public Boolean getImagePublic( ) {
    return imagePublic;
  }
  
  public void setImagePublic( Boolean aPublic ) {
    imagePublic = aPublic;
  }
  
  public String getSignature( ) {
    return signature;
  }
  
  public void setSignature( final String signature ) {
    this.signature = signature;
  }
  
  private Set<LaunchPermission> getPermissions( ) {
    return permissions;
  }
  
  private void setPermissions( final Set<LaunchPermission> permissions ) {
    this.permissions = permissions;
  }
  
  @SuppressWarnings( "unchecked" )
  public ImageInfo grantPermission( final Account account ) {
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new JoinTx<ImageInfo>( ) {
        @Override
        public void fire( EntityWrapper<ImageInfo> db, ImageInfo t ) throws Throwable {
          LaunchPermission imgAuth = new LaunchPermission( t, account.getId( ) );
          if ( !t.getPermissions( ).contains( imgAuth ) ) {
            db.recast( LaunchPermission.class ).add( imgAuth );
            t.getPermissions( ).add( imgAuth );
          }
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return this;
  }
  
  public boolean checkPermission( final Account account ) {
    final boolean[] result = { false };
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new Tx<ImageInfo>( ) {
        @Override
        public void fire( ImageInfo t ) throws Throwable {
          result[0] = t.getPermissions( ).contains( new LaunchPermission( t, account.getId( ) ) );
        }
      }
                  );
    } catch ( TransactionException e ) {
      return false;
    }
    return result[0];
  }
  
  public ImageInfo resetPermission( ) {
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new Tx<ImageInfo>( ) {
        @Override
        public void fire( ImageInfo t ) throws Throwable {
          t.getPermissions( ).clear( );
          t.getPermissions( ).add( new LaunchPermission( t, t.getOwnerAccountId( ) ) );
          t.setImagePublic( ImageConfiguration.getInstance( ).getDefaultVisibility( ) );
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return this;
  }
  public List<String> listProductCodes( ) {
    final List<String> prods = Lists.newArrayList( );
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new Tx<ImageInfo>( ) {
        @Override
        public void fire( ImageInfo t ) throws Throwable {
          for( ProductCode p : t.getProductCodes( ) ) {
            prods.add( p.getValue( ) );
          }
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return prods;
  }
  public List<String> listLaunchPermissions( ) {
    final List<String> perms = Lists.newArrayList( );
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new Tx<ImageInfo>( ) {
        @Override
        public void fire( ImageInfo t ) throws Throwable {
          for( LaunchPermission p : t.getPermissions( ) ) {
            perms.add( p.getAccountId( ) );
          }
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return perms;
  }

  public ImageInfo resetProductCodes( ) {
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new Tx<ImageInfo>( ) {
        @Override
        public void fire( ImageInfo t ) throws Throwable {
          t.getProductCodes( ).clear( );
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return this;
  }

  public ImageInfo revokePermission( final Account account ) {
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new Tx<ImageInfo>( ) {
        @Override
        public void fire( ImageInfo t ) throws Throwable {
          LaunchPermission imgAuth;
          t.getPermissions( ).remove( new LaunchPermission( t, account.getId( ) ) );
        }
      } );
    } catch ( TransactionException e ) {
      LOG.debug( e, e );
    }
    return this;
  }
  
  private Set<ProductCode> getProductCodes( ) {
    return this.productCodes;
  }
  
  private void setProductCodes( final Set<ProductCode> productCodes ) {
    this.productCodes = productCodes;
  }
  
  /**
   * @see com.eucalyptus.util.Mappable#getName()
   * @see com.eucalyptus.entities.UserMetadata#equals(java.lang.Object)
   * @param o
   * @return
   */
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    ImageInfo imageInfo = ( ImageInfo ) o;
    
    if ( !this.getDisplayName( ).equals( imageInfo.getDisplayName( ) ) ) return false;
    
    return true;
  }
  
  /**
   * @see com.eucalyptus.entities.UserMetadata#hashCode()
   * @see com.eucalyptus.util.Mappable#hashCode()
   * @return
   */
  @Override
  public int hashCode( ) {
    return this.getDisplayName( ).hashCode( );
  }
  
  public boolean isAllowed( Account account ) {
    return this.getImagePublic( ) || this.checkPermission( account );
  }
  
  /**
   * @see com.eucalyptus.util.Mappable#toString()
   * @return
   */
  @Override
  public String toString( ) {
    return this.getFullName( ).toString( );
  }
  
  @Override
  public int compareTo( Image o ) {
    return this.getDisplayName( ).compareTo( o.getName( ) );
  }
  
  @Override
  public String getPartition( ) {
    return ComponentIds.lookup( Eucalyptus.class ).name( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.fullName == null
      ? this.fullName = FullName.create.vendor( "euca" )
                                       .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                                       .namespace( ( ( UserFullName ) this.getOwner( ) ).getAccountId( ) )
                                       .relativeId( "image", this.getDisplayName( ) )
      : this.fullName;
  }
  
  public void setImageType( Type imageType ) {
    this.imageType = imageType;
  }

  public boolean addProductCode( final String prodCode ) {
    try {
      ImageInfo search = ForwardImages.exampleWithImageId( this.getDisplayName( ) );
      Transactions.one( search, new Tx<ImageInfo>( ) {
        @Override
        public void fire( ImageInfo t ) throws Throwable {
          t.getProductCodes().add( new ProductCode( t, prodCode ) );
        }
      }
                  );
    } catch ( TransactionException e ) {
      return false;
    }
    return true;
  }
}
