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
 ************************************************************************/

package com.eucalyptus.cloud;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.util.OwnerFullName;

/** GRZE:WARN: values are intentionally opaque strings and /not/ a symbolic reference. **/
@PolicyResourceType( "image" )
public interface ImageMetadata extends CloudMetadata {
  
  public interface StaticDiskImage extends ImageMetadata {
    public abstract String getManifestLocation( );
    
    public abstract String getSignature( );
  }
  
  public static final String TYPE_MANIFEST_XPATH = "/manifest/image/type/text()";
  
  public enum Type {
    machine {
      @Override
      public String getTypePrefix( ) {
        return "emi";
      }
      
      @Override
      public String getManifestPath( ) {
        return "/manifest/image/type/text()";
      }
    },
    kernel {
      @Override
      public String getTypePrefix( ) {
        return "eki";
      }
      
      @Override
      public String getNamePrefix( ) {
        return "vmlinuz";
      }
      
      @Override
      public String getManifestPath( ) {
        return "/manifest/machine_configuration/kernel_id/text()";
      }
    },
    ramdisk {
      @Override
      public String getTypePrefix( ) {
        return "eri";
      }
      
      @Override
      public String getNamePrefix( ) {
        return "initrd";
      }
      
      @Override
      public String getManifestPath( ) {
        return "/manifest/machine_configuration/ramdisk_id/text()";
      }
    };
    public abstract String getTypePrefix( );
    
    public abstract String getManifestPath( );
    
    public String getNamePrefix( ) {
      return SystemIds.cloudName( );//a really large random string which should never appear
    }
  }
  
  public enum State {
    pending, available, failed, deregistered
  }
  
  public enum VirtualizationType {
    paravirtualized, hvm
  }
  
  public enum Hypervisor {
    xen, kvm, other
  }
  
  public enum DeviceMappingType {
    root, swap, suppress, ephemeral, blockstorage
  }
  
  public enum Architecture {
    i386, x86_64, other
  }
  
  public enum Platform {
    linux {
      public String toString( ) {
        return "";
      }
    },
    windows {
      public String toString( ) {
        return this.name( );
      }
    };
    
  }
}
