package com.eucalyptus.cloud.run;


public interface ResourceAllocator {
  public void allocate( Allocation allocInfo ) throws Exception;
  public void fail( Allocation allocInfo, Throwable t );
  
}
