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
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.Users;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.ImageDetails;

@Entity
@PersistenceContext(name="eucalyptus_general")
@Table( name = "Images" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ImageInfo implements Image {
  @Transient
  public static ImageInfo ALL = new ImageInfo();
  @Id
  @GeneratedValue
  @Column( name = "image_id" )
  private Long id = -1l;
  @Column( name = "image_name" )
  private String imageId;
  @Column( name = "image_path" )
  private String imageLocation;
  @Column( name = "image_availability" )
  private String imageState;
  @Column( name = "image_owner_id" )
  private String imageOwnerId;
  @Column( name = "image_arch" )
  private String architecture;
  @Column( name = "image_type" )
  private String imageType;
  @Column( name = "image_kernel_id" )
  private String kernelId;
  @Column( name = "image_ramdisk_id" )
  private String ramdiskId;
  @Column( name = "image_is_public" )
  private Boolean imagePublic;
  @Lob
  @Column( name = "image_signature" )
  private String signature;
  @Column( name = "image_platform" )
  private String platform;
  public String getPlatform( ) {
    return this.platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }
//  @ManyToMany( cascade = CascadeType.PERSIST )
//  @JoinTable(
//      name = "image_has_groups",
//      joinColumns = { @JoinColumn( name = "image_id" ) },
//      inverseJoinColumns = @JoinColumn( name = "user_group_id" )
//  )
//  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
//  private List<Group> userGroups = new ArrayList<Group>();
  @ManyToMany()
  @JoinTable(
      name = "image_has_perms",
      joinColumns = { @JoinColumn( name = "image_id" ) },
      inverseJoinColumns = @JoinColumn( name = "user_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<UserInfo> permissions = new ArrayList<UserInfo>();
  @ManyToMany( cascade = CascadeType.PERSIST )
  @JoinTable(
      name = "image_has_product_codes",
      joinColumns = { @JoinColumn( name = "image_id" ) },
      inverseJoinColumns = @JoinColumn( name = "image_product_code_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<ProductCode> productCodes = new ArrayList<ProductCode>();

  public static ImageInfo deregistered() {
    ImageInfo img = new ImageInfo();
    img.setImageState( "deregistered" );
    return img;
  }

  public static ImageInfo byOwnerId( String ownerId ) {
    ImageInfo img = new ImageInfo();
    img.setImageOwnerId( ownerId );
    return img;
  }

  public ImageInfo() {}

  public ImageInfo( final String imageId ) {
    this.imageId = imageId.substring( 0, 4 ).toLowerCase( ) + imageId.substring( 4 ).toUpperCase();
  }

  public ImageInfo( final String imageLocation, final String imageOwnerId, final String imageState, final Boolean aPublic ) {
    this.imageLocation = imageLocation;
    this.imageOwnerId = imageOwnerId;
    this.imageState = imageState;
    this.imagePublic = aPublic;
  }

  public ImageInfo( String architecture, String imageId, String imageLocation, String imageOwnerId, String imageState, String imageType, Boolean aPublic, String kernelId, String ramdiskId ) {
    this.architecture = architecture;
    this.imageId = imageId;
    this.imageLocation = imageLocation;
    this.imageOwnerId = imageOwnerId;
    this.imageState = imageState;
    this.imageType = imageType;
    this.imagePublic = aPublic;
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
  }

  public Long getId() {
    return this.id;
  }

  /**
   * @see com.eucalyptus.images.Image#getArchitecture()
   * @return
   */
  public String getArchitecture() {
    return architecture;
  }

  /**
   * @see com.eucalyptus.images.Image#setArchitecture(java.lang.String)
   * @param architecture
   */
  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }

  /**
   * @see com.eucalyptus.images.Image#getImageId()
   * @return
   */
  public String getImageId() {
    return imageId;
  }

  /**
   * @see com.eucalyptus.images.Image#setImageId(java.lang.String)
   * @param imageId
   */
  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  /**
   * @see com.eucalyptus.images.Image#getImageLocation()
   * @return
   */
  public String getImageLocation() {
    return imageLocation;
  }

  /**
   * @see com.eucalyptus.images.Image#setImageLocation(java.lang.String)
   * @param imageLocation
   */
  public void setImageLocation( String imageLocation ) {
    this.imageLocation = imageLocation;
  }

  /**
   * @see com.eucalyptus.images.Image#getImageOwnerId()
   * @return
   */
  public String getImageOwnerId() {
    return imageOwnerId;
  }

  /**
   * @see com.eucalyptus.images.Image#setImageOwnerId(java.lang.String)
   * @param imageOwnerId
   */
  public void setImageOwnerId( String imageOwnerId ) {
    this.imageOwnerId = imageOwnerId;
  }

  /**
   * @see com.eucalyptus.images.Image#getImageState()
   * @return
   */
  public String getImageState() {
    return imageState;
  }

  /**
   * @see com.eucalyptus.images.Image#setImageState(java.lang.String)
   * @param imageState
   */
  public void setImageState( String imageState ) {
    this.imageState = imageState;
  }

  /**
   * @see com.eucalyptus.images.Image#getImageType()
   * @return
   */
  public String getImageType() {
    return imageType;
  }

  /**
   * @see com.eucalyptus.images.Image#setImageType(java.lang.String)
   * @param imageType
   */
  public void setImageType( String imageType ) {
    this.imageType = imageType;
  }

  /**
   * @see com.eucalyptus.images.Image#getPublic()
   * @return
   */
  public Boolean getImagePublic() {
    return imagePublic;
  }

  /**
   * @see com.eucalyptus.images.Image#setPublic(java.lang.Boolean)
   * @param aPublic
   */
  public void setImagePublic( Boolean aPublic ) {
    imagePublic = aPublic;
  }

  /**
   * @see com.eucalyptus.images.Image#getKernelId()
   * @return
   */
  public String getKernelId() {
    return kernelId;
  }

  /**
   * @see com.eucalyptus.images.Image#setKernelId(java.lang.String)
   * @param kernelId
   */
  public void setKernelId( String kernelId ) {
    this.kernelId = kernelId;
  }

  /**
   * @see com.eucalyptus.images.Image#getRamdiskId()
   * @return
   */
  public String getRamdiskId() {
    return ramdiskId;
  }

  /**
   * @see com.eucalyptus.images.Image#setRamdiskId(java.lang.String)
   * @param ramdiskId
   */
  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  /**
   * @see com.eucalyptus.images.Image#getSignature()
   * @return
   */
  public String getSignature() {
    return signature;
  }

  /**
   * @see com.eucalyptus.images.Image#setSignature(java.lang.String)
   * @param signature
   */
  public void setSignature( final String signature ) {
    this.signature = signature;
  }

//  public List<UserGroupEntity> getUserGroups() {
//    return userGroups;
//  }
//
//  public void setUserGroups( final List<UserGroupEntity> userGroups ) {
//    this.userGroups = userGroups;
//  }

  public List<UserInfo> getPermissions() {
    return permissions;
  }

  public void setPermissions( final List<UserInfo> permissions ) {
    this.permissions = permissions;
  }

  /**
   * @see com.eucalyptus.images.Image#getAsImageDetails()
   * @return
   */
  public ImageDetails getAsImageDetails() {
    ImageDetails i = new ImageDetails();
    i.setArchitecture( this.getArchitecture() );
    i.setImageId( this.getImageId() );
    i.setImageLocation( this.getImageLocation() );
    i.setImageOwnerId( this.getImageOwnerId() );
    i.setImageState( this.getImageState() );
    i.setImageType( this.getImageType() );
    i.setIsPublic( this.getImagePublic() );
    i.setKernelId( this.getKernelId() );
    i.setRamdiskId( this.getRamdiskId() );
    i.setPlatform( this.getPlatform( ) );
    return i;
  }

  /**
   * @see com.eucalyptus.images.Image#getProductCodes()
   * @return
   */
  public List<ProductCode> getProductCodes() {
    return productCodes;
  }

  /**
   * @see com.eucalyptus.images.Image#setProductCodes(java.util.List)
   * @param productCodes
   */
  public void setProductCodes( final List<ProductCode> productCodes ) {
    this.productCodes = productCodes;
  }

  /**
   * @see com.eucalyptus.images.Image#equals(java.lang.Object)
   * @param o
   * @return
   */
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ImageInfo imageInfo = ( ImageInfo ) o;

    if ( !imageId.equals( imageInfo.imageId ) ) return false;

    return true;
  }

  /**
   * @see com.eucalyptus.images.Image#hashCode()
   * @return
   */
  @Override
  public int hashCode() {
    return imageId.hashCode();
  }

  /**
   * @see com.eucalyptus.images.Image#isAllowed(com.eucalyptus.auth.UserInfo)
   * @param user
   * @return
   */
  public boolean isAllowed( UserInfo user ) {
    try {
      if ( Users.lookupUser( user.getUserName( ) ).isAdministrator() || user.getUserName().equals( this.getImageOwnerId() ) )
        return true;
    } catch ( NoSuchUserException e ) {
      return false;
    }
//    for ( UserGroupEntity g : this.getUserGroups() )
//      if ( "all".equals( g.getName() ) )
        return true;
//    return this.getPermissions().contains( user );
  }

  public static ImageInfo named( String imageId ) throws EucalyptusCloudException {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo image = null;
    try {
      image = db.getUnique( new ImageInfo( imageId ) );
      db.commit( );
    } catch (Throwable t) {
      db.commit();
    }
    return image;
  }

  /**
   * @see com.eucalyptus.images.Image#toString()
   * @return
   */
  @Override
  public String toString() {
    return this.imageId;
  }
  public static ImageInfoToDetails TO_IMAGE_DETAILS = new ImageInfoToDetails( );
  static class ImageInfoToDetails implements Function<ImageInfo,ImageDetails> {
    @Override public ImageDetails apply( ImageInfo arg0 ) {
      return arg0.getAsImageDetails( );
    }    
  }
}
