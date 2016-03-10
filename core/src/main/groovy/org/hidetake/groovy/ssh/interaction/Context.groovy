package org.hidetake.groovy.ssh.interaction

class Context {
    final List<Rule> rules

    private long lineNumber = 0

    def Context(List<Rule> rules1) {
        rules = rules1
    }

    Rule matchLine(Stream stream, String line) {
        lineNumber++
        rules.find { it.matcher(stream, Event.Line, lineNumber, line) }
    }

    Rule matchPartial(Stream stream, String block) {
        rules.find { it.matcher(stream, Event.Partial, lineNumber, block) }
    }

    @Override
    String toString() {
        "${Context.simpleName}$rules"
    }
}
