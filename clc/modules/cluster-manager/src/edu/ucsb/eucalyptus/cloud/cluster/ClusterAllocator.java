package edu.ucsb.eucalyptus.cloud.cluster;

import com.google.common.collect.*;
import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

class ClusterAllocator extends Thread {

  private static Logger LOG = Logger.getLogger( ClusterAllocator.class );

  private State state;
  private AtomicBoolean rollback;
  protected Multimap<State, QueuedEvent> msgMap;
  private Cluster cluster;
  private ConcurrentLinkedQueue<QueuedEvent> pendingEvents;
  private VmAllocationInfo vmAllocInfo;

  public ClusterAllocator( ResourceToken vmToken, VmAllocationInfo vmAllocInfo ) {
    this.msgMap = Multimaps.newHashMultimap();
    this.vmAllocInfo = vmAllocInfo;
    this.pendingEvents = new ConcurrentLinkedQueue<QueuedEvent>();
    this.cluster = Clusters.getInstance().lookup( vmToken.getCluster() );
    this.state = State.START;
    this.rollback = new AtomicBoolean( false );
    for ( NetworkToken networkToken : vmToken.getNetworkTokens() )
      this.setupNetworkMessages( networkToken );
    this.setupVmMessages( vmToken );
  }

  public void setupNetworkMessages( NetworkToken networkToken ) {
    if ( networkToken != null ) {
      StartNetworkType msg = new StartNetworkType( this.vmAllocInfo.getRequest(), networkToken.getVlan(), networkToken.getNetworkName() );
      StartNetworkCallback callback = new StartNetworkCallback( this, networkToken );
      QueuedEvent<StartNetworkType> event = new QueuedEvent<StartNetworkType>( callback, msg );
      this.msgMap.put( State.CREATE_NETWORK, event );
    }
    try {
      Network network = Networks.getInstance().lookup( networkToken.getName() );
      if ( network.getRules().isEmpty() ) return;
      ConfigureNetworkCallback callback = new ConfigureNetworkCallback();
      ConfigureNetworkType msg = new ConfigureNetworkType( this.vmAllocInfo.getRequest(), network.getRules() );
      QueuedEvent event = new QueuedEvent<ConfigureNetworkType>( callback, msg );
      this.msgMap.put( State.CREATE_NETWORK_RULES, event );
    } catch ( NoSuchElementException e ) {}/* just added this network, shouldn't happen, if so just smile and nod */
  }

  public void setupVmMessages( ResourceToken token ) {
    List<String> macs = new ArrayList<String>();
    List<String> networkNames = new ArrayList<String>();

    for ( String instanceId : token.getInstanceIds() )
      macs.add( VmInstances.getAsMAC( instanceId ) );

    int vlan = -1;
    for ( Network net : vmAllocInfo.getNetworks() ) {
      networkNames.add( net.getNetworkName() );
      if ( vlan < 0 ) vlan = Networks.getInstance().lookup( net.getName() ).getToken( token.getCluster() ).getVlan();
    }
    if ( vlan < 0 ) vlan = 9;

    RunInstancesType request = this.vmAllocInfo.getRequest();
    VmImageInfo imgInfo = this.vmAllocInfo.getImageInfo();
    VmTypeInfo vmInfo = this.vmAllocInfo.getVmTypeInfo();
    String rsvId = this.vmAllocInfo.getReservationId();
    VmKeyInfo keyInfo = this.vmAllocInfo.getKeyInfo();

    VmRunType run = new VmRunType( request, rsvId, request.getUserData(), token.getAmount(), imgInfo, vmInfo, keyInfo, token.getInstanceIds(), macs, vlan, networkNames );
    this.msgMap.put( State.CREATE_VMS, new QueuedEvent<VmRunType>( new VmRunCallback( this, token ), run ) );
  }

  public void setState( final State state ) {
    this.clearQueue();
    if ( this.rollback.get() && !State.ROLLBACK.equals( this.state ) )
      this.state = State.ROLLBACK;
    else if ( this.rollback.get() && State.ROLLBACK.equals( this.state ) )
      this.state = State.FINISHED;
    else
      this.state = state;
  }

  public void run() {
    this.state = State.CREATE_NETWORK;
    while ( !this.state.equals( State.FINISHED ) ) {
      switch ( this.state ) {
        case CREATE_NETWORK:
          this.queueEvents();
          this.setState( State.CREATE_NETWORK_RULES );
          break;
        case CREATE_NETWORK_RULES:
          this.queueEvents();
          this.setState( State.CREATE_VMS );
          break;
        case CREATE_VMS:
          this.queueEvents();
          this.setState( State.FINISHED );
          break;
        case ROLLBACK:
          this.queueEvents();
          this.setState( State.FINISHED );
          break;
        case FINISHED:
          break;
      }
      this.clearQueue();
    }
  }

  public void clearQueue() {
    QueuedEvent event = null;
    while ( ( event = this.pendingEvents.poll() ) != null )
      event.getCallback().waitForEvent();
  }

  private void queueEvents() {
    for ( QueuedEvent event : this.msgMap.get( this.state ) ) {
      this.pendingEvents.add( event );
      this.cluster.getMessageQueue().enqueue( event );
    }
  }

  public AtomicBoolean getRollback() {
    return rollback;
  }

  enum State {

    START,
    CREATE_NETWORK,
    CREATE_NETWORK_RULES,
    CREATE_VMS,
    FINISHED,
    ROLLBACK
  }

}
