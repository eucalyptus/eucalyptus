package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmTypeVerify {

  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException
  {
    String instanceType = vmAllocInfo.getRequest().getInstanceType();
    VmType v = VmTypes.getVmType( (instanceType==null)?"m1.small":instanceType );
    if( v == null ) throw new EucalyptusCloudException( "instance type does not exist: " + vmAllocInfo.getRequest().getInstanceType() );

    vmAllocInfo.setVmTypeInfo( new VmTypeInfo( v.getName(),v.getMemory(),v.getDisk(),v.getCpu() ) );
    return vmAllocInfo;
  }

}
