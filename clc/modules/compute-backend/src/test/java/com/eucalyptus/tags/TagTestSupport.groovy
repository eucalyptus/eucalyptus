/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.tags

import com.eucalyptus.compute.common.internal.tags.Tag
import com.eucalyptus.compute.common.internal.tags.TagSupport

/**
 * 
 */
class TagTestSupport {
  
  void assertValidTagSupport( final TagSupport tagSupport,
                              final Class<? extends Tag> tagClass ) {
    tagSupport.resourceClass
    tagSupport.resourceClassIdField
    tagSupport.tagClassResourceField

    assertValidField( "Resource ID for resource class matching", tagSupport.resourceClass, tagSupport.resourceClassIdField, String.class )
    assertValidField( "Resource reference for tag matching", tagClass, tagSupport.tagClassResourceField, tagSupport.resourceClass )
  }
  
  private void assertValidField( final String description,
                                 final Class clazz,
                                 final String field,
                                 final Class type ) {
    org.junit.Assert.assertNotNull( 
        description + ": " + field, 
        org.springframework.util.ReflectionUtils.findField( clazz, field, type ) )    
  }
}
