/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.vm

import com.eucalyptus.util.Strings
import com.eucalyptus.vmtypes.VmTypes
import com.google.common.collect.Sets
import org.junit.Ignore
import org.junit.Test

/**
 * Generated instance types CloudFormation template
 */
@Ignore("Developer test")
class InstanceTypesTemplateGenerator {

  @Test
  void test( ) {
    print """\
    AWSTemplateFormatVersion: "2010-09-09"
    Description: EC2 Instance Types
    Parameters:
    """.stripIndent(4)
    Set<String> families = Sets.newTreeSet( )
    families.addAll( VmTypes.PredefinedTypes.values( ).collect{ Strings.substringBefore( '.', it.getName()).toUpperCase() } )
    families.each {
      print """\
      Enable${it}:
        Description: Enable ${it} instance types
        Type: String
        AllowedValues: [ "True", "False" ]
        Default: "${ it == 'T2' || it == 'M5' ? "True" : "False"}"
      """.stripIndent(4)
    }
    print """\
    Resources:
    """.stripIndent(4)
    VmTypes.PredefinedTypes.values( ).each{
      print """\
      ${it.getName().replace('.','').toUpperCase()}:
        Type: AWS::EC2::InstanceType
        Properties:
          Name: ${it.getName()}
          Enabled: !Ref Enable${Strings.substringBefore( '.', it.getName()).toUpperCase()}
          Cpu: ${it.getCpu()}
          Disk: ${it.getDisk()}
          Memory: ${it.getMemory()}
          NetworkInterfaces: ${it.getEthernetInterfaceLimit()}
      """.stripIndent(4)
    }
  }
}
