/**
 * 
 */
package environment;

import java.util.ArrayList;
import java.util.List;

import agents.Firm;
import agents.Government;
import agents.Household;
import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import utilities.Properties;

/**
 * @author Diogo L. Costa
 *
 */
public class SimBuilder implements ContextBuilder<Object> {

	@Override
	public Context<Object> build(Context<Object> context) {
		context.setId("AMoSI");

		//Stop the run at tick X
		RunEnvironment.getInstance().endAt(Properties.END_TICK);
		Bank.clear();
		StatisticsManager.clear();
		Properties.clear();

		//Guarantees always the same parameters distribution throughout the scenarios/runs
		RandomHelper.setSeed(1);

		Government government = new Government();
		GoodsMarket gm = new GoodsMarket(government);
		LaborMarket lm = new LaborMarket();

		List<Firm> contextFirms = new ArrayList<Firm>();
		for (int i = 0; i < Properties.FIRMS_NUMBER; i++) {
			Firm firm = new Firm(gm, lm, government);
			context.add(firm);
			contextFirms.add(firm);
		}

		List<Household> contextHouseholds = new ArrayList<Household>();

		for (int i = 0; i < Properties.HOUSEHOLDS_NUMBER; i++) {
			Household hh;
			if (i < Properties.SUPED_NUMBER)
				hh = new Household(gm, lm, Properties.SUPERIOR_EDUCATION);
			else if (i < Properties.TECHED_NUMBER)
				hh = new Household(gm, lm, Properties.TECHNICAL_EDUCATION);
			else
				hh = new Household(gm, lm, Properties.SECONDARY_EDUCATION);
			context.add(hh);
			contextHouseholds.add(hh);
		}

		government.setFiels(contextHouseholds);
		StatisticsManager sm = new StatisticsManager(contextFirms,
				contextHouseholds, gm, government);

		context.add(sm);
		context.add(government);

		return context;
	}

}
