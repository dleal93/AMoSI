package environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import agents.Household;
import utilities.Properties;

/**
 * This class represents the Job Market. It works as a pool for the Households
 * that are looking for a job become visible to the Firms that are hiring.
 * 
 * @author Diogo L. Costa
 *
 * @param jobPool
 *            A list of all jobs posted by Firms and open for applications
 */
public final class LaborMarket {

	private List<Household> applicants = new ArrayList<Household>();

	private boolean marketReady = false;
	private boolean marketClose = false;
	private boolean laborClose = false;
	private boolean isSorted = false;
	private int readyCount = 0;
	private int closeCount = 0;
	private int laborCount = 0;

	/**
	 * LaborMarket can only be created in {@link SimBuilder}; throughout the
	 * simulation NO AGENT is able to create an instance of this class.
	 */
	LaborMarket() {

	}

	public final void joinLaborMarket(Household applicant) {
		applicants.add(applicant);

	}

	/**
	 * Clears all the subscriptions made to the market if the {@code firm}
	 * requesting the cleanse is currently subscribed.<br>
	 * 
	 * @param firm
	 */
	public void clearMarket() {
		if (!applicants.isEmpty()) {
			applicants.clear();
			isSorted = false;
		}
	}

	public List<Household> getApplicants() {
		if (isSorted)
			return applicants;
		else {
			// Returns the list of applicants sorted by productivity
			Collections.sort(applicants, new Comparator<Household>() {

				@Override
				public int compare(Household o1, Household o2) {
					if (o1.getProductivity() > o2.getProductivity())
						return -1;
					else if (o1.getProductivity() < o2.getProductivity())
						return 1;
					return 0;
				}

			});
			isSorted = true;
			return applicants;
		}
	}

	public void removeApplicants(List<Household> household) {
		applicants.removeAll(household);
	}

	public void confirmOffers() {
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

	public void closeLabor() {
		laborCount++;
		if (laborCount == Properties.getNumberOfOpenFirms()) {
			laborClose = !laborClose;
			laborCount = 0;
		}
	}

	public void communicateClosure() {
		if (readyCount == Properties.getNumberOfOpenFirms()) {
			readyCount--;
			confirmOffers();
		}
	}

}
