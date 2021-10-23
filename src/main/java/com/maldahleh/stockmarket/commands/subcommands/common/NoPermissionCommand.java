package com.maldahleh.stockmarket.commands.subcommands.common;

public abstract class NoPermissionCommand extends BaseCommand {

  @Override
  public String requiredPerm() {
    return null;
  }
}
