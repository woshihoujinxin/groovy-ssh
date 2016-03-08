package org.hidetake.groovy.ssh.interaction

import groovy.util.logging.Slf4j

import static org.hidetake.groovy.ssh.util.Utility.callWithDelegate

@Slf4j
class Interaction {
    private final OutputStream standardInput

    private final Deque<Context> contextStack = new ArrayDeque<>()

    def Interaction(Closure interactionClosure, OutputStream standardInput1) {
        standardInput = standardInput1
        assert standardInput

        assert interactionClosure
        contextStack.push(new Context(evaluateInteractionClosure(interactionClosure)))
    }

    void processLine(Stream stream, String line) {
        def context = contextStack.first
        def rule = context.matchLine(stream, line)
        if (rule) {
            log.debug("Rule matched from $stream line: $line -> $rule")
            def evaluatedRules = evaluateInteractionClosure(rule.action.curry(line))
            if (!evaluatedRules.empty) {
                def innerContext = new Context(evaluatedRules)
                contextStack.push(innerContext)
                log.debug("Entering context ${contextStack.size()}: $innerContext")
            }
        } else {
            log.debug("No rule matched from $stream line: $line")
        }
    }

    boolean processBlock(Stream stream, String block) {
        def context = contextStack.first
        def rule = context.matchBlock(stream, block)
        if (rule) {
            log.debug("Rule matched from $stream block: $block -> $rule")
            def evaluatedRules = evaluateInteractionClosure(rule.action.curry(block))
            if (!evaluatedRules.empty) {
                def innerContext = new Context(evaluatedRules)
                contextStack.push(innerContext)
                log.debug("Entering context ${contextStack.size()}: $innerContext")
            }
            true
        } else {
            log.debug("No rule matched from $stream block: $block")
            false
        }
    }

    private evaluateInteractionClosure(Closure interactionClosure) {
        def handler = new InteractionHandler(standardInput)
        callWithDelegate(interactionClosure, handler)
        handler.rules
    }
}
