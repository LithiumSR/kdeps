package com.lithium.kdeps

import com.paypal.digraph.parser.GraphEdge
import com.paypal.digraph.parser.GraphNode

data class DependencyGraph(val nodes: MutableMap<String, GraphNode>, val edges: MutableMap<String, GraphEdge>)