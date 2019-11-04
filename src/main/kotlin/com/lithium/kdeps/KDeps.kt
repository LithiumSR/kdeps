package com.lithium.kdeps

import com.paypal.digraph.parser.GraphParser
import java.io.*


class KDeps(private val builder: Builder) {
    private var graph: DependencyGraph? = null

    fun getDependencyGraph(): DependencyGraph {
        return if (graph != null) graph!! else executeJDeps()!!
    }

    private fun executeJDeps(): DependencyGraph? {
        if (graph != null) return graph
        val isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
        println(System.getProperty("user.dir"))
        val savePath =
            File(builder.savePath + if (builder.keepOutputFiles) "/kdeps_output" else "/_kdeps_tmp").absolutePath
        val saveFolder = File(savePath)
        saveFolder.mkdirs()
        val classPath = File(builder.path!!)
        if (!classPath.exists()) throw RuntimeException("Folder/file to analyze does not exist")
        val runtime = Runtime.getRuntime().exec(
            "${if (isWindows) "cmd /c" else ""} jdeps " +
                    "-verbose:${if (builder.verboseMode == Builder.VerboseMode.CLASS) "class" else "package"} " +
                    (if (builder.regex != null) "-e ${builder.regex} " else "") +
                    (if (builder.apiOnly) "-apionly " else "") +
                    (if (builder.recursive) "-recursive " else "") +
                    "-dotoutput $savePath ${builder.path}"
        )
        runtime.waitFor()
        val target = File(builder.path!!)
        if (builder.removeParenthesis) removeParenthesis("$savePath\\${target.name}.dot")
        val parser = GraphParser(FileInputStream("$savePath\\${target.name}.dot"))
        graph = DependencyGraph(parser.nodes, parser.edges)
        if (!builder.keepOutputFiles) saveFolder.deleteRecursively()
        val exitVal = runtime.exitValue()
        if (exitVal != 0) throw RuntimeException("jdeps execution failed, check if jdeps binary is correctly set in the path")
        return graph
    }


    private fun removeParenthesis(filename: String) {
        val br = BufferedReader(FileReader(filename))
        val inputBuffer = StringBuffer()
        br.forEachLine {
            inputBuffer.append(it.replace(" \\(.*?\\) ?".toRegex(), ""))
            inputBuffer.append('\n')
        }
        br.close()
        val fileOut = FileOutputStream(filename)
        fileOut.write(inputBuffer.toString().toByteArray())
        fileOut.close()
    }


    class Builder {
        var path: String? = null
        var savePath: String = "."
            set(value) {
                field = value
                if (value.endsWith("\\") || value.endsWith("/")) field = value.substring(0, value.length - 1)
            }
        var regex: String? = null
        var verboseMode = VerboseMode.CLASS
        var recursive = false
        var removeParenthesis = false
        var apiOnly = false
        var keepOutputFiles = true
        fun build(): KDeps {
            requireNotNull(path) { "You must provide a path to a directory/JAR to read from" }
            return KDeps(this)
        }

        enum class VerboseMode {
            CLASS, PACKAGE
        }
    }
}

