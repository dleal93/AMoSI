/**
 * 
 */
package agents;

import java.util.ArrayList;
import java.util.List;

import environment.Bank;
import environment.Bank.BankAccount;
import environment.GoodsMarket;
import environment.LaborMarket;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.random.RandomHelper;
import utilities.Agent;
import utilities.Good;
import utilities.JobContract;
import utilities.Properties;

/**
 * This class represents the Household Agent.<br>
 * It reacts to the rest of the economy's stimuli, therefore, unlike the
 * {@linkplain Firm} Agent, it does not have a predefined sequence of behavior.
 * 
 * @author Diogo L. Costa
 *
 */
public class Household implements Agent {

	private GoodsMarket goodsMarket;
	private LaborMarket laborMarket;

	private double education;
	private float productivity;
	private double utilityReduction;

	private boolean employed;
	private int unemployedTime;
	private float reservationWage;
	private float lastWage;
	private JobContract job;
	private BankAccount account;
	private int consumed = 0;

	private final float WAGE_REDUCTION;

	/**
	 * @param money
	 * @param goodsmarket
	 * @param labormarket
	 * @throws InvalidJobContractException
	 */
	public Household(GoodsMarket goodsmarket, LaborMarket labormarket,
			float educationLevel) {
		reservationWage = Properties.INIT_RESWAGE;
		account = Bank.registerClient(this, Properties.HOUSEHOLD_FUNDS);
		job = null;
		employed = false;
		productivity = Properties.PRODUCTIVITY_LEVEL;
		goodsMarket = goodsmarket;
		laborMarket = labormarket;
		utilityReduction = RandomHelper.nextDoubleFromTo(
				Properties.INIT_MIN_UTILITY, Properties.INIT_MAX_UTILITY);
		education = educationLevel;
		unemployedTime = 0;
		lastWage = 0;
		WAGE_REDUCTION = (float) RandomHelper.nextDoubleFromTo(
				Properties.LOWER_WAGE_REDUCTION,
				Properties.UPPER_WAGE_REDUCTION);
	}

	/**
	 * Waits for the {@linkplain GoodsMarket} to receive a new stock from the
	 * {@linkplain Firm} Agents and... //TODO
	 */
	@Watch(watcheeClassName = "environment.LaborMarket", watcheeFieldNames = "marketReady", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void jobApplicationStage() {

		if (!isEmployed())
			laborMarket.joinLaborMarket(this);

		laborMarket.confirmVisit();

	}

	/**
	 * Waits for the {@linkplain GoodsMarket} to receive a new stock from the
	 * {@linkplain Firm} Agents and... //TODO
	 */
	@Watch(watcheeClassName = "environment.GoodsMarket", watcheeFieldNames = "marketReady", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void needsManagementStage() {

		if (!isEmployed()) {

			reservationWage = Math.max(getUnemployedBenefit(), Math
					.max(reservationWage * (1 - WAGE_REDUCTION),
							Properties.MIN_WAGE));

			productivity = productivity
					* (1 - Properties.PRODUCTIVITY_REDUCTION);
			unemployedTime++;
		} else {
			float currentIncome = job.getPayCheck()
					+ Properties.calculateEarnedTaxCredit(job.getPayCheck());
			if (reservationWage < currentIncome)
				reservationWage = currentIncome;

			productivity += Properties.PRODUCTIVITY_INCREASE * education
					/ productivity;
		}

		consumed = 0;
		float maxUtility = account.getBalance();

		List<Good> checkOut = new ArrayList<Good>();

		List<Good> cart = goodsMarket.getCheapestGood(maxUtility,
				utilityReduction);
		checkOut = goodsMarket.checkoutCart(cart, this);

		consumed = checkOut.size();

		goodsMarket.confirmVisit();

	}

	/**
	 * It receives a notification from a Firm Agent to inform the Household that
	 * it has been fired.
	 */
	public void notifyFire() {
		setEmployed(false);
		job = null;
	}

	/**
	 * It receives a notification from a Firm Agent to inform the Household that
	 * it has been hired.
	 * 
	 * @param jobContract
	 *            the {@link JobContract} established between a Firm Agent and
	 *            the Household.
	 */
	public void notifyEmployed(JobContract jobContract) {
		unemployedTime = 0;
		job = jobContract;
		lastWage = job.getPayCheck();
		setEmployed(true);
	}

	/**
	 * @return the employed state
	 */
	public final boolean isEmployed() {
		return employed;
	}

	/**
	 * @param employed
	 *            the employed state to set
	 */
	private final void setEmployed(boolean employed) {
		this.employed = employed;
	}

	/**
	 * @return the current balance of its funds
	 */
	public final float getBalance() {
		return account.getBalance();
	}

	public final float getWage() {
		if (isEmployed())
			return job.getPayCheck();
		else
			return 0;
	}

	public final float getReservationWage() {
		return reservationWage;
	}

	public final int getConsumption() {
		return consumed;
	}

	public final Firm getEmployer() {
		if (isEmployed())
			return job.getEmployer();
		return null;
	}

	public final float getProductivity() {
		return productivity;
	}

	public final double getEducation() {
		return education;
	}

	public final float getLastWage() {
		return lastWage;
	}

	public final int getUnemployedTime() {
		return unemployedTime;
	}

	public final float getETCIncome() {
		return Properties.EARNED_TAX_CREDIT
				* Properties.calculateEarnedTaxCredit(getWage());
	}

	public final float getUnemployedBenefit() {
		if (unemployedTime > Properties.UNEMPLOYED_TIME) {
			return Properties.MIN_BENEFIT * Properties.MIN_WAGE
					* (1 - Properties.IRS_TAX);
		} else if (unemployedTime > 0)
			return getLastWage() * Properties.UNEMPLOYED_BENEFIT_FACTOR;
		else
			return 0;
	}

	public final float getIncome() {
		return getETCIncome() + getUnemployedBenefit()
				+ (getWage() * (1 - Properties.IRS_TAX));
	}
}
