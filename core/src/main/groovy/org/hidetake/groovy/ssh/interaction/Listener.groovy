package org.hidetake.groovy.ssh.interaction

import groovy.util.logging.Slf4j

@Slf4j
class Listener {
    private final List<Interaction> interactions = []

    void matchLine(Stream stream, String line) {
        log.debug("Finding match: from: $stream, line: $line")
        interactions*.processLine(stream, line)
    }

    void matchPartial(Stream stream, String partial) {
        log.debug("Finding match: from: $stream, partial: $partial")
        interactions*.processPartial(stream, partial).any()
    }

    void addInteraction(Interaction interaction) {
        interactions.add(interaction)
    }
}
