package environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import agents.Firm;
import agents.Government;
import utilities.Agent;
import utilities.Good;
import utilities.Properties;

/**
 * @author Diogo L. Costa
 *
 */
public class GoodsMarket {

	private Map<Firm, ArrayList<Good>> goodsMapping = new HashMap<Firm, ArrayList<Good>>();

	private Government government;
	private float currentAveragePrice = 0;
	private float previousAveragePrice = 0;
	private boolean marketReady = false;
	private boolean marketClose = false;
	private int readyCount = 0;
	private int closeCount = 0;

	// Number of firms to be omitted from the Households
	private int visibleFirms = (int) Math.round(Properties.FIRMS_NUMBER
			* (1 - Properties.OMISSION_FACTOR));

	/**
	 * GoodsMarket can only be created in {@link SimBuilder}; throughout the
	 * simulation NO AGENT is able to create an instance of this class.
	 * 
	 * @param government
	 */
	GoodsMarket(Government gov) {
		government = gov;
	}

	/**
	 * Searches the market for the cheapest goods while the utility of a good (
	 * {@code initialUtility}) is superior to its cost.<br>
	 * <i>The utility of a good <b>decreases</b></i> according to the number of
	 * goods already bought (BG) and a reduction factor (
	 * {@code Properties.UTILITY_REDUCTION ^ BG}).<br>
	 * In order to provide some <i>stochasticity</i> in the market patterns and
	 * prevent the buyer from always picking the same Firm, every time this
	 * function is called <i>a percentage of the firms is randomly omitted</i>
	 * according to {@link Properties}.{@code OMISSION_FACTOR}.
	 * 
	 * @param wageUtility
	 *            the maximum utility of a good
	 * @return the list of all the goods it was able to retrieve for the given
	 *         {@code initialUtility}
	 */
	public List<Good> getCheapestGood(double maxUtility, double utilityReduction) {

		List<Good> cart = new ArrayList<Good>();

		List<Firm> firmsInMarket = new ArrayList<Firm>(goodsMapping.keySet());

		if (!firmsInMarket.isEmpty()) {

			visibleFirms = (int) Math.ceil(firmsInMarket.size()
					* (1 - Properties.OMISSION_FACTOR));

			List<Firm> visibleFirmsList = new LinkedList<Firm>();

			// Random selection of which firms are to be omitted
			Collections.shuffle(firmsInMarket);
			for (int i = 0; i < visibleFirms; i++) {
				visibleFirmsList.add(firmsInMarket.get(i));
			}

			Collections.sort(visibleFirmsList);
			double cheapest = visibleFirmsList.get(0).getGoodsPrice();

			List<Firm> shuffledFirms = new LinkedList<Firm>();
			ArrayList<Firm> samePriceFirms = new ArrayList<Firm>();

			// Shuffle firms selling goods at the same price to obtain different orders
			for (Firm firm : visibleFirmsList) {

				if (firm.getGoodsPrice() == cheapest) {
					samePriceFirms.add(firm);
				} else {
					cheapest = firm.getGoodsPrice();
					Collections.shuffle(samePriceFirms);
					shuffledFirms.addAll(samePriceFirms);
					samePriceFirms.clear();
					samePriceFirms.add(firm);
				}
			}
			shuffledFirms.addAll(samePriceFirms);

			double goodUtility;

			// Select the goods from the list of firms that are not omitted
			for (Firm firm : shuffledFirms) {

				for (Good good : goodsMapping.get(firm)) {

					// u0 * b^j
					goodUtility = (maxUtility * Math.pow(utilityReduction,
							cart.size()));
					if (goodUtility < good.getMarketPrice()) {
						break;
					}
					cart.add(good);
				}
			}
		}

		return cart;
	}

	/**
	 * Buys each good in the {@code cart} as long as the payment was successful
	 * ({@code Bank.pay(...) == true}).<br>
	 * This function also has the effect of removing firms from the market's
	 * {@code firmsList} if a given firm has no more goods for sale.
	 * 
	 * @param cart
	 *            the list of goods to buy
	 * @param buyer
	 *            the reference of the buyer
	 * @return the amount of goods it was able to buy
	 */
	public List<Good> checkoutCart(List<Good> cart, Agent buyer) {

		List<Good> trash = new ArrayList<Good>();

		for (Good good : cart) {

			Firm seller = good.getFirm();
			if (Bank.pay(buyer, seller, good.getPrice())
					&& government.payVAT(buyer, good.getPrice())) {
				goodsMapping.get(seller).remove(good);
				seller.notifySell(good);
				if (goodsMapping.get(seller).isEmpty())
					goodsMapping.remove(seller);
			} else {
				trash.add(good);
			}
		}
		cart.removeAll(trash);

		return cart;
	}

	/**
	 * The seller ({@code firm}) subscribes to the {@link GoodsMarket} and makes
	 * its goods available for sale by being added to the market's
	 * {@code goodsMapping}.
	 * 
	 * @param firm
	 *            the seller
	 * @param goodsList
	 *            the list of goods to put to sale
	 * @return the number of goods put to sale
	 */
	public int putToSale(Firm firm, ArrayList<Good> goodsList) {
		goodsMapping.put(firm, goodsList);
		return goodsMapping.get(firm).size();
	}

	/**
	 * Clears all the subscriptions made to the market if the {@code firm}
	 * requesting the cleanse is currently subscribed.<br>
	 * 
	 * @param firm
	 */
	public void clearMarket() {
		if (!goodsMapping.isEmpty())
			goodsMapping.clear();
	}

	/**
	 * It allows a Firm Agent to inform the market that its stock load is
	 * complete.<br>
	 * <br>
	 * Once all firms have confirmed their stock in the {@linkplain GoodsMarket}
	 * , the latter will set itself as <i>open for business</i> by changing
	 * {@code marketReady} class field state. All agents listening to this
	 * field, shall immediately visit the market and check out the new stock.
	 */
	public void confirmSale() {
		readyCount++;
		if (readyCount == Properties.getNumberOfOpenFirms()) {
			marketReady = !marketReady;
			readyCount = 0;
		}
	}

	public void confirmVisit() {
		closeCount++;
		if (closeCount == Properties.getNumberOfHouseholds()) {
			marketClose = !marketClose;
			closeCount = 0;
		}
	}

	public double getInflation() {

		float sumPrices = 0;
		float totalGoods = 0;
		for (Entry<Firm, ArrayList<Good>> entry : goodsMapping.entrySet()) {
			for (Good good : entry.getValue()) {
				sumPrices += good.getPrice();
				totalGoods++;
			}
		}

		double inflation = 0;
		currentAveragePrice = totalGoods > 0 ? sumPrices / totalGoods : 0;
		if (previousAveragePrice != 0) {
			inflation = (currentAveragePrice - previousAveragePrice)
					/ previousAveragePrice;
			previousAveragePrice = currentAveragePrice;
			return inflation;
		} else {
			previousAveragePrice = currentAveragePrice;
			return 0;
		}
	}

	public final float getCurrentAvgPrice() {
		return currentAveragePrice;
	}
}