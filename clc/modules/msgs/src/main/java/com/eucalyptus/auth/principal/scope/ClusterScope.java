package com.eucalyptus.auth.principal.scope;

import com.eucalyptus.auth.principal.Cluster;

public interface ClusterScope extends Scope {
  public abstract Cluster getContainingCluster();
}
