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
package com.eucalyptus.compute.service.ws

import com.eucalyptus.compute.common.ComputeMessage
import com.eucalyptus.compute.common.VpcIpv6ComputeMessage
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.junit.Ignore
import org.junit.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

/**
 * Developer test that can be used to generate bindings from message classes for compute.
 */
@Ignore("Developer test")
class ComputeBindingGenerator {

  @Test
  void generate() {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider( false )
    scanner.addIncludeFilter( new AssignableTypeFilter( VpcIpv6ComputeMessage ) );
    Set<String> messageClasses = Sets.newTreeSet( scanner.findCandidateComponents( ComputeMessage.package.name )*.beanClassName )
    messageClasses.removeAll( [ 'com.eucalyptus.compute.common.VpcIpv6ComputeMessage' ] )

    Set<String> beanClasses = Sets.newTreeSet( )
    Set<String> printedBeanClasses = Sets.newHashSet( )
    for ( String messageclass : messageClasses ) {
      String simpleName = messageclass.substring( messageclass.lastIndexOf( '.' ) + 1 )

      if ( messageclass.endsWith( "Type" ) ) {
        println """  <mapping name="${simpleName.substring(0,simpleName.length()-4)}" class="${messageclass}" extends="com.eucalyptus.compute.common.ComputeMessage">"""
        println """    <structure map-as="com.eucalyptus.compute.common.ComputeMessage"/>"""
        printFields( messageclass, beanClasses )
        println """  </mapping>"""
      } else {
        throw new Exception( "Unexpected message class : ${messageclass}" )
      }
    }

    while ( !printedBeanClasses.containsAll( beanClasses ) ) {
      for ( String beanClass : Lists.newArrayList( beanClasses ) ) {
        if ( printedBeanClasses.contains( beanClass ) ) continue;
        println """  <mapping class="${beanClass}" abstract="true">"""
        printFields( beanClass, beanClasses )
        println """  </mapping>"""
        printedBeanClasses << beanClass
      }
    }
  }

  void printFields( String messageclass, Collection<String> beanClasses ) {
    Class.forName( messageclass ).declaredFields.each { Field field ->
      if ( field.name.startsWith( '$' ) || field.name.startsWith( '_' ) || field.type.package.name.startsWith( 'groovy.lang' ) ) {
        // ignore
      } else if ( Collection.class.isAssignableFrom( field.type ) ) {
        Class itemType = ( Class ) ( ( ParameterizedType ) field.genericType ).actualTypeArguments[0];
        if ( itemType.name.startsWith( 'java' ) ) {
          println """    <collection name="${field.name.substring(0,1).toUpperCase()}${field.name.substring(1)}" field="${field.name}" usage="optional">"""
          println """      <value name="member" type="${itemType.name}"/>"""
          println """    </collection>"""
        } else {
          println """    <collection name="${field.name.substring(0,1).toUpperCase()}${field.name.substring(1)}" field="${field.name}" usage="optional">>"""
          println """      <structure name="member" type="${itemType.name}"/>"""
          println """    </collection>"""
          beanClasses << itemType.name
        }
      } else if ( field.type.package.name.startsWith( 'java' ) ) {
        println """    <value name="${field.name.substring(0,1).toUpperCase()}${field.name.substring(1)}" field="${field.name}" usage="optional"/>"""
      } else {
        println """    <structure name="${field.name.substring(0,1).toUpperCase()}${field.name.substring(1)}" field="${field.name}" usage="optional" type="${field.type.name}"/>"""
        beanClasses << field.type.name
      }
    }
  }
}
