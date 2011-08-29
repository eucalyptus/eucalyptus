package com.eucalyptus.network;

import com.eucalyptus.cloud.CloudMetadata;
import com.eucalyptus.event.StatefulNamedRegistry;


public class Networks extends StatefulNamedRegistry<NetworkGroup, Networks.State>{

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
