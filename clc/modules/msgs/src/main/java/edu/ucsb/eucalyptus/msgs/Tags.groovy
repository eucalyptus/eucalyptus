package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded

public class ResourceTag extends EucalyptusData {
  String key;
  String value;
}

public class DeleteResourceTag extends EucalyptusData {
  String key;   // optional
  String value; // optional
}

public class ResourceTagMessage extends EucalyptusMessage {
  public ResourceTagMessage( ) { }

  public ResourceTagMessage( EucalyptusMessage msg ) {
    super( msg );
  }

  public ResourceTagMessage( String userId ) {
    super( userId );
  }
}

public class DescribeTagsType extends ResourceTagMessage  {
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}

public class DescribeTagsResponseType extends ResourceTagMessage  {
  String requestId;
  ArrayList<TagInfo> tagSet = new ArrayList<TagInfo>( );
}

public class DeleteTagsResponseType extends ResourceTagMessage {
  String requestId;
}

public class DeleteTagsType extends ResourceTagMessage {
  @HttpParameterMapping( parameter = "ResourceId" )
  ArrayList<String> resourcesSet = new ArrayList<String>();
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  ArrayList<DeleteResourceTag> tagSet = new ArrayList<DeleteResourceTag>();
}

public class TagInfo extends EucalyptusData {
  String resourceId;
  String resourceType;
  String key;
  String value;
}

public class CreateTagsResponseType extends ResourceTagMessage  {
  String requestId;
}

public class CreateTagsType extends ResourceTagMessage  {
  @HttpParameterMapping( parameter = "ResourceId" )
  ArrayList<String> resourcesSet = new ArrayList<String>();
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
}
