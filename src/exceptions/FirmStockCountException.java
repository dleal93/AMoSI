/**
 * 
 */
package exceptions;

/**
 * @author Diogo L. Costa
 *
 */
public class FirmStockCountException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8659231366655743325L;

	/**
	 * 
	 */
	public FirmStockCountException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public FirmStockCountException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public FirmStockCountException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FirmStockCountException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public FirmStockCountException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
