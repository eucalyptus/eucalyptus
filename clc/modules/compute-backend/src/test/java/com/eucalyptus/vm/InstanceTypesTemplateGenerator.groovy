/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    String desc = ''
    def filter = {
      // backwards compat (have to edit parameters after create)
      desc = 'Eucalyptus 4 '
      [
          'c1.medium',
          'c1.xlarge',
          'cc1.4xlarge',
          'cc2.8xlarge',
          'cg1.4xlarge',
          'cr1.8xlarge',
          'hi1.4xlarge',
          'hs1.8xlarge',
          'm1.large',
          'm1.medium',
          'm1.small',
          'm1.xlarge',
          'm2.2xlarge',
          'm2.4xlarge',
          'm2.xlarge',
          'm3.2xlarge',
          'm3.xlarge',
          't1.micro',
      ].contains(it.getName())

      //desc = 'General '
      //it.getName().startsWith('t') or it.getName().startsWith('m')

      //desc = 'Compute Optimized '
      //it.getName().startsWith('c')

      //desc = 'Memory Optimized '
      //it.getName().startsWith('r') or it.getName().startsWith('x')or it.getName().startsWith('z')

      //desc = 'Storage Optimized '
      //it.getName().startsWith('d') or it.getName().startsWith('h')or it.getName().startsWith('i')

      //desc = 'Accelerated '
      //it.getName().startsWith('f') or it.getName().startsWith('g')or it.getName().startsWith('p')
    }
    filter.call(VmTypes.PredefinedTypes.C1MEDIUM) // warmup ...
    print """\
    AWSTemplateFormatVersion: 2010-09-09
    Description: EC2 ${desc}Instance Types
    Parameters:
    """.stripIndent(4)
    Set<String> families = Sets.newTreeSet( )
    families.addAll( VmTypes.PredefinedTypes.values( ).findAll(filter).collect{ Strings.substringBefore( '.', it.getName()).toUpperCase() } )
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
    families.each { familiy ->
      String lastResourceForFamily = null
      VmTypes.PredefinedTypes.values().findAll(filter).findAll{it.getName().startsWith(familiy.toLowerCase()+'.')}.each {
        String resource = it.getName().replace('.', '').toUpperCase()
        print """\
      ${resource}:
        Type: AWS::EC2::InstanceType${lastResourceForFamily==null ? '' : "\n        DependsOn: ${lastResourceForFamily}"}
        Properties:
          Name: ${it.getName()}
          Enabled: !Ref Enable${Strings.substringBefore('.', it.getName()).toUpperCase()}
          Cpu: ${it.getCpu()}
          Disk: ${it.getDisk()}
          Memory: ${it.getMemory()}
          NetworkInterfaces: ${it.getEthernetInterfaceLimit()}
      """.stripIndent(4)
        lastResourceForFamily = resource
      }
    }
  }
}
