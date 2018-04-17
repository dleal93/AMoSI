/**
 * 
 */
package agents;

import java.util.ArrayList;
import java.util.List;

import environment.Bank;
import environment.Bank.BankAccount;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import utilities.Agent;
import utilities.Properties;

/**
 * @author Diogo L. Costa
 *
 */
public class Government implements Agent {

	private List<Household> households = new ArrayList<Household>();

	private BankAccount account;

	private boolean redistributed = false;

	private float IRC;
	private float IVA;
	private float IRS;

	private int redistTurn;

	/**
	 * @param contextHouseholds
	 * @param contextFirms
	 * 
	 */
	public Government() {
		account = Bank.registerClient(this, 0);
		IVA = IRS = IRC = 0;
		redistTurn = 0;
	}

	public void setFiels(List<Household> contextHouseholds) {
		households = contextHouseholds;
	}

	@Watch(watcheeClassName = "environment.LaborMarket", watcheeFieldNames = "laborClose", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void Redistribute() {

		for (Household household : households) {
			if (!household.isEmployed()) {
				if (household.getUnemployedTime() <= Properties.UNEMPLOYED_TIME) {
					Bank.pay(this, household, Math.max(household.getLastWage() * Properties.UNEMPLOYED_BENEFIT_FACTOR,
							Properties.MIN_BENEFIT * Properties.MIN_WAGE * (1 - Properties.IRS_TAX)));
				} else if (household.getUnemployedTime() > Properties.UNEMPLOYED_TIME)
					Bank.pay(this, household, Properties.MIN_BENEFIT * Properties.MIN_WAGE * (1 - Properties.IRS_TAX));

			} else {

				Bank.pay(this, household,
						Properties.EARNED_TAX_CREDIT * Properties.calculateEarnedTaxCredit(household.getWage()));

			}
		}

		if (redistTurn == Properties.GROWTH_PERIOD) {
			if (account.getBalance() > 0) {
				Properties.IRC_TAX -= Properties.IRC_TAX * Properties.TAX_VARIANCE;
				Properties.IRS_TAX -= Properties.IRS_TAX * Properties.TAX_VARIANCE;
				Properties.IVA_TAX -= Properties.IVA_TAX * Properties.TAX_VARIANCE;
				//System.out.println("TAX DECREASE");
			} else {
				Properties.IRC_TAX += Properties.IRC_TAX * Properties.TAX_VARIANCE;
				Properties.IRS_TAX += Properties.IRS_TAX * Properties.TAX_VARIANCE;
				Properties.IVA_TAX += Properties.IVA_TAX * Properties.TAX_VARIANCE;
				//System.out.println("TAX INCREASE");
			}
			/*System.out.println("IRC: " + Properties.IRC_TAX + "\nIRS: " + Properties.IRS_TAX + "\nIVA: "
					+ Properties.IVA_TAX + "\n");*/
			redistTurn = -1;
		}

		redistTurn++;

		IRS = 0;
		IRC = 0;
		IVA = 0;

		redistributed = !redistributed;
	}

	public float payIncomeTaxes(BankAccount firmAccount, float salary) {
		float incomeTax = Properties.IRS_TAX;
		IRS += salary * incomeTax;
		Bank.transfer(firmAccount, this, salary * incomeTax);
		return salary - (salary * incomeTax);
	}

	public void payProfitTaxes(BankAccount firmAccount, float profit) {
		float firmsTax = Properties.IRC_TAX;
		IRC += profit * firmsTax;
		Bank.transfer(firmAccount, this, profit * firmsTax);
	}

	public boolean payVAT(Agent buyer, float goodPrice) {
		IVA += goodPrice * Properties.IVA_TAX;
		return Bank.pay(buyer, this, goodPrice * Properties.IVA_TAX);
	}

	public float getFunds() {
		return account.getBalance();
	}

	public float getIRC() {
		return IRC;
	}

	public float getIRS() {
		return IRS;
	}

	public float getIVA() {
		return IVA;
	}

}
