package org.hidetake.groovy.ssh.interaction

import org.hidetake.groovy.ssh.operation.LineOutputStream

class InteractionManager {
    private final OutputStream standardInput
    private final LineOutputStream standardOutput
    private final LineOutputStream standardError

    def InteractionManager(OutputStream standardInput1, LineOutputStream standardOutput1, LineOutputStream standardError1) {
        standardInput = standardInput1
        standardOutput = standardOutput1
        standardError = standardError1
        assert standardInput
        assert standardOutput
        assert standardError
    }

    def InteractionManager(OutputStream standardInput1, LineOutputStream standardOutput1) {
        standardInput = standardInput1
        standardOutput = standardOutput1
        standardError = null
        assert standardInput
        assert standardOutput
    }

    void add(@DelegatesTo(InteractionHandler) Closure interactionClosure) {
        def interaction = new Interaction(interactionClosure, standardInput)
        standardOutput.listenLine { String line -> interaction.processLine(Stream.StandardOutput, line) }
        standardError?.listenLine { String line -> interaction.processLine(Stream.StandardError, line) }
        standardOutput.listenPartial { String block -> interaction.processBlock(Stream.StandardOutput, block) }
        standardError?.listenPartial { String block -> interaction.processBlock(Stream.StandardError, block) }
    }
}
