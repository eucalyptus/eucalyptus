/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.images;

import java.util.Collection;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public class Images {
  public static final String SELF = "self";
  public static final String DEFAULT_ROOT_DEVICE = "/dev/sda";
  public static final String DEFAULT_PARTITIONED_ROOT_DEVICE = "/dev/sda1";
  public static final String DEFAULT_EPHEMERAL_DEVICE = "/dev/sdb";

  public static Predicate<ImageInfo> filterExecutableBy( final Collection<String> executableSet ) {
    final boolean executableSelf = executableSet.remove( SELF );
    final boolean executableAll = executableSet.remove( "all" );
    return new Predicate<ImageInfo>( ) {
      @Override
      public boolean apply( ImageInfo image ) {
        if ( executableSet.isEmpty( ) && !executableSelf && !executableAll ) {
          return true;
        } else {
          UserFullName userFullName = Contexts.lookup().getUserFullName( );
          return
              ( executableAll && image.getImagePublic( ) ) ||
                  ( executableSelf && image.hasPermission( userFullName.getAccountNumber( ) ) ) ||
                  image.hasPermission( executableSet.toArray( new String[ executableSet.size() ] ) );
        }
      }

    };
  }

  /**
   * Predicate matching images in a standard state.
   *
   * @see com.eucalyptus.compute.common.ImageMetadata.State#standardState( )
   */
  public static Predicate<ImageInfo> standardStatePredicate( ) {
    return StandardStatePredicate.INSTANCE;
  }

  public static ImageInfo exampleWithImageId( final String imageId ) {
    return new ImageInfo( imageId );
  }

  public static ImageInfo exampleWithName( @Nullable final OwnerFullName owner,
                                           @Nullable final String name ) {
    final ImageInfo example = new ImageInfo( );
    example.setOwner( owner );
    example.setImageName( name );
    return example;
  }

  public static ImageInfo exampleWithImageState( final ImageMetadata.State state ) {
    final ImageInfo img = new ImageInfo( );
    img.setState( state );
    img.setStateChangeStack( null );
    img.setLastState( null );
    return img;
  }

  public static MachineImageInfo exampleMachineWithImageId( final String imageId ) {
    return new MachineImageInfo( imageId );
  }

  public static BlockStorageImageInfo exampleBlockStorageWithImageId( final String imageId ) {
    return new BlockStorageImageInfo( imageId );
  }

  public static BlockStorageImageInfo exampleBlockStorageWithSnapshotId( final String snapshotId ) {
    final BlockStorageImageInfo info = new BlockStorageImageInfo();
    info.setSnapshotId( snapshotId );
    return info;
  }

  public static BlockStorageDeviceMapping exampleBSDMappingWithSnapshotId( final String snapshotId ) {
    final BlockStorageDeviceMapping bsdm = new BlockStorageDeviceMapping();
    bsdm.setSnapshotId(snapshotId);
    return bsdm;
  }

  public static KernelImageInfo exampleKernelWithImageId( final String imageId ) {
    return new KernelImageInfo( imageId );
  }

  public static Function<ImageInfo, ImageDetails> TO_IMAGE_DETAILS = new Function<ImageInfo, ImageDetails>( ) {
    @Override
    public ImageDetails apply( ImageInfo input ) {
      return TypeMappers.transform( input, ImageDetails.class );
    }
  };

  private enum StandardStatePredicate implements Predicate<ImageInfo> {
    INSTANCE;

    @Override
    public boolean apply( final ImageInfo imageInfo ) {
      return imageInfo.getState( ).standardState( );
    }
  }

  public enum FilterImageStates implements Predicate<ImageInfo> {
    INSTANCE;
    @Override
    public boolean apply( ImageInfo input ) {
      if ( ImageMetadata.State.available.name().equals(input.getState().getExternalStateName()))
        return true;
      else
        return false;
    }
  }

  public enum FilterPermissions implements Predicate<ImageInfo> {
    INSTANCE;

    @Override
    public boolean apply( ImageInfo input ) {
      try {
        Context ctx = Contexts.lookup( );
        if ( ctx.isAdministrator( ) ) {
          return true;
        } else {
          UserFullName luser = ctx.getUserFullName( );
          /** GRZE: record why this must be so **/
          if ( input.getImagePublic( ) ) {
            return true;
          } else if ( input.getOwnerAccountNumber( ).equals( luser.getAccountNumber( ) ) ) {
            return true;
          } else if ( input.hasPermission( luser.getAccountNumber( ), luser.getUserId( ) ) ) {
            return true;
          } else {
            for ( AccessKey key : ctx.getUser( ).getKeys( ) ) {
              if ( input.hasPermission( key.getAccessKey( ) ) ) {
                return true;
              }
            }
            return false;
          }
        }
      } catch ( Exception ex ) {
        return false;
      }
    }
  }
}
