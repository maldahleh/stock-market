package com.maldahleh.stockmarket.commands.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandUtils {

  private final String OTHER_SUFFIX = ".other";

  public String buildOtherPermission(String basePermission) {
    return basePermission + OTHER_SUFFIX;
  }
}
