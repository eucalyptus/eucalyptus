package com.eucalyptus.objectstorage;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.util.dns.DnsResolvers.DnsRequest;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class ObjectStorageBucketResolver implements DnsResolver {

  @Override
  public boolean checkAccepts(DnsRequest request) {
    if ( !Bootstrap.isOperational( ))
      return false;
    final Record query = request.getQuery( );
    if ( DomainNames.isSystemSubdomain( query.getName( ) ) &&
      query.getName().toString().matches(".+\\.objectstorage\\..*"))
        return true;
    return false;
  }

  @Override
  public DnsResponse lookupRecords(DnsRequest request) {
    final Record query = request.getQuery( );
    final Name name = query.getName( );
    if (! DomainNames.isSystemSubdomain( query.getName( ) ))
      throw new NoSuchElementException( "Failed to lookup name: " + name );
    
    try{
      final List<InetAddress> osgIps = ObjectStorageAddresses.getObjectStorageAddress();
      final List<Record> records = Lists.newArrayList();
      for(final InetAddress osgIp : osgIps){
        records.add( DomainNameRecords.addressRecord(name, osgIp));
      }

      return DnsResponse.forName( query.getName( ) )
          .answer( RequestType.A.apply( query ) ? 
              records 
              : null );
    }catch(final Exception ex) {
      return DnsResponse.forName( query.getName( ))
          .nxdomain();
    }
  }
  
  private static class ObjectStorageAddresses {
    private static Iterator<ServiceConfiguration> rrStores;
    private static Iterable<ServiceConfiguration> currentStores;

    public static List<InetAddress> getObjectStorageAddress()
        throws EucalyptusCloudException {
      if (Topology.isEnabled(ObjectStorage.class)) {
        Iterable<ServiceConfiguration> newStores = Topology
            .lookupMany(ObjectStorage.class);
        List<InetAddress> addresses = new ArrayList<InetAddress>();
        if (rrStores == null
            || (!Iterables.elementsEqual(currentStores, newStores))) {
          currentStores = newStores;
          rrStores = Iterators.cycle(currentStores);
        }
        Iterator<ServiceConfiguration> current = currentStores.iterator();
        while (current.hasNext()) {
          current.next();
          addresses.add(rrStores.next().getInetAddress());
        }
        return addresses;
      } else {
        throw new EucalyptusCloudException("ObjectStorage not ENABLED");
      }
    }
  }
}
