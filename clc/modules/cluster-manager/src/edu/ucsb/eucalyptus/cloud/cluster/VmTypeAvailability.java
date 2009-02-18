package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.VmType;

public class VmTypeAvailability implements Comparable {
  private VmType type;
  private boolean disabled;
  private int max;
  private int available;


  public VmTypeAvailability( final VmType type, final int max, final int available )
  {
    this.type = type;
    this.max = max;
    this.available = available;
    this.disabled = false;
  }

  public VmType getType()
  {
    return type;
  }

  public void decrement( int quantity )
  {
    this.available -= quantity;
    this.available = (this.available<0)?0:this.available;
  }

  public int getMax()
  {
    return max;
  }

  public void setMax( final int max )
  {
    this.max = max;
  }

  public int getAvailable()
  {
    return available;
  }

  public void setAvailable( final int available )
  {
    this.available = available;
  }

  public boolean isDisabled()
  {
    return disabled;
  }

  public void setDisabled( final boolean disabled )
  {
    this.disabled = disabled;
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( !( o instanceof VmTypeAvailability ) ) return false;

    VmTypeAvailability that = ( VmTypeAvailability ) o;

    if ( !type.equals( that.type ) ) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    return type.hashCode();
  }

  public int compareTo( final Object o )
  {
    VmTypeAvailability v = ( VmTypeAvailability ) o;
    if( !v.isDisabled() && this.isDisabled() ) return -1;
    if( !this.isDisabled() && v.isDisabled() ) return 1;
    return this.type.compareTo( v.getType() );
  }

  public static VmTypeAvailability ZERO = new ZeroTypeAvailability( );
  static class ZeroTypeAvailability extends VmTypeAvailability {
    ZeroTypeAvailability()
    {
      super( new VmType("ZERO",0,0,0), 0, 0 );
    }

    @Override
    public boolean isDisabled()
    {
      return true;
    }

    @Override
    public void setDisabled( final boolean disabled ){}

    @Override
    public int compareTo( final Object o )
    {
      VmTypeAvailability v = ( VmTypeAvailability ) o;
      if( v == ZERO ) return 0;
      if( !v.isDisabled() ) return -1;
      if( v.isDisabled() ) return 1;
      return 0;
    }

    @Override
    public boolean equals( final Object o )
    {
      if ( this == o ) return true;
      return false;
    }

  }

  @Override
  public String toString() {
    return "VmTypeAvailability{" +
           "type=" + type +
           ", disabled=" + disabled +
           ", max=" + max +
           ", available=" + available +
           '}';
  }
}

