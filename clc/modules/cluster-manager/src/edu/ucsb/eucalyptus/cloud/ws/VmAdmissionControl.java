package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.util.List;

public class VmAdmissionControl {

  private static Logger LOG = Logger.getLogger( VmAdmissionControl.class );

  public VmAllocationInfo verify( RunInstancesType request ) throws EucalyptusCloudException {
    //:: encapsulate the request into a VmAllocationInfo object and forward it on :://
    VmAllocationInfo vmAllocInfo = new VmAllocationInfo( request );

    vmAllocInfo.setReservationIndex( Counters.getIdBlock( request.getMaxCount() ) );

    String userData = vmAllocInfo.getRequest().getUserData();
    if ( userData != null )
      userData = new String( Base64.decode( vmAllocInfo.getRequest().getUserData() ) );
    else { userData = ""; }
    vmAllocInfo.setUserData( userData );
    vmAllocInfo.getRequest().setUserData( new String( Base64.encode( userData.getBytes() ) ) );
    return vmAllocInfo;
  }

  public VmAllocationInfo evaluate( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    SLAs sla = new SLAs();
    List<ResourceToken> allocTokeList = null;
    boolean failed = false;
    try {
      allocTokeList = sla.doVmAllocation( vmAllocInfo );
      vmAllocInfo.getAllocationTokens().addAll( allocTokeList );
      try {
        sla.doNetworkAllocation( vmAllocInfo.getRequest().getUserId(), vmAllocInfo.getAllocationTokens(), vmAllocInfo.getNetworks() );
      } catch ( NotEnoughResourcesAvailable notEnoughResourcesAvailable ) {
        failed = true;
      }
    } catch ( FailScriptFailException e ) {
      failed = true;
    } catch ( NotEnoughResourcesAvailable notEnoughResourcesAvailable ) {
      failed = true;
    }
    if ( failed ) {
      if ( allocTokeList != null )
        for ( ResourceToken token : allocTokeList )
          Clusters.getInstance().lookup( token.getCluster() ).getState().releaseResourceToken( token );
      throw new EucalyptusCloudException( "Not enough resources available." );
    }

    return vmAllocInfo;
  }

}


