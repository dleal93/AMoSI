/**
 * 
 */
package exceptions;

/**
 * @author Diogo L. Costa
 *
 */
public class InsufficientFundsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5480845173942900263L;

	/**
	 * 
	 */
	public InsufficientFundsException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public InsufficientFundsException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public InsufficientFundsException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InsufficientFundsException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public InsufficientFundsException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
