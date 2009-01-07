package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table( name = "vm_types" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class VmType implements Comparable {

  public static String M1_SMALL = "m1.small";
  public static String M1_LARGE = "m1.large";
  public static String M1_XLARGE = "m1.xlarge";
  public static String C1_MEDIUM = "c1.medium";
  public static String C1_XLARGE = "c1.xlarge";

  @Id
  @GeneratedValue
  @Column( name = "vm_type_id" )
  private Long id = -1l;
  @Column( name = "vm_type_name" )
  private String name;

  @Column( name = "vm_type_cpu" )
  private Integer cpu;
  @Column( name = "vm_type_disk" )
  private Integer disk;
  @Column( name = "vm_type_memory" )
  private Integer memory;

  public VmType() {}

  public VmType( final String name )
  {
    this.name = name;
  }

  public VmType( final String name, final Integer cpu, final Integer disk, final Integer memory )
  {
    this.name = name;
    this.cpu = cpu;
    this.disk = disk;
    this.memory = memory;
  }

  public Long getId()
  {
    return id;
  }

  public String getName()
  {
    return name;
  }

  public void setName( final String name )
  {
    this.name = name;
  }

  public Integer getCpu()
  {
    return cpu;
  }

  public void setCpu( final Integer cpu )
  {
    this.cpu = cpu;
  }

  public Integer getDisk()
  {
    return disk;
  }

  public void setDisk( final Integer disk )
  {
    this.disk = disk;
  }

  public Integer getMemory()
  {
    return memory;
  }

  public void setMemory( final Integer memory )
  {
    this.memory = memory;
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    VmType vmType = ( VmType ) o;

    if ( !cpu.equals( vmType.cpu ) ) return false;
    if ( !disk.equals( vmType.disk ) ) return false;
    if ( !memory.equals( vmType.memory ) ) return false;
    if ( !name.equals( vmType.name ) ) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = name.hashCode();
    result = 31 * result + cpu.hashCode();
    result = 31 * result + disk.hashCode();
    result = 31 * result + memory.hashCode();
    return result;
  }

  public int compareTo( final Object o )
  {
    VmType that = ( VmType ) o;
    if ( this.equals( that ) ) return 0;
    if ( ( this.getCpu() <= that.getCpu() ) && ( this.getDisk() <= that.getDisk() ) && ( this.getMemory() <= that.getMemory() ) )
      return -1;
    if ( ( this.getCpu() >= that.getCpu() ) && ( this.getDisk() >= that.getDisk() ) && ( this.getMemory() >= that.getMemory() ) )
      return 1;
    return 0;
  }

  public VmTypeInfo getAsVmTypeInfo()
  {
    return new VmTypeInfo( this.getName(), this.getMemory(), this.getDisk(), this.getCpu() );
  }

  @Override
  public String toString() {
    return this.getAsVmTypeInfo().toString();
  }
}
