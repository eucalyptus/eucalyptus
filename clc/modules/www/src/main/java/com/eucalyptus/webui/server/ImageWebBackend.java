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

package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.images.MachineImageInfo;
import com.eucalyptus.images.PutGetImageInfo;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ImageDetails;

public class ImageWebBackend {
  
  private static final Logger LOG = Logger.getLogger( ImageWebBackend.class );
  
  public static final String ID = "id";
  public static final String NAME = "Name";
  public static final String MANIFEST = "Manifest";
  public static final String DESC = "Description";
  public static final String OWNER = "owner";
  public static final String ARCH = "architecture";
  public static final String STATE = "state";
  public static final String PUBLIC = "public";
  public static final String TYPE = "type";
  public static final String PLATFORM = "platform";
  public static final String KERNEL = "kernel";
  public static final String RAMDISK = "ramdisk";

  public static final ArrayList<SearchResultFieldDesc> COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( MANIFEST, "Manifest Location", false, "30%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( KERNEL, "Kernel", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( RAMDISK, "Ramdisk", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( STATE, "State", false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( TYPE, "Type", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNER, "Owner", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ARCH, "Architecture", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PUBLIC, "Public", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PLATFORM, "Platform", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( DESC, "Description", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
  }
  
  private static Function<ImageInfo, SearchResultRow> TO_IMAGE_SEARCH_RESULT = new Function<ImageInfo, SearchResultRow>( ) {
    @Override
    public SearchResultRow apply( ImageInfo image ) {
      return serializeImage( image );
    }
  };
    
  public static List<SearchResultRow> searchImages( final User requestUser, String query ) throws EucalyptusServiceException {
    Predicate<ImageInfo> inter_account_permission_filter = new Predicate<ImageInfo>( ) {
      @Override
      public boolean apply( ImageInfo input ) {
        try {
          if ( requestUser.isSystemAdmin( ) ) {
            return true;
          } else {
            if ( input.getImagePublic( ) ) {
              return true;
            } else if ( input.getOwnerAccountNumber( ).equals( requestUser.getAccount( ).getAccountNumber( ) ) ) {
              return true;
            } else if ( input.hasPermission( requestUser.getAccount( ).getAccountNumber( ), requestUser.getUserId( ) ) ) {
              return true;
            } else {
              for ( AccessKey key : requestUser.getKeys( ) ) {
                if ( input.hasPermission( key.getAccessKey( ) ) ) {
                  return true;
                }
              }
              return false;
            }
          }
        } catch ( Exception e ) {
          LOG.error( e, e );
          return false;
        }
      }
    };
    Predicate<ImageInfo> intra_account_permission_filter = new Predicate<ImageInfo>( ) {
      @Override
      public boolean apply( ImageInfo input) {
        return Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_IMAGE, input.getDisplayName( ), null, PolicySpec.EC2_DESCRIBEIMAGES, requestUser );
      }      
    };
    try {
      return Transactions.filteredTransform( new ImageInfo( ), Predicates.<ImageInfo>and( inter_account_permission_filter, intra_account_permission_filter ), TO_IMAGE_SEARCH_RESULT );
    } catch ( Exception e ) {
      LOG.error( "Failed to lookup image", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get image info", e );
    }
  }

  private static SearchResultRow serializeImage( ImageInfo image ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( image.getDisplayName( ) );
    result.addField( image.getImageName( ) );
    if ( image instanceof PutGetImageInfo ) {
      result.addField( ( ( PutGetImageInfo ) image ).getManifestLocation( ) );
    } else {
      result.addField( "" );
    }
    if ( image instanceof MachineImageInfo ) {
      result.addField( ( ( MachineImageInfo ) image ).getKernelId( ) );
    } else {
      result.addField( "" );
    }
    if ( image instanceof MachineImageInfo ) {
      result.addField( ( ( MachineImageInfo ) image ).getRamdiskId( ) );
    } else {
      result.addField( "" );
    }
    result.addField( image.getState( ).toString( ) );
    result.addField( image.getImageType( ).toString( ) );
    result.addField( image.getOwnerAccountNumber( ) );
    result.addField( image.getArchitecture( ).toString( ) );
    result.addField( image.getImagePublic( ).toString( ) );
    result.addField( image.getPlatform( ).toString( ) );
    result.addField( image.getDescription( ) );
    return result;
  }
  
}
