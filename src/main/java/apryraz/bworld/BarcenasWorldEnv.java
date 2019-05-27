
package apryraz.bworld;

public class BarcenasWorldEnv {
	/**
	 * X,Y position of Barcenas and world dimension.
	 **/
	int BarcenasX, BarcenasY, WorldDim;


	/**
	 * Class constructor.
	 *
	 * @param dim dimension of the world.
	 * @param bx  X position of Barcenas.
	 * @param by  Y position of Barcenas.
	 **/
	public BarcenasWorldEnv(int dim, int bx, int by) {

		BarcenasX = bx;
		BarcenasY = by;
		WorldDim = dim;
	}


	/**
	 * Process a message received by the BFinder agent,
	 * by returning an appropriate answer.
	 *
	 * @param msg message sent by the Agent.
	 * @return a msg with the answer to return to the agent.
	 **/
	public AMessage acceptMessage(AMessage msg) {
		AMessage ans = new AMessage("voidmsg", "", "");

		msg.showMessage();
		if (msg.getComp(0).equals("moveto")) {
			int nx = Integer.parseInt(msg.getComp(1));
			int ny = Integer.parseInt(msg.getComp(2));
			if (withinLimits(nx, ny))
				ans = new AMessage("movedto", msg.getComp(1), msg.getComp(2));
			else
				ans = new AMessage("notmovedto", msg.getComp(1), msg.getComp(2));

		} else {
			if (msg.getComp(0).equals("soundsat")) {
				int nx = Integer.parseInt(msg.getComp(1));
				int ny = Integer.parseInt(msg.getComp(2));
				String barcenasWay = returnBarcenasDirection(nx, ny);
				ans = new AMessage(barcenasWay, msg.getComp(1), msg.getComp(2));
			}
		}
		return ans;

	}

	/**
	 * Check if position x,y is within the limits of the
	 * WorldDim x WorldDim   world.
	 *
	 * @param x x coordinate of agent position.
	 * @param y y coordinate of agent position.
	 * @return true if (x,y) is within the limits of the world.
	 **/
	private boolean withinLimits(int x, int y) {
		return (x >= 1 && x <= WorldDim && y >= 1 && y <= WorldDim);
	}


	/***
	 * Returns which way or ways Barcenas is located according
	 * to given arguments (x,y):
	 *
	 * @param x x coordinate of agent position.
	 * @param y y coordinate of agent position.
	 * @return a string that contains which way Barcenas is located, i.e "TOP,LEFT".
	 */
	private String returnBarcenasDirection(int x, int y) {
		if (y < BarcenasY && x < BarcenasX) {
			return "ABOVE,RIGHT";
		} else if (y < BarcenasY && x > BarcenasX) {
			return "ABOVE,LEFT";
		} else if (y > BarcenasY && x < BarcenasX) {
			return "BELOW,RIGHT";
		} else if (y > BarcenasY && x > BarcenasX) {
			return "BELOW,LEFT";
		} else if (y < BarcenasY) {
			return "ABOVE";
		} else if (y > BarcenasY) {
			return "BELOW";
		} else if (x > BarcenasX) {
			return "LEFT";
		} else if (x < BarcenasX) {
			return "RIGHT";
		} else {
			return "ABOVE,BELOW,LEFT,RIGHT";
		}
	}

}
