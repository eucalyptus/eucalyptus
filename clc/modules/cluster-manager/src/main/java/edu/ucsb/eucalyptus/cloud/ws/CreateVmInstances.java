package edu.ucsb.eucalyptus.cloud.ws;

import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;

public class CreateVmInstances {

  public VmAllocationInfo allocate( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException
  {
    String reservationId = VmInstances.getId( vmAllocInfo.getReservationIndex(), 0 ).replaceAll( "i-", "r-" );
    int i = 1; /*<--- this corresponds to the first instance id CANT COLLIDE WITH RSVID             */
    for ( ResourceToken token : vmAllocInfo.getAllocationTokens() )
      for ( int j = token.getAmount(); j > 0; j-- )
      {
        VmInstance vmInst = new VmInstance( reservationId,
                                            i - 1,
                                            VmInstances.getId( vmAllocInfo.getReservationIndex(), i++ ),
                                            vmAllocInfo.getRequest().getUserId(),
                                            token.getCluster(),
                                            vmAllocInfo.getUserData(),
                                            vmAllocInfo.getImageInfo(),
                                            vmAllocInfo.getKeyInfo(),
                                            vmAllocInfo.getVmTypeInfo(),
                                            vmAllocInfo.getNetworks() );
        VmInstances.getInstance().register( vmInst );
        token.getInstanceIds().add( vmInst.getInstanceId() );
      }
    vmAllocInfo.setReservationId( reservationId );
    return vmAllocInfo;
  }

}
