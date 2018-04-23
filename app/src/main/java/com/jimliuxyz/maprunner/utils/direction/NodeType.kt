package com.jimliuxyz.maprunner.utils.direction

import java.util.*

enum class NodeType{
    Start, Point, End;

    fun toInt(): Int {
        return Arrays.binarySearch(NodeType.values(), this)
    }

    companion object {
        fun fromInt(i: Int): NodeType {
            val values = NodeType.values()
            if (i in 0.. values.size)
                return values[i]
            throw IllegalArgumentException("Out of index!")
        }
    }
}