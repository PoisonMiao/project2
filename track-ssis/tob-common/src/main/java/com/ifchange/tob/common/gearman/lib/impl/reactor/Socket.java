package com.ifchange.tob.common.gearman.lib.impl.reactor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * An asynchronous socket.
 *
 * @author isaiah.v
 */
public interface Socket<X> {

	/**
	 * Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
	 * @param on
	 * 		<code>true</code> to enable TCP_NODELAY, <code>false</code> to disable.
	 * @throws SocketException
	 * 		if there is an error in the underlying protocol, such as a TCP error.
	 */
    void setTcpNoDelay(boolean on) throws IOException;

	/**
	 * Enable/disable SO_KEEPALIVE.
	 * @param on
	 * 		whether or not to have socket keep alive turned on.
	 * @throws SocketException
	 * 		if there is an error in the underlying protocol, such as a TCP error.
	 * @throws IOException
	 */
    void setKeepAlive(boolean on) throws IOException;

	/**
	 * Tests if TCP_NODELAY is enabled.
	 * @return
	 * 		a <code>boolean</code> indicating whether or not TCP_NODELAY is enabled.
	 * @throws SocketException
	 * 		if there is an error in the underlying protocol, such as a TCP error.
	 */
    boolean getTcpNoDelay() throws IOException;

	/**
	 * Closes this socket.
	 */
    void close();

	/**
	 * Returns the address to which the socket is connected.
	 *
	 * @return the remote IP address to which this socket is connected, or null
	 *         if the socket is not connected.
	 * @throws IOException
	 */
    InetAddress getInetAddress();

	/**
	 * Gets the local address to which the socket is bound.
	 *
	 * @return the local address to which the socket is bound or
	 *         InetAddress.anyLocalAddress() if the socket is not bound yet.
	 */
    InetAddress getLocalAddress();

	/**
	 * Returns the local port to which this socket is bound.
	 *
	 * @return the local port number to which this socket is bound or -1 if the
	 *         socket is not bound yet.
	 */
    int getLocalPort();

	/**
	 * Returns the remote port to which this socket is connected.
	 *
	 * @return the remote port number to which this socket is connected, or 0 if
	 *         the socket is not connected yet.
	 */
    int getPort();

	/**
	 * Returns the closed state of the socket.
	 *
	 * @return true if the socket has been closed
	 */
    boolean isClosed();

	/**
	 * Tests if SO_KEEPALIVE is enabled.
	 *
	 * @return a <code>boolean</code> indicating whether or not SO_KEEPALIVE is
	 *         enabled.
	 * @throws SocketException
	 *             if there is an error in the underlying protocol, such as a
	 *             TCP error.
	 */
    boolean getKeepAlive() throws IOException;

	/**
	 * Returns the address of the endpoint this socket is bound to, or
	 * <code>null</code> if it is not bound yet.
	 *
	 * @return a <code>SocketAddress</code> representing the local endpoint of
	 *         this socket, or <code>null</code> if it is not bound yet.
	 * @throws IOException
	 */
    SocketAddress getLocalSocketAddress();

	/**
	 * Returns the address of the endpoint this socket is connected to, or
	 * <code>null</code> if it is unconnected.
	 *
	 * @return
	 * @throws IOException
	 */
    SocketAddress getRemoteSocketAddress();

	/**
	 * Writes data to the socket asynchronously.<br>
	 * <br>
	 * The write operation may write up to r bytes to the channel, where r is the number of bytes remaining in the buffer, that is, data.remaining() at the time that the write is attempted.<br>
	 * <br>
	 * Buffers are not safe for use by multiple concurrent threads so care should be taken to not access the buffer until the operation has completed.
	 */
    <A> void write(ByteBuffer data, A att, CompletionHandler<ByteBuffer, A> callback);

	/**
	 * Returns the ByteBuffer for this socket.
	 *
	 * @return The ByteBuffer for this socket
	 */
    ByteBuffer getByteBuffer();

	/**
	 * Sets the ByteBuffer for this
	 *
	 * @param buffer
	 */
    void setByteBuffer(ByteBuffer buffer);

	/**
	 * Gets the socket's attachment
	 *
	 * @return The attachment
	 */
    X getAttachment();

	/**
	 * Sets the socket's attachment
	 *
	 * @param att
	 *            The attachment
	 */
    void setAttachment(X att);
}
