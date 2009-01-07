package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.ResourceToken;

import java.util.*;

public interface Allocator {

  public abstract List<ResourceToken> allocate( String requestId, String userName, String vmtype, int min, int max, SortedSet<ClusterState> clusters ) throws NotEnoughResourcesAvailable;
}

