package org.hidetake.groovy.ssh.operation

import com.jcraft.jsch.ChannelShell
import groovy.util.logging.Slf4j
import org.hidetake.groovy.ssh.connection.Connection
import org.hidetake.groovy.ssh.core.settings.LoggingMethod
import org.hidetake.groovy.ssh.interaction.Stream
import org.hidetake.groovy.ssh.interaction.Streams

/**
 * A shell operation.
 *
 * @author Hidetake Iwata
 */
@Slf4j
class Shell implements Operation {
    private final Connection connection
    private final ChannelShell channel
    private final Streams streams

    def Shell(Connection connection1, ShellSettings settings) {
        connection = connection1
        channel = connection.createShellChannel()
        channel.agentForwarding = settings.agentForwarding

        streams = new Streams(channel.outputStream, channel.inputStream, settings.encoding)
        if (settings.outputStream) {
            streams.pipe(Stream.StandardOutput, settings.outputStream)
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
        try {
            log.info("Started shell $connection.remote.name#$channel.id")
            streams.start()
            streams.waitForEndOfStream()
            while (!channel.closed) {
                sleep(100)
            }
            int exitStatus = channel.exitStatus
            if (exitStatus == 0) {
                log.info("Success shell $connection.remote.name#$channel.id")
            } else {
                log.error("Failed shell $connection.remote.name#$channel.id with status $exitStatus")
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
                log.info("Success shell $connection.remote.name#$channel.id")
            } else {
                log.error("Failed shell $connection.remote.name#$channel.id with status $exitStatus")
            }
            closure.call(exitStatus)
        }
        channel.connect()
        streams.start()
        log.info("Started shell $connection.remote.name#$channel.id")
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
