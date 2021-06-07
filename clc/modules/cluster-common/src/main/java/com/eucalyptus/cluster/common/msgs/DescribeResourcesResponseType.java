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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;

public class DescribeResourcesResponseType extends CloudClusterMessage {

  private ArrayList<ResourceType> resources = new ArrayList<ResourceType>( );
  private ArrayList<NodeType> nodes = new ArrayList<NodeType>( );
  private ArrayList<String> serviceTags = new ArrayList<String>( );

  public String toString( ) {
    final StringBuilder out = new StringBuilder();
    resources.forEach( resource -> {
      out.append( resource.getClass( ).getSimpleName( ) );
      out.append( ": " );
      out.append( resource );
      out.append( '\n' );
    } );
    nodes.forEach( node -> {
      out.append( node.getClass( ).getSimpleName( ) );
      out.append( ": " );
      out.append( node );
      out.append( '\n' );
    } );
    return out.toString( );
  }

  public ArrayList<ResourceType> getResources( ) {
    return resources;
  }

  public void setResources( ArrayList<ResourceType> resources ) {
    this.resources = resources;
  }

  public ArrayList<NodeType> getNodes( ) {
    return nodes;
  }

  public void setNodes( ArrayList<NodeType> nodes ) {
    this.nodes = nodes;
  }

  public ArrayList<String> getServiceTags( ) {
    return serviceTags;
  }

  public void setServiceTags( ArrayList<String> serviceTags ) {
    this.serviceTags = serviceTags;
  }
}
