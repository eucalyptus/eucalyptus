/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cloudformation.template.dependencies;

import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.template.Template;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.*;

import java.io.IOException;
import java.util.*;

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
      String nodeStr = jsonNode.get("nodes").textValue();
      String dependenciesStr = jsonNode.get("dependencies").textValue();
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
