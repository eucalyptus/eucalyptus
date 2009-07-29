package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.cluster.*;
import org.drools.*;
import org.drools.compiler.*;
import org.drools.rule.builder.dialect.java.JavaDialectConfiguration;

import java.io.*;
import java.util.List;

public class StateSnapshot {


  private StatefulSession workingMemory;

  StateSnapshot( String rules ) throws Exception {
    this.workingMemory = getWorkingMemory( rules );
  }

  public QueryResults findInstances( final String userId, final List<String> instancesSet ) {
    QueryResults res = null;
    if ( instancesSet == null || instancesSet.isEmpty() )
      res = workingMemory.getQueryResults( "list instances", new Object[]{ userId } );
    else
      res = workingMemory.getQueryResults( "describe instances", new Object[]{ userId, instancesSet } );
    return res;
  }

  private static StatefulSession getWorkingMemory( final String ruleFile ) throws Exception {
    InputStream in = SystemState.class.getResourceAsStream( ruleFile );
    Reader source = new InputStreamReader( in );
    PackageBuilderConfiguration pkgBuilderCfg = new PackageBuilderConfiguration();
    JavaDialectConfiguration javaConf = ( JavaDialectConfiguration ) pkgBuilderCfg.getDialectConfiguration( "java" );
    javaConf.setCompiler( JavaDialectConfiguration.JANINO );
    PackageBuilder builder = new PackageBuilder( pkgBuilderCfg );
    builder.addPackageFromDrl( source );
    org.drools.rule.Package pkg = builder.getPackage();
    RuleBase ruleBase = RuleBaseFactory.newRuleBase();
    ruleBase.addPackage( pkg );
    StatefulSession workingMemory = ruleBase.newStatefulSession();
    for ( VmInstance v : VmInstances.getInstance().listValues() )
      workingMemory.insert( v );
    return workingMemory;
  }

  public void insert( Object o ) {
    this.workingMemory.insert( o ); 
  }

  public void destroy() {
    this.workingMemory.dispose();
  }
}
