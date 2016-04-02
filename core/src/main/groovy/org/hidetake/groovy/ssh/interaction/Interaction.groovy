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
        def innerContext = new Context(evaluateInteractionClosure(interactionClosure))
        contextStack.push(innerContext)
        log.trace("Entering context ${contextStack.size()}: $innerContext")
    }

    void processLine(Stream stream, String line) {
        def context = contextStack.first
        def rule = context.matchLine(stream, line)
        if (rule) {
            log.trace("Rule matched: from: $stream, line: $line -> $rule")
            def evaluatedRules = evaluateInteractionClosure(rule.action.curry(line))
            if (!evaluatedRules.empty) {
                def innerContext = new Context(evaluatedRules)
                contextStack.push(innerContext)
                log.trace("Entering context#${contextStack.size()}: $innerContext")
            }
        } else {
            log.trace("No rule matched: from: $stream, line: $line")
        }
    }

    void processPartial(Stream stream, String partial) {
        def context = contextStack.first
        def rule = context.matchPartial(stream, partial)
        if (rule) {
            log.trace("Rule matched: from: $stream, partial: $partial -> $rule")
            def evaluatedRules = evaluateInteractionClosure(rule.action.curry(partial))
            if (!evaluatedRules.empty) {
                def innerContext = new Context(evaluatedRules)
                contextStack.push(innerContext)
                log.trace("Entering context#${contextStack.size()}: $innerContext")
            }
        } else {
            log.trace("No rule matched: from: $stream, partial: $partial")
        }
    }

    private evaluateInteractionClosure(Closure interactionClosure) {
        def handler = new InteractionHandler(standardInput)
        callWithDelegate(interactionClosure, handler)
        handler.rules
    }
}
