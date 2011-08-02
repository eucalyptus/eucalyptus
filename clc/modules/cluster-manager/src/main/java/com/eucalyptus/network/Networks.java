package com.eucalyptus.network;

import com.eucalyptus.event.StatefulNamedRegistry;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;


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

  @TypeMapper
  enum NetworkRulesGroupToNetwork implements Function<NetworkRulesGroup,Network> {
    INSTANCE;

    @Override
    public Network apply( NetworkRulesGroup arg0 ) {
      return arg0.getVmNetwork( );
    }
    
  }

  public static Network lookup( NetworkRulesGroup ruleGroup ) {
    return null;
  }
}
