package com.github.marook.eclipse_remote_control;

public class ShutdownThread extends Thread {

	private CommandServerWorker worker;
	
	public ShutdownThread(CommandServerWorker worker) {
		this.worker = worker;
	}
	
	@Override
	public void run() {
		worker.shutdown();
	}
	
}
