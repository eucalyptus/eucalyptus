package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * User: decker
 * Date: Dec 9, 2008
 * Time: 4:12:50 AM
 */
public class VmTypeWeb implements IsSerializable {
private String name;
  private Integer cpu;
  private Integer memory;
  private Integer disk;

  public VmTypeWeb()
  {
  }

  public VmTypeWeb( final String name, final Integer cpu, final Integer memory, final Integer disk )
  {
    this.name = name;
    this.cpu = cpu;
    this.memory = memory;
    this.disk = disk;
  }

  public String getName()
  {
    return name;
  }

  public Integer getCpu()
  {
    return cpu;
  }

  public void setCpu( final Integer cpu )
  {
    this.cpu = cpu;
  }

  public Integer getMemory()
  {
    return memory;
  }

  public void setMemory( final Integer memory )
  {
    this.memory = memory;
  }

  public Integer getDisk()
  {
    return disk;
  }

  public void setDisk( final Integer disk )
  {
    this.disk = disk;
  }
}
