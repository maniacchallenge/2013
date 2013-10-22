package de.fu_berlin.maniac.exception;

/**
 * This exception occurs when you try to send a negative bid.
 * @author maniacchallenge
 *
 */
public class NegativeBidException extends ManiacException {

	private static final long serialVersionUID = 414756540571614673L;

	public NegativeBidException() {
		
		super(ThrowingComponent.Packets, "The bid must not be negative.");
		
	}
	
}
