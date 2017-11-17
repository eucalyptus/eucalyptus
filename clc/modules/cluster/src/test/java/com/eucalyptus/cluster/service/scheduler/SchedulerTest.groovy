/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cluster.service.scheduler

import com.eucalyptus.cluster.service.node.ClusterNode
import com.eucalyptus.cluster.service.vm.ClusterVmType
import io.vavr.collection.Stream
import io.vavr.control.Option
import org.junit.Assert
import org.junit.Test

/**
 *
 */
class SchedulerTest {
  
  @Test
  void testGreedySchedulerEmpty( ) {
    Scheduler scheduler = new GreedyScheduler( )
    ClusterNode node = node( "one", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
    Assert.assertEquals( 'node', node.node, scheduledNodeOption.get( ).node )
    Assert.assertEquals( 'available cores', 9, node.coresAvailable )
    Assert.assertEquals( 'available disk', 5, node.diskAvailable )
    Assert.assertEquals( 'available memory', 9_488, node.memoryAvailable )
  }

  @Test
  void testGreedySchedulerEmptyMultipleNodes( ) {
    Scheduler scheduler = new GreedyScheduler( )
    ClusterNode node1 = node( "one", 10, 10, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption.get( ).node )
  }

  @Test
  void testGreedySchedulerNodeWithoutCapacity( ) {
    Scheduler scheduler = new GreedyScheduler( )
    ClusterNode node1 = node( "one", 10, 1, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
    Assert.assertEquals( 'node', node2.node, scheduledNodeOption.get( ).node )
  }

  @Test
  void testGreedySchedulerMultipleInstances( ) {
    Scheduler scheduler = new GreedyScheduler( )
    ClusterNode node1 = node( "one", 10, 10, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption1 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption2 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption1.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption1.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption2.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption2.get( ).node )
  }

  @Test
  void testGreedySchedulerInstancesToCapacity( ) {
    Scheduler scheduler = new GreedyScheduler( )
    ClusterNode node1 = node( "one", 10, 10, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption1 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption2 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption3 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption4 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption1.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption1.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption2.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption2.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption3.defined )
    Assert.assertEquals( 'node', node2.node, scheduledNodeOption3.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption4.defined )
    Assert.assertEquals( 'node', node2.node, scheduledNodeOption4.get( ).node )
  }

  @Test
  void testGreedySchedulerManyNodes( ) {
    Scheduler scheduler = new GreedyScheduler( )
    List<ClusterNode> nodes = ( 1..1000 ).collect { Integer number ->
      node("node${number}", 10, 10, 10_000)
    }
    for ( ClusterNode node : nodes ) {
      ( 1..2 ).forEach{ // 2 instances per node
        Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
          scheduler.schedule( Stream.ofAll( nodes ), type( 1, 5, 512 ) )
        }
        Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
        Assert.assertEquals( 'node', node.node, scheduledNodeOption.get( ).node )
      }
    }
  }

  @Test
  void testRoundRobinSchedulerEmpty( ) {
    Scheduler scheduler = new RoundRobinScheduler( )
    ClusterNode node = node( "one", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
    Assert.assertEquals( 'node', node.node, scheduledNodeOption.get( ).node )
    Assert.assertEquals( 'available cores', 9, node.coresAvailable )
    Assert.assertEquals( 'available disk', 5, node.diskAvailable )
    Assert.assertEquals( 'available memory', 9_488, node.memoryAvailable )
  }

  @Test
  void testRoundRobinSchedulerMultipleNodes( ) {
    Scheduler scheduler = new RoundRobinScheduler( )
    ClusterNode node1 = node( "one", 10, 10, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption.get( ).node )
  }

  @Test
  void testRoundRobinSchedulerNodeWithoutCapacity( ) {
    Scheduler scheduler = new RoundRobinScheduler( )
    ClusterNode node1 = node( "one", 10, 1, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
    Assert.assertEquals( 'node', node2.node, scheduledNodeOption.get( ).node )
  }

  @Test
  void testRoundRobinSchedulerMultipleInstances( ) {
    Scheduler scheduler = new RoundRobinScheduler( )
    ClusterNode node1 = node( "one", 10, 10, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption1 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption2 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption1.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption1.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption2.defined )
    Assert.assertEquals( 'node', node2.node, scheduledNodeOption2.get( ).node )
  }

  @Test
  void testRoundRobinSchedulerInstancesToCapacity( ) {
    Scheduler scheduler = new RoundRobinScheduler( )
    ClusterNode node1 = node( "one", 10, 10, 10_000 )
    ClusterNode node2 = node( "two", 10, 10, 10_000 )
    Option<ClusterNode> scheduledNodeOption1 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption2 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption3 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Option<ClusterNode> scheduledNodeOption4 = Schedulers.withAutoCommitContext{
      scheduler.schedule( Stream.of( node1, node2 ), type( 1, 5, 512 ) )
    }
    Assert.assertTrue( 'scheduled', scheduledNodeOption1.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption1.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption2.defined )
    Assert.assertEquals( 'node', node2.node, scheduledNodeOption2.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption3.defined )
    Assert.assertEquals( 'node', node1.node, scheduledNodeOption3.get( ).node )
    Assert.assertTrue( 'scheduled', scheduledNodeOption4.defined )
    Assert.assertEquals( 'node', node2.node, scheduledNodeOption4.get( ).node )
  }

  @Test
  void testRoundRobinSchedulerManyNodes( ) {
    Scheduler scheduler = new RoundRobinScheduler( )
    List<ClusterNode> nodes = ( 1..1000 ).collect { Integer number ->
      node("node${number}", 10, 10, 10_000)
    }
    ( 1..2 ).forEach{ // 2 instances per node
      for ( ClusterNode node : nodes ) {
        Option<ClusterNode> scheduledNodeOption = Schedulers.withAutoCommitContext{
          scheduler.schedule( Stream.ofAll( nodes ), type( 1, 5, 512 ) )
        }
        Assert.assertTrue( 'scheduled', scheduledNodeOption.defined )
        Assert.assertEquals( 'node', node.node, scheduledNodeOption.get( ).node )
      }
    }
  }

  private static ClusterNode node( String name, int cores, int disk, int memory ) {
    new ClusterNode( name, "iqn.1994-05.com.redhat:c7ec6fad289", cores, disk, memory )
  }

  private static ClusterVmType type( int cores, int disk, int memory  ) {
    ClusterVmType.of( "t1.simple", cores, disk, memory )
  }
}
