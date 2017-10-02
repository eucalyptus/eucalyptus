/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.cluster.common.broadcast;

import static org.junit.Assert.assertEquals;
import static com.eucalyptus.cluster.common.broadcast.BNI.cluster;
import static com.eucalyptus.cluster.common.broadcast.BNI.clusters;
import static com.eucalyptus.cluster.common.broadcast.BNI.configuration;
import static com.eucalyptus.cluster.common.broadcast.BNI.gateway;
import static com.eucalyptus.cluster.common.broadcast.BNI.gateways;
import static com.eucalyptus.cluster.common.broadcast.BNI.instance;
import static com.eucalyptus.cluster.common.broadcast.BNI.internetGateway;
import static com.eucalyptus.cluster.common.broadcast.BNI.midonet;
import static com.eucalyptus.cluster.common.broadcast.BNI.networkInfo;
import static com.eucalyptus.cluster.common.broadcast.BNI.networkInterface;
import static com.eucalyptus.cluster.common.broadcast.BNI.node;
import static com.eucalyptus.cluster.common.broadcast.BNI.nodes;
import static com.eucalyptus.cluster.common.broadcast.BNI.property;
import static com.eucalyptus.cluster.common.broadcast.BNI.route;
import static com.eucalyptus.cluster.common.broadcast.BNI.routeTable;
import static com.eucalyptus.cluster.common.broadcast.BNI.securityGroup;
import static com.eucalyptus.cluster.common.broadcast.BNI.securityGroupRules;
import static com.eucalyptus.cluster.common.broadcast.BNI.subnet;
import static com.eucalyptus.cluster.common.broadcast.BNI.subnets;
import static com.eucalyptus.cluster.common.broadcast.BNI.vpc;
import static com.eucalyptus.cluster.common.broadcast.BNI.vpcSubnet;
import java.io.IOException;
import org.junit.Test;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.Json.JsonOption;
import io.vavr.collection.Array;

/**
 *
 */
public class NetworkInfoTest {

  @Test
  public void testImmutable( ) throws IOException {
    BNetworkInfo info = networkInfo( )
        .setValueAppliedTime( "at" )
        .setValueVersion( "v" )
        .setValueAppliedVersion( "av" )
        .configuration( configuration( )
            .property( property( )
                .name( "name" )
                .value( "value" )
                .o( ) )
            .property( midonet( )
                .name( "mido-name" )
                .o( ) )
            .property( clusters( )
                .name( "cluster" )
                .cluster( cluster( )
                    .name( "cluster-1" )
                    .setValueSubnet( subnet( )
                        .name( "subnet" )
                        .property( property( )
                            .name( "subnet-1" )
                            .value( "value" )
                            .o( ) )
                        .o( ) )
                    .property( property( )
                        .name( "name" )
                        .value( "value" )
                        .o( ) )
                    .property( nodes( )
                        .name( "name" )
                        .node( node( )
                            .name( "node-1" )
                            .instanceId( "i-00000001" )
                            .instanceId( "i-00000002" )
                            .o( ) )
                        .o( ) )
                    .o( ) )
                .o( ) )
            .o( ) )
        .o( );


    System.out.println( Json.writeObjectAsString( info ) );
    String value = BNI.toString( info );
    System.out.println( value );
    Object objectValue = BNI.parse( value );
    System.out.println( Json.writeObjectAsString( objectValue ) );
  }

  @Test
  public void testXml() throws IOException {
    BNetworkInfo info = networkInfo( )
        .configuration( configuration( )
            .property( property( "enabledCLCIp", "10.111.5.11" ) )
            .property( property( "instanceDNSDomain", "eucalyptus.internal" ) )
            .property( property( "instanceDNSServers", "10.1.1.254" ) )
            .property( property( ).name( "publicIps" ).setIterableValues( Array.range( 1, 11 ).map( it -> "10.111.200." + it ) ).o( ) )
            .property( subnets( )
                .name( "subnets" )
                .subnet( subnet( )
                    .name( "1.0.0.0" )
                    .property( property( "subnet", "1.0.0.0" ) )
                    .property( property( "netmask", "255.255.0.0" ) )
                    .property( property( "gateway", "1.0.0.1" ) )
                    .o( ) )
                .subnet( subnet( )
                    .name( "2.0.0.0" )
                    .property( property( "subnet", "2.0.0.0" ) )
                    .property( property( "netmask", "255.255.0.0" ) )
                    .property( property( "gateway", "2.0.0.1" ) )
                    .o( ) )
                .o( ) )
            .property( clusters( )
                .name( "clusters" )
                .cluster( cluster( )
                    .name( "edgecluster0" )
                    .setValueSubnet( subnet( )
                        .name( "1.0.0.0" )
                        .property( property( "subnet", "1.0.0.0" ) )
                        .property( property( "netmask", "255.255.0.0" ) )
                        .property( property( "gateway", "1.0.0.1" ) )
                        .o() )
                    .property( property( "enabledCCIp", "10.111.5.16" ) )
                    .property( property( "macPrefix", "d0:0d" ) )
                    .property( property( ).name( "privateIps" ).setIterableValues( Array.range( 11, 21 ).map( it -> "1.0.0." + it ) ).o( ) )
                    .property( nodes( )
                        .name( "nodes" )
                        .node( node( )
                            .name( "10.111.5.70" )
                            .instanceId( "i-AB6B409C" )
                            .o( ) )
                        .o( ) )
                    .o( ) )
                .o( ) )
            .o( ) )
        .securityGroup( securityGroup( )
            .name( "c4d8e62e-ea5d-49fb-b1e9-b908e9becf8" )
            .ownerId( "856501978207" )
            .o( ) )
        .instance( instance( )
            .name( "i-AB6B409C" )
            .ownerId( "856501978207" )
            .setValueMacAddress( "d0:0d:01:00:00:21" )
            .setValuePublicIp( "10.111.200.1" )
            .setValuePrivateIp( "1.0.0.33" )
            .securityGroup( "c4d8e62e-ea5d-49fb-b1e9-b908e9becf8" )
            .o( ) )
        .o( );

    String outputXml = BNI.toFormattedString( info );
    System.out.println( outputXml );

    String expectedXml =  "<network-data>\n" +
        "  <configuration>\n" +
        "    <property type=\"simple\" name=\"enabledCLCIp\">\n" +
        "      <value>10.111.5.11</value>\n" +
        "    </property>\n" +
        "    <property type=\"simple\" name=\"instanceDNSDomain\">\n" +
        "      <value>eucalyptus.internal</value>\n" +
        "    </property>\n" +
        "    <property type=\"simple\" name=\"instanceDNSServers\">\n" +
        "      <value>10.1.1.254</value>\n" +
        "    </property>\n" +
        "    <property type=\"simple\" name=\"publicIps\">\n" +
        "      <value>10.111.200.1</value>\n" +
        "      <value>10.111.200.2</value>\n" +
        "      <value>10.111.200.3</value>\n" +
        "      <value>10.111.200.4</value>\n" +
        "      <value>10.111.200.5</value>\n" +
        "      <value>10.111.200.6</value>\n" +
        "      <value>10.111.200.7</value>\n" +
        "      <value>10.111.200.8</value>\n" +
        "      <value>10.111.200.9</value>\n" +
        "      <value>10.111.200.10</value>\n" +
        "    </property>\n" +
        "    <property type=\"subnet\" name=\"subnets\">\n" +
        "      <subnet name=\"1.0.0.0\">\n" +
        "        <property type=\"simple\" name=\"subnet\">\n" +
        "          <value>1.0.0.0</value>\n" +
        "        </property>\n" +
        "        <property type=\"simple\" name=\"netmask\">\n" +
        "          <value>255.255.0.0</value>\n" +
        "        </property>\n" +
        "        <property type=\"simple\" name=\"gateway\">\n" +
        "          <value>1.0.0.1</value>\n" +
        "        </property>\n" +
        "      </subnet>\n" +
        "      <subnet name=\"2.0.0.0\">\n" +
        "        <property type=\"simple\" name=\"subnet\">\n" +
        "          <value>2.0.0.0</value>\n" +
        "        </property>\n" +
        "        <property type=\"simple\" name=\"netmask\">\n" +
        "          <value>255.255.0.0</value>\n" +
        "        </property>\n" +
        "        <property type=\"simple\" name=\"gateway\">\n" +
        "          <value>2.0.0.1</value>\n" +
        "        </property>\n" +
        "      </subnet>\n" +
        "    </property>\n" +
        "    <property type=\"cluster\" name=\"clusters\">\n" +
        "      <cluster name=\"edgecluster0\">\n" +
        "        <subnet name=\"1.0.0.0\">\n" +
        "          <property type=\"simple\" name=\"subnet\">\n" +
        "            <value>1.0.0.0</value>\n" +
        "          </property>\n" +
        "          <property type=\"simple\" name=\"netmask\">\n" +
        "            <value>255.255.0.0</value>\n" +
        "          </property>\n" +
        "          <property type=\"simple\" name=\"gateway\">\n" +
        "            <value>1.0.0.1</value>\n" +
        "          </property>\n" +
        "        </subnet>\n" +
        "        <property type=\"simple\" name=\"enabledCCIp\">\n" +
        "          <value>10.111.5.16</value>\n" +
        "        </property>\n" +
        "        <property type=\"simple\" name=\"macPrefix\">\n" +
        "          <value>d0:0d</value>\n" +
        "        </property>\n" +
        "        <property type=\"simple\" name=\"privateIps\">\n" +
        "          <value>1.0.0.11</value>\n" +
        "          <value>1.0.0.12</value>\n" +
        "          <value>1.0.0.13</value>\n" +
        "          <value>1.0.0.14</value>\n" +
        "          <value>1.0.0.15</value>\n" +
        "          <value>1.0.0.16</value>\n" +
        "          <value>1.0.0.17</value>\n" +
        "          <value>1.0.0.18</value>\n" +
        "          <value>1.0.0.19</value>\n" +
        "          <value>1.0.0.20</value>\n" +
        "        </property>\n" +
        "        <property type=\"node\" name=\"nodes\">\n" +
        "          <node name=\"10.111.5.70\">\n" +
        "            <instanceIds>\n" +
        "              <value>i-AB6B409C</value>\n" +
        "            </instanceIds>\n" +
        "          </node>\n" +
        "        </property>\n" +
        "      </cluster>\n" +
        "    </property>\n" +
        "  </configuration>\n" +
        "  <vpcs/>\n" +
        "  <instances>\n" +
        "    <instance name=\"i-AB6B409C\">\n" +
        "      <ownerId>856501978207</ownerId>\n" +
        "      <macAddress>d0:0d:01:00:00:21</macAddress>\n" +
        "      <publicIp>10.111.200.1</publicIp>\n" +
        "      <privateIp>1.0.0.33</privateIp>\n" +
        "      <vpc/>\n" +
        "      <subnet/>\n" +
        "      <networkInterfaces/>\n" +
        "      <securityGroups>\n" +
        "        <value>c4d8e62e-ea5d-49fb-b1e9-b908e9becf8</value>\n" +
        "      </securityGroups>\n" +
        "    </instance>\n" +
        "  </instances>\n" +
        "  <dhcpOptionSets/>\n" +
        "  <internetGateways/>\n" +
        "  <securityGroups>\n" +
        "    <securityGroup name=\"c4d8e62e-ea5d-49fb-b1e9-b908e9becf8\">\n" +
        "      <ownerId>856501978207</ownerId>\n" +
        "      <ingressRules/>\n" +
        "      <egressRules/>\n" +
        "    </securityGroup>\n" +
        "  </securityGroups>\n" +
        "</network-data>\n";

    assertEquals( "Message output", expectedXml, outputXml );
  }

  @Test
  public void testParse( ) throws IOException {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<network-data>\n" +
            "    <configuration>\n" +
            "        <property type=\"simple\" name=\"enabledCLCIp\">\n" +
            "            <value>10.111.5.11</value>\n" +
            "        </property>\n" +
            "        <property type=\"simple\" name=\"instanceDNSDomain\">\n" +
            "            <value>eucalyptus.internal</value>\n" +
            "        </property>\n" +
            "        <property type=\"simple\" name=\"instanceDNSServers\">\n" +
            "            <value>10.1.1.254</value>\n" +
            "        </property>\n" +
            "        <property type=\"simple\" name=\"publicIps\">\n" +
            "            <value>10.111.200.1</value>\n" +
            "            <value>10.111.200.2</value>\n" +
            "            <value>10.111.200.3</value>\n" +
            "            <value>10.111.200.4</value>\n" +
            "            <value>10.111.200.5</value>\n" +
            "            <value>10.111.200.6</value>\n" +
            "            <value>10.111.200.7</value>\n" +
            "            <value>10.111.200.8</value>\n" +
            "            <value>10.111.200.9</value>\n" +
            "            <value>10.111.200.10</value>\n" +
            "        </property>\n" +
            "        <property type=\"subnet\" name=\"subnets\">\n" +
            "            <subnet name=\"1.0.0.0\">\n" +
            "                <property type=\"simple\" name=\"subnet\">\n" +
            "                    <value>1.0.0.0</value>\n" +
            "                </property>\n" +
            "                <property type=\"simple\" name=\"netmask\">\n" +
            "                    <value>255.255.0.0</value>\n" +
            "                </property>\n" +
            "                <property type=\"simple\" name=\"gateway\">\n" +
            "                    <value>1.0.0.1</value>\n" +
            "                </property>\n" +
            "            </subnet>\n" +
            "            <subnet name=\"2.0.0.0\">\n" +
            "                <property type=\"simple\" name=\"subnet\">\n" +
            "                    <value>2.0.0.0</value>\n" +
            "                </property>\n" +
            "                <property type=\"simple\" name=\"netmask\">\n" +
            "                    <value>255.255.0.0</value>\n" +
            "                </property>\n" +
            "                <property type=\"simple\" name=\"gateway\">\n" +
            "                    <value>2.0.0.1</value>\n" +
            "                </property>\n" +
            "            </subnet>\n" +
            "        </property>\n" +
            "        <property type=\"cluster\" name=\"clusters\">\n" +
            "            <cluster name=\"edgecluster0\">\n" +
            "                <subnet name=\"1.0.0.0\">\n" +
            "                    <property type=\"simple\" name=\"subnet\">\n" +
            "                        <value>1.0.0.0</value>\n" +
            "                    </property>\n" +
            "                    <property type=\"simple\" name=\"netmask\">\n" +
            "                        <value>255.255.0.0</value>\n" +
            "                    </property>\n" +
            "                    <property type=\"simple\" name=\"gateway\">\n" +
            "                        <value>1.0.0.1</value>\n" +
            "                    </property>\n" +
            "                </subnet>\n" +
            "                <property type=\"simple\" name=\"enabledCCIp\">\n" +
            "                    <value>10.111.5.16</value>\n" +
            "                </property>\n" +
            "                <property type=\"simple\" name=\"macPrefix\">\n" +
            "                    <value>d0:0d</value>\n" +
            "                </property>\n" +
            "                <property type=\"simple\" name=\"privateIps\">\n" +
            "                    <value>1.0.0.11</value>\n" +
            "                    <value>1.0.0.12</value>\n" +
            "                    <value>1.0.0.13</value>\n" +
            "                    <value>1.0.0.14</value>\n" +
            "                    <value>1.0.0.15</value>\n" +
            "                    <value>1.0.0.16</value>\n" +
            "                    <value>1.0.0.17</value>\n" +
            "                    <value>1.0.0.18</value>\n" +
            "                    <value>1.0.0.19</value>\n" +
            "                    <value>1.0.0.20</value>\n" +
            "                </property>\n" +
            "                <property type=\"node\" name=\"nodes\">\n" +
            "                    <node name=\"10.111.5.70\">\n" +
            "                        <instanceIds>\n" +
            "                            <value>i-AB6B409C</value>\n" +
            "                        </instanceIds>\n" +
            "                    </node>\n" +
            "                </property>\n" +
            "            </cluster>\n" +
            "        </property>\n" +
            "    </configuration>\n" +
            "    <vpcs/>\n" +
            "    <instances>\n" +
            "        <instance name=\"i-AB6B409C\">\n" +
            "            <ownerId>856501978207</ownerId>\n" +
            "            <macAddress>d0:0d:01:00:00:21</macAddress>\n" +
            "            <publicIp>10.111.200.1</publicIp>\n" +
            "            <privateIp>1.0.0.33</privateIp>\n" +
            "            <networkInterfaces/>\n" +
            "            <securityGroups>\n" +
            "                <value>c4d8e62e-ea5d-49fb-b1e9-b908e9becf8</value>\n" +
            "            </securityGroups>\n" +
            "        </instance>\n" +
            "    </instances>\n" +
            "    <dhcpOptionSets/>\n" +
            "    <internetGateways/>\n" +
            "    <securityGroups>\n" +
            "        <securityGroup name=\"c4d8e62e-ea5d-49fb-b1e9-b908e9becf8\">\n" +
            "            <ownerId>856501978207</ownerId>\n" +
            "            <ingressRules/>\n" +
            "            <egressRules/>\n" +
            "        </securityGroup>\n" +
            "    </securityGroups>\n" +
            "</network-data>";

    Object objectValue = BNI.parse( xml );
    System.out.println( Json.mapper( Json.mapper( ), JsonOption.IgnoreVavr ).writeValueAsString( objectValue ) );
  }

  @Test
  public void testModelBasic( ) throws IOException {
    BNetworkInfo info = networkInfo( )
        .configuration( configuration( )
            .property( property( "mode", "EDGE" ) )
            .property( property( "publicIps", "2.0.0.2-2.0.0.255" ) )
            .property( property( "enabledCLCIp", "1.1.1.1" ) )
            .property( property( "instanceDNSDomain", "eucalyptus.internal" ) )
            .property( property( "instanceDNSServers", "1.2.3.4" ) )
            .property( property( "publicGateway", "2.0.0.1" ) )
            .property( subnets( )
                .name( "subnets" )
                .subnet( subnet( )
                    .name( "192.168.0.0" )
                    .property( property( "subnet", "192.168.0.0" ) )
                    .property( property( "netmask", "255.255.0.0" ) )
                    .property( property( "gateway", "192.168.0.1" ) )
                    .o( ) )
                .o( ) )
            .property( clusters( )
                .name( "clusters" )
                .cluster( cluster( )
                    .name( "cluster1" )
                    .setValueSubnet( subnet( )
                        .name( "10.0.0.0" )
                        .property( property( "subnet", "10.0.0.0" ) )
                        .property( property( "netmask", "255.255.0.0" ) )
                        .property( property( "gateway", "10.0.1.0" ) ).o( ) )
                    .property( property( "enabledCCIp", "6.6.6.6" ) )
                    .property( property( "macPrefix", "d0:0d" ) )
                    .property( property( "privateIps", "10.0.0.0-10.0.0.255" ) )
                    .property( nodes( )
                        .name( "nodes" )
                        .node( node( )
                            .name( "node1" )
                            .instanceId( "i-00000001" )
                            .o( ) )
                        .o( ) )
                    .o( ) )
                .o( ) )
            .o( ) )
        .instance( instance( )
            .name( "i-00000001" )
            .ownerId( "000000000002" )
            .setValueMacAddress( "00:00:00:00:00:00" )
            .setValuePublicIp( "2.0.0.2" )
            .setValuePrivateIp( "10.0.0.0" )
            .o( ) )
        .o( );

    System.out.println( BNI.toString( info ) );
  }

  @Test
  public void testModelVpcMido( ) throws IOException {
    BNetworkInfo info = networkInfo( )
        .configuration( configuration( )
            .property( property( "mode", "VPCMIDO" ) )
            .property( property( "publicIps", "2.0.0.2-2.0.0.255" ) )
            .property( property( "enabledCLCIp", "1.1.1.1" ) )
            .property( property( "instanceDNSDomain", "eucalyptus.internal" ) )
            .property( property( "instanceDNSServers", "127.0.0.1" ) )
            .property( midonet( )
                .name( "mido" )
                .property( gateways( )
                    .name( "gateways" )
                    .gateway( gateway( )
                        .property( property( "ip", "10.111.5.11" ) )
                        .property( property( "externalCidr", "10.116.128.0/17" ) )
                        .property( property( "externalDevice", "em1.116" ) )
                        .property( property( "externalIp", "10.116.133.11" ) )
                        .property( property( "bgpPeerIp", "10.116.133.173" ) )
                        .property( property( "bgpPeerAsn", "65000" ) )
                        .property( property( "bgpAdRoutes", "10.116.150.0/24" ) )
                        .o( ) )
                    .gateway( gateway( )
                        .property( property( "ip", "10.111.5.22" ) )
                        .property( property( "externalCidr", "10.117.128.0/17" ) )
                        .property( property( "externalDevice", "em1.117" ) )
                        .property( property( "externalIp", "10.117.133.22" ) )
                        .property( property( "bgpPeerIp", "10.117.133.173" ) )
                        .property( property( "bgpPeerAsn", "65001" ) )
                        .property( property( "bgpAdRoutes", "10.117.150.0/24" ) )
                        .o( ) )
                    .o( ) )
                .property( property( "eucanetdHost", "a-35.qa1.eucalyptus-systems.com" ) )
                .property( property( "bgpAsn", "64512" ) )
                .o( ) )
            .property( clusters( )
                .name( "clusters" )
                .cluster( cluster( )
                    .name( "cluster1" )
                    .setValueSubnet( subnet( )
                        .name( "172.31.0.0" )
                        .property( property( "subnet", "172.31.0.0" ) )
                        .property( property( "netmask", "255.255.0.0" ) )
                        .property( property( "gateway", "172.31.0.1" ) ).o( ) )
                    .property( property( "enabledCCIp", "6.6.6.6" ) )
                    .property( property( "macPrefix", "d0:0d" ) )
                    .property( property( "privateIps", "172.31.0.5" ) )
                    .property( nodes( )
                        .name( "nodes" )
                        .node( node( )
                            .name( "node1" )
                            .instanceId( "i-00000001" )
                            .o( ) )
                        .o( ) )
                    .o( ) )
                .o( ) )
            .o( ) )
        .vpc( vpc( )
            .name( "vpc-00000001" )
            .ownerId( "000000000002" )
            .cidr( "10.0.0.0/16" )
            .setValueDhcpOptionSet( "dopt-0000001" )
            .subnet( vpcSubnet( )
                .name( "subnet-00000001" )
                .ownerId( "000000000002" )
                .cidr( "10.0.0.0/16" )
                .cluster( "cluster1" )
                .setValueNetworkAcl( "acl-00000001" )
                .routeTable( "rtb-00000001" )
                .o( ) )
            .routeTable( routeTable( )
                .name( "rtb-00000001" )
                .ownerId( "000000000002" )
                .route( route( )
                    .destinationCidr( "192.168.0.0/16" )
                    .setValueGatewayId( "igw-00000001" )
                    .o( ) )
                .o( ) )
            .internetGateway( "igw-00000001" )
            .o( )
        )
        .internetGateway( internetGateway( )
            .name( "igw-00000001" )
            .ownerId( "000000000002" )
            .o( )
        )
        .instance( instance( )
            .name( "i-00000001" )
            .ownerId( "000000000002" )
            .setValueMacAddress( "00:00:00:00:00:00" )
            .setValuePublicIp( "2.0.0.0" )
            .setValuePrivateIp( "10.0.0.0" )
            .setValueVpc( "vpc-00000001" )
            .setValueSubnet( "subnet-00000001" )
            .networkInterface( networkInterface( )
                .name( "eni-00000001" )
                .ownerId( "000000000002" )
                .deviceIndex( 0 )
                .attachmentId( "eni-attach-00000001" )
                .setValueMacAddress( "00:00:00:00:00:00" )
                .setValuePublicIp( "2.0.0.0" )
                .setValuePrivateIp( "10.0.0.0" )
                .sourceDestCheck( true )
                .vpc( "vpc-00000001" )
                .subnet( "subnet-00000001" )
                .o( )
            )
            .o( )
        )
        .securityGroup( securityGroup( )
            .name( "sg-00000001" )
            .ownerId( "000000000002" )
            .setValueIngressRules( securityGroupRules( )
                .o( )
            )
            .o( )
        )
        .o( );

    System.out.println( info );
    System.out.println( BNI.toString( info ) );
  }
}
