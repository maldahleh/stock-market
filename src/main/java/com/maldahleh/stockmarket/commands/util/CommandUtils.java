package com.maldahleh.stockmarket.commands.util;

import com.maldahleh.stockmarket.utils.Utils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandUtils {

  /**
   * The minimum number of args we need to look up a quantity.
   * This is because the user supplied quantity is always the 2nd index (3rd argument).
   */
  private final int MIN_ARGS_QUANTITY = 3;
  private final int QUANTITY_ARG_INDEX = 2;

  public final int INVALID_QUANTITY = -1;
  private final int DEFAULT_QUANTITY = 1;

  private final String OTHER_SUFFIX = ".other";

  public String buildOtherPermission(String basePermission) {
    return basePermission + OTHER_SUFFIX;
  }

  public int determineQuantity(String[] args) {
    if (args.length != MIN_ARGS_QUANTITY) {
      return DEFAULT_QUANTITY;
    }

    Integer quantity = Utils.getInteger(args[QUANTITY_ARG_INDEX]);
    if (quantity == null || quantity <= 0) {
      return INVALID_QUANTITY;
    }

    return quantity;
  }
}
