/*
 * Copyright 2010 Markus Pielmeier
 *
 * This file is part of eclipse remote control.
 *
 * eclipse remote control is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * eclipse remote control is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eclipse remote control.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.marook.eclipse_remote_control.client;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Arrays;

import com.github.marook.eclipse_remote_control.client.parser.CommandParseException;
import com.github.marook.eclipse_remote_control.client.parser.CommandParser;
import com.github.marook.eclipse_remote_control.client.parser.ExecuteCommandParser;
import com.github.marook.eclipse_remote_control.client.parser.OpenFileCommandParser;
import com.github.marook.eclipse_remote_control.client.parser.RemoteDebugParser;
import com.github.marook.eclipse_remote_control.command.command.Command;
import com.github.marook.eclipse_remote_control.command.command.ExternalToolsCommand;
import com.github.marook.eclipse_remote_control.command.command.OpenFileCommand;
import com.github.marook.eclipse_remote_control.command.serialize.ICommandEncoder;
import com.github.marook.eclipse_remote_control.command.serialize.impl.serialize.SerializeCommandDecoder;
import com.github.marook.eclipse_remote_control.command.serialize.impl.serialize.SerializeCommandEncoder;

public class Client {
	
	private static final int PORT = 53343;
	
	private CommandParser[] PARSERS = new CommandParser[]{
		new OpenFileCommandParser(),
		new ExecuteCommandParser(),
		new RemoteDebugParser()
	};

	private void printUsage(final PrintStream out){
		out.println("Possible commands are:");
		
		for(final CommandParser parser : PARSERS){
			out.print("  ");
			out.print(parser.getName());
			out.print(" ");
			out.println(parser.getUsage());
		}
	}
	
	public boolean fireCmd(final Command cmd) throws IOException{
		return fireCommand(cmd);
	}

	/**
	 * @deprecated Instantiate Client and run fireCmd instead.
	 */
	public static boolean fireCommand(final Command cmd) throws IOException{
		final Socket s = new Socket("localhost", PORT);
			
		final ICommandEncoder ce = new SerializeCommandEncoder();
		final ObjectOutput cmdEncoder = ce.createEncoder(s.getOutputStream());
		cmdEncoder.writeObject(cmd);
		cmdEncoder.flush();
		
		SerializeCommandDecoder decoder = new SerializeCommandDecoder();
		ObjectInput in = decoder.createDecoder(s.getInputStream());
		boolean hasMsg = in.readBoolean();
		String msg = null;
		if (hasMsg) {
			msg = in.readUTF();
		}
				
		boolean success = true;
		if (msg != null) {
			System.err.println(msg);
			success = false;
		}

		s.close();
		
		return success;
	}
	
	private static void handleCommandRunError(final String[] args, final Exception e){
		throw new RuntimeException("Can't run command " + Arrays.toString(args), e);
	}
	
	private void run(final String[] args){
		if(args.length < 1){
			printUsage(System.err);
			
			System.exit(1);
			
			return;
		}
		
		final String command = args[0];
		
		for(final CommandParser parser : PARSERS){
			if(!parser.getName().equals(command)){
				continue;
			}
			
			final Command cmd;
			try{ 
				cmd = parser.parseCommand(args);
			}
			catch(final CommandParseException e){
				System.err.println(e.getMessage());
				printUsage(System.err);
				
				System.exit(1);
				
				return;
			}
			
			boolean success = false;
			try {
				success = fireCmd(cmd);
			}
			catch(final ConnectException e) {
				System.err.println("Could not connect to Eclipse on 'localhost:" + PORT + "'! "
						+ "Maybe plugin is not installed or Eclipse is not running.");
				System.exit(2);
			}
			catch(final Exception e){
				handleCommandRunError(args, e);
			}
			
			if (!success) {
				System.exit(1);
			} else {
				System.exit(0);
			}
		}
		
		// not command could be identified
		System.err.println("Unknown command: " + command);
		printUsage(System.err);
		System.exit(1);
	}
	
	public static void main(final String[] args) {
		new Client().run(args);
	}

	/**
	 * @deprecated Instantiate ExternalToolsCommand and run fireCmd instead.
	 */
	public static void runExternalTool(final String configurationName) throws IOException {
		final ExternalToolsCommand cmd = new ExternalToolsCommand();
		cmd.setConfigurationName(configurationName);
		
		fireCommand(cmd);
	}

	/**
	 * @deprecated Instantiate OpenFileCommand and run fireCmd instead.
	 */
	public static void openFile(final String fileName, final int lineNumber) throws IOException {
		final OpenFileCommand cmd = new OpenFileCommand();
		cmd.setFileName(fileName);
		cmd.setLineNumber(lineNumber);
		
		fireCommand(cmd);
	}

}
