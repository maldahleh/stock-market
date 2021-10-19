package com.maldahleh.stockmarket.commands.util;

import com.maldahleh.stockmarket.utils.Utils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandUtils {

  public final int INVALID_QUANTITY = -1;
  private final int DEFAULT_QUANTITY = 1;

  private final String OTHER_SUFFIX = ".other";

  public String buildOtherPermission(String basePermission) {
    return basePermission + OTHER_SUFFIX;
  }

  public int determineQuantity(String[] args) {
    if (args.length != 3) {
      return DEFAULT_QUANTITY;
    }

    Integer quantity = Utils.getInteger(args[2]);
    if (quantity == null || quantity <= 0) {
      return INVALID_QUANTITY;
    }

    return quantity;
  }
}
