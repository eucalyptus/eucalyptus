package com.eucalyptus.cluster;

import com.eucalyptus.event.StatefulNamedRegistry;

import edu.ucsb.eucalyptus.cloud.Network;

public class Networks extends StatefulNamedRegistry<Network, Networks.State>{
  public enum State {
    ACTIVE,
    DISABLED,
    AWAITING_PEER,
  }
  private static Networks singleton = getInstance();

  public Networks( State... states ) {
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
