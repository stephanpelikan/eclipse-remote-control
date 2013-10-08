package com.github.marook.eclipse_remote_control.run.runner.impl.simple.atom;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.github.marook.eclipse_remote_control.command.command.Command;
import com.github.marook.eclipse_remote_control.command.command.RemoteDebugCommand;

public class RemoteDebugCommandRunner extends AbstractAtomCommandRunner
		implements IDebugEventSetListener {
	
	public static class ProgressMonitor implements IProgressMonitor {

		private Object lock;
		
		private boolean canceled = false;
		
		public ProgressMonitor(final Object lock) {
			this.lock = lock;
		}
		
		@Override
		public void beginTask(String arg0, int arg1) {
			// nothing to do
		}

		@Override
		public void done() {
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void internalWorked(double arg0) {
			// nothing to do
		}

		@Override
		public boolean isCanceled() {
			return canceled;
		}

		@Override
		public void setCanceled(boolean canceled) {
			this.canceled = canceled;
		}

		@Override
		public void setTaskName(String arg0) {
			// ignore
		}

		@Override
		public void subTask(String arg0) {
			// ignore
		}

		@Override
		public void worked(int arg0) {
			// ignore
		}
		
	}
	
	public static class RemoteDebugRunner implements Runnable {
		
		private ILaunch launch = null;
		
		private Exception thrownException = null;
		
		private ILaunchConfiguration launchConfiguration = null;
		
		public RemoteDebugRunner(final ILaunchConfiguration launchConfiguration) {
			this.launchConfiguration = launchConfiguration;
		}
		
		@Override
		public void run() {
			
			/*
			 * launch debugger
			 */
			try {
				launch = launchConfiguration.launch(ILaunchManager.DEBUG_MODE, new ProgressMonitor(this));
			} catch (Exception e) {
				thrownException = e;
			}
				
		}
		
		public ILaunch getLaunch() {
			return launch;
		}
		
		public Exception getThrownException() {
			return thrownException;
		}
		
	}
	
	private ILaunch launch = null;

	private String resultMsg = null;
	
	public RemoteDebugCommandRunner() {
		super(RemoteDebugCommand.ID);
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected String internalExecute(final Command cmd) throws Exception {
		
		final DebugPlugin debugPlugin = DebugPlugin.getDefault();
		
		final ILaunchManager manager = debugPlugin.getLaunchManager();

		/*
		 * terminate current session
		 */
		terminateExistingDebugSession(manager);
		
		final RemoteDebugCommand c = (RemoteDebugCommand) cmd;
		final String requestedCfgName = c.getConfigurationName();
		
		boolean found = false;
		for(final ILaunchConfiguration cfg : manager.getLaunchConfigurations()){
			final String cfgName = cfg.getName();
			
			if(!requestedCfgName.equals(cfgName)) continue;
			found = true;
			
			/*
			 * set defaults
			 */
			if ((c.getPort() == null) || (c.getPort().trim().length() == 0)) {
				c.setPort("8000");
			}
			if ((c.getHostname() == null) || (c.getHostname().trim().length() == 0)) {
				c.setHostname("localhost");
			}
			
			/*
			 * Build a copy of launch config to adapt some attributes
			 */
			ILaunchConfigurationWorkingCopy newInstance = cfg.getWorkingCopy();
			
			// see http://www.docjar.com/html/api/org/eclipse/jdt/internal/launching/SocketAttachConnector.java.html
			Map argMap = newInstance.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, new HashMap());
			argMap.put("port", c.getPort());
			argMap.put("hostname", c.getHostname());
			newInstance.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, argMap);
			
			/*
			 * get launch timeout
			 */
			String timeoutString = (String)argMap.get("timeout");
			long launchTimeout = 20000;
			if ((timeoutString != null) && (timeoutString.trim().length() != 0)) {
				launchTimeout = Long.parseLong(timeoutString);
			}
			
			/*
			 * adapt classpath
			 */
			//newInstance.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, classPathList);
			
			/*
			 * Add event listener for cleaning up
			 */
			debugPlugin.addDebugEventListener(this);
			
			/*
			 * save changes
			 */
			newInstance.doSave();
			
			/*
			 * show debug perspective
			 */
			boolean showDebugPerspective = Platform.getPreferencesService().getBoolean(
					DebugPlugin.getUniqueIdentifier(),
					IDebugUIConstants.PREF_SHOW_DEBUG_PERSPECTIVE_DEFAULT, true, null); 
			if (showDebugPerspective) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							PlatformUI.getWorkbench().showPerspective(IDebugUIConstants.ID_DEBUG_PERSPECTIVE,
									PlatformUI.getWorkbench().getActiveWorkbenchWindow());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
			
			/*
			 * launch debugger in brackground
			 */
			RemoteDebugRunner runner = new RemoteDebugRunner(newInstance);
			synchronized (runner) {

				// start thread
				Display.getDefault().asyncExec(runner);
				
				try {
					runner.wait(launchTimeout + 2000); // add two seconds to be sure to get errors
				} catch (InterruptedException e) {
					// ignore
				}
				
				if (runner.getThrownException() != null) {
					resultMsg = runner.getThrownException().getMessage();
				}
				
			}
			
			break;
		}
		
		if (!found && (resultMsg == null)) {
			resultMsg = "run-profile '" + c.getConfigurationName() + "' not found!";
		}
		
		return resultMsg;
		
	}

	private void terminateExistingDebugSession(final ILaunchManager manager) throws DebugException {
		if (launch != null) {
			try {
				launch.terminate();
			} finally {
				ILaunch oldLaunch = launch;
				launch = null;
				resultMsg = null;
				try {
					manager.removeLaunch(oldLaunch);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		
		if (events == null) {
			return;
		}
		
		for (DebugEvent event : events) {
			
			if (DebugEvent.TERMINATE == event.getKind()) {
				final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
				try {
					terminateExistingDebugSession(launchManager);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}

}