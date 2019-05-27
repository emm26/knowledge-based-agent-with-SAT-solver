
package apryraz.bworld;

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
 * This agent performs a sequence of movements, and after each
 * movement it "senses" from the environment the resulting position
 * and then the outcome from the smell sensor, to try to locate
 * the position of Barcenas.
 **/
public class BarcenasFinder {

	/**
	 * List of steps to perform.
	 */
	private ArrayList<Position> listOfSteps;

	/**
	 * Index for the next movement to perform, and total number of movements.
	 */
	private int idNextStep, numMovements;

	/**
	 * Array of clauses that represent conclusions obtained in the most recent
	 * call to the inference function, but rewritten using the "past" variables.
	 */
	private ArrayList<VecInt> futureToPast = null;

	/**
	 * Current state of knowledge of the agent (what it knows about
	 * every position of the world).
	 */
	private BFState bfstate;

	/**
	 * Object that represents the interface for the Barcenas World.
	 */
	private BarcenasWorldEnv envAgent;

	/**
	 * SAT solver object that stores the logical boolean formula with the rules
	 * and current knowledge about not possible locations for Barcenas.
	 */
	private ISolver solver;

	/**
	 * Agent position in the world.
	 */
	private int agentX, agentY;

	/**
	 * Dimension of the world and total size of the world (Dim^2).
	 */
	private int worldDim, worldLinealDim;

	/**
	 * First ever literal in past variables concerning
	 * information about Barcenas' location.
	 */
	private int barcenasPastOffset;

	/**
	 * First ever literal in future variables concerning
	 * information about Barcenas' location.
	 */
	private int barcenasFutureOffset;

	/**
	 * First ever literal in present variables related to
	 * when sound is gathered above a certain position
	 * by the sound sensors.
	 */
	private int soundAboveOffset = 0;

	/**
	 * First ever literal in present variables related to
	 * when sound is gathered below a certain position
	 * by the sound sensors.
	 */
	private int soundBelowOffset = 0;

	/**
	 * First ever literal in present variables related to
	 * when sound is gathered on the left of a certain position
	 * by the sound sensors.
	 */
	private int soundLeftOffset = 0;

	/**
	 * First ever literal in present variables related to
	 * when sound is gathered on the right a certain position
	 * by the sound sensors.
	 */
	private int soundRightOffset = 0;

	/**
	 * First ever literal without meaning, free to assign.
	 */
	private int currentLiteral = 0;


	/**
	 * The class constructor must create the initial Boolean formula with the
	 * rules of the Barcenas World, initialize the variables for indicating
	 * that we do not have yet any movements to perform, make the initial state.
	 *
	 * @param WDim the dimension of the Barcenas World.
	 **/
	public BarcenasFinder(int WDim) {
		worldDim = WDim;
		worldLinealDim = worldDim * worldDim;

		try {
			solver = buildGamma();
		} catch (ContradictionException ex) {
			Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
		}
		numMovements = 0;
		idNextStep = 0;
		System.out.println("STARTING FINDER AGENT...");


		bfstate = new BFState(worldDim);  // Initialize state (matrix) of knowledge with '?'
		bfstate.printState();
	}


	/**
	 * Stores a reference to the Environment Object that will be used by the
	 * agent to interact with the BarcenasWorld class, by sending messages
	 * and getting answers for them. This function must be called before trying
	 * to perform any sort of computation by the agent.
	 *
	 * @param environment the Environment object.
	 **/
	public void setEnvironment(BarcenasWorldEnv environment) {
		envAgent = environment;
	}


	/**
	 * Loads a sequence of steps to be performed by the agent. This sequence will
	 * be stored in the listOfSteps ArrayList of the agent.  Steps are represented
	 * as objects of the class Position.
	 *
	 * @param numSteps  number of steps to read from the file.
	 * @param stepsFile name of the text file with the line that contains
	 *                  the sequence of steps: x1,y1 x2,y2 ...  xn,yn.
	 **/
	public void loadListOfSteps(int numSteps, String stepsFile) {
		String[] stepsList;
		String steps = ""; // Prepare a list of movements for the FINDER Agent to check
		try {
			BufferedReader br = new BufferedReader(new FileReader(stepsFile));
			System.out.println("STEPS FILE OPENED ...");
			steps = br.readLine();
			br.close();
		} catch (FileNotFoundException ex) {
			System.out.println("MSG.   => Steps file not found");
			exit(1);
		} catch (IOException ex) {
			Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
			exit(2);
		}
		stepsList = steps.split(" ");
		listOfSteps = new ArrayList<>(numSteps);
		for (int i = 0; i < numSteps; i++) {
			String[] coords = stepsList[i].split(",");
			listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
		}
		numMovements = listOfSteps.size(); // Initialization of numMovements
		idNextStep = 0;
	}


	/**
	 * Returns the current state of the agent.
	 *
	 * @return the current state of the agent, as an object of class BFState.
	 **/
	public BFState getState() {
		return bfstate;
	}


	/**
	 * Executes the next step in the sequence of steps of the agent, and then
	 * uses the agent sensors to get information from the environment. In the
	 * original Barcenas World, this would be to use the Smelll Sensor to get
	 * a binary answer, and then to update the current state according to the
	 * result of the logical inferences performed by the agent with its formula.
	 *
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 * @throws TimeoutException       if solver's isSatisfiable operation spends more
	 *                                time computing than a certain timeout.
	 **/
	public void runNextStep() throws ContradictionException, TimeoutException {
		// Ask to move and check whether it was successful
		processMoveAnswer(moveToNext());

		// Add the conclusions obtained in the previous step
		// but as clauses that use the "past" variables
		addLastFutureClausesToPastClauses();

		// Perform inference to discover new information
		processSoundSensorAnswer(soundsAt());
	}


	/**
	 * Asks the agent to move to the next position, by sending an appropriate
	 * message to the environment object. The answer returned by the environment
	 * will be returned to the caller of the function.
	 *
	 * @return the answer message from the environment, that will tell whether the
	 * movement was successful or not.
	 **/
	private AMessage moveToNext() {
		Position nextPosition;

		if (idNextStep < numMovements) {
			nextPosition = listOfSteps.get(idNextStep);
			idNextStep = idNextStep + 1;
			return moveTo(nextPosition.x, nextPosition.y);
		} else {
			System.out.println("NO MORE steps to perform at agent!");
			return (new AMessage("NOMESSAGE", "", ""));
		}
	}


	/**
	 * Uses agent "actuators" to move to (x,y)
	 * We simulate this by letting the World Agent (environment) know
	 * that we want to move, but we need the answer from it
	 * to be sure that the movement was successfully made.
	 *
	 * @param x horizontal coordinate of the movement to perform.
	 * @param y vertical coordinate of the movement to perform.
	 * @return returns the answer obtained from the environment object to the
	 * moveto message sent.
	 **/
	private AMessage moveTo(int x, int y) {
		// Let the EnvironmentAgentID know that we want to move
		AMessage msg, ans;

		msg = new AMessage("moveto", (new Integer(x)).toString(), (new Integer(y)).toString());
		ans = envAgent.acceptMessage(msg);
		System.out.println("FINDER => moving to : (" + x + "," + y + ")");

		return ans;
	}


	/**
	 * Processes the answer obtained from the environment when
	 * asked to perform a movement
	 *
	 * @param moveAns the answer given by the environment to the last move message
	 **/
	private void processMoveAnswer(AMessage moveAns) {
		if (moveAns.getComp(0).equals("movedto")) {
			agentX = Integer.parseInt(moveAns.getComp(1));
			agentY = Integer.parseInt(moveAns.getComp(2));
			System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")");
		}
	}


	/**
	 * Sends to the environment object the question:
	 * "Is there any sound at (agentX,agentY) ?".
	 *
	 * @return return the answer given by the environment.
	 **/
	private AMessage soundsAt() {
		AMessage msg, ans;
		msg = new AMessage("soundsat", (new Integer(agentX)).toString(),
			   (new Integer(agentY)).toString());
		ans = envAgent.acceptMessage(msg);
		System.out.println("FINDER => checking for sound at : (" + agentX + "," + agentY + ")");
		// Return answer obtained from environment object
		return ans;
	}


	/**
	 * Processes the answer obtained for the query "Is there any sound at
	 * (agentX,agentY)?" by adding the appropriate evidence clause/s
	 * to the formula and then performing the inference questions to discover
	 * new positions where Barcenas is NOT located.
	 *
	 * @param ans message obtained to the query "Is there any sound at (agentX,agentY)?".
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 * @throws TimeoutException       if solver's isSatisfiable operation spends more
	 *                                time computing than a certain timeout.
	 **/
	private void processSoundSensorAnswer(AMessage ans) throws
		   ContradictionException, TimeoutException {

		// Parse the answer received to get the new evidence
		String sounds = ans.getComp(0);
		int x = Integer.parseInt(ans.getComp(1));
		int y = Integer.parseInt(ans.getComp(2));

		// Add the evidence
		addSoundSensorEvidence(x, y, sounds);

		performInferenceQuestions();
		bfstate.printState(); // Printing resulting knowledge matrix
	}


	/**
	 * Adds the information obtained with the sound sensor
	 * as appropriate clauses to the formula of the agent (stored in solver).
	 *
	 * @param x      x coordinate of position.
	 * @param y      y coordinate of position.
	 * @param sounds direction/s answer to the question "Where does it sound if in (x,y) ?".
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private void addSoundSensorEvidence(int x, int y, String sounds) throws ContradictionException {
		System.out.println("Sound sensor returned: " + sounds);
		System.out.println("Inserting smell evidence");
		switch (sounds) {
			case "ABOVE,RIGHT":
				addCoordToFormula(x, y, +1, soundAboveOffset);
				addCoordToFormula(x, y, +1, soundRightOffset);
				break;

			case "ABOVE,LEFT":
				addCoordToFormula(x, y, +1, soundAboveOffset);
				addCoordToFormula(x, y, +1, soundLeftOffset);
				break;

			case "BELOW,RIGHT":
				addCoordToFormula(x, y, +1, soundBelowOffset);
				addCoordToFormula(x, y, +1, soundRightOffset);
				break;

			case "BELOW,LEFT":
				addCoordToFormula(x, y, +1, soundBelowOffset);
				addCoordToFormula(x, y, +1, soundLeftOffset);
				break;

			case "ABOVE":
				addCoordToFormula(x, y, +1, soundAboveOffset);
				// add missing clauses
				for (int i = 1; i <= worldDim; i++) {
					for (int j = y + 1; j <= worldDim; j++) {
						if (i != x) {
							addCoordToFormula(i, j, -1, barcenasFutureOffset);
						}
					}
				}
				break;

			case "BELOW":
				addCoordToFormula(x, y, +1, soundBelowOffset);
				// add missing clauses
				for (int i = 1; i <= worldDim; i++) {
					for (int j = y - 1; j > 0; j--) {
						if (i != x) {
							addCoordToFormula(i, j, -1, barcenasFutureOffset);
						}
					}
				}
				break;

			case "LEFT":
				addCoordToFormula(x, y, +1, soundLeftOffset);
				// add missing clauses
				for (int i = x - 1; i > 0; i++) {
					for (int j = 1; j <= worldDim; j++) {
						if (j != y) {
							addCoordToFormula(i, j, -1, barcenasFutureOffset);
						}
					}
				}
				break;

			case "RIGHT":
				addCoordToFormula(x, y, +1, soundRightOffset);
				// add missing clauses
				for (int i = x + 1; i <= worldDim; i++) {
					for (int j = 1; j <= worldDim; j++) {
						if (j != y) {
							addCoordToFormula(i, j, -1, barcenasFutureOffset);
						}
					}
				}
				break;

			default:  // case "ABOVE,BELOW,LEFT,RIGHT"
				System.out.println("FINDER => Barcenas found at current position");
				addBarcenasHereClauses(x, y);
				break;
		}
	}

	/**
	 * When sound sensor gathers sound in all possible positions that means Barcenas
	 * is as that certain current position. This method adds the clauses when the previous
	 * situation has happened.
	 *
	 * @param x x coordinate of position.
	 * @param y y coordinate of position.
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 */
	private void addBarcenasHereClauses(int x, int y) throws ContradictionException {
		for (int i = 1; i <= worldDim; i++) {
			for (int j = 1; j <= worldDim; j++) {
				if (x != i || y != j) {
					VecInt evidence = new VecInt();
					evidence.insertFirst(-(coordToLineal(i, j, barcenasFutureOffset)));
					solver.addClause(evidence);
				}
			}
		}
	}


	/* Given a coordinate (x,y), the sense for it and an offset,
	 * transforms the coordinate to a lineal literal and simply
	 * adds it to the formula.
	 *
	 * @param x      x coordinate.
	 * @param y	  y coordinate.
	 * @param sense  -1 or 1. Adds negative or positive sense to literal.
	 * @param offset offset associated with the subset of variables
	 * that literal belongs to.
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 */
	private void addCoordToFormula(int x, int y, int sense, int offset) throws ContradictionException {
		VecInt evidence = new VecInt();
		int eval;

		if (sense == +1) {
			eval = coordToLineal(x, y, offset);
			System.out.println("Adding: +" + eval + " literal to formula");
		} else {
			eval = -(coordToLineal(x, y, offset));
		}
		evidence.insertFirst(eval);
		solver.addClause(evidence);
	}


	/**
	 * Adds all the clauses stored in the list
	 * futureToPast to the formula stored in solver.
	 * Uses the function addClause( VecInt ) to add each clause to the solver.
	 *
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private void addLastFutureClausesToPastClauses() throws ContradictionException {
		if (futureToPast != null) {
			for (VecInt vecInt : futureToPast) {
				solver.addClause(vecInt);
			}
		}
	}


	/**
	 * Checks, using the future variables related
	 * to possible positions of Barcenas, whether it is a logical consequence
	 * that Barcenas is NOT at certain positions. The previous is checked for all the
	 * positions of the Barcenas World.
	 * The logical consequences obtained are then stored in the futureToPast list
	 * but using the variables corresponding to the "past" variables of the same positions.
	 * <p>
	 * An efficient version of this function should try to not add to the futureToPast
	 * conclusions that were already added in previous steps, although this will not produce
	 * any bad functioning in the reasoning process with the formula.
	 *
	 * @throws TimeoutException if solver's isSatisfiable operation spends more
	 *                          time computing than a certain timeout.
	 **/
	private void performInferenceQuestions() throws TimeoutException {
		futureToPast = new ArrayList<>();
		for (int i = 1; i <= worldDim; i++) {
			for (int j = 1; j <= worldDim; j++) {
				// Get variable number for position i,j in past variables
				int linealIndex = coordToLineal(i, j, barcenasFutureOffset);
				// Get the same variable, but in the past subset
				int linealIndexPast = coordToLineal(i, j, barcenasPastOffset);

				VecInt variablePositive = new VecInt();
				variablePositive.insertFirst(linealIndex);

				// Check if Gamma + variablePositive is unsatisfiable:
				if (!(solver.isSatisfiable(variablePositive))) {
					// Add conclusion to list, but rewritten with respect to "past" variables
					VecInt concPast = new VecInt();
					concPast.insertFirst(-(linealIndexPast));

					futureToPast.add(concPast);
					bfstate.set(i, j, "X");
				}
			}
		}
	}


	/**
	 * Builds the initial logical formula of the agent and stores it
	 * into the solver object.
	 *
	 * @return returns the solver object where the formula has been stored.
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private ISolver buildGamma() throws ContradictionException {
		int totalNumVariables;

		// You must set this variable to the total number of boolean variables
		// in your formula Gamma
		totalNumVariables = worldLinealDim * 2 + worldLinealDim * 4; // wDim * 6
		solver = SolverFactory.newDefault();
		solver.setTimeout(3600);
		solver.newVar(totalNumVariables);
		// This variable is used to generate, in a particular sequential order,
		// the variable indentifiers of all the variables
		currentLiteral = 1;

		pastBarcenas(); // Barcenas t-1, from 1,1 to n,n (1 clause)
		futureBarcenas(); // Barcenas t+1, from 1,1 to n,n (1 clause)
		pastBarcenasToFutureBarcenas(); // Barcenas t-1 -> Barcenas t+1 (nxn clauses)
		// smellsImplications(   ); // Smells implications (nxnxnxn clauses)
		soundImplications(); // Sound sensor implications (nxnxnxn clauses)

		notInFirstPosition(); // Not in the 1,1 clauses (2 clauses)

		return solver;
	}


	/**
	 * Adds the clause that says that Barcenas must be in some position
	 * with respect to the variables that talk about past positions.
	 *
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private void pastBarcenas() throws ContradictionException {
		barcenasPastOffset = currentLiteral;
		VecInt pastClause = new VecInt();
		for (int i = 0; i < worldLinealDim; i++) {
			pastClause.insertFirst(currentLiteral);
			currentLiteral++;
		}
		solver.addClause(pastClause);
	}


	/**
	 * Adds the clause that says that Barcenas must be in some position
	 * with respect to the variables that talk about future positions.
	 *
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private void futureBarcenas() throws ContradictionException {
		barcenasFutureOffset = currentLiteral;
		VecInt futureClause = new VecInt();
		for (int i = 0; i < worldLinealDim; i++) {
			futureClause.insertFirst(currentLiteral);
			currentLiteral++;
		}
		solver.addClause(futureClause);
	}


	/**
	 * Adds the clauses that say that if in the past we reached the conclusion
	 * that Barcenas cannot be in a position (x,y), then this should be also true
	 * in the future.
	 *
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private void pastBarcenasToFutureBarcenas() throws ContradictionException {
		for (int i = 0; i < worldLinealDim; i++) {
			VecInt clause = new VecInt();
			clause.insertFirst(i + 1);
			clause.insertFirst(-(i + barcenasFutureOffset));
			solver.addClause(clause);
		}
	}


	/**
	 * Adds the clauses that say that Barcenas can never (past and future) be in
	 * the first position.
	 *
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private void notInFirstPosition() throws ContradictionException {
		VecInt notInFuture = new VecInt();
		VecInt notInPast = new VecInt();
		notInFuture.insertFirst(-barcenasFutureOffset);
		notInPast.insertFirst(-barcenasPastOffset);
		solver.addClause(notInFuture);
		solver.addClause(notInPast);
	}


	/**
	 * Adds the clauses related to implications between sound sensor evidence and
	 * not possible positions of Barcenas.
	 *
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 **/
	private void soundImplications() throws ContradictionException {
		// Store the identifier for the first variable of the
		// sounds subset of variables

		// Generate and store all the implications between sound
		// sensor evidence and not possible positions of Barcenas
		for (int k = 1; k <= 4; k++) {
			for (int i = 1; i <= worldDim; i++) {
				for (int j = 1; j <= worldDim; j++) {
					if (k == 1) {
						if (soundAboveOffset == 0) {
							soundAboveOffset = currentLiteral;
						}
						insertSoundAboveImplications(i, j);
						currentLiteral++;

					} else if (k == 2) {
						if (soundBelowOffset == 0) {
							soundBelowOffset = currentLiteral;
						}
						insertSoundBelowImplications(i, j);
						currentLiteral++;

					} else if (k == 3) {
						if (soundLeftOffset == 0) {
							soundLeftOffset = currentLiteral;
						}
						insertSoundLeftImplications(i, j);
						currentLiteral++;

					} else {
						if (soundRightOffset == 0) {
							soundRightOffset = currentLiteral;
						}
						insertSoundRightImplications(i, j);
						currentLiteral++;
					}
				}
			}
		}
	}


	/**
	 * Adds to formula the clauses related to implications between the sound sensor
	 * and not possible positions of Barcenas when the sound is heard above.
	 *
	 * @param x x coordinate related to the sound literal.
	 * @param y y coordinate related to the sound literal.
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 */
	private void insertSoundAboveImplications(int x, int y) throws ContradictionException {
		for (int k = 1; k <= worldDim; k++) {
			for (int l = y; l > 0; l--) {
				// literal related to the sound sensor
				int implicationFirstPart = coordToLineal(x, y, soundAboveOffset);
				//int implicationFirstPart = currentLiteral;
				// literal related to Barcenas not possible position
				int implicationSecondPart = -(coordToLineal(k, l, barcenasFutureOffset));
				addImplicationToFormula(implicationFirstPart, implicationSecondPart);
			}
		}
	}


	/**
	 * Adds to formula the clauses related to implications between the sound sensor
	 * and not possible positions of Barcenas when the sound is heard below.
	 *
	 * @param x x coordinate related to the sound literal.
	 * @param y y coordinate related to the sound literal.
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 */
	private void insertSoundBelowImplications(int x, int y) throws ContradictionException {
		for (int k = 1; k <= worldDim; k++) {
			for (int l = y; l <= worldDim; l++) {
				// literal related to the sound sensor
				int implicationFirstPart = coordToLineal(x, y, soundBelowOffset);
				// int implicationFirstPart = currentLiteral;
				// literal related to Barcenas not possible position
				int implicationSecondPart = -(coordToLineal(k, l, barcenasFutureOffset));
				addImplicationToFormula(implicationFirstPart, implicationSecondPart);
			}
		}
	}


	/**
	 * Adds to formula the clauses related to implications between the sound sensor
	 * and not possible positions of Barcenas when the sound is heard on the left.
	 *
	 * @param x x coordinate related to the sound literal.
	 * @param y y coordinate related to the sound literal.
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 */
	private void insertSoundLeftImplications(int x, int y) throws ContradictionException {
		for (int k = x; k <= worldDim; k++) {
			for (int l = 1; l <= worldDim; l++) {
				// literal related to the sound sensor
				int implicationFirstPart = coordToLineal(x, y, soundLeftOffset);
				// int implicationFirstPart = currentLiteral;
				// literal related to Barcenas not possible position
				int implicationSecondPart = -(coordToLineal(k, l, barcenasFutureOffset));
				addImplicationToFormula(implicationFirstPart, implicationSecondPart);
			}
		}
	}


	/**
	 * Adds to formula the clauses related to implications between the sound sensor
	 * and not possible positions of Barcenas when the sound is heard on the right.
	 *
	 * @param x x coordinate related to the sound literal.
	 * @param y y coordinate related to the sound literal.
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 */
	private void insertSoundRightImplications(int x, int y) throws ContradictionException {
		for (int k = x; k > 0; k--) {
			for (int l = 1; l <= worldDim; l++) {
				// literal related to the sound sensor
				int implicationFirstPart = coordToLineal(x, y, soundRightOffset);
				// int implicationFirstPart = currentLiteral;
				// literal related to Barcenas not possible position
				int implicationSecondPart = -(coordToLineal(k, l, barcenasFutureOffset));
				addImplicationToFormula(implicationFirstPart, implicationSecondPart);
			}
		}
	}


	/**
	 * Adds the implication: firstPart -> secondPart to formula.
	 *
	 * @param firstPart  first part of the implication (before the arrow ->).
	 * @param secondPart second part of the implication (after the arrow ->).
	 * @throws ContradictionException if inserting contradictory clauses in formula (solver).
	 */
	private void addImplicationToFormula(int firstPart, int secondPart) throws ContradictionException {
		VecInt implication = new VecInt();
		implication.insertFirst(-(firstPart));
		implication.insertFirst(secondPart);
		solver.addClause(implication);
	}


	/**
	 * Converts a coordinate pair (x,y) to the integer value  b_[x,y]
	 * of variable that stores that information in the formula, using
	 * offset as the initial index for that subset of position variables
	 * (past and future position variables have different variables,
	 * so different offset values).
	 *
	 * @param x      x coordinate of the position variable to encode.
	 * @param y      y coordinate of the position variable to encode.
	 * @param offset initial value for the subset of position variables
	 *               (past or future subset).
	 * @return the integer identifier of the variable  b_[x,y] in the formula.
	 **/
	public int coordToLineal(int x, int y, int offset) {
		return ((x - 1) * worldDim) + (y - 1) + offset;
	}


	/**
	 * Performs the inverse computation of the previous function.
	 * That is, from the identifier b_[x,y] to the coordinates  (x,y)
	 * that it represents.
	 *
	 * @param lineal identifier of the variable.
	 * @param offset offset associated with the subset of variables that
	 *               lineal belongs to.
	 * @return array with x and y coordinates.
	 **/
	public int[] linealToCoord(int lineal, int offset) {
		lineal = lineal - offset + 1;
		int[] coords = new int[2];
		coords[1] = ((lineal - 1) % worldDim) + 1;
		coords[0] = (lineal - 1) / worldDim + 1;
		return coords;
	}

}
