/**
 * 
 */
package utilities;

import agents.Firm;

/**
 * @author Diogo L. Costa
 *
 */
public class Good {

	private Firm firm;
	private float price;

	public Good(Firm firm, float price) {
		this.firm = firm;
		this.price = price;

	}

	public Firm getFirm() {
		return firm;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float pc) {
		price = pc;
	}

	public float getMarketPrice() {
		return price * (1 + Properties.IVA_TAX);
	}

}
