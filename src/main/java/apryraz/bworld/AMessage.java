
package apryraz.bworld;

/**
 * Class for representing messages exchanged between agents and the
 * World interface object.
 **/
public class AMessage {
	/*
	 *  Array of String objects, that represents the different fields of each message.
	 *  So far, we assume a fixed pattern, with always three fields in any message:
	 *  field0:  message type: moveto, movedto, notmovedto, smellat, YES/NO  (smell answer)
	 *  field1:  first parameter of message
	 *  field2:  second parameter of message
	 */
	String[] msg;

	/**
	 * Class constructor.
	 *
	 * @param msgtype message type.
	 * @param par1    first parameter of message.
	 * @param par2    second parameter of message.
	 **/
	public AMessage(String msgtype, String par1, String par2) {
		msg = new String[3];

		msg[0] = msgtype;
		msg[1] = par1;
		msg[2] = par2;
	}

	/**
	 * Shows message on screen.
	 **/
	public void showMessage() {
		System.out.println("MESSAGE: " + msg[0] + " " + msg[1] + " " + msg[2]);
	}

	/**
	 * Gets some part of the message.
	 *
	 * @param c index of the component to return.
	 * @return the String corresponding to the component requested.
	 **/
	public String getComp(int c) {
		return msg[c];
	}

}
