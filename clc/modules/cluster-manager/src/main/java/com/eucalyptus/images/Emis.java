/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Lookup;
import com.eucalyptus.util.Lookups;
import com.google.common.base.Preconditions;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class Emis {
  public static String                   IMAGE_MACHINE          = "machine";
  public static String                   IMAGE_KERNEL           = "kernel";
  public static String                   IMAGE_RAMDISK          = "ramdisk";
  public static String                   IMAGE_MACHINE_PREFIX   = "emi";
  public static String                   IMAGE_KERNEL_PREFIX    = "eki";
  public static String                   IMAGE_RAMDISK_PREFIX   = "eri";
  public static String                   IMAGE_PLATFORM_DEFAULT = "linux";
  public static String                   IMAGE_PLATFORM_WINDOWS = "windows";
  private static final Lookup<ImageInfo> lookupImg              = new Lookup<ImageInfo>( ) {
                                                                  @Override
                                                                  public ImageInfo lookup( String identifier ) {
                                                                    return EntityWrapper.get( ImageInfo.class ).lookupAndClose( new ImageInfo( identifier ) );
                                                                  }
                                                                };
  private static Logger                  LOG                    = Logger.getLogger( Emis.class );
  
  public static class BootableSet {
    private final ImageInfo disk;
    
    private BootableSet( ImageInfo disk ) {
      this.disk = disk;
    }
    
    public ImageInfo getMachine( ) {
      return this.disk;
    }
    
    public ImageInfo getRamdisk( ) {
      throw new NoSuchElementException( this + " does not have a kernel." );
    }
    
    public ImageInfo getKernel( ) {
      throw new NoSuchElementException( this + " does not have a kernel." );
    }
    
    public boolean hasKernel( ) {
      try {
        this.getKernel( );
        return false;
      } catch ( NoSuchElementException ex ) {
        return false;
      }
    }
    
    public boolean hasRamdisk( ) {
      try {
        this.getRamdisk( );
        return false;
      } catch ( NoSuchElementException ex ) {
        return false;
      }
    }
    
    public boolean isLinux( ) {
      return IMAGE_PLATFORM_DEFAULT.equals( this.getMachine( ).getPlatform( ) ) || this.getMachine( ).getPlatform( ) == null;
    }
    
    @Override
    public String toString( ) {
      return String.format( "BootableSet:machine=%s:ramdisk=%s:kernel=%s:isLinux=%s", this.getMachine( ),
                            this.hasRamdisk( )
                              ? this.getRamdisk( )
                              : "false",
                              this.hasKernel( )
                                ? this.getKernel( )
                                : "false",
                                this.isLinux( ) );
    }
    
    public void populateVirtualBootRecord( VmTypeInfo vmType ) throws EucalyptusCloudException {
      Long imgSize = ImageUtil.getSize( this.getMachine( ).getImageLocation( ) );
      if ( imgSize > 1024l * 1024l * 1024l * vmType.getDisk( ) ) {
        throw new EucalyptusCloudException( "image too large [size=" + imgSize / ( 1024l * 1024l ) + "MB] for instance type " + vmType.getName( ) + " [disk="
                                            + vmType.getDisk( ) * 1024l + "MB]" );
      }
      
      vmType.setRoot( this.getMachine( ).getImageId( ), this.getMachine( ).getImageLocation( ), imgSize * 1024 );
      if ( this.hasKernel( ) ) {
        vmType.setKernel( this.getKernel( ).getImageId( ), this.getKernel( ).getImageLocation( ) );
      }
      if ( this.hasRamdisk( ) ) {
        vmType.setRamdisk( this.getRamdisk( ).getImageId( ), this.getRamdisk( ).getImageLocation( ) );
      }
    }
  }
  
  static class NoRamdiskBootableSet extends BootableSet {
    private final ImageInfo kernel;
    
    private NoRamdiskBootableSet( ImageInfo disk, ImageInfo kernel ) {
      super( disk );
      this.kernel = kernel;
    }
    
    @Override
    public ImageInfo getKernel( ) {
      return this.kernel;
    }
  }
  
  static class TrifectaBootableSet extends NoRamdiskBootableSet {
    private final ImageInfo ramdisk;
    
    public TrifectaBootableSet( ImageInfo disk, ImageInfo kernel, ImageInfo ramdisk ) {
      super( disk, kernel );
      this.ramdisk = ramdisk;
    }
    
    @Override
    public ImageInfo getRamdisk( ) {
      return this.ramdisk;
    }
  }
  
  public static BootableSet newBootableSet( String imageId ) throws EucalyptusCloudException {
    try {
      return new BootableSet( Lookups.doPrivileged( imageId, lookupImg ) );
    } catch ( AuthException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    } catch ( IllegalContextAccessException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    } catch ( PersistenceException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    }
  }
  
  public static BootableSet bootsetWithKernel( BootableSet bootSet ) throws EucalyptusCloudException {
    String kernelId = determineKernelId( bootSet );
    try {
      ImageInfo kernel = Lookups.doPrivileged( kernelId, lookupImg );
      return new NoRamdiskBootableSet( bootSet.getMachine( ), kernel );
    } catch ( AuthException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    } catch ( IllegalContextAccessException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    } catch ( PersistenceException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( ex );
    }
  }
  
  public static BootableSet bootsetWithRamdisk( BootableSet bootSet ) throws EucalyptusCloudException {
    String ramdiskId = determineRamdiskId( bootSet );
    if( ramdiskId == null ) {
      return bootSet;
    } else {
      try {
        ImageInfo ramdisk = Lookups.doPrivileged( ramdiskId, lookupImg );
        return new TrifectaBootableSet( bootSet.getMachine( ), bootSet.getKernel( ), ramdisk );
      } catch ( AuthException ex ) {
        LOG.error( ex, ex );
        throw new EucalyptusCloudException( ex );
      } catch ( IllegalContextAccessException ex ) {
        LOG.error( ex, ex );
        throw new EucalyptusCloudException( ex );
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
        throw new EucalyptusCloudException( ex );
      } catch ( PersistenceException ex ) {
        LOG.error( ex, ex );
        throw new EucalyptusCloudException( ex );
      }
    }
  }
  
  private static String determineKernelId( BootableSet bootSet ) throws EucalyptusCloudException {
    ImageInfo disk = bootSet.getMachine( );
    String kernelId = null;
    Context ctx = null;
    try {
      ctx = Contexts.lookup( );
      if ( ctx.getRequest( ) instanceof RunInstancesType ) {
        kernelId = ( ( RunInstancesType ) ctx.getRequest( ) ).getKernelId( );
      }
    } catch ( IllegalContextAccessException ex ) {
      LOG.error( ex, ex );
    }
    if ( kernelId == null || "".equals( kernelId ) ) {
      kernelId = disk.getKernelId( );
    }
    if ( kernelId == null || "".equals( kernelId ) ) {
      kernelId = SystemConfiguration.getSystemConfiguration( ).getDefaultKernel( );
    }
    Preconditions.checkNotNull( kernelId, "Attempt to resolve a kerneId for " + bootSet.toString( ) + " during request " + ( ctx != null
      ? ctx.getRequest( ).toSimpleString( )
      : "UNKNOWN" ) );
    if ( kernelId == null ) {
      throw new EucalyptusCloudException( "Unable to determine required kernel image for " + disk.getName( ) );
    } else if ( kernelId.startsWith( Emis.IMAGE_KERNEL_PREFIX ) ) {
      throw new EucalyptusCloudException( "Image specified is not a kernel: " + kernelId );
    }
    return kernelId;
  }
  
  private static String determineRamdiskId( BootableSet bootSet ) throws EucalyptusCloudException {
    if ( !bootSet.hasKernel( ) ) {
      throw new EucalyptusCloudException( "Image specified does not have a kernel: " + bootSet );
    }
    boolean skipRamdisk = false;
    String ramdiskId = null;
    Context ctx = null;
    try {
      ctx = Contexts.lookup( );
      if ( ctx.getRequest( ) instanceof RunInstancesType ) {
        RunInstancesType msg = (RunInstancesType)ctx.getRequest( );
        if( ImageUtil.isSet( msg.getKernelId( ) ) ) {
          skipRamdisk |= ( !ImageUtil.isSet( msg.getRamdiskId( ) ) );//explicit kernel given w/o rd
        } else {
          skipRamdisk |= ( ImageUtil.isSet( bootSet.getMachine( ).getKernelId( ) ) 
              && !ImageUtil.isSet( bootSet.getMachine( ).getRamdiskId( ) ) 
              && !ImageUtil.isSet( msg.getRamdiskId( ) ) );//no explicit kernel, skip default ramdiskId if none other given
        }
        ramdiskId = ( ( RunInstancesType ) ctx.getRequest( ) ).getRamdiskId( );
      }
    } catch ( IllegalContextAccessException ex ) {
      LOG.error( ex, ex );
    }
    if ( ramdiskId == null || "".equals( ramdiskId ) ) {
      ramdiskId = bootSet.getMachine( ).getRamdiskId( );
    }
    if ( ramdiskId == null || "".equals( ramdiskId ) ) {
      ramdiskId = SystemConfiguration.getSystemConfiguration( ).getDefaultRamdisk( );
    }
    Preconditions.checkNotNull( ramdiskId, "Attempt to resolve a ramdiskId for " + bootSet.toString( ) + " during request " + ( ctx != null
        ? ctx.getRequest( ).toSimpleString( )
        : "UNKNOWN" ) );
    if ( ramdiskId == null ) {
      throw new EucalyptusCloudException( "Unable to determine required ramdisk image for " + bootSet.toString( )  );
    } else if ( ramdiskId.startsWith( Emis.IMAGE_RAMDISK_PREFIX ) ) {
      throw new EucalyptusCloudException( "Image specified is not a ramdisk: " + ramdiskId );
    }
    return ramdiskId;
  }
  
  public static void checkStoredImage( BootableSet bootSet ) {
    try {
      ImageUtil.checkStoredImage( bootSet.getMachine( ) );
      if ( bootSet.hasKernel( ) ) {
        ImageUtil.checkStoredImage( bootSet.getKernel( ) );
      }
      if ( bootSet.hasRamdisk( ) ) {
        ImageUtil.checkStoredImage( bootSet.getRamdisk( ) );
      }
    } catch ( EucalyptusCloudException ex ) {
      LOG.error( ex, ex );
    }
  }
  
}
