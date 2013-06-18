package com.eucalyptus.vm

import static org.junit.Assert.*
import org.junit.Test

/**
 * 
 */
class MetadataRequestTest {

  @Test
  void testPathCanonicalization( ) {
    assertPath( "a", "b", "a/b" )
    assertPath( "a", "b/c/d", "a/b/c/d" )
    assertPath( "a", "b/c/d/", "a/b/c/d/" )
    assertPath( "a", "b/c/d/", "a//b/c/d/" )
    assertPath( "a", "b/c/d/", "a/b//c/d/" )
    assertPath( "a", "b/c/d/", "a/b/c/d////" )
  }

  private void assertPath( String expectedMetadata, String expectedPath, String input ) {
    MetadataRequest request = new MetadataRequest( "127.0.0.1", input ){
      @Override
      protected VmInstance resolveVm(final String requestIp) {
        return null;
      }
    }

    assertEquals( expectedMetadata, request.getMetadataName() )
    assertEquals( expectedPath, request.getLocalPath() )
  }
}
