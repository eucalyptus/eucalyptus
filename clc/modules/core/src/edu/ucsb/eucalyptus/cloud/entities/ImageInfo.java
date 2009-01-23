/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
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
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.CacheImageType;
import edu.ucsb.eucalyptus.msgs.CheckImageType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.util.Messaging;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table( name = "Images" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ImageInfo {

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
  private Boolean isPublic;
  @Lob
  @Column( name = "image_signature" )
  private String signature;
  @ManyToMany( cascade = CascadeType.PERSIST )
  @JoinTable(
      name = "image_has_groups",
      joinColumns = { @JoinColumn( name = "image_id" ) },
      inverseJoinColumns = @JoinColumn( name = "user_group_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<UserGroupInfo> userGroups = new ArrayList<UserGroupInfo>();
  @ManyToMany()
  @JoinTable(
      name = "image_has_perms",
      joinColumns = { @JoinColumn( name = "image_id" ) },
      inverseJoinColumns = @JoinColumn( name = "user_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<UserInfo> permissions = new ArrayList<UserInfo>();

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
    this.imageId = imageId;
  }

  public ImageInfo( final String imageLocation, final String imageOwnerId, final String imageState, final Boolean aPublic ) {
    this.imageLocation = imageLocation;
    this.imageOwnerId = imageOwnerId;
    this.imageState = imageState;
    this.isPublic = aPublic;
  }

  public ImageInfo( String architecture, String imageId, String imageLocation, String imageOwnerId, String imageState, String imageType, Boolean aPublic, String kernelId, String ramdiskId ) {
    this.architecture = architecture;
    this.imageId = imageId;
    this.imageLocation = imageLocation;
    this.imageOwnerId = imageOwnerId;
    this.imageState = imageState;
    this.imageType = imageType;
    this.isPublic = aPublic;
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
  }

  public Long getId() {
    return this.id;
  }

  public String getArchitecture() {
    return architecture;
  }

  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }

  public String getImageId() {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getImageLocation() {
    return imageLocation;
  }

  public void setImageLocation( String imageLocation ) {
    this.imageLocation = imageLocation;
  }

  public String getImageOwnerId() {
    return imageOwnerId;
  }

  public void setImageOwnerId( String imageOwnerId ) {
    this.imageOwnerId = imageOwnerId;
  }

  public String getImageState() {
    return imageState;
  }

  public void setImageState( String imageState ) {
    this.imageState = imageState;
  }

  public String getImageType() {
    return imageType;
  }

  public void setImageType( String imageType ) {
    this.imageType = imageType;
  }

  public Boolean getPublic() {
    return isPublic;
  }

  public void setPublic( Boolean aPublic ) {
    isPublic = aPublic;
  }

  public String getKernelId() {
    return kernelId;
  }

  public void setKernelId( String kernelId ) {
    this.kernelId = kernelId;
  }

  public String getRamdiskId() {
    return ramdiskId;
  }

  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature( final String signature ) {
    this.signature = signature;
  }

  public List<UserGroupInfo> getUserGroups() {
    return userGroups;
  }

  public void setUserGroups( final List<UserGroupInfo> userGroups ) {
    this.userGroups = userGroups;
  }

  public List<UserInfo> getPermissions() {
    return permissions;
  }

  public void setPermissions( final List<UserInfo> permissions ) {
    this.permissions = permissions;
  }

  public ImageDetails getAsImageDetails() {
    ImageDetails i = new ImageDetails();
    i.setArchitecture( this.getArchitecture() );
    i.setImageId( this.getImageId() );
    i.setImageLocation( this.getImageLocation() );
    i.setImageOwnerId( this.getImageOwnerId() );
    i.setImageState( this.getImageState() );
    i.setImageType( this.getImageType() );
    i.setIsPublic( this.getPublic() );
    i.setKernelId( this.getKernelId() );
    i.setRamdiskId( this.getRamdiskId() );
    return i;
  }

  public void checkValid() {
    String[] parts = this.getImageLocation().split( "/" );
    CheckImageType check = new CheckImageType();
    check.setUserId( imageOwnerId );
    check.setBucket( parts[ 0 ] );
    check.setKey( parts[ 1 ] );
    Messaging.dispatch( WalrusProperties.WALRUS_REF, check );
  }

  public void triggerCaching() {
    String[] parts = this.getImageLocation().split( "/" );
    CacheImageType cache = new CacheImageType();
    cache.setUserId( imageOwnerId );
    cache.setBucket( parts[ 0 ] );
    cache.setKey( parts[ 1 ] );
    Messaging.dispatch( WalrusProperties.WALRUS_REF, cache );
  }

  public void invalidate() {
    String[] parts = this.getImageLocation().split( "/" );
    this.setImageState( "deregistered" );
    try {
      Messaging.dispatch( WalrusProperties.WALRUS_REF, new FlushCachedImageType( parts[ 0 ], parts[ 1 ] ) );
    } catch ( Exception e ) {}
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ImageInfo imageInfo = ( ImageInfo ) o;

    if ( !imageId.equals( imageInfo.imageId ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return imageId.hashCode();
  }

  public boolean isAllowed( UserInfo user ) {
    if ( user.isAdministrator() || user.getUserName().equals( this.getImageOwnerId() ) )
      return true;
    for ( UserGroupInfo g : this.getUserGroups() )
      if ( "all".equals( g.getName() ) )
        return true;
    return this.getPermissions().contains( user );
  }

  public static ImageInfo named( String imageId ) throws EucalyptusCloudException {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo image = null;
    try {
      image = db.getUnique( new ImageInfo( imageId ) );
    } finally {
      db.commit();
    }
    return image;
  }
}
