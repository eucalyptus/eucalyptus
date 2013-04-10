package com.eucalyptus.cloudwatch.ws;

import com.eucalyptus.binding.BindingException;
import com.eucalyptus.cloudwatch.CloudWatchException;
import com.eucalyptus.cloudwatch.MissingParameterException;
import com.eucalyptus.http.MappingHttpRequest;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class WrappedBindingException extends BindingException {

  public WrappedBindingException() {
  }

  public WrappedBindingException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public WrappedBindingException(String msg) {
    super(msg);
  }

  public WrappedBindingException(Throwable cause) {
    super(cause);
  }
  
  public WrappedBindingException(Throwable cause, MappingHttpRequest mappingHttpRequest) {
    super(cause);
    this.mappingHttpRequest = mappingHttpRequest;
  }

  private MappingHttpRequest mappingHttpRequest;

  public MappingHttpRequest getMappingHttpRequest() {
    return mappingHttpRequest;
  }

  public void setMappingHttpRequest(MappingHttpRequest mappingHttpRequest) {
    this.mappingHttpRequest = mappingHttpRequest;
  }

}
