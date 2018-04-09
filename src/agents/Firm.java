/**
 * 
 */
package agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import environment.Bank;
import environment.Bank.BankAccount;
import environment.GoodsMarket;
import environment.LaborMarket;
import environment.StatisticsManager;
import exceptions.FirmStockCountException;
import exceptions.InsufficientFundsException;
import exceptions.InvalidResourcesException;
import exceptions.TargetProductionException;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.random.RandomHelper;
import utilities.Agent;
import utilities.Good;
import utilities.JobContract;
import utilities.Properties;

/**
 * This class represents the Firm Agent.<br>
 * Firms employ {@linkplain Household} Agents in order to produce their goods
 * and then put them to sale in the {@linkplain GoodsMarket}.
 * 
 * @author Diogo L. Costa
 */
public class Firm implements Agent, Comparable<Firm> {

	private int firmID;

	private List<JobContract> contracts = new ArrayList<JobContract>();
	private List<Good> stock = new ArrayList<Good>();

	private GoodsMarket goodsMarket;
	private LaborMarket laborMarket;
	private Government government;

	private double firmProductivity;
	private long previousProduction;
	private long finalProduction;
	private long targetProduction;
	private long targetLabor;

	private BankAccount account;
	private float wageOffer;
	private float averageWage;
	private float paidWages;

	private float goodsPrice;
	private float lowestPrice;
	private int soldGoods;
	private float soldGoodsProfit;

	private long unfilledVacancies;
	private float previousBalance;
	private float annualProfits;

	private boolean status = true;
	private boolean visitedLaborMarket = false;

	private boolean endCycle = false;

	// CONSTANTS
	private final boolean OPEN = true;
	private final boolean CLOSED = false;
	private final int NEEDED_LABOR = 0;
	private final int COST_MARGIN = 1;
	private final int OUTSIDE_MARGIN = 0;
	private final int INSIDE_MARGIN = 1;
	private final float MAX_PRODUCTION_THRESHOLD;
	private final float MIN_PRODUCTION_THRESHOLD;
	private final float PRICE_RESISTANCE;
	private final float WAGE_VARIANCE;

	/**
	 * Each Firm's fields are initialized by means of the initial settings defined
	 * in {@linkplain utilities.Properties}.
	 * 
	 */
	public Firm(GoodsMarket gm, LaborMarket lm, Government gov) {
		firmID = Properties.getID();
		account = Bank.registerClient(this, Properties.INIT_FUNDS);
		goodsPrice = Properties.INIT_GOODS_PRICE;
		lowestPrice = Properties.INIT_GOODS_PRICE;
		wageOffer = Properties.INIT_WAGE_OFFER;
		firmProductivity = Properties.PRODUCTIVITY_LEVEL;
		goodsMarket = gm;
		laborMarket = lm;
		government = gov;
		previousProduction = 0;
		soldGoods = -1;
		unfilledVacancies = 0;
		annualProfits = 0;
		stock = new ArrayList<Good>();
		MAX_PRODUCTION_THRESHOLD = (float) RandomHelper.nextDoubleFromTo(Properties.LOWER_MAX_PRODUCTION_THRESHOLD,
				Properties.UPPER_MAX_PRODUCTION_THRESHOLD);
		MIN_PRODUCTION_THRESHOLD = (float) RandomHelper.nextDoubleFromTo(Properties.LOWER_MIN_PRODUCTION_THRESHOLD,
				Properties.UPPER_MIN_PRODUCTION_THRESHOLD);
		PRICE_RESISTANCE = (float) RandomHelper.nextDoubleFromTo(Properties.LOWER_PRICE_RESISTANCE,
				Properties.UPPER_PRICE_RESISTANCE);
		WAGE_VARIANCE = (float) RandomHelper.nextDoubleFromTo(Properties.LOWER_WAGE_VARIANCE,
				Properties.UPPER_WAGE_VARIANCE);
	}

	/**********************************************************
	 ******************** FIRM SEQUENCE ***********************
	 **********************************************************/

	/**
	 * In this stage the Firms define their strategy as to what their target
	 * production should be and what resources do they need to accomplish it. <br>
	 * It marks the beginning of the economic cycle.
	 */

	@ScheduledMethod(start = 1, interval = 1)
	public void planingStage() {

		if (getStatus() == OPEN) {

			// Before starting new round:
			clearMarkets();

			// Update/Reset last round markers
			setPreviousProduction(getFinalProduction());
			setVisitedLaborMarket(false);
			previousBalance = account.getBalance();

			try {

				assessTargetProduction();

				long[] resources = planResources();

				// cannot afford any resources
				if (resources == null)
					return;
				else
					setTargetLabor(resources[NEEDED_LABOR]);

				laborMarket.confirmOffers();

			} catch (TargetProductionException | InvalidResourcesException ex) {
				System.err.println(ex.getMessage());
				System.exit(-1);
			}
		}
	}

	@Watch(watcheeClassName = "environment.LaborMarket", watcheeFieldNames = "marketClose", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void recruitingStage() {

		if (getStatus() == OPEN) {

			manageResources();

			determineFinalProduction();

			laborMarket.closeLabor();
		}
	}

	@Watch(watcheeClassName = "agents.Government", watcheeFieldNames = "redistributed", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void productionStage() {
		if (getStatus() == OPEN) {
			try {

				payEmployees();

				determineGoodsPrice();

				manageGoods();

			} catch (InsufficientFundsException | FirmStockCountException ex) {

				System.err.println(ex.getMessage());
				System.exit(-1);
			}

			calculateAverageWage();
		}

	}

	@Watch(watcheeClassName = "environment.GoodsMarket", watcheeFieldNames = "marketClose", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void accountingStage() {
		if (getStatus() == OPEN) {
			updateWages();
			annualProfits += getProfits();

			if (Properties.getIteration() % 12 == 0) {
				if (annualProfits > 0)
					government.payProfitTaxes(account, annualProfits);

				annualProfits = 0;
			}

			Properties.nextIteration();
			if (Properties.getNumberOfOpenFirms() == Properties.getIterationLoad())
				endCycle = !endCycle;
		}
	}

	/**********************************************************
	 ******************** FIRM ACTUATORS **********************
	 **********************************************************/

	private void manageResources() {
		long numberVacancies = getTargetLabor() - contracts.size();

		if (numberVacancies > 0) {

			setVisitedLaborMarket(true);

			List<Household> candidates = laborMarket.getApplicants();
			List<Household> accepted = new ArrayList<Household>();

			for (Household household : candidates) {
				if (household.getReservationWage() <= (getWageOffer()
						+ Properties.EARNED_TAX_CREDIT * Properties.calculateEarnedTaxCredit(getWageOffer()))) {
					JobContract jc = new JobContract(wageOffer, this, household);
					contracts.add(jc);
					household.notifyEmployed(jc);
					accepted.add(household);
					numberVacancies--;
				}

				if (numberVacancies == 0)
					break;

			}
			laborMarket.removeApplicants(accepted);
			unfilledVacancies = numberVacancies;

		} else if (numberVacancies < 0) {
			fireEmployees(Math.abs(numberVacancies));

		} else {
			// System.out.println(" !- No need to hire nor fire employees.");
		}

	}

	/**
	 * Defines an estimated value for the {@code targetProduction} based on the
	 * firm's sales.<br>
	 * 
	 * @throws TargetProductionException
	 */
	private void assessTargetProduction() throws TargetProductionException {

		long newProduction = 0;

		if (Properties.getIteration() > 1) {

			// the firm sold more than x% (production_threshold) of everything
			// it owned last round
			if (getSoldGoods() >= (getSoldGoods() + stock.size()) * MAX_PRODUCTION_THRESHOLD) {

				newProduction = (long) Math.ceil(getPreviousProduction() * (1 + Properties.WILL_TO_PRODUCE)); // increase
																												// estimate
				// production

			} else if (getSoldGoods() <= (getSoldGoods() + stock.size()) * MIN_PRODUCTION_THRESHOLD) {

				newProduction = (long) Math.floor(getPreviousProduction() * (1 - Properties.REDUCE_PRODUCTION));

			} else
				newProduction = getPreviousProduction();

			setTargetProduction(newProduction);

			if (getTargetProduction() < 0) {
				throw new TargetProductionException(
						"@assessTargetProduction: Target production cannot be lower than zero: TP = "
								+ getTargetProduction());
			}

		} else {
			// set initial target production
			setTargetProduction(Properties.INIT_PRODUCTION);
		}
	}

	/**
	 * Calculates needed resources to achieve the Target Production estimated by
	 * {@link #assessTargetProduction()}.<br>
	 * <br>
	 * Uses two functions to calculate resources increase or reduction,
	 * respectively: {@link #calcOptimalResources()} and
	 * {@link #calcResourcesReduction()}.
	 *
	 * @return an array [{@code resources}] with 2 positions (0-needed labor, 1-cost
	 *         margin) OR null if the firm has reached bankruptcy
	 * @throws TargetProductionException
	 * @throws InvalidResourcesException
	 */
	private long[] planResources() throws TargetProductionException, InvalidResourcesException {

		// Unsustainable firm -> bankruptcy
		if (account.getBalance() == 0) {
			fireEmployees();
			setStatus(CLOSED);
			Properties.communicateClosure();
			laborMarket.communicateClosure();
			return null;
		}

		long[] resources = new long[3];

		// Y = P * L
		long possibleProduction = Math.round(firmProductivity * getEmployees().size());

		// insufficient resources
		if (possibleProduction < getTargetProduction()) {

			resources = calcOptimalResources();

			// the cost exceeds the funds, lower the target (while it can be
			// lowered) and recalculate resources
			while (resources[COST_MARGIN] == OUTSIDE_MARGIN && getTargetProduction() > 0) {
				setTargetProduction(getTargetProduction() - 1);
				resources = calcOptimalResources();
			}

		} else // excessive resources
		if (possibleProduction > getTargetProduction()) {

			resources = calcResourcesReduction();

			while (resources[COST_MARGIN] == OUTSIDE_MARGIN && getTargetProduction() > 0) {
				setTargetProduction(getTargetProduction() - 1);
				resources = calcResourcesReduction();
			}
		} else {// resources cover the target production

			double costs = calculateCosts();

			// the cost is no longer bearable
			if (costs > account.getBalance()) {
				resources[COST_MARGIN] = OUTSIDE_MARGIN;

				while (resources[COST_MARGIN] == OUTSIDE_MARGIN && getTargetProduction() > 0) {
					setTargetProduction(getTargetProduction() - 1);
					resources = calcResourcesReduction();
				}
			} else {
				resources[NEEDED_LABOR] = getEmployees().size();
				resources[COST_MARGIN] = INSIDE_MARGIN;
			}
		}

		if (getTargetProduction() == 0) {
			fireEmployees();
			float minOffer = StatisticsManager.getLowesReservationWage();
			if (minOffer < account.getBalance())
				setWageOffer(minOffer);
			else
				setWageOffer(account.getBalance());
			setGoodsPrice(
					Math.max((getWageOffer() / Properties.INIT_PRODUCTION) * 0.9f, StatisticsManager.getLowestPrice()));
			setTargetProduction(Properties.INIT_PRODUCTION);
			resources[NEEDED_LABOR] = 1;
			resources[COST_MARGIN] = INSIDE_MARGIN;
		}

		if (getTargetProduction() <= 0) {
			throw new TargetProductionException(
					"@planResources: Target production cannot be lower nor equal to zero: TP = "
							+ getTargetProduction());
		} else if (resources[NEEDED_LABOR] <= 0 || resources[COST_MARGIN] == OUTSIDE_MARGIN)
			throw new InvalidResourcesException("@planResources: Needed Labor cannot be lower nor equal to zero: NL="
					+ resources[NEEDED_LABOR] + ", INSIDE_MARGIN=" + (resources[COST_MARGIN] == OUTSIDE_MARGIN));

		return resources;

	}

	/**
	 * Verifies if the resources for the estimated target production were achieved.
	 * If not, it calculates the real target production.
	 */
	private void determineFinalProduction() {
		updateFirmProductivity();
		setFinalProduction(Math.round(getNumberOfEmployees() * firmProductivity));

		if (getFinalProduction() == 0)
			setWageOffer(getWageOffer() * (1 + WAGE_VARIANCE));
	}

	private void updateFirmProductivity() {
		double sumProd = 0;

		if (contracts.isEmpty()) {
			firmProductivity = 0;
			return;
		}

		for (JobContract jobContract : contracts)
			sumProd += jobContract.getEmployee().getProductivity();

		firmProductivity = Math.round(sumProd / contracts.size());

	}

	/**
	 * Performs the payment of the contracted paycheck in the
	 * {@linkplain JobContract} of each employee.
	 * 
	 * @throws InsufficientFundsException
	 */
	private void payEmployees() throws InsufficientFundsException {
		paidWages = 0;

		for (JobContract jobContract : contracts) {

			Household employee = jobContract.getEmployee();

			float payment = government.payIncomeTaxes(account, jobContract.getPayCheck());

			paidWages += jobContract.getPayCheck();

			if (!Bank.transfer(account, employee, payment))
				throw new InsufficientFundsException(
						"@payEmployees: Labor Cost cannot be higher than available funds: LC = "
								+ jobContract.getPayCheck() + ", Funds = " + account.getBalance() + ", Labor="
								+ getNumberOfEmployees() + ", Wage=" + getWageOffer() + ", avgWage="
								+ getAverageWage());

		}
	}

	/**
	 * Increases or reduces the {@code goodsPrice} based on the amount of sold
	 * goods. <br>
	 * 
	 * @throws UnprofitablePriceSettingException
	 */
	private void determineGoodsPrice() {

		float newPrice = getGoodsPrice();
		float unitaryCosts = (getFinalProduction() != 0
				? ((getNumberOfEmployees() * calculateAverageWage()) / getFinalProduction())
				: 0);

		Float priceVariance = PRICE_RESISTANCE;

		// all goods sold last round, increase prices
		if (getSoldGoods() >= (getSoldGoods() + stock.size()) * Properties.MAX_PRICE_THRESHOLD) {

			// Costs may increase faster than the price, we need to make sure
			// the price always overcomes the cost
			if (getGoodsPrice() < unitaryCosts)
				newPrice = unitaryCosts * (1 + priceVariance);
			else
				newPrice = getGoodsPrice() * (1 + priceVariance);

		} else if (Properties.getIteration() > 1
				&& getSoldGoods() < (getSoldGoods() + stock.size()) * Properties.MIN_PRICE_THRESHOLD) {

			float checkProfitMargin = getGoodsPrice() * (1 - priceVariance);

			// reduce only if there is still a profit margin
			if (checkProfitMargin >= unitaryCosts) {

				newPrice = checkProfitMargin;

			} else {

				// force the price to be at least as much as the firm's costs
				if (getGoodsPrice() <= unitaryCosts) {
					newPrice = unitaryCosts;
				}
			}
		}

		setGoodsPrice((float) newPrice);

		lowestPrice = getGoodsPrice();
		List<Good> trash = new ArrayList<Good>();

		Collections.sort(stock, new Comparator<Good>() {

			@Override
			public int compare(Good arg0, Good arg1) {
				if (arg0.getPrice() < arg1.getPrice())
					return -1;
				else if (arg0.getPrice() > arg1.getPrice())
					return 1;
				return 0;
			}

		});

		// DISCOUNT SALES
		int i = 0;
		for (Good good : stock) {
			i++;
			// Only allow for a stock up to 110 goods
			if (i > 110) {
				trash.add(good);
				continue;
			}
			if (good.getPrice() < lowestPrice)
				lowestPrice = good.getPrice();

			float price = good.getPrice() * Properties.SALES_DISCOUNT;
			if (Properties.getIteration() % 12 == 0) {
				good.setPrice(price);
				if (price < lowestPrice)
					lowestPrice = price;
			}

		}

		stock.removeAll(trash);

	}

	/**
	 * Puts as many goods in the {@link GoodsMarket} as those defined by the
	 * {@code targetProduction}.
	 * 
	 * @throws FirmStockCountException
	 */
	private void manageGoods() throws FirmStockCountException {

		setSoldGoods(0);
		soldGoodsProfit = 0;
		int StockCount = stock.size();

		for (int i = 0; i < getFinalProduction(); i++) {
			Good good = new Good(this, getGoodsPrice());
			stock.add(good);
		}

		int response = 0;
		if (!stock.isEmpty())
			response = goodsMarket.putToSale(this, (ArrayList<Good>) stock);

		if (getFinalProduction() != response - StockCount) {
			throw new FirmStockCountException(
					"@manageGoods: The Firm's Target Production does not match its stock in the Goods Market: TargetProduction = "
							+ getFinalProduction() + ", GoodsMarket = " + (response - StockCount));
		}

		goodsMarket.confirmSale();

	}

	/**
	 * Updates employees' wage based on the labor's search/demand. <br>
	 * <br>
	 * <i>Decrease Wages</i> - if the employer was able to obtain the labor it aimed
	 * for.<br>
	 * <i>Increase Wages</i> - if the employer was <i>not</i> able to obtain the
	 * labor it aimed for; <b>or</b> if during the previous round employees quit
	 * their job and the employer could not fill in the opened vacancies.
	 */
	private void updateWages() {

		// went to the labor market
		if (isVisitedLaborMarket()) {

			// and achieved needed labor
			if (getTargetLabor() == getNumberOfEmployees()) {
				setWageOffer(Math.max(Properties.MIN_WAGE, getWageOffer() * (1 - WAGE_VARIANCE)));

			} else {
				setWageOffer(getWageOffer() * (1 + WAGE_VARIANCE));
			}
		}

		if (StatisticsManager.getEmploymentRate() >= Properties.EMPLOYMENT_PROSPERITY) {
			setWageOffer(getWageOffer() * (1 + Properties.PROSPERITY_BONUS));
			for (JobContract jobContract : contracts) {
				jobContract.setPayCheck(getWageOffer());
			}
		}

	}

	/**********************************************************
	 ************* ALGORITHMS AND AUX FUNCTIONS ***************
	 **********************************************************/

	/**
	 * Calculates the {@code neededLabor} to achieve the {@code targetProduction}.
	 * 
	 * @return an array with 2 positions: 0 - {@code neededLabor}, 1 -
	 *         {@code costExceeded}<br>
	 *         If the needed resources are not bearable by the firm, then
	 *         {@code costExceeded=true}, otherwise {@code =false}.
	 */
	private long[] calcOptimalResources() {

		// L = Y / P
		long needed_labor = (long) Math.ceil(getTargetProduction() / firmProductivity);

		double cost = calculateCosts() + ((needed_labor - getNumberOfEmployees()) * getWageOffer());

		int costExceeded = 0;
		if (cost > account.getBalance()) {
			costExceeded = OUTSIDE_MARGIN;
		} else
			costExceeded = INSIDE_MARGIN;

		return new long[] { needed_labor, costExceeded };
	}

	/**
	 * Calculates the {@code neededLabor} to achieve the
	 * {@code targetProduction}.<br>
	 * 
	 * @return an array with 2 positions: 0 - {@code neededLabor}, 1 -
	 *         {@code costExceeded}<br>
	 *         If the needed resources are not bearable by the firm, then
	 *         {@code costExceeded=true}, otherwise {@code =false}.
	 */
	private long[] calcResourcesReduction() {

		long needed_labor = (long) Math.ceil(getTargetProduction() / firmProductivity);

		double cost = predictCosts(contracts.size() - needed_labor);

		int costExceeded = 0;
		if (cost > account.getBalance()) {
			costExceeded = OUTSIDE_MARGIN;
		} else
			costExceeded = INSIDE_MARGIN;

		return new long[] { needed_labor, costExceeded };
	}

	/**
	 * Fires all the employees from this firm.
	 */
	private void fireEmployees() {

		for (JobContract jc : contracts) {
			jc.getEmployee().notifyFire();
		}

		contracts.clear();
	}

	/**
	 * Fires a specific number of employees from the firm.
	 * 
	 * @param excess
	 *            the number of employees to be fired.
	 */
	private void fireEmployees(long excess) {

		Collections.sort(contracts, new Comparator<JobContract>() {
			@Override
			public int compare(JobContract o1, JobContract o2) {
				if (o1.getPayCheck() > o2.getPayCheck())
					return -1;
				else if (o1.getPayCheck() < o2.getPayCheck())
					return 1;
				else
					return 0;
			}
		});
		for (int i = 0; i < excess; i++) {
			Household hh = contracts.get(0).getEmployee();
			hh.notifyFire();
			contracts.remove(0);
		}
	}

	public double predictCosts(long reduction) {

		Collections.sort(contracts, new Comparator<JobContract>() {
			@Override
			public int compare(JobContract o1, JobContract o2) {
				if (o1.getPayCheck() > o2.getPayCheck())
					return -1;
				else if (o1.getPayCheck() < o2.getPayCheck())
					return 1;
				else
					return 0;
			}
		});

		double cost = 0;
		int employeesNumber = getNumberOfEmployees();
		for (int i = (int) reduction; i < employeesNumber; i++)
			cost += contracts.get(i).getPayCheck();

		return cost;

	}

	/**
	 * Deletes all goods that the firm put to sale in the {@linkplain GoodsMarket}.
	 * <br>
	 * Deletes all job offers that the firm published in the
	 * {@linkplain LaborMarket}
	 */
	private void clearMarkets() {
		goodsMarket.clearMarket();
		laborMarket.clearMarket();
	}

	/**
	 * Receive a notification from the {@linkplain GoodsMarket} to inform the Firm
	 * that the given {@code good} has been sold.
	 * 
	 * @param good
	 *            the sold good.
	 */
	public void notifySell(Good good) {
		setSoldGoods(soldGoods + 1);
		soldGoodsProfit += good.getPrice();
		stock.remove(good);
	}

	public void notifyRescission(JobContract contract) {
		contracts.remove(contract);
		contract.getEmployee().notifyFire();
		if (((getNumberOfEmployees() * calculateAverageWage()) + getWageOffer()) <= account.getBalance()) {
			manageResources();
		}
	}

	/**********************************************************
	 ****************** GETTERS AND SETTERS *******************
	 **********************************************************/

	/**
	 * @return the {@code targetProduction} <u><i>of the previous round</i></u>.
	 */
	public final long getPreviousProduction() {
		return previousProduction;
	}

	/**
	 * @return the amount of money the firm currently has.
	 * 
	 */
	public final double getBalance() {
		return account.getBalance();
	}

	/**
	 * @return the targetProduction for the current round.
	 */
	public final long getFinalProduction() {
		return finalProduction;
	}

	/**
	 * @return the current number of the firm's employees.
	 */
	public final int getNumberOfEmployees() {
		return contracts.size();
	}

	/**
	 * @return the wage
	 */
	public final float getWageOffer() {
		return wageOffer;
	}

	/**
	 * @return the price at which the Firm is selling its goods.
	 */
	public final float getGoodsPrice() {
		return goodsPrice;
	}

	/**
	 * @return the goods the firm sold in the previous round.
	 */
	public final int getSoldGoods() {
		return soldGoods;
	}

	/**
	 * @param lastProduction
	 *            the lastProduction to set
	 */
	private final void setPreviousProduction(long lastProduction) {
		this.previousProduction = lastProduction;
	}

	/**
	 * @param targetProduction
	 *            the targetProduction to set
	 */
	private final void setFinalProduction(long targetProduction) {
		this.finalProduction = targetProduction;
	}

	/**
	 * @return the employees
	 */
	private final List<JobContract> getEmployees() {
		return contracts;
	}

	/**
	 * {@code True} if the firm is OPEN, {@code False} otherwise.
	 * 
	 * @return the status
	 */
	public final boolean getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	private final void setStatus(boolean status) {
		this.status = status;
	}

	/**
	 * @return the average wage of the firm's current employees.
	 */
	private float calculateAverageWage() {
		if (contracts.size() > 0) {
			float sum = 0;
			for (JobContract jc : contracts)
				sum += jc.getPayCheck();
			averageWage = sum / contracts.size();
			return averageWage;
		} else
			return 0;
	}

	private double calculateCosts() {
		if (contracts.size() > 0) {
			double sum = 0;
			for (JobContract jc : contracts)
				sum += jc.getPayCheck();

			return sum;
		} else
			return 0;
	}

	/**
	 * @param goodsPrice
	 *            the goodsPrice to set
	 */
	private final void setGoodsPrice(float goodsPrice) {
		this.goodsPrice = goodsPrice;
	}

	/**
	 * @param soldGoods
	 *            the soldGoods to set
	 */
	private final void setSoldGoods(int soldGoods) {
		this.soldGoods = soldGoods;
	}

	/**
	 * @return the firmID
	 */
	private final int getFirmID() {
		return firmID;
	}

	@Override
	public int compareTo(Firm firm) {

		if (getLowestPrice() > firm.getLowestPrice())
			return 1;
		else if (getLowestPrice() < firm.getLowestPrice())
			return -1;
		else
			return firmID - firm.getFirmID();
	}

	/**
	 * @param visitedLaborMarket
	 *            the visitedLaborMarket to set
	 */
	private final void setVisitedLaborMarket(boolean visitedLaborMarket) {
		this.visitedLaborMarket = visitedLaborMarket;
	}

	/**
	 * @return the visitedLaborMarket
	 */
	private final boolean isVisitedLaborMarket() {
		return visitedLaborMarket;
	}

	/**
	 * @return the targetLabor
	 */
	private final long getTargetLabor() {
		return targetLabor;
	}

	/**
	 * @param targetLabor
	 *            the targetLabor to set
	 */
	private final void setTargetLabor(long targetLabor) {
		this.targetLabor = targetLabor;
	}

	/**
	 * @param wage
	 *            the wage to set
	 */
	private final void setWageOffer(float wage) {
		this.wageOffer = wage;
	}

	public final float getProfits() {
		return account.getBalance() - previousBalance;
	}

	/**
	 * @param averageWage
	 *            the averageWage to set
	 */

	public final float getAverageWage() {
		return averageWage;
	}

	/**
	 * @return the targetProduction
	 */
	private final long getTargetProduction() {
		return targetProduction;
	}

	/**
	 * @param targetProduction
	 *            the targetProduction to set
	 */
	private final void setTargetProduction(long targetProduction) {
		this.targetProduction = targetProduction;
	}

	public final float getLowestPrice() {
		return lowestPrice;
	}

	public final int getStockSize() {
		return stock.size();
	}

	public final long getUnfilledVacancies() {
		return unfilledVacancies;
	}

	public final float getPaidWages() {
		return paidWages;
	}

	public final float getSoldGoodsProfits() {
		return soldGoodsProfit;
	}
}