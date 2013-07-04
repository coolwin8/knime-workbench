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
 */
package org.knime.workbench.explorer.view;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.MessageFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.actions.validators.FileStoreNameValidator;
import org.knime.workbench.repository.util.ContextAwareNodeFactoryMapper;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Content and label provider for one source in the user space view. One
 * instance represents one mount point. It might be used by multiple views to
 * show the content of that one mount point.
 *
 * @author ohl, University of Konstanz
 */
public abstract class AbstractContentProvider extends LabelProvider implements
        ITreeContentProvider, Comparable<AbstractContentProvider> {

    /**
     * Enumeration for the different link types for metanode templates.
     *
     * @since 5.0
     */
    public enum LinkType {
        /** Link with absolute URI, i.e. with mountpoint name. */
        Absolute,
        /** Link with mountpoint-relative URI, i.e. <tt>knime://knime.mountpoint/...</tt>. */
        MountpointRelative,
        /** Link with workflow-relative URI, i.e. <tt>knime://knime.workflow/...</tt>. */
        WorkflowRelative
    }

    /**
     * Empty result array.
     */
    protected static final AbstractExplorerFileStore[] NO_CHILD =
            new AbstractExplorerFileStore[0];

    /**
     * Files not displayed if contained in a workflow group.
     * @since 4.0
     */
    protected static final Collection<String> HIDDEN_FILENAMES = new ArrayList<String>();
    static {
        HIDDEN_FILENAMES.add("workflowset.meta");
        HIDDEN_FILENAMES.add(".project");
    }

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractContentProvider.class);

    private final AbstractContentProviderFactory m_creator;

    private final String m_id;

    /**
     * @param myCreator the factory creating this instance.
     * @param id mount id of this content provider
     *
     */
    public AbstractContentProvider(
            final AbstractContentProviderFactory myCreator, final String id) {
        if (myCreator == null) {
            throw new NullPointerException(
                    "The factory creating this object must be set");
        }
        if (id == null || id.isEmpty()) {
            throw new NullPointerException(
                    "The mount id can't be null nor empty");
        }
        m_creator = myCreator;
        m_id = id;
    }

    /**
     * @return the factory that created this object.
     */
    public AbstractContentProviderFactory getFactory() {
        return m_creator;
    }

    /**
     * Returns the ID this content is mounted with.
     *
     * @return the mount id of this content provider.
     */
    public String getMountID() {
        return m_id;
    }

    /**
     * The refresh goes up and tells the view to refresh our content.
     */
    public final void refresh() {
        refresh(getFileStore("/"));
    }

    public final void refresh(final AbstractExplorerFileStore changedChild) {
        fireLabelProviderChanged(new LabelProviderChangedEvent(this,
                changedChild));
    }

    /**
     * Save state and parameters.
     *
     * @return a string representation of this factory
     *
     * @see AbstractContentProviderFactory
     */
    public abstract String saveState();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void dispose();

    /**
     * @return displayed name of this instance. {@inheritDoc}
     */
    @Override
    public abstract String toString();

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AbstractContentProvider provider) {
        return m_id.compareTo(provider.getMountID());
    }

    /**
     * @return icon of this instance. Or null, if you don't have any.
     */
    public abstract Image getImage();

    /**
     * @param fullPath the path to the item.
     * @return the file store for the specified path.
     */
    public abstract AbstractExplorerFileStore getFileStore(final String fullPath);

    /**
     * @param uri the uri of the item
     * @return the file store for the specified uri
     */
    public final AbstractExplorerFileStore getFileStore(final URI uri) {
        return ExplorerFileSystem.INSTANCE.getStore(uri);
    }

    /**
     * Implementation of {@link ExplorerFileSystem#fromLocalFile(File)}. If the
     * file does not exist in this space or this is not a file based mount, null
     * is returned.
     *
     * @param file The file in question.
     * @return the file store or null.
     */
    public abstract LocalExplorerFileStore fromLocalFile(final File file);

    /**
     * Helper class to find the path segment for a given local (absolute) file.
     * It will traverse the file's parents until it finds the root file (which
     * is the root of the caller). If that matches, it will assemble the
     * relative path ("/" separated).
     *
     * @param file The file to query, never null.
     * @param root The root file of the argument content provider, never null.
     * @return The path segments in a string or null if the argument file does
     *         not have the root argument as parent.
     */
    public static String getRelativePath(final File file, final File root) {
        LinkedList<String> segments = new LinkedList<String>();
        File parent = file;
        while (parent != null && !parent.equals(root)) {
            segments.addFirst(parent.getName());
            parent = parent.getParentFile();
        }
        if (parent == null || !parent.equals(root)) {
            return null;
        }
        if (segments.size() == 0) {
            return ("/");
        }
        StringBuilder path = new StringBuilder();
        for (String s : segments) {
            if (!s.isEmpty()) {
                path.append("/").append(s);
            }
        }
        return path.toString();
    }

    /* ---------------- view context menu methods ------------------- */
    /**
     * Add items to the context menu.
     *
     * @param view the explorer viewer
     * @param manager the context menu manager
     * @param visibleMountIDs the ids of the mount points currently viewed
     * @param selection the current selection sorted by content provider (with
     *            all selected item for all providers!)
     */
    public abstract void addContextMenuActions(
            final ExplorerView view,
            final IMenuManager manager,
            final Set<String> visibleMountIDs,
            final Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selection);

    /* ---------------- drag and drop methods ----------------------- */

    /**
     * @param target the target the data is dropped on
     * @param operation the operation to be performed
     * @param transferType the transfer type
     * @return true if the drop is valid, false otherwise
     */
    public abstract boolean validateDrop(
            final AbstractExplorerFileStore target, final int operation,
            TransferData transferType);

    /**
     * Performs any work associated with the drop. Drop data might be null. In
     * this case the implementing classes should try to retrieve the data from
     * the {@link LocalSelectionTransfer}. Implementors must finish the drop!
     * I.e. if the operation is a move, the source should be deleted!
     *
     * @param view the view that displays the content
     * @param data the drop data, might be null
     * @param operation the operation to be performed as received from
     *            {@link ViewerDropAdapter#getCurrentOperation()}
     * @param target the drop target
     * @return true if the drop was successful, false otherwise
     * @see ViewerDropAdapter#getCurrentOperation()
     */
    public abstract boolean performDrop(final ExplorerView view,
            Object data, final AbstractExplorerFileStore target, int operation);

    /**
     * @param fileStores the dragged file stores of the content provider
     * @return true if dragging is allowed for the selection, false otherwise
     */
    public abstract boolean dragStart(List<AbstractExplorerFileStore> fileStores);


    /**
     * Saves the given metanode as template into the given file store. The metanode is marked as linked metanode
     * in its parent workflow manager. The metanode is linked with an absolute URI to its template.
     *
     * @param metaNode the meta node
     * @param target the target for the template
     * @return <code>true</code> if the operation was successful, <code>false</code> otherwise
     */
    public boolean saveMetaNodeTemplate(final WorkflowManager metaNode,
            final AbstractExplorerFileStore target) {
        return saveMetaNodeTemplate(metaNode, target, LinkType.Absolute);
    }

    /**
     * Saves the given metanode as template into the given file store. The metanode is marked as linked metanode
     * in its parent workflow manager. You can specify how the metanode should be linked to the template.
     *
     * @param metaNode the meta node
     * @param target the target for the template
     * @param linkType the link type
     * @return <code>true</code> if the operation was successful, <code>false</code> otherwise
     * @since 5.0
     */
    @SuppressWarnings("unchecked")
    public boolean saveMetaNodeTemplate(final WorkflowManager metaNode,
            final AbstractExplorerFileStore target, final LinkType linkType) {

        if (!AbstractExplorerFileStore.isWorkflowGroup(target)) {
            return false;
        }

        final String originalName = metaNode.getName();

        String mountIDWithFullPath = target.getMountIDWithFullPath();
        Shell shell = Display.getDefault().getActiveShell();
        String uniqueName = originalName;
        if (new FileStoreNameValidator().isValid(uniqueName) != null) {
            InputDialog dialog = new InputDialog(shell, "Metanode rename",
                    "The name \"" + uniqueName + "\" is not a valid "
                    + "template name.\n\nChoose a new name under which the "
                    + "template will be saved.", uniqueName,
                    new FileStoreNameValidator());
            dialog.setBlockOnOpen(true);
            if (dialog.open() == Window.CANCEL) {
                return false;
            }
            uniqueName = dialog.getValue();
        }
        AbstractExplorerFileStore templateLoc = target.getChild(uniqueName);
        boolean doesTargetExist = templateLoc.fetchInfo().exists();
        // don't allow to overwrite existing workflow groups with a template
        final boolean overwriteOK = doesTargetExist
            && !AbstractExplorerFileStore.isWorkflowGroup(templateLoc);
        boolean isOverwrite = false;
        if (doesTargetExist) {
            DestinationChecker<AbstractExplorerFileStore,
                AbstractExplorerFileStore> dc = new DestinationChecker
                    <AbstractExplorerFileStore, AbstractExplorerFileStore>(
                            shell, "create template", false, false);
            dc.setIsOverwriteEnabled(overwriteOK);
            dc.setIsOverwriteDefault(overwriteOK);

            AbstractExplorerFileStore old = templateLoc;
            templateLoc = dc.openOverwriteDialog(
                    templateLoc, overwriteOK, Collections.EMPTY_SET);
            if (templateLoc == null) { // canceled
                return false;
            }
            isOverwrite = old.equals(templateLoc);
        }

        String newName = templateLoc.getName();
        boolean linkMetaNodeToNewTemplate;
        switch (promptLinkMetaNodeTemplate(originalName, newName)) {
        case IDialogConstants.YES_ID:
            linkMetaNodeToNewTemplate = true;
            break;
        case IDialogConstants.NO_ID:
            linkMetaNodeToNewTemplate = false;
            break;
        default: // Cancel
            return false;
        }

        File directory = metaTemplateDropGetTempDir(templateLoc);

        try {
            if (directory == null) {
                LOGGER.error("Unable to convert \"" + templateLoc
                        + "\" to local path " + "(mount provider \""
                        + toString() + "\"");
                return false;
            }
            if (directory.exists()) {
                if (!directory.isDirectory()) {
                    LOGGER.error("Implementation error: Provided storage path"
                            + " doesn't denote a directory!");
                    return false;
                }
                if (!directory.canWrite()) {
                    MessageDialog.openWarning(shell, "No write permission",
                            "You don't have sufficient privileges to write "
                                    + "to the target directory \""
                                    + mountIDWithFullPath + "\"");
                    return false;
                }
            }

            if (!metaTemplateDropPrepareForSave(templateLoc, directory,
                    isOverwrite)) {
                LOGGER.debug("Preparation for MetaTemplate save failed.");
                return false;
            }
            try {
                MetaNodeTemplateInformation template =
                        metaNode.saveAsMetaNodeTemplate(directory,
                                new ExecutionMonitor());
                if (linkMetaNodeToNewTemplate) {
                    // TODO this needs to be done via the command stack,
                    // the rename can currently not be undone.
                    if (!originalName.equals(newName)) {
                        metaNode.setName(newName);
                    }

                    URI uri = createMetanodeLinkUri(metaNode, templateLoc, linkType);
                    MetaNodeTemplateInformation link = template.createLink(uri);
                    metaNode.getParent().setTemplateInformation(
                            metaNode.getID(), link);
                }
            } catch (Exception e) {
                String error = "Unable to save template: " + e.getMessage();
                LOGGER.warn(error, e);
                MessageDialog.openError(shell, "Error while writing template",
                        error);
            }

            metaTemplateDropFinish(templateLoc, directory);

        } finally {
            metaTemplateDropCleanup(directory);
        }
        target.refresh();
        return true;
    }


    private static URI createMetanodeLinkUri(final WorkflowManager metaNode, final AbstractExplorerFileStore templateLocation, final LinkType linkType) throws CoreException, URISyntaxException, UnsupportedEncodingException {
        URI originalUri = templateLocation.toURI();
        if (linkType.equals(LinkType.Absolute)) {
            return originalUri;
        } else {
            File templateMountpointRoot = templateLocation.getContentProvider().getFileStore("/").toLocalFile();
            if (templateMountpointRoot == null) {
                LOGGER.warn("Cannot determine mountpoint for template, using absolute link instead of relative link");
                return originalUri;
            }

            WorkflowManager wfm = metaNode;
            while (!wfm.isProject()) {
                wfm = wfm.getParent();
            }
            File workflowMountpointRoot = wfm.getContext().getMountpointRoot();
            if (workflowMountpointRoot == null) {
                LOGGER.warn("Cannot determine mountpoint for workflow, using absolute link instead of relative link");
                return originalUri;
            }

            if (!templateMountpointRoot.equals(workflowMountpointRoot)) {
                LOGGER.warn("Template and workflow are not in same mountpoint, using absolute link instead");
                return originalUri;
            }

            if (linkType.equals(LinkType.MountpointRelative)) {
                return new URI(originalUri.getScheme(), originalUri.getUserInfo(), "knime.mountpoint", -1,
                    originalUri.getPath(), originalUri.getQuery(), originalUri.getFragment());
            } else if (linkType.equals(LinkType.WorkflowRelative)) {
                String[] templatePathParts = templateLocation.toLocalFile().getAbsolutePath().split("[/\\\\]");
                String[] workflowPathParts = wfm.getContext().getCurrentLocation().getAbsolutePath().split("[/\\\\]");

                int indexWhereDifferent = 0;
                while ((indexWhereDifferent < templatePathParts.length)
                    && (indexWhereDifferent < workflowPathParts.length)
                    && templatePathParts[indexWhereDifferent].equals(workflowPathParts[indexWhereDifferent])) {
                    indexWhereDifferent++;
                }

                StringBuilder relPath = new StringBuilder();
                for (int i = indexWhereDifferent; i < workflowPathParts.length; i++) {
                    relPath.append("/..");
                }
                for (int i = indexWhereDifferent; i < templatePathParts.length; i++) {
                    relPath.append('/').append(templatePathParts[i]);
                }
                return new URI(ExplorerFileSystem.SCHEME, "knime.workflow", relPath.toString(), "");
            } else {
                throw new IllegalArgumentException("Unknown metanode link type: " + linkType);
            }
        }
    }

    /**
     * Remote targets create and return a tmp dir, local targets can just return
     * their local file.
     *
     * @param target
     * @return never null
     */
    protected File metaTemplateDropGetTempDir(
            final AbstractExplorerFileStore target) {
        File result;
        try {
            result = target.toLocalFile();
        } catch (CoreException e) {
            throw new IllegalStateException("IMPLEMENTATION ERROR: Remote file"
                    + "content provider must overwrite this method");
        }
        if (result == null) {
            throw new IllegalStateException("IMPLEMENTATION ERROR: Remote file"
                    + "content provider must overwrite this method");
        }
        return result;
    }

    /**
     * After a successful or unsuccessful finish of the template save/drop this
     * is called to get the (possible) tmp file cleaned.
     *
     * @param tmpDir the temp location provided by
     *            {@link #metaTemplateDropGetTempDir(AbstractExplorerFileStore)}
     */
    protected void metaTemplateDropCleanup(final File tmpDir) {
        // default implementation for local file stores doesn't create tmp dirs
    }

    /**
     * Called right before the meta template is saved to the tmpDir. Locals may
     * delete existing/overwritten templates. Target could be locked for
     * writing.
     *
     * @param target the final target
     * @param tmpDir the dir provided by
     *            {@link #metaTemplateDropGetTempDir(AbstractExplorerFileStore)}
     * @param overwrite if true the target/tmpDir should be cleaned for the
     *            following template save
     * @return true if drop can proceed. If false is return the drop method
     *         silently return.
     */
    protected boolean metaTemplateDropPrepareForSave(
            final AbstractExplorerFileStore target, final File tmpDir,
            final boolean overwrite) {
        /*
         * default implementation assumes a local target file store and tries to
         * lock it for writing (and deletes it to provide a clean target)
         */
        if (target.fetchInfo().exists() && overwrite) {
            while (!VMFileLocker.lockForVM(tmpDir)) {
                MessageDialog dialog =
                        new MessageDialog(
                                Display.getDefault().getActiveShell(),
                                "Unable to lock directory", null,
                                "The target folder \""
                                        + target.getMountIDWithFullPath()
                                        + "\" can currently not be locked. ",
                                MessageDialog.QUESTION, new String[]{
                                        "&Try again", "&Cancel"}, 0);
                if (dialog.open() == 0) {
                    continue; // next try
                } else {
                    return false; // abort
                }
            }
            assert VMFileLocker.isLockedForVM(tmpDir);
            ExplorerFileSystemUtils.deleteLockedWorkflows(Collections
                    .singletonList(target));
            // the deletion method unlocks
        }
        return true;
    }

    /**
     * Called after the meta template is stored in the temp dir. Implementations
     * can now synch the tempDir with the actual target file store.
     *
     * @param target
     * @param tmpDir
     */
    protected void metaTemplateDropFinish(
            final AbstractExplorerFileStore target, final File tmpDir) {
        // default local implementation doesn't need to do nothing
    }

    private int promptLinkMetaNodeTemplate(
            final String oldName, final String newName) {
        IPreferenceStore prefStore =
                ExplorerActivator.getDefault().getPreferenceStore();
        String pref = prefStore.getString(
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE);
        if (MessageDialogWithToggle.ALWAYS.equals(pref)) {
            return IDialogConstants.YES_ID;
        } else if (MessageDialogWithToggle.NEVER.equals(pref)) {
            return IDialogConstants.NO_ID;
        }
        Shell activeShell = Display.getDefault().getActiveShell();
        String msg = "Update meta node to link to the template?";
        if (!oldName.equals(newName)) {
            msg = msg + "\n(The node will be renamed to \"" + newName + "\".)";
        }
        MessageDialogWithToggle dlg =
                MessageDialogWithToggle.openYesNoCancelQuestion(activeShell,
                        "Link Meta Node Template", msg,
                        "Remember my decision", false, prefStore,
                        PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE);
        return dlg.getReturnCode();
    }

    /**
     * @return whether this content provider is able to host meta node templates,
     *         this is true for server or team spaces but false for the local
     *         space (or the the RO public server)
     */
    public abstract boolean canHostMetaNodeTemplates();

    /**
     * @return whether this content provider is able to host data files. This is true for server or team spaces, but
     *         false for the local space.
     * @since 4.0
     */
    public abstract boolean canHostDataFiles();


    /* -------------- content provider methods ---------------------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore[] getChildren(Object parentElement);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore[] getElements(Object inputElement);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore getParent(Object element);

    /* ---------- helper methods for content provider ------------------- */
    /**
     * Helper method for content providers. Returns children of a workflow.
     *
     * @param workflow the workflow to return the children for
     * @return children of a workflow
     */
    public static AbstractExplorerFileStore[] getWorkflowChildren(
            final AbstractExplorerFileStore workflow) {
        assert AbstractExplorerFileStore.isWorkflow(workflow);

        try {
            IFileStore[] childs = workflow.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            /*
             * currently we are not showing nodes
             */
            return NO_CHILD;
            // ArrayList<ExplorerFileStore> result =
            // new ArrayList<ExplorerFileStore>();
            // for (IFileStore c : childs) {
            // not adding nodes for now.
            // if (ExplorerFileStore.isMetaNode((ExplorerFileStore)c)) {
            // || ExplorerFileStore.isNode(childFile)) {
            // result.add((ExplorerFileStore)c);
            // }
            // }
            // return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }

    }

    /**
     *
     * @param template to return children for
     * @return children of the template.
     */
    public static AbstractExplorerFileStore[] getWorkflowTemplateChildren(
            final AbstractExplorerFileStore template) {
        // meta nodes have not children (as long as we don't show their nodes)
        return NO_CHILD;
    }

    /**
     * Helper method for content providers. Returns children of a workflowgroup.
     *
     * @param workflowGroup the workflow group to return the children for
     * @return the content of the workflow group
     */
    public static AbstractExplorerFileStore[] getWorkflowgroupChildren(
            final AbstractExplorerFileStore workflowGroup) {

        assert AbstractExplorerFileStore.isWorkflowGroup(workflowGroup);

        try {
            AbstractExplorerFileStore[] childs =
                    workflowGroup.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            ArrayList<AbstractExplorerFileStore> result =
                    new ArrayList<AbstractExplorerFileStore>();
            for (AbstractExplorerFileStore c : childs) {
                if (AbstractExplorerFileStore.isWorkflowGroup(c)
                        || AbstractExplorerFileStore.isWorkflow(c)
                        || AbstractExplorerFileStore.isWorkflowTemplate(c)) {
                    result.add(c);
                }
                if (AbstractExplorerFileStore.isDataFile(c)) {
                    if (!HIDDEN_FILENAMES.contains(c.getName())) {
                        result.add(c);
                    }
                }
            }
            return result.toArray(new AbstractExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }
    }

    public static AbstractExplorerFileStore[] getMetaNodeChildren(
            final AbstractExplorerFileStore metaNode) {
        assert AbstractExplorerFileStore.isMetaNode(metaNode);

        try {
            IFileStore[] childs = metaNode.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            /*
             * currently we are not showing nodes
             */
            return NO_CHILD;
            // ArrayList<ExplorerFileStore> result =
            // new ArrayList<ExplorerFileStore>();
            // for (IFileStore c : childs) {
            // not adding nodes for now.
            // if (ExplorerFileStore.isMetaNode((ExplorerFileStore)c)) {
            // || ExplorerFileStore.isNode(childFile)) {
            // result.add((ExplorerFileStore)c);
            // }
            // }
            // return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }

    }

    /* ------------ helper methods for label provider (icons) ------------- */
    /**
     * Returns an icon/image for the passed file, if it is something like a
     * workflow, group, node or meta node. If it is not a store representing one
     * of these, null is returned.
     *
     * @param efs the explorer file store
     * @return the icon/image for the passed file store
     */
    public static Image getWorkspaceImage(final AbstractExplorerFileStore efs) {

        if (AbstractExplorerFileStore.isNode(efs)) {
            return ImageRepository.getImage(SharedImages.Node);
        }
        if (AbstractExplorerFileStore.isMetaNode(efs)) {
            return ImageRepository.getImage(SharedImages.Node);
        }
        if (AbstractExplorerFileStore.isWorkflowGroup(efs)) {
            return ImageRepository.getImage(SharedImages.WorkflowGroup);
        }
        if (AbstractExplorerFileStore.isWorkflowTemplate(efs)) {
            return ImageRepository.getImage(SharedImages.MetaNodeTemplate);
        }
        if (AbstractExplorerFileStore.isDataFile(efs)) {
            Image img = ContextAwareNodeFactoryMapper.getImage(efs.getName());
            if (img != null) {
                return img;
            }
            return ImageRepository.getImage(SharedImages.File);
        }
        if (!AbstractExplorerFileStore.isWorkflow(efs)) {
            return null;
        }

        // if it is a local workflow return the correct icon for open flows
        File f;
        try {
            f = efs.toLocalFile(EFS.NONE, null);
        } catch (CoreException ce) {
            return ImageRepository.getImage(SharedImages.WorkflowClosed);
        }

        if (f == null) {
            return ImageRepository.getImage(SharedImages.WorkflowClosed);
        }
        URI wfURI = f.toURI();
        NodeContainer nc = ProjectWorkflowMap.getWorkflow(wfURI);
        if (nc == null) {
            return ImageRepository.getImage(SharedImages.WorkflowClosed);
        }
        if (nc instanceof WorkflowManager) {
            if (nc.getID().hasSamePrefix(WorkflowManager.ROOT.getID())) {
                // only show workflow directly off the root
                if (nc.getNodeMessage().getMessageType()
                        .equals(NodeMessage.Type.ERROR)) {
                    return ImageRepository.getImage(SharedImages.WorkflowError);
                }
                NodeContainerState ncState = nc.getNodeContainerState();
                if (ncState.isExecuted()) {
                    return ImageRepository.getImage(SharedImages.WorkflowExecuted);
                } else if (ncState.isExecutionInProgress()) {
                    return ImageRepository.getImage(SharedImages.WorkflowExecuting);
                } else if (ncState.isConfigured()) {
                    return ImageRepository.getImage(SharedImages.WorkflowConfigured);
                } else {
                    return ImageRepository.getImage(SharedImages.WorkflowConfigured);
                }
            } else {
                return ImageRepository.getImage(SharedImages.Node);
            }
        } else {
            return ImageRepository.getImage(SharedImages.WorkflowUnknown);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        if (element instanceof MessageFileStore) {
            return ((MessageFileStore)element).getImage();
        }
        return null;
    }

    /**
     * @return true if this content provider is accessing a remote file system
     */
    public abstract boolean isRemote();


    /**
     * Checks whether it is possible to add items to the content provider or
     * change its content. This might not be the case, for example, if
     * authentification is required but the user is not authenticated yet or on
     * a read-only server like the public server is accessed.
     *
     * @return true if the provider's content cannot be modified, false
     *      otherwise
     */
    public abstract boolean isWritable();

    /**
     * Copies or moves one or multiple file stores into the target directory.
     *
     * @param view the view that displays the content
     * @param fileStores the file stores to copy or move
     * @param targetDir the target directory. Make sure to call the content
     *            provider that can handle the target dir type.
     * @param performMove true, if the file stores should be moved, false
     *            otherwise
     * @return true if the operation was successful, false otherwise
     */
    public abstract boolean copyOrMove(final ExplorerView view,
            List<AbstractExplorerFileStore> fileStores,
            AbstractExplorerFileStore targetDir, boolean performMove);

    /**
     * Downloads a file store from a remote provider to a local provider.
     *
     * @param source the file store to be downloaded
     * @param target the file store to download to
     * @param monitor the monitor to report progress
     * @throws CoreException if this method fails. Reasons include: A
     *         corresponding file could not be created in the local file system.
     */
    public abstract void performDownload(RemoteExplorerFileStore source,
            LocalExplorerFileStore target, IProgressMonitor monitor)
            throws CoreException;

    /**
     * Uploads a file store from a local provider to a remote provider.
     *
     * @param source the file store to be uploaded
     * @param target the file store to upload to
     * @param monitor the monitor to report progress
     * @throws CoreException if this method fails. Reasons include: A
     *         corresponding file could not be created in the local file system.
     */
    public abstract void performUpload(final LocalExplorerFileStore source,
            final RemoteExplorerFileStore target,
            final IProgressMonitor monitor)
            throws CoreException;

    /**
     * @param fileStores the file stores to be copied / moved
     * @param performMove true if moving, false for copying
     * @return an error message describing the problem or null, if the no open
     *      editor blocks the operation
     * @since 3.0
     */
    protected String checkOpenEditors(
            final List<AbstractExplorerFileStore> fileStores,
            final boolean performMove) {
        // even saved editors are note allowed when moving
        String msg = ExplorerFileSystemUtils.isLockable(fileStores,
                !performMove);
        if (msg != null) {
            MessageBox mb =
                    new MessageBox(Display.getCurrent().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setText("Dragging canceled");
            mb.setMessage(msg);
            mb.open();
        }
        return msg;
    }

}
