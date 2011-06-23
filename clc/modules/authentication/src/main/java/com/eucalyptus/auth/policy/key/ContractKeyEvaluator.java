package com.eucalyptus.auth.policy.key;

import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Contract;
import com.google.common.collect.Maps;

public class ContractKeyEvaluator {
  
  private static final Logger LOG = Logger.getLogger( ContractKeyEvaluator.class );

  private Map<Contract.Type, Contract> contracts;
  
  public ContractKeyEvaluator( Map<Contract.Type, Contract> contracts ) {
    this.contracts = contracts;
  }
  
  public void addContract( ContractKey contractKey, Set<String> values ) {
    Contract update = contractKey.getContract( values.toArray( new String[0] ) );
    Contract current = contracts.get( update.getType( ) );
    if ( current == null || contractKey.isBetter( current, update ) ) {
      contracts.put( update.getType( ), update );
    }
  }
  
  public Map<Contract.Type, Contract> getContracts( ) {
    return this.contracts;
  }
  
}
