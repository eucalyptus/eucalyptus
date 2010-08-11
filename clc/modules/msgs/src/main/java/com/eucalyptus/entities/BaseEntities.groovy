package com.eucalyptus.entities;

import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.FetchType;
import javax.persistence.CascadeType;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.Version;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import org.hibernate.annotations.GenericGenerator;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.msgs.PacketFilterRule;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;



@MappedSuperclass
public class AbstractPersistent implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "id" )
  String id;
  @Version
  @Column(name = "version")
  Integer version = 0;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update_timestamp")
  Date lastUpdate;
  
  public AbstractPersistent( ) {
    super( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( id == null ) ? 0 : id.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).is( obj.getClass( ) ) ) return false;
    AbstractPersistent other = ( AbstractPersistent ) obj;
    if ( id == null ) {
      if ( other.id != null ) return false;
    } else if ( !id.equals( other.id ) ) return false;
    return true;
  }
  
}

@MappedSuperclass
public abstract class UserMetadata extends AbstractPersistent implements Serializable {
  @Column( name = "metadata_user_name" )
  String userName;
  @Column( name = "metadata_display_name" )
  String displayName;
  public UserMetadata( ) {
  }
  public UserMetadata( String userName ) {
    super( );
    this.userName = userName;
  }
  public UserMetadata( String userName, String displayName ) {
    super( );
    this.userName = userName;
    this.displayName = displayName;
  }  
}

@Entity
@PersistenceContext(name="eucalyptus_general")
@Table( name = "metadata_keypair" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class SshKeyPair extends UserMetadata implements Serializable {
  @Column( name = "metadata_keypair_user_keyname", unique=true )
  String uniqueName;//bogus field to enforce uniqueness
  @Lob
  @Column( name = "metadata_keypair_public_key" )
  String publicKey;
  @Column( name = "metadata_keypair_finger_print" )
  String fingerPrint;
  @Transient
  public static String NO_KEY_NAME = "";
  @Transient
  public static SshKeyPair NO_KEY = new SshKeyPair( "", "", "", "", "" );
  public SshKeyPair( ) {
  }
  public SshKeyPair( String userName, String keyName, String publicKey, String fingerPrint ) {
    super( userName, keyName );
    this.uniqueName = userName + keyName;
    this.publicKey = publicKey;
    this.fingerPrint = fingerPrint;
  }  
  public SshKeyPair( String userName, String keyName, String asdfsdffsdf, String publicKey, String fingerPrint ) {
    super( userName, keyName );
    this.uniqueName = userName + keyName;
    this.publicKey = publicKey;
    this.fingerPrint = fingerPrint;
  }  
  public SshKeyPair( String userName, String displayName ) {
    super( userName, displayName );
    this.uniqueName = userName+displayName;
  }
  public SshKeyPair( String userName ) {
    super( userName );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( fingerPrint == null ) ? 0 : fingerPrint.hashCode( ) );
    result = prime * result + ( ( publicKey == null ) ? 0 : publicKey.hashCode( ) );
    return result;
  }
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !getClass( ).is( obj.getClass( ) ) ) return false;
    SshKeyPair other = ( SshKeyPair ) obj;
    if ( fingerPrint == null ) {
      if ( other.fingerPrint != null ) return false;
    } else if ( !fingerPrint.equals( other.fingerPrint ) ) return false;
    if ( publicKey == null ) {
      if ( other.publicKey != null ) return false;
    } else if ( !publicKey.equals( other.publicKey ) ) return false;
    return true;
  }  
}


@Entity
@PersistenceContext(name="eucalyptus_general")
@Table( name = "metadata_network_group" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class NetworkRulesGroup extends UserMetadata implements Serializable {
  @Column( name = "metadata_network_group_user_network_group_name", unique=true )
  String uniqueName;//bogus field to enforce uniqueness
  @Column( name = "metadata_network_group_description" )
  String            description;
  @OneToMany( cascade=[CascadeType.ALL], fetch=FetchType.EAGER )
  @JoinTable( name = "metadata_network_group_has_rules", joinColumns = [ @JoinColumn( name = "id" ) ], inverseJoinColumns = [ @JoinColumn( name = "metadata_network_rule_id" ) ] )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  List<NetworkRule> networkRules = new ArrayList<NetworkRule>( );
  public static String NETWORK_DEFAULT_NAME = "default";
  public NetworkRulesGroup( ) {
  }  
  public NetworkRulesGroup( String userName ) {
    super( userName );
  }
  public NetworkRulesGroup( String userName, String groupName, String groupDescription ) {
    this( userName, groupName );
    this.description = groupDescription;
  }
  public static NetworkRulesGroup getDefaultGroup( String userName ) {
    return new NetworkRulesGroup( userName, NETWORK_DEFAULT_NAME, "default group", new ArrayList<NetworkRule>( ) );
  }
  public NetworkRulesGroup( final String userName, final String groupName ) {
    super( userName, groupName );
    this.uniqueName = userName + groupName;
  }  
  public NetworkRulesGroup( final String userName, final String groupName, final String description, final List<NetworkRule> networkRules ) {
    this( userName, groupName );
    this.description = description;
    this.networkRules = networkRules;
  }
  public Network getVmNetwork( ) {
    Network vmNetwork = new Network( this.getUserName( ), this.getDisplayName( ) );
    for ( NetworkRule networkRule : this.getNetworkRules( ) ) {
      PacketFilterRule pfrule = new PacketFilterRule( this.getUserName( ), this.getDisplayName( ), networkRule.getProtocol( ), networkRule.getLowPort( ), networkRule.getHighPort( ) );
      for ( IpRange cidr : networkRule.getIpRanges( ) )
        pfrule.getSourceCidrs( ).add( cidr.getValue( ) );
      for ( NetworkPeer peer : networkRule.getNetworkPeers( ) )
        pfrule.addPeer( peer.getUserQueryKey( ), peer.getGroupName( ) );
      vmNetwork.getRules( ).add( pfrule );
    }
    return vmNetwork;
  }  
  public static NetworkRulesGroup named( String userName, String groupName ) {
    return new NetworkRulesGroup( userName, groupName );
  }
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( uniqueName == null ) ? 0 : uniqueName.hashCode( ) );
    return result;
  }
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    NetworkRulesGroup other = ( NetworkRulesGroup ) obj;
    if ( uniqueName == null ) {
      if ( other.uniqueName != null ) return false;
    } else if ( !uniqueName.equals( other.uniqueName ) ) return false;
    return true;
  }  
}

@Entity
@PersistenceContext(name="eucalyptus_general")
@Table( name = "network_rule_peer_network" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class NetworkPeer {  
  @Id
  @GeneratedValue
  @Column( name = "network_rule_peer_network_id" )
  Long id = -1l;
  @Column( name = "network_rule_peer_network_user_query_key" )
  String userQueryKey;
  @Column( name = "network_rule_peer_network_user_group" )
  String groupName;
  public NetworkPeer() {
  }
  public NetworkPeer( final String userQueryKey, final String groupName ) {
    this.userQueryKey = userQueryKey;
    this.groupName = groupName;
  }
  public boolean equals( final Object o ) {
    if ( this.is(o) ) return true;
    if ( o == null || !getClass().equals(o.getClass()) ) return false;
    
    NetworkPeer that = ( NetworkPeer ) o;
    
    if ( !groupName.equals( that.groupName ) ) return false;
    if ( !userQueryKey.equals( that.userQueryKey ) ) return false;
    
    return true;
  }
  
  public int hashCode() {
    int result;
    result = userQueryKey.hashCode();
    result = 31 * result + groupName.hashCode();
    return result;
  }
  
  public List<NetworkRule> getAsNetworkRules() {
    List<NetworkRule> ruleList = new ArrayList<NetworkRule>();
    ruleList.add( new NetworkRule( "tcp", 0, 65535, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    ruleList.add( new NetworkRule( "udp", 0, 65535, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    ruleList.add( new NetworkRule( "icmp", -1, -1, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    return ruleList;
  }

  @Override
  public String toString( ) {
    return String.format( "NetworkPeer [groupName=%s, userQueryKey=%s]", this.groupName, this.userQueryKey );
  }
  
}

@Entity
@PersistenceContext(name="eucalyptus_general")
@Table( name = "metadata_network_rule" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class NetworkRule {
  
  @Id
  @GeneratedValue
  @Column( name = "metadata_network_rule_id" )
  private Long id = -1l;
  @Column( name = "metadata_network_rule_protocol" )
  String protocol;
  @Column( name = "metadata_network_rule_low_port" )
  Integer lowPort;
  @Column( name = "metadata_network_rule_high_port" )
  Integer highPort;
  @OneToMany( cascade=[CascadeType.REMOVE,CascadeType.MERGE,CascadeType.PERSIST], fetch=FetchType.EAGER )
  @JoinTable( name = "metadata_network_rule_has_ip_range", joinColumns = [ @JoinColumn( name = "metadata_network_rule_id" ) ], inverseJoinColumns = [ @JoinColumn( name = "metadata_network_rule_ip_range_id" ) ] )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  Set<IpRange> ipRanges = new HashSet<IpRange>();
  @OneToMany( cascade=[CascadeType.REMOVE,CascadeType.MERGE,CascadeType.PERSIST], fetch=FetchType.EAGER )
  @JoinTable( name = "metadata_network_rule_has_peer_network", joinColumns = [ @JoinColumn( name = "metadata_network_rule_id" ) ], inverseJoinColumns = [ @JoinColumn( name = "metadata_network_rule_peer_network_id" ) ] )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  Set<NetworkPeer> networkPeers = new HashSet<NetworkPeer>();
  public NetworkRule() {
  }
  public NetworkRule( final String protocol, final Integer lowPort, final Integer highPort, final List<IpRange> ipRanges ) {
    this.protocol = protocol;
    this.lowPort = lowPort;
    this.highPort = highPort;
    this.ipRanges = ipRanges;
  }
  public NetworkRule( final String protocol, final Integer lowPort, final Integer highPort, final NetworkPeer peer ) {
    this.protocol = protocol;
    this.lowPort = lowPort;
    this.highPort = highPort;
    this.networkPeers.add( peer );
  }
  public NetworkRule( final String protocol, final Integer lowPort, final Integer highPort ) {
    this.protocol = protocol;
    this.lowPort = lowPort;
    this.highPort = highPort;
  }
  
  public boolean isValid() {
    return "tcp".equals( this.protocol ) || "udp".equals( this.protocol ) || "icmp".equals( this.protocol );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( highPort == null ) ? 0 : highPort.hashCode( ) );
    result = prime * result + ( ( lowPort == null ) ? 0 : lowPort.hashCode( ) );
    result = prime * result + ( ( protocol == null ) ? 0 : protocol.hashCode( ) );
    return result;
  }
  @Override
  public boolean equals( Object obj ) {
    if ( this.is(obj) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    NetworkRule other = ( NetworkRule ) obj;
    if ( highPort == null ) {
      if ( other.highPort != null ) return false;
    } else if ( !highPort.equals( other.highPort ) ) return false;
    if ( lowPort == null ) {
      if ( other.lowPort != null ) return false;
    } else if ( !lowPort.equals( other.lowPort ) ) return false;
    if ( protocol == null ) {
      if ( other.protocol != null ) return false;
    } else if ( !protocol.equals( other.protocol ) ) return false;
    return true;
  }

  @Override
  public String toString( ) {
    return String.format( "NetworkRule [highPort=%s, id=%s, ipRanges=%s, lowPort=%s, networkPeers=%s, protocol=%s]",
                          this.highPort, this.id, this.ipRanges, this.lowPort, this.networkPeers, this.protocol );
  }
  
  
}

@Entity
@PersistenceContext(name="eucalyptus_general")
@Table( name = "metadata_network_rule_ip_range" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class IpRange {
  @Id
  @GeneratedValue
  @Column( name = "metadata_network_rule_ip_range_id" )
  Long id = -1l;
  @Column( name = "metadata_network_rule_ip_range_value" )
  String value;
  public IpRange(){
  }
  public IpRange( final String value ) {
    this.value = value;
  }

  
  @Override
  public String toString( ) {
    return String.format( "IpRange [value=%s]", this.value );
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.value == null ) ? 0 : this.value.hashCode( ) );
    return result;
  }
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    IpRange other = ( IpRange ) obj;
    if ( this.value == null ) {
      if ( other.value != null ) return false;
    } else if ( !this.value.equals( other.value ) ) return false;
    return true;
  }  
}
