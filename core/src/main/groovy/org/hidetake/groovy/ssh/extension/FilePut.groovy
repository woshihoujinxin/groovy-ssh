package org.hidetake.groovy.ssh.extension

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.hidetake.groovy.ssh.core.settings.FileTransferMethod
import org.hidetake.groovy.ssh.operation.SftpException
import org.hidetake.groovy.ssh.session.SessionExtension

import static org.hidetake.groovy.ssh.operation.SftpError.SSH_FX_FAILURE
import static org.hidetake.groovy.ssh.util.Utility.currySelf

/**
 * An extension class to put a file or directory.
 *
 * @author Hidetake Iwata
 */
@Slf4j
trait FilePut extends SessionExtension {

    @ToString
    private static class PutOptions {
        def from
        def text
        def bytes
        def into

        static usage = '''put() accepts following signatures:
put(from: String or File, into: String)  // put a file or directory
put(from: Iterable<File>, into: String) // put files or directories
put(from: InputStream, into: String)     // put a stream into the remote file
put(text: String, into: String)          // put a string into the remote file
put(bytes: byte[], into: String)         // put a byte array into the remote file'''

        static create(HashMap map) {
            try {
                assert map.into, 'into must be given'
                new PutOptions(map)
            } catch (MissingPropertyException e) {
                throw new IllegalArgumentException(usage, e)
            } catch (AssertionError e) {
                throw new IllegalArgumentException(usage, e)
            }
        }
    }

    /**
     * Put file(s) or content to the remote host.
     */
    void put(HashMap map) {
        def options = PutOptions.create(map)
        if (options.from) {
            put(options.from, options.into)
        } else if (options.text) {
            def stream = new ByteArrayInputStream(options.text.toString().bytes)
            put(stream, options.into)
        } else if (options.bytes) {
            def stream = new ByteArrayInputStream(options.bytes as byte[])
            put(stream, options.into)
        } else {
            throw new IllegalArgumentException(PutOptions.usage)
        }
    }

    /**
     * Put a file or directory to the remote host.
     *
     * @param local
     * @param remote
     */
    void put(String local, String remote) {
        assert remote, 'remote path must be given'
        assert local,  'local path must be given'
        put(new File(local), remote)
    }

    /**
     * Put a file or directory to the remote host.
     *
     * @param local
     * @param remote
     */
    void put(File local, String remote) {
        assert remote, 'remote path must be given'
        assert local,  'local file must be given'
        put([local], remote)
    }

    /**
     * Put a collection of a file or directory to the remote host.
     *
     * @param localFiles
     * @param remotePath
     */
    void put(Iterable<File> localFiles, String remotePath) {
        assert remotePath, 'remote path must be given'
        assert localFiles,  'local files must be given'
        if (operationSettings.fileTransfer == FileTransferMethod.sftp) {
            sftpPutRecursive(localFiles, remotePath)
        } else if (operationSettings.fileTransfer == FileTransferMethod.scp) {
            scpPutRecursive(localFiles, remotePath)
        } else {
            throw new IllegalStateException("Unknown file transfer method: ${operationSettings.fileTransfer}")
        }
    }

    /**
     * Put a file to the remote host.
     *
     * @param stream
     * @param remote
     */
    void put(InputStream stream, String remote) {
        assert stream, 'input stream must be given'
        assert remote, 'remote path must be given'
        if (operationSettings.fileTransfer == FileTransferMethod.sftp) {
            sftp {
                putContent(stream, remote)
            }
        } else if (operationSettings.fileTransfer == FileTransferMethod.scp) {
            def helper = new ScpHelper(operations)
            def m = (~'(.*/)(.+?)').matcher(remote)
            if (m.matches()) {
                def bytes = stream.bytes
                def dirname = m.group(1)
                def filename = m.group(2)
                new ByteArrayInputStream(bytes).withStream { byteStream ->
                    helper.createFile(dirname, filename, byteStream, bytes.length)
                }
            } else {
                throw new IllegalArgumentException("Remote path must be an absolute path: $remote")
            }
        } else {
            throw new IllegalStateException("Unknown file transfer method: ${operationSettings.fileTransfer}")
        }
    }


    private void sftpPutRecursive(Iterable<File> baseLocalFiles, String baseRemotePath) {
        sftp {
            currySelf { Closure self, Iterable<File> localFiles, String remotePath ->
                localFiles.findAll { !it.directory }.each { localFile ->
                    putFile(localFile.path, remotePath)
                }
                localFiles.findAll { it.directory }.each { localDir ->
                    log.debug("Entering directory $localDir.path")
                    def remoteDir = "$remotePath/${localDir.name}"
                    try {
                        mkdir(remoteDir)
                    } catch (SftpException e) {
                        if (e.error == SSH_FX_FAILURE) {
                            log.info("Remote directory already exists: ${e.localizedMessage}")
                        } else {
                            throw new RuntimeException(e)
                        }
                    }

                    self.call(self, localDir.listFiles().toList(), remoteDir)
                    log.debug("Leaving directory $localDir.path")
                }
            }.call(baseLocalFiles, baseRemotePath)
        }
    }

    private void scpPutRecursive(Iterable<File> baseLocalFiles, String baseRemotePath) {
        def helper = new ScpHelper(operations)

        currySelf { Closure self, Iterable<File> localFiles, String remotePath ->
            localFiles.findAll { !it.directory }.each { localFile ->
                localFile.withInputStream { stream ->
                    helper.createFile(remotePath, localFile.name, stream, localFile.length())
                }
            }
            localFiles.findAll { it.directory }.each { localDir ->
                log.debug("Entering directory $localDir.path")
                helper.createDirectory(remotePath, localDir.name)
                def remoteDir = "$remotePath/$localDir.name"
                self.call(self, localDir.listFiles().toList(), remoteDir)
                log.debug("Leaving directory $localDir.path")
            }
        }.call(baseLocalFiles, baseRemotePath)
    }

}