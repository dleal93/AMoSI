/**
 * 
 */
package environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import agents.Firm;
import agents.Government;
import agents.Household;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import utilities.Properties;

/**
 * @author Diogo L. Costa
 *
 */
public class StatisticsManager {

	private static List<Firm> firms = new ArrayList<Firm>();
	private static List<Household> households = new ArrayList<Household>();
	private static GoodsMarket goodsMarket;
	private static Government government;

	private float giniIndexIncome;
	private float giniIndexConsumption;
	private float giniIndexFunds;
	private float giniIndexWage;
	private float firmFunds;
	private float householdFunds;
	private int householdConsumption;
	private int totalStock;
	private int totalSoldGoods;
	private float avgPracticedPrice;
	private float GDP;
	private int productionOutput;
	private float averageWage;
	private double inflation;
	private long unfilledVacancies;
	private static double employmentRate;

	private float avgGdp;
	private float avgGini;

	protected StatisticsManager(List<Firm> contextFirms,
			List<Household> contextHouseholds, GoodsMarket gm, Government gov) {
		firms.addAll(contextFirms);
		households.addAll(contextHouseholds);
		goodsMarket = gm;
		giniIndexIncome = 0;
		government = gov;
	}

	@Watch(watcheeClassName = "agents.Firm", watcheeFieldNames = "endCycle", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void calculateStatistics() {

		// GINI INDEX
		Collections.sort(households, new Comparator<Household>() {
			@Override
			public int compare(Household hh1, Household hh2) {
				if (hh1.getIncome() < hh2.getIncome())
					return -1;
				if (hh1.getIncome() > hh2.getIncome())
					return 1;
				else
					return 0;
			}
		});

		float population = households.size(), numIncome = 0, denomIncome = 0;
		for (int i = 1; i <= population; i++) {
			Household household = households.get(i - 1);
			numIncome += (population + 1 - i) * household.getIncome();
			denomIncome += household.getIncome();
		}

		Collections.sort(households, new Comparator<Household>() {
			@Override
			public int compare(Household hh1, Household hh2) {
				if (hh1.getWage() < hh2.getWage())
					return -1;
				if (hh1.getWage() > hh2.getWage())
					return 1;
				else
					return 0;
			}
		});

		float numWage = 0, denomWage = 0;
		for (int i = 1; i <= population; i++) {
			Household household = households.get(i - 1);
			numWage += (population + 1 - i) * household.getWage();
			denomWage += household.getWage();
		}

		Collections.sort(households, new Comparator<Household>() {
			@Override
			public int compare(Household hh1, Household hh2) {
				if (hh1.getConsumption() < hh2.getConsumption())
					return -1;
				if (hh1.getConsumption() > hh2.getConsumption())
					return 1;
				else
					return 0;
			}
		});

		float numConsumption = 0, denomConsumption = 0;
		for (int i = 1; i <= population; i++) {
			Household household = households.get(i - 1);
			numConsumption += (population + 1 - i) * household.getConsumption();
			denomConsumption += household.getConsumption();
		}

		Collections.sort(households, new Comparator<Household>() {
			@Override
			public int compare(Household hh1, Household hh2) {
				if (hh1.getBalance() < hh2.getBalance())
					return -1;
				if (hh1.getBalance() > hh2.getBalance())
					return 1;
				else
					return 0;
			}
		});

		float numFunds = 0, denomFunds = 0;
		for (int i = 1; i <= population; i++) {
			Household household = households.get(i - 1);
			numFunds += (population + 1 - i) * household.getBalance();
			denomFunds += household.getBalance();
		}

		giniIndexIncome = denomIncome > 0 ? (1 / population)
				* (population + 1 - 2 * numIncome / denomIncome) : 0;
		giniIndexConsumption = denomConsumption > 0 ? (1 / population)
				* (population + 1 - 2 * numConsumption / denomConsumption) : 0;
		giniIndexFunds = denomFunds > 0 ? (1 / population)
				* (population + 1 - 2 * numFunds / denomFunds) : 0;
		giniIndexWage = denomWage > 0 ? (1 / population)
				* (population + 1 - 2 * numWage / denomWage) : 0;

		// AVERAGE WAGE & UNFILLED VACANCIES & GDP & OUTPUT
		float sum = 0;
		float n = 0;
		float gdp = 0;
		int production = 0;
		long sumUnfilledVacancies = 0;
		firmFunds = 0;
		totalSoldGoods = 0;
		totalStock = 0;
		for (Firm firm : firms) {
			if (firm.getNumberOfEmployees() > 0) {
				n += firm.getNumberOfEmployees();
				sum += (firm.getNumberOfEmployees() * firm.getAverageWage());
				sumUnfilledVacancies += firm.getUnfilledVacancies();
			}

			gdp += (firm.getPaidWages() + firm.getSoldGoodsProfits());
			production += firm.getFinalProduction();
			avgPracticedPrice += firm.getGoodsPrice();
			firmFunds += firm.getBalance();
			totalSoldGoods += firm.getSoldGoods();
			totalStock += firm.getStockSize();
		}

		avgPracticedPrice /= firms.size();
		productionOutput = production;
		GDP = gdp;
		unfilledVacancies = sumUnfilledVacancies;
		averageWage = sum / n;

		// % EMPLOYED
		double count = 0;
		householdConsumption = 0;
		householdFunds = 0;
		for (Household hh : households) {
			if (hh.isEmployed())
				count++;
			householdConsumption += hh.getConsumption();
			householdFunds += hh.getBalance();
		}
		employmentRate = count / households.size();

		// INFLATION
		inflation = goodsMarket.getInflation();

		avgPracticedPrice += goodsMarket.getCurrentAvgPrice();

		// Only needs to calculate depending on the time window chosen to
		// extract data
		if (Properties.getIteration() >= Properties.START_TICK
				& Properties.getIteration() <= Properties.END_TICK) {
			avgGdp += householdConsumption;
			avgGini += giniIndexIncome;
		}

		if (Properties.getIteration() == Properties.END_TICK) {
			avgGdp /= 1200;
			avgGini /= 1200;
		}

	}

	public double getGiniIndexIncome() {
		return giniIndexIncome;
	}

	public double getGiniIndexConsumption() {
		return giniIndexConsumption;
	}

	public double getGiniIndexFunds() {
		return giniIndexFunds;
	}

	public double getGiniIndexWage() {
		return giniIndexWage;
	}

	public double getAverageWage() {
		return averageWage;
	}

	public static double getEmploymentRate() {
		return employmentRate;
	}

	public static double getUnemployementRate() {
		return 1 - employmentRate;
	}

	public static float getLowesReservationWage() {
		float lowest = Float.MAX_VALUE;

		for (Household hh : households) {
			if (hh.getReservationWage() < lowest)
				lowest = hh.getReservationWage();
		}

		return lowest;
	}

	public static float getLowestPrice() {
		float lowest = Float.MAX_VALUE;
		for (Firm firm : firms)
			if (firm.getLowestPrice() < lowest)
				lowest = firm.getLowestPrice();

		return lowest;
	}

	public final int getEmployed() {
		int count = 0;
		for (Household hh : households) {
			if (hh.isEmployed())
				count++;
		}
		return count;
	}

	public final double getInflation() {
		return inflation;
	}

	public final double getUnfilledVacancies() {
		return unfilledVacancies;
	}

	public final float getGDP() {
		return GDP;
	}

	public final int getProductionOutput() {
		return productionOutput;
	}

	public final float getFirmFunds() {
		return firmFunds;
	}

	public final float getHouseholdFunds() {
		return householdFunds;
	}

	public final float getHouseholdConsumption() {
		return householdConsumption;
	}

	public final float getGovFunds() {
		return government.getFunds();
	}

	public float getTotalStock() {
		return totalStock;
	}

	public float getTotalSoldGoods() {
		return totalSoldGoods;
	}

	public float getAvgGoodsPrice() {
		return goodsMarket.getCurrentAvgPrice();
	}

	public float getAvgPracticedPrice() {
		return avgPracticedPrice;
	}

	public float getAvgGDP() {
		return avgGdp;
	}

	public float getAvgGini() {
		return avgGini;
	}

	public static void clear() {
		firms = new ArrayList<Firm>();
		households = new ArrayList<Household>();
		goodsMarket = null;
		government = null;
		employmentRate = 0;

	}
}
