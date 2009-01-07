package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.msgs.*;

import java.util.*;

public class VmReplyTransform {

  public RunInstancesResponseType allocate( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException
  {
    RunInstancesResponseType reply = vmAllocInfo.getReply();

    List<String> networkNames = new ArrayList<String>();
    for( Network vmNet : vmAllocInfo.getNetworks() ) networkNames.add( vmNet.getName() );

    ReservationInfoType reservation = new ReservationInfoType( vmAllocInfo.getReservationId(),
                                                               reply.getUserId(),
                                                               networkNames );

    for( ResourceToken allocToken : vmAllocInfo.getAllocationTokens() )
      for( String instId : allocToken.getInstanceIds() )
        reservation.getInstancesSet().add( VmInstances.getInstance().lookup( instId ).getAsRunningInstanceItemType() );

    reply.setRsvInfo( reservation );
    return vmAllocInfo.getReply();
  }

}
