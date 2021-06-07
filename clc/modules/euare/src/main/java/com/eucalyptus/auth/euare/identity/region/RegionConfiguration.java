package com.eucalyptus.auth.euare.identity.region;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class RegionConfiguration implements Iterable<Region> {

  private List<Region> regions;
  private List<String> remoteCidrs;
  private List<String> forwardedForCidrs;

  @Override
  public Iterator<Region> iterator( ) {
    return regions == null ? Collections.emptyIterator( ) : regions.iterator( );
  }

  public List<Region> getRegions( ) {
    return regions;
  }

  public void setRegions( List<Region> regions ) {
    this.regions = regions;
  }

  public List<String> getRemoteCidrs( ) {
    return remoteCidrs;
  }

  public void setRemoteCidrs( List<String> remoteCidrs ) {
    this.remoteCidrs = remoteCidrs;
  }

  public List<String> getForwardedForCidrs( ) {
    return forwardedForCidrs;
  }

  public void setForwardedForCidrs( List<String> forwardedForCidrs ) {
    this.forwardedForCidrs = forwardedForCidrs;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final RegionConfiguration regions1 = (RegionConfiguration) o;
    return Objects.equals( getRegions( ), regions1.getRegions( ) ) &&
        Objects.equals( getRemoteCidrs( ), regions1.getRemoteCidrs( ) ) &&
        Objects.equals( getForwardedForCidrs( ), regions1.getForwardedForCidrs( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getRegions( ), getRemoteCidrs( ), getForwardedForCidrs( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "regions", regions )
        .add( "remoteCidrs", remoteCidrs )
        .add( "forwardedForCidrs", forwardedForCidrs )
        .toString( );
  }
}

