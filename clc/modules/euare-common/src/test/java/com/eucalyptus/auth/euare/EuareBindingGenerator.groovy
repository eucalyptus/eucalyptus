/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare

import com.eucalyptus.auth.euare.common.msgs.EuareMessage
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.junit.Ignore
import org.junit.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

/**
 * Developer test that can be used to generate bindings from message classes for euare.
 */
@Ignore("Developer test")
class EuareBindingGenerator {

  @Test
  void generate() {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider( false )
    scanner.addIncludeFilter( new AssignableTypeFilter( EuareMessage ) );
    Set<String> messageClasses = Sets.newTreeSet( scanner.findCandidateComponents( EuareMessage.package.name )*.beanClassName )
    messageClasses.removeAll( [ 'com.eucalyptus.auth.euare.common.msgs.EuareMessage' ] )
    messageClasses.retainAll( [
      'com.eucalyptus.auth.euare.common.msgs.AttachGroupPolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.AttachGroupPolicyType',
      'com.eucalyptus.auth.euare.common.msgs.AttachRolePolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.AttachRolePolicyType',
      'com.eucalyptus.auth.euare.common.msgs.AttachUserPolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.AttachUserPolicyType',
      'com.eucalyptus.auth.euare.common.msgs.CreatePolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.CreatePolicyType',
      'com.eucalyptus.auth.euare.common.msgs.CreatePolicyVersionResponseType',
      'com.eucalyptus.auth.euare.common.msgs.CreatePolicyVersionType',
      'com.eucalyptus.auth.euare.common.msgs.DeletePolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.DeletePolicyType',
      'com.eucalyptus.auth.euare.common.msgs.DeletePolicyVersionResponseType',
      'com.eucalyptus.auth.euare.common.msgs.DeletePolicyVersionType',
      'com.eucalyptus.auth.euare.common.msgs.DetachGroupPolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.DetachGroupPolicyType',
      'com.eucalyptus.auth.euare.common.msgs.DetachRolePolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.DetachRolePolicyType',
      'com.eucalyptus.auth.euare.common.msgs.DetachUserPolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.DetachUserPolicyType',
      'com.eucalyptus.auth.euare.common.msgs.GetPolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.GetPolicyType',
      'com.eucalyptus.auth.euare.common.msgs.GetPolicyVersionResponseType',
      'com.eucalyptus.auth.euare.common.msgs.GetPolicyVersionType',
      'com.eucalyptus.auth.euare.common.msgs.ListAttachedGroupPoliciesResponseType',
      'com.eucalyptus.auth.euare.common.msgs.ListAttachedGroupPoliciesType',
      'com.eucalyptus.auth.euare.common.msgs.ListAttachedRolePoliciesResponseType',
      'com.eucalyptus.auth.euare.common.msgs.ListAttachedRolePoliciesType',
      'com.eucalyptus.auth.euare.common.msgs.ListAttachedUserPoliciesResponseType',
      'com.eucalyptus.auth.euare.common.msgs.ListAttachedUserPoliciesType',
      'com.eucalyptus.auth.euare.common.msgs.ListEntitiesForPolicyResponseType',
      'com.eucalyptus.auth.euare.common.msgs.ListEntitiesForPolicyType',
      'com.eucalyptus.auth.euare.common.msgs.ListPoliciesResponseType',
      'com.eucalyptus.auth.euare.common.msgs.ListPoliciesType',
      'com.eucalyptus.auth.euare.common.msgs.ListPolicyVersionsResponseType',
      'com.eucalyptus.auth.euare.common.msgs.ListPolicyVersionsType',
      'com.eucalyptus.auth.euare.common.msgs.SetDefaultPolicyVersionResponseType',
      'com.eucalyptus.auth.euare.common.msgs.SetDefaultPolicyVersionType'
     ] )

    Set<String> beanClasses = Sets.newTreeSet( )
    Set<String> printedBeanClasses = Sets.newHashSet( )
    for ( String messageclass : messageClasses ) {
      String simpleName = messageclass.substring( messageclass.lastIndexOf( '.' ) + 1 )

      if ( messageclass.endsWith( "Type" ) ) {
        println """  <mapping name="${simpleName.substring(0,simpleName.length()-4)}" class="${messageclass}" extends="com.eucalyptus.auth.euare.common.msgs.EuareMessage">"""
        println """    <structure map-as="com.eucalyptus.auth.euare.common.msgs.EuareMessage"/>"""
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
