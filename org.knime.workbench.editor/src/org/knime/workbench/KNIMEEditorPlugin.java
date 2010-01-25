/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class for the editor.
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEEditorPlugin extends AbstractUIPlugin {
    // Make sure that this *always* matches the ID in plugin.xml
    /** The Plugin ID. */
    public static final String PLUGIN_ID = "org.knime.workbench.editor";

    // The shared instance.
    private static KNIMEEditorPlugin plugin;

    /**
     * Type of this port.
     */
    public static final PortType PMML_PORT_TYPE = new PortType(PMMLPortObject.class);

    /**
     * The constructor.
     */
    public KNIMEEditorPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     *
     * @param context The bundle context
     * @throws Exception If failed
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        // TODO: temporary hug for preference page, to ensure that the
        // MasterKeySupplier is set correctly before the editor is started
        KNIMEUIPlugin.getDefault().getPreferenceStore();
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The bundle context
     * @throws Exception If failed
     *
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     *
     * @return The shared instance of this plugin
     */
    public static KNIMEEditorPlugin getDefault() {
        return plugin;
    }
}
