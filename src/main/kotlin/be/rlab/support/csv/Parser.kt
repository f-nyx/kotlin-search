package be.rlab.support.csv

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode

class Parser(
    config: ParserConfig = ParserConfig.default()
) {

    companion object {
        private const val DOUBLE_QUOTE: Byte = 34
        private const val LINE_FEED: Byte = 10
        private const val CARRIAGE_RETURN: Byte = 13
        private const val ESCAPE: Byte = 92
        /** Size of the buffer used to detect file format. */
        private const val PROBE_BUFFER_SIZE: Long = 1024 * 1024
    }

    private val logger: Logger = LoggerFactory.getLogger(Parser::class.java)
    private val bufferSize: Long = config.bufferSize
    private val separator: Byte = config.separator

    fun parse(
        csvFile: String,
        callback: (Position, List<Field>) -> Unit
    ) {

        logger.info("parsing csv started")

        val handle = RandomAccessFile(csvFile, "r")
        val probeBufferSize: Long = if (handle.length() < PROBE_BUFFER_SIZE) {
            handle.length()
        } else {
            PROBE_BUFFER_SIZE
        }
        val lineBreakSize = lineSeparatorLength(
            handle.channel.map(MapMode.READ_ONLY, 0, probeBufferSize),
            handle.length()
        )
        var pointer: Long = 0
        var tail = ByteArray(0)

        logger.info("csv line reader ready to send records")

        while(true) {
            val bytesRead: Long = if (pointer + bufferSize > handle.length()) {
                handle.length() - pointer
            } else {
                bufferSize
            }
            logger.info("reading $bytesRead bytes")

            val buffer: ByteBuffer = handle.channel.map(
                MapMode.READ_ONLY, pointer, bytesRead
            )
            var offset = 0
            var lineStart = 0

            while (offset < bytesRead) {
                val char: Byte = buffer[offset]

                if (char == LINE_FEED || char == CARRIAGE_RETURN) {

                    val lineEnd = lineStart + (offset - lineStart)
                    val line = if (tail.isNotEmpty()) {
                        val lineWithTail = tail + readLine(buffer, lineStart, lineEnd)
                        tail = ByteArray(0)
                        lineWithTail
                    } else
                        readLine(buffer, lineStart, lineEnd)

                    callback(Position(
                        start = pointer + lineStart,
                        end = pointer + lineEnd
                    ), parseRecord(line))

                    buffer.position(buffer.position() + lineBreakSize)
                    offset += lineBreakSize
                    lineStart = offset
                } else {
                    offset += 1
                }
            }

            if (bytesRead < bufferSize) {
                break
            }

            tail = readLine(buffer, lineStart, bytesRead.toInt())
            pointer += bytesRead
        }

        logger.info("parsing csv finished")
    }

    private fun lineSeparatorLength(
        buffer: ByteBuffer,
        size: Long
    ): Int {
        var offset = 0
        var char: Byte = buffer[0]

        while (offset < size && char != LINE_FEED && char != CARRIAGE_RETURN) {
            char = buffer[++offset]
        }

        val nextChar: Byte = if (offset < size) {
            buffer[++offset]
        } else {
            -1
        }

        return when {
            char == CARRIAGE_RETURN && nextChar == LINE_FEED -> 2
            else -> 1
        }
    }

    private fun parseRecord(rawRecord: ByteArray): List<Field> {
        var withinField = false
        var escape = false
        var startIndex = 0
        val record: MutableList<Field> = mutableListOf()
        var addend = 0

        for (index in rawRecord.indices) {
            val char = rawRecord[index]

            when {
                !escape && !withinField && char == separator -> {
                    record.add(Field(
                        rawRecord.copyOfRange(startIndex + addend, startIndex + (index - startIndex) - addend)
                    ))
                    startIndex = index + 1
                    addend = 0
                }
                !escape && char == DOUBLE_QUOTE -> {
                    if (!withinField) {
                        addend += 1
                    }

                    withinField = !withinField
                }
                !escape && char == ESCAPE ->
                    escape = true
                escape -> {
                    escape = false
                }
            }
        }

        record.add(Field(
            rawRecord.copyOfRange(startIndex + addend, startIndex + (rawRecord.size - startIndex) - addend)
        ))

        return record
    }

    private fun readLine(
        buffer: ByteBuffer,
        start: Int,
        end: Int
    ): ByteArray {
        val lineBuffer = ByteArray(end - start)
        buffer.get(lineBuffer)
        return lineBuffer
    }
}
