package org.hidetake.groovy.ssh.interaction

import groovy.transform.ToString

@ToString
class Context {
    final List<Rule> rules

    private long lineNumber = 0

    def Context(List<Rule> rules1) {
        rules = rules1
        assert rules
    }

    Rule matchLine(Stream stream, String line) {
        lineNumber++
        rules.find { it.matcher(stream, Event.Line, lineNumber, line) }
    }

    Rule matchBlock(Stream stream, String block) {
        rules.find { it.matcher(stream, Event.Partial, lineNumber, block) }
    }
}
