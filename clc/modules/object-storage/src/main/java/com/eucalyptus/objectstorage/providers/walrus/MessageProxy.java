package com.eucalyptus.objectstorage.providers.walrus;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import io.vavr.collection.Stream;

/**
 * Based on CompositeHelper
 * Converts messages from ObjectStorageRequestType to another message type. Both must
 * have BaseMessage as the base class.
 */
class MessageProxy {

  private static final List<String> baseMsgProps = Stream.of( BeanUtils.getPropertyDescriptors( BaseMessage.class ) )
      .map( PropertyDescriptor::getName )
      .toJavaList( );

  /**
   * Clones the source to dest on a property-name basis.
   * Requires that both source and dest are not null. Will not
   * set values to null in destination that are null in source
   */
  static <O extends BaseMessage, I extends BaseMessage> O mapExcludeNulls( final I source, final O dest ) {
    return mapExcludeNulls( source, dest, baseMsgProps );
  }

  static <T> T mapExcludeNulls( final Object source, final T dest, final Collection<String> excludes ) {
    final Set<String> excludedProperties = Sets.newHashSet( excludes );
    for ( final PropertyDescriptor propertyDescriptor : BeanUtils.getPropertyDescriptors( source.getClass( ) ) ) {
      if ( !excludedProperties.contains( propertyDescriptor.getName() ) &&
          getObjectProperty( source, propertyDescriptor ) == null ) {
        excludedProperties.add( propertyDescriptor.getName( ) );
      }
    }
    BeanUtils.copyProperties( source, dest, excludedProperties.toArray( new String[ excludedProperties.size( ) ] ) );
    return dest;
  }

  private static Object getObjectProperty( final Object object, final PropertyDescriptor property ) {
    return property.getReadMethod( ) == null ?
        null :
        ReflectionUtils.invokeMethod( property.getReadMethod( ), object );
  }
}
