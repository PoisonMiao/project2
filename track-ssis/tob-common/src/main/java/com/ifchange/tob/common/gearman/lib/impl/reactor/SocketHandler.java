package com.ifchange.tob.common.gearman.lib.impl.reactor;

import java.nio.ByteBuffer;

/**
 * The event handler for a set of sockets. The event handler allows the user to define
 * what actions to take when socket events are triggered.
 *
 * @author isaiah.v
 * @param <X>
 *            Socket Attachment Type
 */
public interface SocketHandler<X> {

	/**
	 * The <i>onAccept</i> event is triggered when a new connection has been established.
	 * This can be due to a server accepting a new connection
	 * or a client being created using the <i>openSocket</i> method in the
	 */
    void onAccept(Socket<X> socket);

	/**
	 * The <i>onDisconnect</i> event is triggered when the connection is lost.
	 *
	 * @param socket
	 *            The socket who has lost the connection
	 */
    void onDisconnect(Socket<X> socket);

	/**
	 * The <i>onRead</i> event is triggered after an attempt to write information
	 * from the TCP buffer into the socket's buffer has been made.<br>
	 *
	 * @param socket
	 *            The socket who received data
	 */
    void onRead(Integer bytes, Socket<X> socket);

	/**
	 * Creates a new {@link ByteBuffer}.<br>
	 * <br>
	 * This will define the byte buffer for newly created sockets. The user
	 * should set the buffer's applicable state variables so it's ready to
	 * read data off the socket
	 *
	 * @return A new buffer
	 */
    ByteBuffer createSocketBuffer();
}
