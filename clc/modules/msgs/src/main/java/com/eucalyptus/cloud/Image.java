package com.eucalyptus.cloud;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.HasOwningAccount;

@PolicyResourceType( vendor = PolicySpec.VENDOR_EC2, resource = PolicySpec.EC2_RESOURCE_IMAGE )
public interface Image extends HasFullName<Image>, HasOwningAccount {
  
  public interface StaticDiskImage extends Image {
    public abstract String getImageLocation( );
  }
  
  public static final String IMAGE_RAMDISK_PREFIX = "eri";
  public static final String IMAGE_KERNEL_PREFIX  = "eki";
  public static final String IMAGE_MACHINE_PREFIX = "emi";
  
  public enum Type {
    machine {
      @Override
      public String getTypePrefix( ) {
        return IMAGE_MACHINE_PREFIX;
      }
    },
    kernel {
      @Override
      public String getTypePrefix( ) {
        return IMAGE_KERNEL_PREFIX;
      }
    },
    ramdisk {
      @Override
      public String getTypePrefix( ) {
        return IMAGE_RAMDISK_PREFIX;
      }
    };
    public abstract String getTypePrefix( );
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
