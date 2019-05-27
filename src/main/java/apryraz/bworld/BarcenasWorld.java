
package apryraz.bworld;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.io.IOException;


/**
 * The class for the main program of the Barcenas World.
 **/
public class BarcenasWorld {


	/**
	 * Executes the sequence of steps stored in the file fileSteps,
	 * but only up to numSteps steps. Each step is executed by calling the
	 * function runNextStep() of the BarcenasFinder agent.
	 *
	 * @param wDim      the dimension of world.
	 * @param barX      x coordinate of Barcenas position.
	 * @param barY      y coordinate of Barcenas position.
	 * @param numSteps  num of steps to perform.
	 * @param fileSteps file name with sequence of steps to perform.
	 **/
	private static void runStepsSequence(int wDim, int barX, int barY,
								  int numSteps, String fileSteps) throws
		   ContradictionException, TimeoutException {

		// Make instances of BarcenasFinder agent and environment object classes
		BarcenasFinder BAgent;
		BarcenasWorldEnv EnvAgent;
		BAgent = new BarcenasFinder(wDim);
		EnvAgent = new BarcenasWorldEnv(wDim, barX, barY);

		// Set environment object and load list of steps into the Agent
		BAgent.setEnvironment(EnvAgent);
		BAgent.loadListOfSteps(numSteps, fileSteps);

		// Execute sequence of steps with the Agent
		for (int stepNum = 0; stepNum < numSteps; stepNum++) {
			BAgent.runNextStep();
		}
	}

	/**
	 * Loads five arguments from the command line:
	 * arg[0] = dimension of the word.
	 * arg[1] = x coordinate of Barcenas position.
	 * arg[2] = y coordinate of Barcenas position.
	 * arg[3] = num of steps to perform.
	 * arg[4] = file name with sequence of steps to perform.
	 * Then runs the steps sequence contained in the stepsFile (args[4]).
	 **/
	public static void main(String[] args) throws IOException,
		   ContradictionException, TimeoutException {
		if (args != null && args.length > 4) {
			int worldDim = Integer.parseInt(args[0]);
			int barcenasX = Integer.parseInt(args[1]);
			int barcenasY = Integer.parseInt(args[2]);
			int numSteps = Integer.parseInt(args[3]);
			String stepsFile = args[4];
			runStepsSequence(worldDim, barcenasX, barcenasY, numSteps, stepsFile);
		} else {
			System.out.println("WORLD => Not enough arguments given");
		}
	}

}
