import edu.ucsb.eucalyptus.cloud.ResourceToken
import edu.ucsb.eucalyptus.cloud.cluster.ClusterState
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable
import edu.ucsb.eucalyptus.cloud.cluster.Allocator

public class LeastFullFirst implements Allocator {

  public List<ResourceToken> allocate(String requestId, String userName, String type, int min, int max, SortedSet<ClusterState> clusters) throws NotEnoughResourcesAvailable
  {
    def amount = [clusters.last().getAvailability(type).getAvailable(), max].min();
    if ( amount < min ) throw new NotEnoughResourcesAvailable();
    return [clusters.last().getResourceAllocation(requestId, userName, type, amount)];
  }

}
