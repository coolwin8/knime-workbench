/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.extrainfo;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.UIInformation;

/**
 * Special <code>NodeExtraInfo</code> object used by the workflow editor.
 * Basically this stores the visual bounds of the node in the workflow editor
 * pane. Note: To be independent of draw2d/GEF this doesn't use the "natural"
 * <code>Rectangle</code> object, but simply stores an <code>int[]</code>.
 *
 * TODO This needs to be in "core", as by now the WFM tries to make instances of
 * this class while <code>load()</code>ing.
 *
 *
 * see org.eclipse.draw2d.geometry.Rectangle
 *
 * @author Florian Georg, University of Konstanz
 */
public class ModellingNodeExtraInfo implements UIInformation {
    /** Version id of this extra info implementation. */
    public static final String VERSION = "1.0";

    /** The key under which the type is registered. * */
    public static final String KEY_VERSION = "extrainfo.node.version";

    /** The key under which the bounds are registered. * */
    public static final String KEY_BOUNDS = "extrainfo.node.bounds";

    /** The key under which the factory name is registered. * */
    public static final String KEY_FACTORY = "extrainfo.node.factory";

    /** The key under which the pluginID is registered. * */
    public static final String KEY_PLUGIN = "extrainfo.node.pluginID";

    /** The key under which the icon path is registered. * */
    public static final String KEY_ICON = "extrainfo.node.icon";

    /** The key under which the type is registered. * */
    public static final String KEY_TYPE = "extrainfo.node.type";

    /** The key under which the description is registered. * */
    public static final String KEY_DESCRIPTION = "extrainfo.node.description";

    private int[] m_bounds = new int[]{0, 0, -1, -1};

    /**
     * Constructs a <code>ModellingConnectionExtraInfo</code>.
     *
     */
    public ModellingNodeExtraInfo() {
    }

    /**
     * {@inheritDoc}
     */
    public void save(final NodeSettingsWO config) {
        config.addIntArray(KEY_BOUNDS, m_bounds);
    }

    /**
     * {@inheritDoc}
     */
    public void load(final NodeSettingsRO conf) throws InvalidSettingsException {
        m_bounds = conf.getIntArray(KEY_BOUNDS);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFilledProperly() {
        if (m_bounds == null) {
            return false;
        }
        return true;
    }

    /**
     * Sets the location. *
     *
     * @param x x-ccordinate
     * @param y y-coordinate
     * @param w width
     * @param h height
     *
     */
    public void setNodeLocation(final int x, final int y, final int w,
            final int h) {
        m_bounds[0] = x;
        m_bounds[1] = y;
        m_bounds[2] = w;
        m_bounds[3] = h;
    }

    /**
     * @return Returns the bounds.
     */
    public int[] getBounds() {
        return m_bounds;
    }

    /**
     * @param bounds The bounds to set.
     */
    public void setBounds(final int[] bounds) {
        m_bounds = bounds;
    }

    /**
     * Changes the position by setting the bounds left top corner according to
     * the given moving distance.
     *
     * @param moveDist the distance to change the left top corner
     */
    public void changePosition(final int[] moveDist) {

        // first change the x value
        m_bounds[0] = m_bounds[0] + moveDist[0];
        m_bounds[1] = m_bounds[1] + moveDist[1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        ModellingNodeExtraInfo newObject = new ModellingNodeExtraInfo();
        newObject.m_bounds = this.m_bounds.clone();
        return newObject;
    }
}
