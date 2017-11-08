/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloudformation.template.dependencies;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class DependencyManager {

  private Set<String> nodes = Sets.newLinkedHashSet();
  private Table<String, String, Integer> edgeTable = TreeBasedTable.create();

  public Set<String> getNodes() {
    return nodes;
  }

  public Collection<String> getDependentNodes(String independentNode) {
    return edgeTable.row(independentNode).keySet();
  }

  public Collection<String> getReverseDependentNodes(String independentNode) {
    return edgeTable.column(independentNode).keySet();
  }

  public synchronized void addNode(String node) {
    nodes.add(node);
  }

  public synchronized boolean containsNode(String node) {
    return nodes.contains(node);
  }

  public synchronized void addDependency(String dependentNode, String independentNode) throws NoSuchElementException {
    if (!nodes.contains(dependentNode)) throw new NoSuchElementException(dependentNode);
    if (!nodes.contains(independentNode)) throw new NoSuchElementException(independentNode);
    edgeTable.put(independentNode, dependentNode, 1); // An edge from A to B means B depends on A. (i.e. can start with A)
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
      for (String adjacentNode: edgeTable.row(currentNode).keySet()) {
        visitNode(adjacentNode, unmarkedNodes, temporarilyMarkedNodes, permanentlyMarkedNodes, sortedNodes);
      }
      temporarilyMarkedNodes.remove(currentNode);
      permanentlyMarkedNodes.add(currentNode);
      sortedNodes.addFirst(currentNode);
    }
  }

  public String toJson() throws CloudFormationException {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String nodesStr = mapper.writeValueAsString(nodes);
      Map<String,List<String>> dependencies = convertToMap(edgeTable);
      String dependenciesStr = mapper.writeValueAsString(dependencies);
      ObjectNode objectNode = mapper.createObjectNode();
      objectNode.put("nodes", nodesStr);
      objectNode.put("dependencies", dependenciesStr);
      return objectNode.toString();
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  private Map<String,List<String>> convertToMap(Table<String, String, Integer> table) {
    Map<String, List<String>> map = Maps.newLinkedHashMap();
    for (String row:table.rowKeySet()) {
      map.put(row, Lists.newArrayList(table.row(row).keySet()));
    }
    return map;
  }

  public static DependencyManager fromJson(String json) throws CloudFormationException {
    if (json == null) return new DependencyManager();
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonNode = mapper.readTree(json);
      String nodeStr = jsonNode.get("nodes").asText();
      String dependenciesStr = jsonNode.get("dependencies").asText();
      ArrayList<String> nodes = mapper.readValue(nodeStr,
        new TypeReference<ArrayList<String>>(){});
      Map<String, List<String>> dependencies = mapper.readValue(dependenciesStr,
        new TypeReference<LinkedHashMap<String, List<String>>>(){});
      DependencyManager dependencyManager = new DependencyManager();
      for (String node:nodes) {
        dependencyManager.addNode(node);
      }
      for (String row: dependencies.keySet()) {
        for (String column: dependencies.get(row)) {
          dependencyManager.addDependency(column, row);
        }
      }
      return dependencyManager;
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  private List<String> subListFrom(List<String> list, String element) {
    int index = list.indexOf(element);
    if (index == -1) return list;
    return list.subList(index, list.size());
  }
}
