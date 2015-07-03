package com.criteo.tableau

import scala.xml.{Node, NodeSeq}

package object validation {

  /**
   * Get the values of the "name" attribute of a list of nodes.
   * @param nodes List of nodes to get the names of.
   * @return The names of the nodes.
   */
  def getNames(nodes: NodeSeq): Seq[String] = nodes collect {
    case node if (node \ "@name").nonEmpty => (node \ "@name").toString()
  }

  /**
   * Get the value of the "name" attribute of a node.
   * @param node The node to get the name of.
   * @return The name of the node.
   */
  def getName(node: Node): String = (node \ "@name").head.toString()
}
