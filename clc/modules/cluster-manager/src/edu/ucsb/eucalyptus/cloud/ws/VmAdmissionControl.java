package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.FailScriptFailException;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.SLAs;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.cluster.Clusters;
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.cloud.entities.Counters;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.util.List;
import java.util.NavigableSet;

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
    boolean hasAddr = false;
    try {
      allocTokeList = sla.doVmAllocation( vmAllocInfo );
      int addrCount = 0;
      for ( ResourceToken token : allocTokeList ) {
        addrCount += token.getAmount();
      }
      if ( !EucalyptusProperties.disableNetworking && ( "public".equals( vmAllocInfo.getRequest().getAddressingType() ) || vmAllocInfo.getRequest().getAddressingType() == null ) ) {
        NavigableSet<String> addresses = AddressManager.allocateAddresses( addrCount );
        for ( ResourceToken token : allocTokeList ) {
          for ( int i = 0; i < token.getAmount(); i++ ) {
            token.getAddresses().add( addresses.pollFirst() );
          }
        }
      }
      hasAddr = true;
      vmAllocInfo.getAllocationTokens().addAll( allocTokeList );
      sla.doNetworkAllocation( vmAllocInfo.getRequest().getUserId(), vmAllocInfo.getAllocationTokens(), vmAllocInfo.getNetworks() );
    } catch ( FailScriptFailException e ) {
      failed = true;
    } catch ( NotEnoughResourcesAvailable notEnoughResourcesAvailable ) {
      failed = true;
    }
    if ( failed ) {
      if ( allocTokeList != null )
        for ( ResourceToken token : allocTokeList )
          Clusters.getInstance().lookup( token.getCluster() ).getNodeState().releaseToken( token );
      throw new EucalyptusCloudException( "Not enough resources available: " + (hasAddr?"vms":" addresses (try --addressing private)") );
    }

    return vmAllocInfo;
  }

}


