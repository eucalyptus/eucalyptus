package com.eucalyptus.objectstorage.providers.walrus;

import java.beans.PropertyDescriptor;
import java.util.List;
import org.springframework.beans.BeanUtils;
import com.eucalyptus.BaseException;
import javaslang.collection.Stream;

/**
 * Based on CompositeHelper
 * Converts exceptions to another message type. Both must
 * have BaseException as the base class.
 */
class WalrusExceptionProxy {

  private static final List<String> baseExceptionProps = Stream.of( BeanUtils.getPropertyDescriptors( BaseException.class ) )
        .map( PropertyDescriptor::getName )
        .toJavaList( );

  /**
   * Clones the source to dest on a property-name basis.
   * Requires that both source and dest are not null. Will not
   * set values to null in destination that are null in source
   */
  static <O extends BaseException, I extends BaseException> O mapExcludeNulls( final I source, final O dest ) {
    return MessageProxy.mapExcludeNulls( source, dest, baseExceptionProps );
  }
}
