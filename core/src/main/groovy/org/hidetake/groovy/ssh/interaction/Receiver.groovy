package org.hidetake.groovy.ssh.interaction

import groovy.util.logging.Slf4j

@Slf4j
class Receiver implements Runnable {
    private static final LINE_SEPARATOR = ~/\r\n|[\n\r\u2028\u2029\u0085]/

    final Stream stream
    final List<OutputStream> pipes = []

    private final Listener listener
    private final InputStream inputStream
    private final String encoding

    private lineBuffer = ''

    def Receiver(Listener listener1, Stream stream1, InputStream inputStream1, String encoding1) {
        listener = listener1
        stream = stream1
        inputStream = inputStream1
        encoding = encoding1
        assert listener
        assert stream
        assert inputStream
        assert encoding
    }

    @Override
    void run() {
        log.debug("Started receiver for $stream")
        try {
            def readBuffer = new byte[1024]
            def byteBuffer = new ByteArrayOutputStream(1024)
            while (true) {
                def readLength = inputStream.read(readBuffer)
                if (readLength < 0) {
                    break
                }

                log.debug("Received $readLength bytes from $stream")
                pipes*.write(readBuffer, 0, readLength)
                byteBuffer.write(readBuffer, 0, readLength)
                def block = new String(byteBuffer.toByteArray(), encoding)
                byteBuffer.reset()
                onReceivedBlock(block)
            }

            log.debug("Reached end of stream on $stream")
            onEndOfStream()
        } finally {
            inputStream.close()
            log.debug("Closed $stream")
        }
    }

    private void onReceivedBlock(String block) {
        def cumulativeBlock = lineBuffer + block
        def lines = LINE_SEPARATOR.split(cumulativeBlock, Integer.MIN_VALUE)
        if (lines.length > 0) {
            log.debug("Received $lines.length lines from $stream")
            lines.take(lines.length - 1).each { line ->
                listener.matchLine(stream, line)
            }

            lineBuffer = lines.last()
            if (block && lineBuffer) {
                listener.matchPartial(stream, lineBuffer)
            }
        }
    }

    private void onEndOfStream() {
        def cumulativeBlock = lineBuffer
        def lines = LINE_SEPARATOR.split(cumulativeBlock, Integer.MIN_VALUE)
        if (lines.length > 0) {
            log.debug("Received $lines.length lines from $stream")
            lines.take(lines.length - 1).each { line ->
                listener.matchLine(stream, line)
            }

            lineBuffer = lines.last()
            if (lineBuffer) {
                listener.matchLine(stream, lineBuffer)
                lineBuffer = ''
            }
        }
    }
}
