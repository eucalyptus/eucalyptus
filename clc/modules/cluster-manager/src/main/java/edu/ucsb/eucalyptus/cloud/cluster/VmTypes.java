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
package edu.ucsb.eucalyptus.cloud.cluster;

import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.VmType;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class VmTypes {

  private static VmTypes singleton = new VmTypes();
  private ConcurrentNavigableMap<String,VmType> vmTypeMap;

  private VmTypes()
  {
    this.vmTypeMap = new ConcurrentSkipListMap<String,VmType>();
    this.update();
  }

  public static synchronized void update( Set<VmType> newVmTypes ) throws EucalyptusCloudException
  {
    NavigableSet<VmType> newList = VmTypes.list();
    if( newVmTypes.size() != newList.size() )
      throw new EucalyptusCloudException( "Proposed VmTypes fail to satisfy well-ordering requirement.");
    for( VmType newVm : newVmTypes ) {
      if( !singleton.vmTypeMap.containsValue( newVm ) ) {
        EntityWrapper<VmType> db = new EntityWrapper<VmType>();
        try {
          VmType oldVm = db.getUnique( new VmType( newVm.getName() ) );
          oldVm.setCpu( newVm.getCpu() );
          oldVm.setDisk( newVm.getDisk() );
          oldVm.setMemory( newVm.getMemory() );
          db.commit();
        }
        catch ( EucalyptusCloudException e ) {
          db.rollback();
          throw e;
        }
      }
    }
  }

  private synchronized void update()
  {
    EntityWrapper<VmType> db = new EntityWrapper<VmType>();
    List<VmType> vmTypeList = db.query( new VmType() );

    for(VmType v : vmTypeList)
    {
      this.vmTypeMap.putIfAbsent( v.getName(), v );

      if( !this.vmTypeMap.get( v.getName()).equals( v ) )
        this.vmTypeMap.replace( v.getName(), v );
    }

    if( vmTypeList.isEmpty() )
    {
      db.add( new VmType("m1.small",1,10,128) );
      db.add( new VmType("c1.medium",2,10,128) );
      db.add( new VmType("m1.large",2,10,512) );
      db.add( new VmType("m1.xlarge",2,10,1024) );
      db.add( new VmType("c1.xlarge",4,10,2048) );
    }

    db.commit();
  }

  public static synchronized VmType getVmType( String name )
  {
    singleton.update();
    return singleton.vmTypeMap.get( name );
  }

  public static synchronized NavigableSet<VmType> list()
  {
    singleton.update();
    return new TreeSet<VmType>( singleton.vmTypeMap.values() );
  }

}
