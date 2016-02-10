package org.hidetake.groovy.ssh.extension

import groovy.util.logging.Slf4j
import org.hidetake.groovy.ssh.core.settings.LoggingMethod
import org.hidetake.groovy.ssh.core.settings.OperationSettings
import org.hidetake.groovy.ssh.operation.Operations

@Slf4j
class ScpHelper {
    final Operations operations

    def ScpHelper(Operations operations1) {
        operations = operations1
        assert operations
    }

    /**
     * Create a file.
     *
     * @param remoteDir
     * @param remoteFile
     * @param stream should be closed by caller
     * @param size
     */
    void createFile(String remoteDir, String remoteFile, InputStream stream, long size) {
        log.debug("Creating file: $remoteDir/$remoteFile")
        def command = "C0644 $size $remoteFile"
        operations.execute(OperationSettings.DEFAULT + new OperationSettings(
                logging: LoggingMethod.none,
                interaction: {
                    log.debug("Sending SCP command: $command")
                    standardInput << command << '\n'
                    standardInput.flush()

                    log.debug("Sending $size bytes")
                    standardInput << stream

                    log.debug("Sending null")
                    standardInput << [0 as byte]
                    standardInput.flush()
                    standardInput.close()
                }
        ), "scp -t $remoteDir", null)
    }

    /**
     * Create a directory.
     *
     * @param remoteBase
     * @param remoteDir
     * @param stream should be closed by caller
     * @param size
     */
    void createDirectory(String remoteBase, String remoteDir) {
        log.debug("Creating directory: $remoteBase/$remoteDir")
        def command = "D0755 0 $remoteDir"
        operations.execute(OperationSettings.DEFAULT + new OperationSettings(
                logging: LoggingMethod.none,
                interaction: {
                    log.debug("Sending SCP command: $command")
                    standardInput << command << '\n'
                    standardInput.flush()

                    log.debug("Sending E")
                    standardInput << 'E' << '\n'
                    standardInput.flush()

                    log.debug("Sending null")
                    standardInput << [0 as byte]
                    standardInput.flush()
                    standardInput.close()
                }
        ), "scp -t -r $remoteBase", null)
    }

    /**
     * Fetch the file.
     *
     * @param remotePath
     * @param stream
     * @return
     */
    void fetchFile(String remotePath, OutputStream stream) {
        log.debug("Fetching file: $remotePath")
//        operations.execute(OperationSettings.DEFAULT + new OperationSettings(
//                logging: LoggingMethod.none,
//                interaction: {
//                    log.debug("Sending null")
//                    standardInput << [0 as byte]
//
//                    when(nextLine: ~/C0644 \d+ .+/, from: standardOutput) { String line ->
//                        def m = line.split(' ')
//                        def size = m[1] as long
//                        def path = m[2]
//                        log.debug("Found $path of size $size")
//
//                        //FIXME
//                    }
//                }
//        ), "scp -f $remotePath", null)
        throw new UnsupportedOperationException('Not implemented yet')
    }

}