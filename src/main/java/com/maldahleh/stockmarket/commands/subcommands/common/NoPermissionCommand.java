package com.maldahleh.stockmarket.commands.subcommands.common;

import com.maldahleh.stockmarket.config.Messages;

public abstract class NoPermissionCommand extends BaseCommand {

  protected NoPermissionCommand(Messages messages) {
    super(messages);
  }

  @Override
  public String requiredPerm() {
    return null;
  }
}
