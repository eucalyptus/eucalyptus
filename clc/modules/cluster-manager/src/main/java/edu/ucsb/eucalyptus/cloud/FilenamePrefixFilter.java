package edu.ucsb.eucalyptus.cloud;

import java.io.*;

public class FilenamePrefixFilter implements FilenameFilter
{
  private String prefix;

  FilenamePrefixFilter( final String prefix )
  {
    this.prefix = prefix;
  }

  public boolean accept( final File dir, final String name )
  {
    return name.startsWith( this.prefix );
  }


}
