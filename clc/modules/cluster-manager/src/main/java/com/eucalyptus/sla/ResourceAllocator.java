package com.eucalyptus.sla;

import com.eucalyptus.cloud.run.Allocations.Allocation;

public interface ResourceAllocator {
  public void allocate( Allocation allocInfo ) throws Exception;
  public void fail( Allocation allocInfo, Throwable t );
  
}
