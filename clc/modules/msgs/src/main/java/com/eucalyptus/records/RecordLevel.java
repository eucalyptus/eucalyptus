package com.eucalyptus.records;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.log4j.Logger;
import com.google.common.collect.Lists;

public enum RecordLevel {
  TRACE, DEBUG, INFO, WARN, FATAL, ERROR;
  private static Logger             LOG     = Logger.getLogger( RecordLevel.class );
}
