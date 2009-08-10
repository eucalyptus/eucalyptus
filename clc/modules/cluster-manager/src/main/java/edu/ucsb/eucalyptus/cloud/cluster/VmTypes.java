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
