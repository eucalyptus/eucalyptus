package com.eucalyptus.tags

/**
 * 
 */
class TagSupportTest {
  
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
