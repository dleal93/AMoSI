package utilities;

import agents.Firm;
import agents.Household;

/**
 * @author Diogo L. Costa
 *
 */
public class JobContract {

	private float payCheck;
	private Firm employer;
	private Household employee;

	public JobContract(float pc, Firm firm, Household household) {
		payCheck = pc;
		employer = firm;
		employee = household;
	}

	public void rescindContract() {
		employer.notifyRescission(this);
	}

	/**
	 * @return the payCheck
	 */
	public final float getPayCheck() {
		return payCheck;
	}

	/**
	 * @param payCheck
	 *            the payCheck to set
	 */
	public final void setPayCheck(float payCheck) {
		this.payCheck = payCheck;
	}

	/**
	 * @return the employer
	 */
	public final Firm getEmployer() {
		return employer;
	}

	/**
	 * @param employer
	 *            the employer to set
	 */
	public final void setEmployer(Firm employer) {
		this.employer = employer;
	}

	/**
	 * @return the employee
	 */
	public final Household getEmployee() {
		return employee;
	}

	/**
	 * @param employee
	 *            the employee to set
	 */
	public final void setEmployee(Household employee) {
		this.employee = employee;
	}
	
	public final float getEmployeeProductivity(){
		return employee.getProductivity();
	}


}
