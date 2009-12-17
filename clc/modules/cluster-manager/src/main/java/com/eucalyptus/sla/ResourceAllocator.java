package com.eucalyptus.sla;

import java.util.List;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public interface ResourceAllocator {
  public void allocate( VmAllocationInfo vmInfo ) throws Exception;
  public void fail( VmAllocationInfo vmInfo, Throwable t );
  
}
