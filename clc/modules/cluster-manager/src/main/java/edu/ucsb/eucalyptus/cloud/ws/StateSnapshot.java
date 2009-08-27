/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
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
