package apryraz.bworld;

/**
 * Class for representing the Barcenas Finder problem
 * using a canvas and a visual interface.
 **/
public class BFState {

	/**
	 * World dimension, that means as many rows as
	 * wDim and as many columns as wDim. So wDim x wDim cells or
	 * possible positions where Barcenas could be.
	 */
	int wDim;
	/**
	 * Matrix representing a visual approach for the Barcenas
	 * Finder problem and the current state of the problem.
	 * Cells will be filled with '?' when Barcenas could be in that
	 * position (cell) and with 'X' when Barcenas is cannot be in
	 * that certain position (cell).
	 */
	String[][] matrix;


	/**
	 * Class constructor.
	 *
	 * @param dim number of columns and rows for the Barcenas
	 *            Finder problem.
	 */
	public BFState(int dim) {
		wDim = dim;
		matrix = new String[wDim][wDim];
		initializeState();
	}


	/**
	 * Initializes the matrix representing the state of the 'Barcenas World'.
	 */
	public void initializeState() {
		for (int i = 0; i < wDim; i++) {
			for (int j = 0; j < wDim; j++) {
				matrix[i][j] = "?";
			}
		}
	}


	/**
	 * Sets a cell to the specified value.
	 *
	 * @param i   x coordinate to the cell to set.
	 * @param j   y coordinate to the cell to set.
	 * @param val value to set.
	 */
	public void set(int i, int j, String val) {

		matrix[i - 1][j - 1] = val;
	}


	/**
	 * Compares two BFS states (Barcenas world states),
	 * that means two matrixes and its cells.
	 *
	 * @param obj other BFState object to compare.
	 * @return true if both are equal, false otherwise.
	 */
	public boolean equals(Object obj) {
		BFState bfstate2 = (BFState) obj;
		boolean status = true;

		for (int i = 0; i < wDim; i++) {
			for (int j = 0; j < wDim; j++) {
				if (!matrix[i][j].equals(bfstate2.matrix[i][j]))
					status = false;
			}
		}

		return status;
	}


	/**
	 * Prints Barcenas world matrix.
	 */
	public void printState() {
		System.out.println("FINDER => Printing Barcenas world matrix");
		for (int i = wDim - 1; i > -1; i--) {
			System.out.print("\t#\t");
			for (int j = 0; j < wDim; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println("\t#");
		}
	}

}
