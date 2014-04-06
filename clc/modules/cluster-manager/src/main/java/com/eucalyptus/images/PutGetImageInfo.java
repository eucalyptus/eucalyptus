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

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.ImageMetadata;

@MappedSuperclass
public class PutGetImageInfo extends ImageInfo implements ImageMetadata.StaticDiskImage {
  @Column( name = "metadata_image_manifest_path" )
  private String manifestLocation;
  
  @Lob
  @org.hibernate.annotations.Type(type="org.hibernate.type.StringClobType")
  @Column( name = "metadata_image_signature" )
  private String signature;
  
  @Column( name = "metadata_image_unencrypted_checksum" )
  private String checksum;
  
  @Column( name = "metadata_image_unencrypted_checksum_type" )
  private String checksumType;
  
  @Column( name = "metadata_image_bundle_size" )
  private Long   bundleSizeBytes;
  
  protected PutGetImageInfo( final UserFullName userFullName, final String imageId,
                             final ImageMetadata.Type imageType, final String imageName, final String imageDescription, final Long imageSizeBytes,
                             final ImageMetadata.Architecture arch, final ImageMetadata.Platform platform,
                             final String manifestLocation, final Long imageBundleSizeBytes, final String imageChecksum, final String imageChecksumType) {
    super( userFullName, imageId, imageType, imageName, imageDescription, imageSizeBytes, arch, platform );
    checkParam( manifestLocation, notNullValue() );
    this.manifestLocation = manifestLocation;
    this.bundleSizeBytes = imageBundleSizeBytes;
    this.checksum = imageChecksum;
    this.checksumType = imageChecksumType;
  }
  
  protected PutGetImageInfo( final ImageMetadata.Type imageType ) {
    super( imageType );
  }
  
  protected PutGetImageInfo( final ImageMetadata.Type imageType, final String imageId ) {
    super( imageType, imageId );
  }
  
  @Override
  public String getSignature( ) {
    return this.signature;
  }
  
  public void setSignature( final String signature ) {
    this.signature = signature;
  }
  
  @Override
  public String getManifestLocation( ) {
    return this.manifestLocation;
  }
  
  public void setManifestLocation( final String manifestLocation ) {
    this.manifestLocation = manifestLocation;
  }
  
  public Long getBundleSizeBytes( ) {
    return this.bundleSizeBytes;
  }
  
  public void setBundleSizeBytes( final Long bundleSizeBytes ) {
    this.bundleSizeBytes = bundleSizeBytes;
  }
  
  public String getChecksum( ) {
    return this.checksum;
  }
  
  public void setChecksum( String checksum ) {
    this.checksum = checksum;
  }
  
  public String getChecksumType( ) {
    return this.checksumType;
  }
  
  public void setChecksumType( String checksumType ) {
    this.checksumType = checksumType;
  }

  @Override
  public String getRunManifestLocation() {
    return this.getManifestLocation();
  }
  
}
