package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class VmAllocationTransaction {

  private static Logger LOG = Logger.getLogger( VmAllocationTransaction.class );

  private Map<String, ClusterAllocator> clusterMap;
  private VmAllocationInfo vmAllocInfo;

  public VmAllocationTransaction( final VmAllocationInfo vmAllocInfo )
  {
    this.vmAllocInfo = vmAllocInfo;
    this.clusterMap = new ConcurrentHashMap<String, ClusterAllocator>();
    for ( ResourceToken token : this.vmAllocInfo.getAllocationTokens() )
      this.clusterMap.put(token.getCluster(), new ClusterAllocator(token,vmAllocInfo) );
  }

  public void start()
  {
    for(ClusterAllocator c : this.clusterMap.values() )
      c.start();
  }

}
