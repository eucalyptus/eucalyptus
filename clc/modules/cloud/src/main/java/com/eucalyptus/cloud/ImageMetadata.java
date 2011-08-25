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
