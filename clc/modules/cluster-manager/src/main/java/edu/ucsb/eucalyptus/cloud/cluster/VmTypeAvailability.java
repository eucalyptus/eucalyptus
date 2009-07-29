package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.VmType;

public class VmTypeAvailability implements Comparable {
  private VmType type;
  private int max;
  private int available;

  public VmTypeAvailability( final VmType type, final int max, final int available ) {
    this.type = type;
    this.max = max;
    this.available = available;
  }

  public VmType getType() {
    return type;
  }

  public void decrement( int quantity ) {
    this.available -= quantity;
    this.available = ( this.available < 0 ) ? 0 : this.available;
  }

  public int getMax() {
    return max;
  }

  public void setMax( final int max ) {
    this.max = max;
  }

  public int getAvailable() {
    return available;
  }

  public void setAvailable( final int available ) {
    this.available = available;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof VmTypeAvailability ) ) return false;

    VmTypeAvailability that = ( VmTypeAvailability ) o;

    if ( !type.equals( that.type ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  public int compareTo( final Object o ) {
    VmTypeAvailability v = ( VmTypeAvailability ) o;
    if ( v.getAvailable() == this.getAvailable() ) return this.type.compareTo( v.getType() );
    return v.getAvailable() - this.getAvailable();
  }

  @Override
  public String toString() {
    return "VmTypeAvailability{" +
           "type=" + type +
           ", max=" + max +
           ", available=" + available +
           "}\n";
  }

  public static VmTypeAvailability ZERO = new ZeroTypeAvailability();

  static class ZeroTypeAvailability extends VmTypeAvailability {
    ZeroTypeAvailability() {
      super( new VmType( "ZERO", -1, -1, -1 ), 0, 0 );
    }

    @Override
    public int compareTo( final Object o ) {
      VmTypeAvailability v = ( VmTypeAvailability ) o;
      if ( v == ZERO ) return 0;
      if ( v.getAvailable() > 0 ) return 1;
      else return -1;
    }

    @Override
    public void setAvailable( final int available ) {}

    @Override
    public void decrement( final int quantity ) {}

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      return false;
    }

  }

}

