package org.hidetake.groovy.ssh.core.settings

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.hidetake.groovy.ssh.extension.SftpGet
import org.hidetake.groovy.ssh.extension.SftpPut
import org.hidetake.groovy.ssh.extension.SudoExecution

import static org.hidetake.groovy.ssh.util.Utility.findNotNull

@EqualsAndHashCode
@ToString
class OperationSettings implements Settings<OperationSettings> {
    /**
     * Dry-run flag.
     * If <code>true</code>, performs no action.
     */
    Boolean dryRun

    /**
     * Ignores the exit status of the command or shell.
     */
    Boolean ignoreError

    /**
     * PTY allocation flag.
     * If <code>true</code>, PTY will be allocated on command execution.
     */
    Boolean pty

    /**
     * A logging method of the remote command or shell.
     */
    LoggingMethod logging

    /**
     * An output stream to forward the standard output.
     */
    OutputStream outputStream

    /**
     * An output stream to forward the standard error.
     */
    OutputStream errorStream

    /**
     * Encoding of input and output stream.
     */
    String encoding

    /**
     * Stream interaction.
     * @see org.hidetake.groovy.ssh.interaction.InteractionHandler
     */
    Closure interaction

    /**
     * Extension classes to mixin to {@link org.hidetake.groovy.ssh.session.SessionHandler}.
     */
    List<Class> extensions = []

    static final DEFAULT = new OperationSettings(
            dryRun: false,
            ignoreError: false,
            pty: false,
            logging: LoggingMethod.slf4j,
            encoding: 'UTF-8',
            extensions: [SudoExecution, SftpGet, SftpPut]
    )

    OperationSettings plus(OperationSettings right) {
        new OperationSettings(
                dryRun:         findNotNull(right.dryRun, dryRun),
                ignoreError: findNotNull(right.ignoreError, ignoreError),
                pty:            findNotNull(right.pty, pty),
                logging:        findNotNull(right.logging, logging),
                encoding:       findNotNull(right.encoding, encoding),
                interaction:    findNotNull(right.interaction, interaction),
                outputStream:   findNotNull(right.outputStream, outputStream),
                errorStream:    findNotNull(right.errorStream, errorStream),
                extensions:     extensions + right.extensions
        )
    }
}