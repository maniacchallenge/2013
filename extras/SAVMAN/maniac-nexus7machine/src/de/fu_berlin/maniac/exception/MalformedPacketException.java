package de.fu_berlin.maniac.exception;

/**
 * This exception occurs, when the packet type is neither of A, D, B, W or C or if the data in the packet
 * is useless.
 * @author maniacchallenge
 *
 */
public class MalformedPacketException extends ManiacException {

	private static final long serialVersionUID = 1914687258486324628L;

	public MalformedPacketException() {
		
		super(ThrowingComponent.PacketBuilder, "The packet builder was unable to identify the received packet.");
		
	}
	
}
