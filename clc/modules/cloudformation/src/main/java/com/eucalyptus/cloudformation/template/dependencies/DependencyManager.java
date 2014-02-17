package com.eucalyptus.cloudformation.template.dependencies;

import com.google.common.collect.*;

import java.util.*;

/**
 * Created by ethomas on 1/27/14.
 */
public class DependencyManager {

  Set<String> nodes = Sets.newHashSet();
  // key = node which edge starts from, value = destination
  Multimap<String, String> outEdges = TreeMultimap.create(); // sorted so consistent dependency list result

  public synchronized void addNode(String node) {
    nodes.add(node);
  }

  public synchronized boolean containsNode(String node) {
    return nodes.contains(node);
  }

  public synchronized void addDependency(String dependentNode, String independentNode) throws NoSuchElementException {
    if (!nodes.contains(dependentNode)) throw new NoSuchElementException(dependentNode);
    if (!nodes.contains(independentNode)) throw new NoSuchElementException(independentNode);
    outEdges.put(independentNode, dependentNode); // An edge from A to B means B depends on A. (i.e. can start with A)
  }

  public synchronized List<String> dependencyList() throws CyclicDependencyException {
    LinkedList<String> sortedNodes = Lists.newLinkedList();
    Set<String> unmarkedNodes = Sets.newTreeSet(nodes);
    Set<String> temporarilyMarkedNodes = Sets.newLinkedHashSet(); // this also represents the current path...
    Set<String> permanentlyMarkedNodes = Sets.newHashSet();
    while (!unmarkedNodes.isEmpty()) {
      String currentNode = unmarkedNodes.iterator().next();
      visitNode(currentNode, unmarkedNodes, temporarilyMarkedNodes, permanentlyMarkedNodes, sortedNodes);
    }
    return sortedNodes;
  }
  private void visitNode(String currentNode, Set<String> unmarkedNodes, Set<String> temporarilyMarkedNodes, Set<String> permanentlyMarkedNodes,
                         LinkedList<String> sortedNodes) throws CyclicDependencyException {
    if (temporarilyMarkedNodes.contains(currentNode)) {
      throw new CyclicDependencyException(subListFrom(Lists.newArrayList(temporarilyMarkedNodes), currentNode).toString());
    }
    if (unmarkedNodes.contains(currentNode)) {
      unmarkedNodes.remove(currentNode);
      temporarilyMarkedNodes.add(currentNode);
      for (String adjacentNode: outEdges.get(currentNode)) {
        visitNode(adjacentNode, unmarkedNodes, temporarilyMarkedNodes, permanentlyMarkedNodes, sortedNodes);
      }
      temporarilyMarkedNodes.remove(currentNode);
      permanentlyMarkedNodes.add(currentNode);
      sortedNodes.addFirst(currentNode);
    }
  }


  private List<String> subListFrom(List<String> list, String element) {
    int index = list.indexOf(element);
    if (index == -1) return list;
    return list.subList(index, list.size());
  }

  @Override
  public String toString() {
    return "DependencyManager{" +
      "nodes=" + nodes +
      ", outEdges=" + outEdges +
      '}';
  }
}
