package com.github.marook.eclipse_remote_control.client.parser;

import com.github.marook.eclipse_remote_control.command.command.Command;
import com.github.marook.eclipse_remote_control.command.command.RemoteDebugCommand;

public class RemoteDebugParser extends CommandParser {

	@Override
	public String getName() {
		return "remotedebug_command";
	}

	@Override
	public String getUsage() {
		return "[command memento] [port] [hostname]";
	}

	@Override
	public Command parseCommand(final String[] args) {
		if(args.length < 2 || args.length > 4){
			throw new CommandParseException("Expected 2 to 4 argument.");
		}
		
		final RemoteDebugCommand cmd = new RemoteDebugCommand();
		cmd.setConfigurationName(args[1]);
		
		if(args.length >= 3) {
			cmd.setPort(args[2]);
		}
		if (args.length >= 4) {
			cmd.setHostname(args[3]);
		}
		
		return cmd;
	}

}