package com.maldahleh.stockmarket.commands.util;

import com.maldahleh.stockmarket.utils.Utils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandUtils {

  private final String OTHER_SUFFIX = ".other";

  public String buildOtherPermission(String basePermission) {
    return basePermission + OTHER_SUFFIX;
  }

  public int determineQuantity(String[] args) {
    if (args.length != 3) {
      return 1;
    }

    Integer quantity = Utils.getInteger(args[2]);
    if (quantity == null || quantity <= 0) {
      return -1;
    }

    return quantity;
  }
}
