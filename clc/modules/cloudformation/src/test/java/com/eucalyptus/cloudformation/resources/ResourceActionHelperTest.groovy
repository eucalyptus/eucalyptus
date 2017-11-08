/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.resources

import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 *
 */
class ResourceActionHelperTest {
  
  @Test
  void testTruncatedName( ) {
    String id = ResourceActionHelper.getDefaultPhysicalResourceId( "nginx-stack-1", "NginxELB", 32 );
    assertEquals( 'Generated identifier length', 32, id.length( ) )
    assertEquals( 'Generated identifier prefix', 'nginx-sta-NginxELB-', id.substring( 0, 19 ) )
  }

  @Test
  void testFullName( ) {
    String id = ResourceActionHelper.getDefaultPhysicalResourceId( "nginx-stack-1", "NginxELB", 1_000_000 );
    assertEquals( 'Generated identifier length', 36, id.length( ) )
    assertEquals( 'Generated identifier prefix', 'nginx-stack-1-NginxELB-', id.substring( 0, 23 ) )
  }

}
