package com.eucalyptus.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HoldMe {

  public static Lock           canHas = new ReentrantLock( );

}
