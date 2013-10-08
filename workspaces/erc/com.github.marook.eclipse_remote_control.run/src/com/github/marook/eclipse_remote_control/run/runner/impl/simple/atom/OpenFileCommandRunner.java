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
 * 
 * ----
 * Go to line by Bedrich Hovorka with using code from LogBack.
 */

package com.github.marook.eclipse_remote_control.run.runner.impl.simple.atom;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.github.marook.eclipse_remote_control.command.command.Command;
import com.github.marook.eclipse_remote_control.command.command.OpenFileCommand;

public class OpenFileCommandRunner extends AbstractAtomCommandRunner {
	
	private static final String JAVA_EDITOR_ID = "org.eclipse.jdt.internal.ui.javaeditor.JavaEditor"; //$NON-NLS-1$

	public OpenFileCommandRunner() {
		super(OpenFileCommand.ID);
	}
	
	//this method comes from http://logback.qos.ch/dist/ch.qos.logback.eclipse_1.1.0.zip
	private static void openInEditor(IWorkbenchPage page, IFile file, int lineNumber) {
		final HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
		map.put(IDE.EDITOR_ID_ATTR, JAVA_EDITOR_ID);
		try {
			final IMarker marker = file.createMarker(IMarker.TEXT);
			marker.setAttributes(map);
			IDE.openEditor(page, marker);
			marker.delete();
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String internalExecute(final Command cmd) throws Exception {
		final OpenFileCommand c = (OpenFileCommand) cmd;
		
		final IFile file = javaFileToPluginFile(c.getFileName());
		if (file != null){
			// it seems that file is in the workspace
			
			final IWorkbench workbench = PlatformUI.getWorkbench();
		    workbench.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					final IWorkbenchWindow activeWorkbenchWindow =
						workbench.getActiveWorkbenchWindow();
					final IWorkbenchPage page =
				    	activeWorkbenchWindow.getActivePage();
				 
				    openInEditor(page, file, c.getLineNumber());
				}
			});
		    
		    return null;
		}
		
		// here comes the alternative approach: open file through EFS local fs 
		final File f = new File(c.getFileName());
		final IFileStore fileStore =
			EFS.getLocalFileSystem().getStore(f.toURI());
	    final IWorkbench workbench = PlatformUI.getWorkbench();
	    workbench.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				final IWorkbenchWindow activeWorkbenchWindow =
					workbench.getActiveWorkbenchWindow();
				final IWorkbenchPage page =
			    	activeWorkbenchWindow.getActivePage();

			    try {
					IDE.openEditorOnFileStore(page, fileStore);
				}
			    catch(final PartInitException e) {
					// TODO handle error
					e.printStackTrace();
				}
			}
		});
	    
	    return null;
	}

	private IFile javaFileToPluginFile(String javaFile) {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (!project.isOpen()) continue; // TODO opening of project ??
			
			if (javaFile.startsWith('/' + project.getName())) {
				javaFile = javaFile.substring(project.getName().length() + 1);
			} else if (javaFile.startsWith("/")) { // absolute system path
				final String projectPath = project.getLocation().toOSString();
				if (javaFile.length() < projectPath.length()) continue;
				javaFile = javaFile.substring(projectPath.length());
			}
			
			IFile file = project.getFile(javaFile);
			if (file.exists()) {
				return file;
			}
    	}
		return null;
	}

}
