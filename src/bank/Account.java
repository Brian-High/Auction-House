package bank;

import java.util.HashMap;

/**
 * The Account is a record which can not be overwritten so account updates manually recreate the account wit the right
 * amount
 */

public record Account(String User, double Account, int ID, int Type, HashMap<Integer, Double> Holds) {
}

