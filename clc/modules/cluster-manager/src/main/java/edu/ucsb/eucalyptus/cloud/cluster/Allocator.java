package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.ResourceToken;

import java.util.*;

import com.eucalyptus.cluster.ClusterNodeState;

public interface Allocator {

  public abstract List<ResourceToken> allocate( String requestId, String userName, String vmtype, int min, int max, SortedSet<ClusterNodeState> clusters ) throws NotEnoughResourcesAvailable;
}

