/**
 * 
 */
package utilities;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

/**
 * This is a static class that allows limited access to all the simulation's
 * parameters. <br>
 * Note that it is not possible to modify the parameters during runtime nor
 * outside of this class's scope.
 * 
 * @author Diogo L. Costa
 *
 */
public final class Properties {

	// Cannot extend nor create an instance of this class.
	private Properties() {
	}

	// RUNENVIRONMENT PARAMETERS

	private final static Parameters params = RunEnvironment.getInstance().getParameters();

	// GOODS MARKET

	public final static double OMISSION_FACTOR = 0.7;

	// HOUSEHOLDS

	public final static int HOUSEHOLDS_NUMBER = (int) params.getValue("hhnumber");
	public final static float HOUSEHOLD_FUNDS = 0;
	public final static float INIT_RESWAGE = 5;
	public final static float INIT_MIN_UTILITY = 0.4f;
	public final static float INIT_MAX_UTILITY = 0.6f;

	public final static float UPPER_WAGE_REDUCTION = 0.2f;
	public final static float LOWER_WAGE_REDUCTION = 0.1f;

	public final static float PRODUCTIVITY_LEVEL = 2f;
	public final static float PRODUCTIVITY_INCREASE = 0.01f;
	public final static float PRODUCTIVITY_REDUCTION = 0.005f;

	public final static float SUPERIOR_EDUCATION = 1.75f;
	public final static float TECHNICAL_EDUCATION = 1.45f;
	public final static float SECONDARY_EDUCATION = 1.20f;
	public final static int SUPED_NUMBER = 25;
	public final static int TECHED_NUMBER = 100;

	// FIRMS

	public final static int FIRMS_NUMBER = (int) params.getValue("firmsnumber");

	public final static int INIT_FUNDS = 250;
	public final static int INIT_PRODUCTION = 10;
	public final static float INIT_GOODS_PRICE = 3f;

	public final static float INIT_WAGE_OFFER = 5;

	public final static float UPPER_WAGE_VARIANCE = 0.15f;
	public final static float LOWER_WAGE_VARIANCE = 0.08f;

	public final static float WILL_TO_PRODUCE = 0.05f;
	public final static float REDUCE_PRODUCTION = 0.05f;
	public final static float PRICE_INCREASE = 0.02f;
	public final static float UPPER_PRICE_RESISTANCE = 0.12f;
	public final static float LOWER_PRICE_RESISTANCE = 0.08f;

	public final static float UPPER_MAX_PRODUCTION_THRESHOLD = 0.95f;
	public final static float LOWER_MAX_PRODUCTION_THRESHOLD = 0.85f;
	public final static float UPPER_MIN_PRODUCTION_THRESHOLD = 0.55f;
	public final static float LOWER_MIN_PRODUCTION_THRESHOLD = 0.45f;

	public final static float MAX_PRICE_THRESHOLD = 0.90f;
	public final static float MIN_PRICE_THRESHOLD = 0.80f;

	public final static float SALES_DISCOUNT = 0.6f;

	public final static float EMPLOYMENT_PROSPERITY = 0.90f;
	public final static float PROSPERITY_BONUS = 0.05f;

	// GOVERNMENT

	public final static float MIN_WAGE = 1;
	public final static float IRC_TAX = (float) params.getValue("IRC");
	public final static float IRS_TAX = (float) params.getValue("IRS");
	public final static float IVA_TAX = (float) params.getValue("IVA");
	public final static float MIN_BENEFIT = (float) params.getValue("minbenefit");
	public final static float EARNED_TAX_CREDIT = (float) params.getValue("eitcbenefit");
	public final static float UNEMPLOYED_BENEFIT_FACTOR = (float) params.getValue("unempbenefit");
	public final static int UNEMPLOYED_TIME = 9;

	// ITERATIONS

	public final static int START_TICK = 1;
	public final static int END_TICK = 2400;
	private static int iterationLoad = 1;
	private static int iteration = 1;

	public final static double START_TIME = System.currentTimeMillis();

	private static int ID = 0;
	private static int aliveFirms = FIRMS_NUMBER;

	public final static void nextIteration() {

		if (iterationLoad == aliveFirms) {
			iterationLoad = 1;
			iteration++;
		} else
			iterationLoad++;
	}

	public final static int getIteration() {
		return iteration;
	}
	
	public final static int getIterationLoad() {
		return iterationLoad;
	}

	public final static int getID() {
		ID++;
		return ID;
	}

	public final static void communicateClosure() {
		aliveFirms--;
	}

	public final static int getNumberOfOpenFirms() {
		return aliveFirms;
	}

	public static int getNumberOfHouseholds() {
		return HOUSEHOLDS_NUMBER;
	}

	public static float calculateEarnedTaxCredit(float salary) {
		if (salary >= MIN_WAGE && salary < 3)
			return 1.5f - salary / 2;
		else
			return 0;
	}

	public static void clear() {
		iterationLoad = 1;
		iteration = 1;
		ID = 0;
		aliveFirms = FIRMS_NUMBER;

	}

}
