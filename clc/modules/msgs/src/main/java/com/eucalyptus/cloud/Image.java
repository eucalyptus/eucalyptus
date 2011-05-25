package com.eucalyptus.cloud;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.HasOwningAccount;

@PolicyResourceType( vendor = PolicySpec.VENDOR_EC2, resource = PolicySpec.EC2_RESOURCE_IMAGE )
public interface Image extends HasFullName<Image>, HasOwningAccount {
  
  public interface StaticDiskImage extends Image {
    public abstract String getImageLocation( );
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
    xen, kvm, vmware
  }
  
  public enum DeviceMappingType {
    suppress, ephemeral, blockstorage
  }
  
  public enum Architecture {
    i386, x86_64
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
