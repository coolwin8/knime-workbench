/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2013
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   May 27, 2011 (morent): created
 */

package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class GlobalConfigureWorkflowAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalConfigureWorkflowAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String CONFIGUREWF_ACTION_ID =
            "org.knime.workbench.explorer.action.configure-workflow";

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalConfigureWorkflowAction(final ExplorerView viewer) {
        super(viewer, "Configure...");
        setImageDescriptor(ImageRepository
                .getImageDescriptor(SharedImages.ConfigureNode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return CONFIGUREWF_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<AbstractExplorerFileStore> fileStores =
                DragAndDropUtils.getExplorerFileStores(getSelection());
        AbstractExplorerFileStore wfStore = fileStores.get(0);
        if (!(wfStore instanceof LocalExplorerFileStore)) {
            LOGGER.error("Can only configure local workflows.");
            return;
        }
        try {
            if (ExplorerFileSystemUtils
                    .lockWorkflow((LocalExplorerFileStore)wfStore)) {
                showDialog(getWorkflow());
            } else {
                LOGGER.info("The workflow cannot be configured. "
                        + "It is in use by another user/instance.");
                showCantConfigureLockMessage();
            }
        } finally {
            ExplorerFileSystemUtils
                    .unlockWorkflow((LocalExplorerFileStore)wfStore);
        }

    }

    private void showDialog(final WorkflowManager wfm) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    WrappedNodeDialog dialog =
                            new WrappedNodeDialog(Display.getDefault()
                                    .getActiveShell(), wfm);
                    dialog.setBlockOnOpen(true);
                    dialog.open();
                } catch (final NotConfigurableException nce) {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog
                                    .openError(
                                            Display.getDefault()
                                                    .getActiveShell(),
                                            "Workflow Not Configurable",
                                            "This workflow can not be "
                                                    + "configured: "
                                                    + nce.getMessage());
                        }
                    });
                }

            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getWorkflow() != null && getWorkflow().hasDialog();
    }

    private void showCantConfigureLockMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Configuration");
        mb.setMessage("The workflow cannot be configured as "
                + "is in use by another user/instance.");
        mb.open();
    }
}
