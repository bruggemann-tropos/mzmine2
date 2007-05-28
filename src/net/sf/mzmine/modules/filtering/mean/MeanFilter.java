/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.filtering.mean;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.DataProcessingMethod;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * This class represent a method for filtering scans in raw data file with
 * moving average filter.
 */
public class MeanFilter implements DataProcessingMethod, TaskListener, ListSelectionListener,
        ActionListener {

    public static final Parameter parameterOneSidedWindowLength = new SimpleParameter(
            ParameterType.DOUBLE, "Window length",
            "One-sided length of the smoothing window", "Da", new Double(0.1),
            new Double(0.0), null);

    private ParameterSet parameters;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        parameters = new SimpleParameterSet(
                new Parameter[] { parameterOneSidedWindowLength });

        myMenuItem = desktop.addMenuItem(MZmineMenu.FILTERING,
                "Mean filter spectra", this, null, KeyEvent.VK_M, false, false);

        desktop.addSelectionListener(this);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == myMenuItem) {
            ParameterSet param = setupParameters(parameters);
            if (param == null)
                return;
            OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();
            runMethod(dataFiles, null, param, null);
        }
    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        myMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    /**
     * @see net.sf.mzmine.modules.DataProcessingMethod#toString()
     */
    public String toString() {
        return "Moving average filter";
    }

    /**
     * @see net.sf.mzmine.modules.DataProcessingMethod#setupParameters()
     */
    public ParameterSet setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check parameter values for "
                        + toString(), (SimpleParameterSet) currentParameters);
        dialog.setVisible(true);
        if (dialog.getExitCode() == ExitCode.CANCEL)
            return null;
        return currentParameters.clone();
    }

    /**
     * @see net.sf.mzmine.modules.DataProcessingMethod#runMethod(net.sf.mzmine.io.OpenedRawDataFile[],
     *      net.sf.mzmine.data.AlignmentResult[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runMethod(OpenedRawDataFile[] dataFiles,
            AlignmentResult[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        // prepare a new sequence of tasks
        Task tasks[] = new MeanFilterTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new MeanFilterTask(dataFiles[i], parameters);
        }
        TaskGroup newSequence = new TaskGroup(tasks, this,
                methodListener, taskController);

        // execute the sequence
        newSequence.run();
        
        return newSequence;

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        logger.info("Running mean filter on "
                + ((MeanFilterTask) task).getDataFile());
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished mean filter on "
                    + ((MeanFilterTask) task).getDataFile());

            OpenedRawDataFile openedFile = ((MeanFilterTask) task).getDataFile();
            RawDataFile newFile = (RawDataFile) task.getResult();

            openedFile.updateFile(newFile, this, parameters);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while filtering a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }
}
