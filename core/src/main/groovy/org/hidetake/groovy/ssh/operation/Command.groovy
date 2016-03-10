package org.hidetake.groovy.ssh.operation

import com.jcraft.jsch.ChannelExec
import groovy.util.logging.Slf4j
import org.hidetake.groovy.ssh.connection.Connection
import org.hidetake.groovy.ssh.core.settings.LoggingMethod
import org.hidetake.groovy.ssh.interaction.Stream
import org.hidetake.groovy.ssh.interaction.Streams

/**
 * A command operation.
 *
 * @author Hidetake Iwata
 */
@Slf4j
class Command implements Operation {
    private final Connection connection
    private final ChannelExec channel
    private final String commandLine
    private final Streams streams

    def Command(Connection connection1, CommandSettings settings, String commandLine1) {
        connection = connection1
        commandLine = commandLine1

        channel = connection.createExecutionChannel()
        channel.command = commandLine
        channel.pty = settings.pty
        channel.agentForwarding = settings.agentForwarding

        streams = new Streams(channel.outputStream, channel.inputStream, channel.errStream, settings.encoding)
        if (settings.outputStream) {
            streams.pipe(Stream.StandardOutput, settings.outputStream)
        }
        if (settings.errorStream) {
            streams.pipe(Stream.StandardError, settings.errorStream)
        }
        if (settings.logging == LoggingMethod.slf4j) {
            streams.addInteraction {
                when(line: _, from: standardOutput) {
                    log.info("$connection.remote.name#$channel.id|$it")
                }
                when(line: _, from: standardError) {
                    log.error("$connection.remote.name#$channel.id|$it")
                }
            }
        } else if (settings.logging == LoggingMethod.stdout) {
            streams.addInteraction {
                when(line: _, from: standardOutput) {
                    System.out.println("$connection.remote.name#$channel.id|$it")
                }
                when(line: _, from: standardError) {
                    System.err.println("$connection.remote.name#$channel.id|$it")
                }
            }
        }
        if (settings.interaction) {
            streams.addInteraction(settings.interaction)
        }
    }

    @Override
    int startSync() {
        channel.connect()
        log.info("Started command $connection.remote.name#$channel.id: $commandLine")
        try {
            streams.start()
            streams.waitForEndOfStream()
            while (!channel.closed) {
                sleep(100)
            }
            int exitStatus = channel.exitStatus
            if (exitStatus == 0) {
                log.info("Success command $connection.remote.name#$channel.id: $commandLine")
            } else {
                log.error("Failed command $connection.remote.name#$channel.id with status $exitStatus: $commandLine")
            }
            exitStatus
        } finally {
            channel.disconnect()
        }
    }

    @Override
    void startAsync(Closure closure) {
        connection.whenClosed(channel) {
            streams.waitForEndOfStream()
            int exitStatus = channel.exitStatus
            if (exitStatus == 0) {
                log.info("Success command $connection.remote.name#$channel.id: $commandLine")
            } else {
                log.error("Failed command $connection.remote.name#$channel.id with status $exitStatus: $commandLine")
            }
            closure.call(exitStatus)
        }
        channel.connect()
        streams.start()
        log.info("Started command $connection.remote.name#$channel.id: $commandLine")
    }

    @Override
    void onEachLineOfStandardOutput(Closure closure) {
        // TODO: make it better
        streams.addInteraction {
            when(line: _, from: Stream.StandardOutput) {
                closure(it)
            }
        }
    }
}
