package com.eucalyptus.auth.policy.key;

import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Contract;
import com.google.common.collect.Maps;

public class ContractKeyEvaluator {
  
  private static final Logger LOG = Logger.getLogger( ContractKeyEvaluator.class );

  private Map<String, Contract> contracts = Maps.newHashMap( );
  
  public ContractKeyEvaluator( ) {
  }
  
  public void addContract( ContractKey contractKey, Set<String> values ) {
    Contract contract = contractKey.getContract( values.toArray( new String[0] ) );
    if ( contracts.containsKey( contract.getName( ) ) ) {
      LOG.warn( "Contract key conflicts: " + contract.getName( ) );
    }
    contracts.put( contract.getName( ), contract );
  }
  
  public Map<String, Contract> getContracts( ) {
    return this.contracts;
  }
  
}
