import apryraz.bworld.BFState;
import apryraz.bworld.BarcenasFinder;
import apryraz.bworld.BarcenasWorldEnv;
import org.junit.Assert;
import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;

/**
 * Class for testing the BarcenasFinder agent.
 **/
public class BarcenasFinderTest {


	/**
	 * Executes the next step of the agent, and the assert
	 * whether the resulting state is equal to the targetState.
	 *
	 * @param bAgent      BarcenasFinder agent.
	 * @param targetState the state that should be equal to the resulting state of
	 *                    the agent after performing the next step.
	 **/
	public void testMakeSimpleStep(BarcenasFinder bAgent,
							 BFState targetState) throws
		   IOException, ContradictionException, TimeoutException {
		// Check whether the resulting state is equal to
		// the targetState after performing action runNextStep with bAgent
		bAgent.runNextStep();
		Assert.assertTrue(targetState.equals(bAgent.getState()));
	}


	/**
	 * Reads an state from the current position of the file trough the
	 * BufferedReader object.
	 *
	 * @param br   BufferedReader object interface to the opened file of states.
	 * @param wDim dimension of the world.
	 **/
	public BFState readTargetStateFromFile(BufferedReader br, int wDim) throws
		   IOException {
		BFState bfstate = new BFState(wDim);
		String row;
		String[] rowvalues;

		for (int i = wDim; i > 0; i--) {
			row = br.readLine();
			rowvalues = row.split(" ");
			for (int j = 1; j <= wDim; j++) {
				bfstate.set(i, j, rowvalues[j - 1]);
			}
		}
		return bfstate;
	}

	/**
	 * Loads a sequence of states from a file, and returns the list.
	 *
	 * @param wDim       dimension of the world.
	 * @param numStates  num of states to read from the file.
	 * @param statesFile file name with sequence of target states, that should
	 *                   be the resulting states after each movement in fileSteps.
	 * @return returns an ArrayList of BFState with the resulting list of states.
	 **/
	ArrayList<BFState> loadListOfTargetStates(int wDim, int numStates, String statesFile) {

		ArrayList<BFState> listOfStates = new ArrayList<BFState>(numStates);

		try {
			BufferedReader br = new BufferedReader(new FileReader(statesFile));
			String row;

			// steps = br.readLine();
			for (int s = 0; s < numStates; s++) {
				listOfStates.add(readTargetStateFromFile(br, wDim));
				// Read a blank line between states
				row = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException ex) {
			System.out.println("MSG.   => States file not found");
			exit(1);
		} catch (IOException ex) {
			Logger.getLogger(BarcenasFinderTest.class.getName()).log(Level.SEVERE, null, ex);
			exit(2);
		}

		return listOfStates;
	}


	/**
	 * This function should run the sequence of steps stored in the file fileSteps,
	 * but only up to numSteps steps.
	 *
	 * @param wDim       the dimension of world
	 * @param barX       x coordinate of Barcenas position
	 * @param barY       y coordinate of Barcenas position
	 * @param numSteps   num of steps to perform
	 * @param fileSteps  file name with sequence of steps to perform
	 * @param fileStates file name with sequence of target states, that should
	 *                   be the resulting states after each movement in fileSteps
	 **/
	public void testMakeSeqOfSteps(int wDim, int barX, int barY,
							 int numSteps, String fileSteps, String fileStates) throws
		   IOException, ContradictionException, TimeoutException {
		// You should make BarcenasFinder and BarcenasWorldEnv objects to test.
		// Then load sequence of target states, load sequence of steps into the bAgent
		// and then test the sequence calling testMakeSimpleStep once for each step.

		BarcenasFinder BAgent = new BarcenasFinder(wDim);
		BarcenasWorldEnv EnvAgent = new BarcenasWorldEnv(wDim, barX, barY);

		// Load list of states
		ArrayList<BFState> seqOfStates = loadListOfTargetStates(wDim, numSteps, fileStates);

		// Set environment agent and load list of steps into the agent
		BAgent.setEnvironment(EnvAgent);
		BAgent.loadListOfSteps(numSteps, fileSteps);

		// Test here the sequence of steps and check the resulting states with the
		// ones in seqOfStates
		for (int i = 0; i < numSteps; i++) {
			testMakeSimpleStep(BAgent, seqOfStates.get(i));
		}

	}


	/**
	 * Runs first test(states1.txt  steps1.txt) for the BarcenasFinder:
	 * 4x4 world,
	 * Barcenas at (3,3),
	 * 5 steps.
	 *
	 * @throws IOException            when opening states or steps file.
	 * @throws ContradictionException if inserting contradictory information to solver.
	 * @throws TimeoutException       if solver's isSatisfiable operation spends more
	 *                                time computing than a certain timeout.
	 */
	@Test
	public void BWorldTest1() throws
		   IOException, ContradictionException, TimeoutException {
		// Example test for 4x4 world , barcenas at 3,3 and 5 steps
		testMakeSeqOfSteps(4, 3, 3, 5, "tests/steps1.txt", "tests/states1.txt");
	}


	/**
	 * Runs second test(states2.txt  steps2.txt) for the BarcenasFinder:
	 * 4x4 world,
	 * Barcenas at (4,1),
	 * 4 steps.
	 *
	 * @throws IOException            when opening states or steps file.
	 * @throws ContradictionException if inserting contradictory information to solver.
	 * @throws TimeoutException       if solver's isSatisfiable operation spends more
	 *                                * time computing than a certain timeout.
	 */
	@Test
	public void BWorldTest2() throws
		   IOException, ContradictionException, TimeoutException {
		// Example test for 4x4 world , barcenas at 3,3 and 5 steps
		testMakeSeqOfSteps(4, 4, 1, 4, "tests/steps2.txt", "tests/states2.txt");
	}

	/**
	 * Runs third test(states3.txt  steps3.txt) for the BarcenasFinder:
	 * 5x5 world,
	 * Barcenas at (3,3),
	 * 7 steps.
	 *
	 * @throws IOException            when opening states or steps file.
	 * @throws ContradictionException if inserting contradictory information to solver.
	 * @throws TimeoutException       if solver's isSatisfiable operation spends more
	 *                                time computing than a certain timeout.
	 */
	@Test
	public void BWorldTest3() throws
		   IOException, ContradictionException, TimeoutException {
		// Example test for 4x4 world , barcenas at 3,3 and 5 steps
		testMakeSeqOfSteps(5, 3, 3, 7, "tests/steps3.txt", "tests/states3.txt");
	}

	/**
	 * Runs fourth test(states4.txt  steps4.txt) for the BarcenasFinder:
	 * 5x5 world,
	 * Barcenas at (5,5),
	 * 7 steps.
	 *
	 * @throws IOException            when opening states or steps file.
	 * @throws ContradictionException if inserting contradictory information to solver.
	 * @throws TimeoutException       if solver's isSatisfiable operation spends more
	 *                                time computing than a certain timeout.
	 */
	@Test
	public void BWorldTest4() throws
		   IOException, ContradictionException, TimeoutException {
		// Example test for 4x4 world , barcenas at 3,3 and 5 steps
		testMakeSeqOfSteps(5, 5, 5, 7, "tests/steps4.txt", "tests/states4.txt");
	}

	/**
	 * Tests solver by adding a simple implication and
	 * performing all possible situations for the implication,
	 * the goal is to check that all possible
	 * solver inferences are correct.
	 *
	 * @throws ContradictionException if inserting contradictory information to solver.
	 * @throws TimeoutException       if solver's isSatisfiable operation spends more
	 *                                time computing than a certain timeout.
	 */
	@Test
	public void testSolver() throws ContradictionException, TimeoutException {
		ISolver solver;
		int totalNumVariables;

		totalNumVariables = 2;
		solver = SolverFactory.newDefault();
		solver.setTimeout(3600);
		solver.newVar(totalNumVariables);

		// add a simple implication: 1 -> 2
		VecInt implication = new VecInt();
		implication.insertFirst(2);
		implication.insertFirst(-1);

		solver.addClause(implication);

		checkImplicationSatisfiability(solver, implication);
		checkImplicationUnsatisifiability(solver, implication);
	}

	/**
	 * Checks all possible satisfiable situations for the implication.
	 * Then checks solver does indeed point that there are satisfiable
	 * interpretations.
	 *
	 * @param solver      sat solver object that stores the formula.
	 * @param implication a simple implication such as 1 -> 2 = -1 or 2
	 * @throws TimeoutException if solver's isSatisfiable operation spends more
	 *                          time computing than a certain timeout.
	 */
	public void checkImplicationSatisfiability(ISolver solver, VecInt implication) throws TimeoutException {
		// case 1: (-1 or 2) and 1
		VecInt toPerformInference1 = new VecInt();
		toPerformInference1.insertFirst(-(implication.get(0)));
		Assert.assertTrue(solver.isSatisfiable(toPerformInference1));

		// case 2: (-1 or 2) and -2
		VecInt toPerformInference2 = new VecInt();
		toPerformInference2.insertFirst(-(implication.get(1)));
		Assert.assertTrue(solver.isSatisfiable(toPerformInference2));

		// case 3: (-1 or 2) and -1
		VecInt toPerformInference3 = new VecInt();
		toPerformInference3.insertFirst((implication.get(0)));
		Assert.assertTrue(solver.isSatisfiable(toPerformInference3));

		// case 4: (-1 or 2) and 2
		VecInt toPerformInference4 = new VecInt();
		toPerformInference4.insertFirst((implication.get(1)));
		Assert.assertTrue(solver.isSatisfiable(toPerformInference4));

		// case 5: (-1 or 2) and (-1 and 2)
		VecInt toPerformInference5 = new VecInt();
		toPerformInference5.insertFirst((implication.get(1)));
		toPerformInference5.insertFirst((implication.get(0)));
		Assert.assertTrue(solver.isSatisfiable(toPerformInference5));

	}

	/**
	 * Checks the only possible unsatisfiable situation for the implication.
	 * Then checks solver does indeed point that there is an unsatisfiable
	 * interpretation.
	 *
	 * @param solver      sat solver object that stores the formula.
	 * @param implication a simple implication such as 1 -> 2 = -1 or 2
	 * @throws TimeoutException if solver's isSatisfiable operation spends more
	 *                          time computing than a certain timeout.
	 */
	public void checkImplicationUnsatisifiability(ISolver solver, VecInt implication) throws TimeoutException {
		// case 4: (-1 or 2) and (1 and -2)
		VecInt toPerformInference = new VecInt();
		toPerformInference.insertFirst(-(implication.get(0)));
		toPerformInference.insertFirst(-(implication.get(1)));

		Assert.assertFalse(solver.isSatisfiable(toPerformInference));
	}

	/**
	 * Tests coordToLineal function for all positions in
	 * a 4x4 BarcenasWorld.
	 */
	@Test
	public void testCoordToLineal() {
		BarcenasFinder BAgent = new BarcenasFinder(4);
		int offset = 33;
		int currentLiteral = 33;

		for (int x = 1; x <= 4; x++) {
			for (int y = 1; y <= 4; y++) {
				int lineal = BAgent.coordToLineal(x, y, offset);
				Assert.assertEquals(currentLiteral, lineal);
				currentLiteral++;
			}
		}
	}

	/**
	 * Tests linealToCoord function for all positions in
	 * a 4x4 BarcenasWorld.
	 */
	@Test
	public void testLinealToCoord() {
		BarcenasFinder BAgent = new BarcenasFinder(4);
		int offset = 33;
		int currentLiteral = 33;

		for (int x = 1; x <= 4; x++) {
			for (int y = 1; y <= 4; y++) {
				int[] expectedCoords = new int[]{x, y};
				int[] coords = BAgent.linealToCoord(currentLiteral, offset);

				Assert.assertArrayEquals(expectedCoords, coords);
				currentLiteral++;
			}
		}
	}

}
