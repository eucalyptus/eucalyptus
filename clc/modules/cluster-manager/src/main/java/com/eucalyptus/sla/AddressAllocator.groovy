package com.eucalyptus.sla;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import static com.eucalyptus.address.Addresses.allocateSystemAddresses as alloc
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class AddressAllocator implements ResourceAllocator {
  
  private static String PUBLIC = "public";
  private static boolean isPublic( String addressingType ) {
    return "public".equals( addressingType ) || addressingType == null;
  }
  @Override
  public void allocate( VmAllocationInfo vmInfo ) {
    if ( isPublic( vmInfo.getRequest( ).getAddressingType( ) ) ) {
      vmInfo.allocationTokens.each{ ResourceToken it ->
        it.addresses += alloc( it.cluster, it.amount ).collect{ it.getName() };
      };
    }
  }
  @Override
  public void fail(VmAllocationInfo vmInfo, Throwable t) {
    vmInfo.allocationTokens.each{ ResourceToken it -> 
      it.addresses.each{ String addr ->
        Addresses.release( Addresses.getInstance().lookup( addr ) );
      }
    }
  }
}  