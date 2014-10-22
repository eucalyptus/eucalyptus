package com.eucalyptus.cloudformation;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.ws.WebServices;

/**
 * Created by ethomas on 10/20/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")

public class Limits {

  public final static long STACK_NAME_MAX_LENGTH_CHARS = 255;
  public volatile static long CFN_SIGNAL_MAX_DATA_BYTES = 4096;
  public volatile static long CUSTOM_RESOURCE_PROVIDER_MAX_DATA_BYTES = 4096;
  @ConfigurableField(initial = "100", description = "The maximum number of mappings allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_MAPPINGS_PER_TEMPLATE = 100;
  @ConfigurableField(initial = "30", description = "The maximum number of attributes allowed in a mapping in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_ATTRIBUTES_PER_MAPPING = 30;
  public final static long MAPPING_NAME_MAX_LENGTH_CHARS = 255;
  @ConfigurableField(initial = "60", description = "The maximum number of outputs allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_OUTPUTS_PER_TEMPLATE = 60;
  public final static long OUTPUT_NAME_MAX_LENGTH_CHARS = 255;
  @ConfigurableField(initial = "60", description = "The maximum number of outputs allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_PARAMETERS_PER_TEMPLATE = 60;
  public final static long PARAMETER_NAME_MAX_LENGTH_CHARS = 255;
  public final static long PARAMETER_VALUE_MAX_LENGTH_BYTES = 4096;
  @ConfigurableField(initial = "200", description = "The maximum number of resources allowed in a template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long MAX_RESOURCES_PER_TEMPLATE = 200;
  public final static long RESOURCE_NAME_MAX_LENGTH_CHARS = 255;
  public static long MAX_STACKS_PER_ACCOUNT = 20; // TODO: quota
  @ConfigurableField(initial = "51200", description = "The maximum number of bytes in a request-embedded template", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long REQUEST_TEMPLATE_BODY_MAX_LENGTH_BYTES = 51200;
  @ConfigurableField(initial = "460800", description = "The maximum number of bytes in a template referenced via a URL", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class)
  public volatile static long REQUEST_TEMPLATE_URL_MAX_CONTENT_LENGTH_BYTES = 460800;
  public final static long TEMPLATE_DESCRIPTION_MAX_LENGTH_BYTES = 1024;

}
