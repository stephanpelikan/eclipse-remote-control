package com.github.marook.eclipse_remote_control.command.command;

public class RemoteDebugCommand extends Command {
	private static final long serialVersionUID = 1L;

	public static final String ID = ExternalToolsCommand.class.getName();
	
	private String configurationName;
	private String port;
	private String hostname;

	public RemoteDebugCommand() {
		super(ID);
	}
	
	public String getConfigurationName() {
		return configurationName;
	}
	
	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}
	
	public String getPort() {
		return port;
	}
	
	public void setPort(String port) {
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}
	
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
}
