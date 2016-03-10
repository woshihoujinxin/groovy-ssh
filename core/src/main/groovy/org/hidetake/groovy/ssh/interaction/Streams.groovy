package org.hidetake.groovy.ssh.interaction

class Streams {
    private final OutputStream standardInput

    private final Listener listener
    private final List<Receiver> receivers = []
    private final List<Thread> threads = []

    def Streams(OutputStream standardInput1, InputStream standardOutput, InputStream standardError, String encoding) {
        standardInput = standardInput1
        listener = new Listener()
        receivers.add(new Receiver(listener, Stream.StandardOutput, standardOutput, encoding))
        receivers.add(new Receiver(listener, Stream.StandardError, standardError, encoding))
    }

    def Streams(OutputStream standardInput1, InputStream standardOutput, String encoding) {
        standardInput = standardInput1
        listener = new Listener()
        receivers.add(new Receiver(listener, Stream.StandardOutput, standardOutput, encoding))
    }

    void pipe(Stream stream, OutputStream outputStream) {
        receivers.find { it.stream == stream }.pipes.add(outputStream)
    }

    void addInteraction(@DelegatesTo(InteractionHandler) Closure closure) {
        listener.addInteraction(new Interaction(closure, standardInput))
    }

    void start() {
        threads.addAll(receivers.collect { new Thread(it) })
        threads*.start()
    }

    void waitForEndOfStream() {
        threads*.join()
    }
}
