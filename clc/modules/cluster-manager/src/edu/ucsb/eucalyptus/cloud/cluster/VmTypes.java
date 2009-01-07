package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;

import java.util.*;
import java.util.concurrent.*;

public class VmTypes {

  private static VmTypes singleton = new VmTypes();
  private ConcurrentNavigableMap<String,VmType> vmTypeMap;

  private VmTypes()
  {
    this.vmTypeMap = new ConcurrentSkipListMap<String,VmType>();
    this.update();
  }

  public static synchronized void update( String name, int cpu, int disk, int memory ) throws EucalyptusCloudException
  {
    VmType sameVm = VmTypes.getVmType( name );
    if( sameVm.getDisk() == disk
        && sameVm.getCpu() == cpu
        && sameVm.getMemory() == memory )
      return;
    VmType temp = new VmType(name, cpu, disk, memory );
    for( VmType vm : VmTypes.list() )
      if( !vm.getName().equals( name )  && vm.compareTo( temp ) == 0 && temp.compareTo( vm ) == 0 )
        throw new EucalyptusCloudException( "Proposed VmType fails to satisfy well-ordering requirement.");
    EntityWrapper<VmType> db = new EntityWrapper<VmType>();
    try
    {
      sameVm = db.getUnique( new VmType( name ) );
      sameVm.setCpu( cpu );
      sameVm.setDisk( disk );
      sameVm.setMemory( memory );
      db.commit();
    }
    catch ( EucalyptusCloudException e )
    {
      db.rollback();
      throw e;
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
