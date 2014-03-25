/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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
 ************************************************************************ 
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import org.apache.log4j.Logger;

import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.ImageMetadata.StaticDiskImage;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Strings;

public class StaticDiskImages {
  private static Logger LOG = Logger.getLogger( StaticDiskImages.class );
  
  //TODO: zhill - can we remove this method altogether once image service is done?
  public static void flush( final StaticDiskImage staticImage ) {
	  return;
    /*String[] parts = staticImage.getManifestLocation( ).split( "/" );
    try {
      AsyncRequests.dispatch( Topology.lookup( Walrus.class ), new FlushCachedImageType( parts[0], parts[1] ) );
    } catch ( Exception e ) {}
    */
  }
  
  //TODO: zhill - can we remove this method when image service is done?
  public static void prepare( String imageLocation ) throws Exception {
	  return;
    /*String[] parts = imageLocation.split( "/" );
    CacheImageType cache = new CacheImageType( ).regarding( Contexts.lookup( ).getRequest( ) );
    cache.setBucket( parts[0] );
    cache.setKey( parts[1] );
    try {
    	//Fix for EUCA-7554: this should be an async call.
    	AsyncRequests.dispatch( Topology.lookup( Walrus.class ), cache );    	
    } catch(Exception e) {
    	LOG.error("Error with cache image request for " + imageLocation,e);
    }
    */
  }
  
  public static void check( final StaticDiskImage staticImage ) throws Exception {
    if ( staticImage != null ) {
      ImageManifest manifest = ImageManifests.lookup( staticImage.getRunManifestLocation() );
      if ( Strings.isNullOrEmpty( manifest.getSignature( ) ) || !manifest.getSignature( ).equals( staticImage.getSignature() ) ) {
        throw new EucalyptusCloudException( "Manifest signature has changed since registration." );
      }
      LOG.info( "Triggering caching: " + staticImage.getRunManifestLocation( ) );
      if ( staticImage instanceof ImageMetadata.StaticDiskImage ) {
        StaticDiskImages.prepare( staticImage.getRunManifestLocation( ) );
      }
    }
  }
  
}
