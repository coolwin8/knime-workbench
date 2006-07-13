/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   09.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.ConnectionDragCreationTool;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.anchor.InPortConnectionAnchor;
import de.unikn.knime.workbench.editor2.editparts.anchor.
       OutPortConnectionAnchor;
import de.unikn.knime.workbench.editor2.editparts.policy.
       PortGraphicalRoleEditPolicy;
import de.unikn.knime.workbench.editor2.figures.AbstractNodePortFigure;

/**
 * Abstract base class for the edit parts that control the nodes. This editpart
 * returns a <code>DragTracker</code> for starting connections between in- and
 * out ports. Note that all(!) nodes are registered as listener for workflow
 * events on the underlying <code>WorkflowManager</code>. This is necessary
 * because we need de be able to react on connection changes.
 * 
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractPortEditPart extends AbstractGraphicalEditPart
        implements NodeEditPart, WorkflowListener {

    private int m_id;

    /**
     * Subclasses must call this with the appropriate portID (= portIndex).
     * 
     * @param portID The id for this port
     */
    public AbstractPortEditPart(final int portID) {
        m_id = portID;
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return m_id;
    }
    
    /**
     * @return if this is a model port.
     */    
    public abstract boolean isModelPort();

    /**
     * Convenience, returns the hosting container.
     * 
     * @return the container
     */
    protected final NodeContainer getNodeContainer() {
        return (NodeContainer) getParent().getModel();
    }

    /**
     * Convenience, returns the WFM.
     * 
     * @return the workflow manager
     */
    protected final WorkflowManager getManager() {
        return ((WorkflowRootEditPart) getParent().getParent())
                .getWorkflowManager();
    }

    /**
     * We must register *every* node as a listener on the Workflow, as we have
     * not real objects for it.
     * 
     * @see org.eclipse.gef.EditPart#activate()
     */
    @Override
    public void activate() {
        super.activate();
        getManager().addListener(this);
    }

    /**
     * Remove the port as a listener from the workflow.
     * 
     * @see org.eclipse.gef.EditPart#deactivate()
     */
    @Override
    public void deactivate() {
        super.deactivate();
        getManager().removeListener(this);
    }

    /**
     * We install the the <code>GRAPHICAL_NODE_ROLE</code> which enables the
     * edit part to create connections to other edit parts.
     * 
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    @Override
    protected void createEditPolicies() {
        // This policy provides create/reconnect commands for connections that
        // are associated at this port
        this.installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
                new PortGraphicalRoleEditPolicy());

    }

    /**
     * Refreshes the visuals of the port visuals.
     * 
     * @see org.eclipse.gef.editparts.AbstractEditPart#refreshVisuals()
     */
    @Override
    protected void refreshVisuals() {
        // get the figure and update the constraint for it - locator is provided
        // by the figure itself
        NodeContainerEditPart parent = (NodeContainerEditPart) getParent();
        AbstractNodePortFigure f = (AbstractNodePortFigure) getFigure();
        parent.setLayoutConstraint(this, f, f.getLocator());
    }

    /**
     * We're just interessted in events that have something to do with our port.
     * In this case we need to update the connections and visuals.
     * 
     * @param event the workflow event
     */
    public void workflowChanged(final WorkflowEvent event) {
        ConnectionContainer c = null;

        if (event instanceof WorkflowEvent.ConnectionAdded) {
            c = (ConnectionContainer) event.getNewValue();
        } else if (event instanceof WorkflowEvent.ConnectionRemoved) {
            c = (ConnectionContainer) event.getOldValue();
        }

        // if we have a connection to refresh...
        if (c != null) {
            // only refresh if we are actually involved in the connection change
            if (c.getSource() == getNodeContainer()
                    || c.getTarget() == getNodeContainer()) {
                refreshChildren();
                refreshSourceConnections();
                refreshTargetConnections();
            }
        }

    }

    /**
     * This activates the ConnectionDragCreationTool, as soon as the user clicks
     * on this edit part. (event REQ_SELECTION)
     * 
     * @see org.eclipse.gef.EditPart#getDragTracker(org.eclipse.gef.Request)
     */
    @Override
    public DragTracker getDragTracker(final Request request) {

        // Selection event: Start the connection creation
        if (request.getType().equals(REQ_SELECTION)) {

            // we need to select the parent edit part !
            // Be sure to preserve already selected nodes, so check for keyboard
            // modifiers
            SelectionRequest req = (SelectionRequest) request;
            if (req.isControlKeyPressed() || req.isShiftKeyPressed()
                    || req.isAltKeyPressed()) {
                // append parent to current selection
                getViewer().appendSelection(getParent());
            } else {
                // single select
                getViewer().select(getParent());
            }
            return new ConnectionDragCreationTool();
        }

        return super.getDragTracker(request);
    }

    /**
     * @see org.eclipse.gef.NodeEditPart
     *      #getSourceConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
     */
    public ConnectionAnchor getSourceConnectionAnchor(
            final ConnectionEditPart connection) {
        return new OutPortConnectionAnchor(getFigure());
    }

    /**
     * @see org.eclipse.gef.NodeEditPart
     *      #getSourceConnectionAnchor(org.eclipse.gef.Request)
     */
    public ConnectionAnchor getSourceConnectionAnchor(final Request request) {
        return new OutPortConnectionAnchor(getFigure());
    }

    /**
     * @see org.eclipse.gef.NodeEditPart
     *      #getTargetConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
     */
    public ConnectionAnchor getTargetConnectionAnchor(
            final ConnectionEditPart connection) {
        return new InPortConnectionAnchor(getFigure());
    }

    /**
     * @see org.eclipse.gef.NodeEditPart
     *      #getTargetConnectionAnchor(org.eclipse.gef.Request)
     */
    public ConnectionAnchor getTargetConnectionAnchor(final Request request) {
        return new InPortConnectionAnchor(getFigure());
    }

}
