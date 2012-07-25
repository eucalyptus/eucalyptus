package com.eucalyptus.reporting.event;

/**
 * <p>InstanceEvent is an event sent from the CLC to the reporting mechanism,
 * indicating resource usage by an instance.
 * 
 * <p>InstanceEvent contains the <i>cumulative</i> usage of an instance up until
 * this point. For example, it contains all network and disk bandwidth used up
 * until the point when the InstanceEvent was instantiated. This is different
 * from data returned by the reporting mechanism, which contains only usage data
 * for a specific period.
 * 
 * <p>By using cumulative usage totals, we gain resilience to lost packets
 * despite unreliable transmission. If an event is lost, then the next event
 * will contain the cumulative usage and nothing will be lost, and the sampling
 * period will be assumed to have begun at the end of the last successfully
 * received event. As a result, lost packets cause only loss of granularity of
 * time periods, but no loss of usage information.
 * 
 * <p>InstanceEvent allows null values for usage statistics like
 * cumulativeDiskIo. Null values signify missing information and not zero
 * usage. Null values will be ignored while calculating aggregate usage
 * information for reports. Null values should be used only when we don't
 * support gathering information from an instance <i>at all</i>. Null values
 * for resource usage will be represented as "N/A" or something similar in
 * UIs.
 * 
 * @author tom.werges
 */
public class InstanceEvent extends InstanceEventSupport {
  private final Long   cumulativeNetworkIoMegs;
  private final Long   cumulativeDiskIoMegs;

  /**
   * Constructor for InstanceEvent.
   *
   * NOTE: We must include separate userId, username, accountId, and
   *  accountName with each event sent, even though the names can be looked
   *  up using ID's. We must include this redundant information, for
   *  several reasons. First, the reporting subsystem may run on a totally
   *  separate machine outside of eucalyptus (data warehouse configuration)
   *  so it may not have access to the regular eucalyptus database to lookup
   *  usernames or account names. Second, the reporting subsystem stores
   *  <b>historical</b> information, and its possible that usernames and
   *  account names can change, or their users or accounts can be deleted.
   *  Thus we need the user name or account name at the time an event was
   *  sent.
   */
  public InstanceEvent( final String uuid,
                        final String instanceId,
                        final String instanceType,
                        final String userId,
                        final String userName,
                        final String accountId,
                        final String accountName,
                        final String clusterName,
                        final String availabilityZone,
                        final Long cumulativeNetworkIoMegs,
                        final Long cumulativeDiskIoMegs ) {
    super( uuid, instanceId, instanceType, userId, userName, accountId, accountName, clusterName, availabilityZone );

    if ( cumulativeDiskIoMegs != null && cumulativeDiskIoMegs < 0) {
      throw new IllegalArgumentException("cumulativeDiskIoMegs cant be negative");
    }
    if (cumulativeNetworkIoMegs != null && cumulativeNetworkIoMegs < 0) {
      throw new IllegalArgumentException("cumulativeNetworkIoMegs cant be negative");
    }

    this.cumulativeNetworkIoMegs = cumulativeNetworkIoMegs;
    this.cumulativeDiskIoMegs = cumulativeDiskIoMegs;
  }


  /**
   * Copy Constructor.
   *
   * NOTE: We must include separate userId, username, accountId, and
   *  accountName with each event sent, even though the names can be looked
   *  up using ID's. We must include this redundant information, for
   *  several reasons. First, the reporting subsystem may run on a totally
   *  separate machine outside of eucalyptus (data warehouse configuration)
   *  so it may not have access to the regular eucalyptus database to lookup
   *  usernames or account names. Second, the reporting subsystem stores
   *  <b>historical</b> information, and its possible that usernames and
   *  account names can change, or their users or accounts can be deleted.
   *  Thus we need the user name or account name at the time an event was
   *  sent.
   */
  public InstanceEvent( final InstanceEvent e ) {
    super( e );
    this.cumulativeNetworkIoMegs = e.cumulativeNetworkIoMegs;
    this.cumulativeDiskIoMegs = e.cumulativeDiskIoMegs;
  }

  /**
   * Copy constructor with different resource usage
   */
  public InstanceEvent( final InstanceEvent e,
                        final Long cumulativeNetworkIoMegs,
                        final Long cumulativeDiskIoMegs) {
    super( e );

    if (cumulativeDiskIoMegs!= null && cumulativeDiskIoMegs < 0) {
      throw new IllegalArgumentException("cumulativeDiskIoMegs cant be negative");
    }
    if (cumulativeNetworkIoMegs!= null && cumulativeNetworkIoMegs < 0) {
      throw new IllegalArgumentException("cumulativeNetworkIoMegs cant be negative");
    }

    this.cumulativeNetworkIoMegs = cumulativeNetworkIoMegs;
    this.cumulativeDiskIoMegs = cumulativeDiskIoMegs;
  }

  public Long getCumulativeNetworkIoMegs() {
    return cumulativeNetworkIoMegs;
  }

  public Long getCumulativeDiskIoMegs() {
    return cumulativeDiskIoMegs;
  }


  public String toString() {
    return String.format("[uuid:%s,instanceId:%s,instanceType:%s,userId:%s"
        + ",accountId:%s,cluster:%s,zone:%s,net:%d,disk:%d]",
          getUuid(), getInstanceId(), getInstanceType(), getUserId(), getAccountId(),
          getClusterName(), getAvailabilityZone(), getCumulativeNetworkIoMegs(),
          getCumulativeDiskIoMegs() );
  }

}
