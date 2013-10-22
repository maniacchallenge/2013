package de.fu_berlin.maniac.exception;

/**
 * This exception is used for occuring errors in the app. If the error is fatal, the app needs to be
 * restarted.
 * @author lennart
 *
 */
public class ManiacException extends Exception {
	
	private static final long serialVersionUID = 2769380203796634710L;

	public enum ThrowingComponent {
		
		AuctionManager,
		Bank,
		ConnectionManager,
		Exception,
		General,
		Logging,
		NetworkManager,
		PacketBuilder,
		Packets
		
	}
	
	private ThrowingComponent throwingComponent;
	
	public ManiacException(ThrowingComponent component) {
		
		this(component, "ManiacException");
		this.throwingComponent = component;
		
	}

	public ManiacException(ThrowingComponent component, String message) {
		
		super(message);
		this.throwingComponent = component;
		
	}
	
	public ThrowingComponent getThrowingComponent() {
		
		return throwingComponent;
		
	}
	
	@Override
	public String toString() {
		
		return super.toString() + " from " + throwingComponent.name();
		
	}

}
