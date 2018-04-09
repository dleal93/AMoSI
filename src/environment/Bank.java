/**
 * 
 */
package environment;

import java.util.HashMap;
import java.util.Map;

import agents.Government;
import utilities.Agent;

/**
 * @author Diogo L. Costa
 *
 */
public final class Bank {

	public final static class BankAccount {
		private float funds;

		private BankAccount(float money) {
			this.funds = money;
		}

		/**
		 * @return the funds
		 */
		public final float getBalance() {
			return funds;
		}

		/**
		 * @param funds
		 *            the funds to set
		 */
		private final void setFunds(float funds) {
			this.funds = funds;
		}

	}

	private static Map<Agent, BankAccount> clients = new HashMap<Agent, BankAccount>();

	public static BankAccount registerClient(Agent ent, float money) {
		BankAccount account = new BankAccount(money);
		clients.put(ent, account);
		return account;

	}

	private static void deposit(Agent recepient, float amount) {
		BankAccount account = clients.get(recepient);
		account.setFunds(account.getBalance() + amount);
	}

	public static boolean transfer(BankAccount drawee, Agent recepient, float amount) {

		if (drawee.getBalance() - amount >= 0) {
			drawee.setFunds(drawee.getBalance() - amount);
			deposit(recepient, amount);
			return true;
		}
		return false;
	}

	public static boolean pay(Agent drawee, Agent recepient, float amount) {

		// The government always pays what it owes, which means it can incur in debt
		if (drawee instanceof Government) {
			BankAccount govern = clients.get(drawee);
			BankAccount household = clients.get(recepient);
			govern.setFunds(govern.getBalance() - amount);
			household.setFunds(household.getBalance() + amount);
			return true;
		} else {
			BankAccount draweeAcc = clients.get(drawee);
			return transfer(draweeAcc, recepient, amount);
		}
	}

	protected static void clear() {
		clients = new HashMap<Agent, BankAccount>();
	}
}
