package com.eucalyptus.cluster;

import com.eucalyptus.event.StatefulNamedRegistry;

import edu.ucsb.eucalyptus.cloud.Network;

public class Networks extends StatefulNamedRegistry<Network, Networks.State>{
  public enum State {
    DISABLED,
    AWAITING_PEER,
    PENDING,
    ACTIVE,
  }
  private static Networks singleton = getInstance();

  private Networks( State... states ) {
    super( State.values( ) );
  }

  public static Networks getInstance()
  {
    synchronized ( Networks.class )
    {
      if ( singleton == null )
        singleton = new Networks();
    }
    return singleton;
  }

}
