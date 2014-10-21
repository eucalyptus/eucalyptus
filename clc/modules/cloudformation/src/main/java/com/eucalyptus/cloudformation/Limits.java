package com.eucalyptus.cloudformation;

/**
 * Created by ethomas on 10/20/14.
 */
public class Limits {

  public volatile static long STACK_NAME_MAX_LENGTH_CHARS = 255;
  public volatile static long CFN_SIGNAL_MAX_DATA_BYTES = 4096;
  public volatile static long CUSTOM_RESOURCE_PROVIDER_MAX_DATA_BYTES = 4096;
  public volatile static long MAX_MAPPINGS_PER_TEMPLATE = 100;
  public volatile static long MAX_ATTRIBUTES_PER_MAPPING = 30;
  public volatile static long MAPPING_NAME_MAX_LENGTH_CHARS = 255;
  public volatile static long MAX_OUTPUTS_PER_TEMPLATE = 60;
  public volatile static long OUTPUT_NAME_MAX_LENGTH_CHARS = 255;
  public volatile static long MAX_PARAMETERS_PER_TEMPLATE = 60;
  public volatile static long PARAMETER_NAME_MAX_LENGTH_CHARS = 255;
  public volatile static long PARAMETER_VALUE_MAX_LENGTH_BYTES = 4096;
  public volatile static long MAX_RESOURCES_PER_TEMPLATE = 200;
  public volatile static long RESOURCE_NAME_MAX_LENGTH_CHARS = 255;
  public volatile static long MAX_STACKS_PER_ACCOUNT = 20; // TODO: quota
  public volatile static long REQUEST_TEMPLATE_BODY_MAX_LEMGTH_BYTES = 51200;
  public volatile static long REQUEST_TEMPLATE_URL_MAX_CONTENT_LEMGTH_BYTES = 460800;
  public volatile static long TEMPLATE_DESCRIPTION_MAX_LENGTH_BYTES = 1024;

}
