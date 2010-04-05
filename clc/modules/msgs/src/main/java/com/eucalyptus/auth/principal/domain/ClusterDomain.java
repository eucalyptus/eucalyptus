package com.eucalyptus.auth.principal.domain;

import com.eucalyptus.auth.principal.Cluster;

public interface ClusterDomain extends Domain {
  public abstract Cluster getContainingCluster();
}
