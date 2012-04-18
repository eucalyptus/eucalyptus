package com.eucalyptus.cluster.event;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.GenericEvent;

public class NewClusterEvent extends GenericEvent<Cluster> {}
public class TeardownClusterEvent extends GenericEvent<Cluster> {}
public class InitializeClusterEvent extends GenericEvent<Cluster> {}

