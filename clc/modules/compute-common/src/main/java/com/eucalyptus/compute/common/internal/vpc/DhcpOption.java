/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.common.internal.vpc;

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_dhcp_options" )
public class DhcpOption extends AbstractPersistent {
  private static final long serialVersionUID = 1L;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_dhcp_option_set_id" )
  private DhcpOptionSet dhcpOptionSet;

  @Column( name = "metadata_key", nullable = false )
  private String key;

  @ElementCollection
  @CollectionTable( name = "metadata_dhcp_option_values" )
  @Column( name = "metadata_value" )
  @OrderColumn( name = "metadata_value_index")
  private List<String> values;

  protected DhcpOption() {
  }

  protected DhcpOption( final DhcpOptionSet dhcpOptionSet, final String key, final List<String> values ) {
    this.dhcpOptionSet = dhcpOptionSet;
    this.key = key;
    this.values = values;
  }

  public static DhcpOption create( final DhcpOptionSet dhcpOptionSet, final String key, final String value ) {
    return create( dhcpOptionSet, key, Lists.newArrayList( value ) );
  }

  public static DhcpOption create( final DhcpOptionSet dhcpOptionSet, final String key, final List<String> values ) {
    return new DhcpOption( dhcpOptionSet, key, values );
  }

  public DhcpOptionSet getDhcpOptionSet() {
    return dhcpOptionSet;
  }

  public void setDhcpOptionSet( final DhcpOptionSet dhcpOptionSet ) {
    this.dhcpOptionSet = dhcpOptionSet;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( final String key ) {
    this.key = key;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues( final List<String> values ) {
    this.values = values;
  }
}
