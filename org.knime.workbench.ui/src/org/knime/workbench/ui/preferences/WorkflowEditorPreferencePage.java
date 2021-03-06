/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Preference page for global workflow editor settings, such as the node label prefix, grid settings or node connections
 * settings.
 *
 * @author Martin Horn, KNIME.com
 */
public class WorkflowEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private StringFieldEditor m_nodeLabelPrefix;

    private BooleanFieldEditor m_emptyNodeLabel;

    /**
     * Constructor.
     */
    public WorkflowEditorPreferencePage() {
        super(GRID);
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to manipulate various
     * types of preferences. Each field editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        m_emptyNodeLabel =
            new BooleanFieldEditor(PreferenceConstants.P_SET_NODE_LABEL, "Set node label prefix", parent) {
                /** {@inheritDoc} */
                @Override
                protected void valueChanged(final boolean old, final boolean neu) {
                    m_nodeLabelPrefix.setEnabled(neu, parent);
                }
            };
        m_nodeLabelPrefix =
            new StringFieldEditor(PreferenceConstants.P_DEFAULT_NODE_LABEL, "Default node label (prefix): ", parent);
        addField(m_emptyNodeLabel);
        addField(m_nodeLabelPrefix);
        final IntegerFieldEditor fontSizeEditor = new IntegerFieldEditor(PreferenceConstants.P_NODE_LABEL_FONT_SIZE,
            "Font size of node name and label:", parent);
        addField(fontSizeEditor);

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "These grid preferences apply to new workflows only."));
        addField(new BooleanFieldEditor(PreferenceConstants.P_GRID_SHOW, "Show grid", parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_GRID_SNAP_TO, "Snap to grid", parent));
        final IntegerFieldEditor gridSizeXEditor =
            new IntegerFieldEditor(PreferenceConstants.P_GRID_SIZE_X, "Horizontal grid size (in px):", parent);
        gridSizeXEditor.setValidRange(3, 500);
        gridSizeXEditor.setTextLimit(3);
        gridSizeXEditor.load();
        addField(gridSizeXEditor);
        final IntegerFieldEditor gridSizeYEditor =
            new IntegerFieldEditor(PreferenceConstants.P_GRID_SIZE_Y, "Vertical grid size (in px):", parent);
        gridSizeYEditor.setValidRange(3, 500);
        gridSizeYEditor.setTextLimit(3);
        gridSizeYEditor.load();
        addField(gridSizeYEditor);
        addField(new LabelField(parent,
            "To change the grid settings of a workflow, use the\n'Workflow Editor Settings' toolbar button."));

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "These node connection settings apply to new workflows only."));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CURVED_CONNECTIONS, "Curved connections", parent));
        final ComboFieldEditor lineWidthEditor = new ComboFieldEditor(PreferenceConstants.P_CONNECTIONS_LINE_WIDTH,
            "Node connections line width:", new String[][]{{"1", "1"}, {"2", "2"}, {"3", "3"}}, parent);
        lineWidthEditor.load();
        addField(lineWidthEditor);
        addField(new LabelField(parent,
            "To change the node connection settings of a workflow, use the\n 'Workflow Editor Settings' "
                + "toolbar button."));

        addField(new HorizontalLineField(parent));
        addField(new ZoomLevelsFieldEditor(PreferenceConstants.P_EDITOR_ZOOM_LEVELS,
            "Comma delimited list of zoom values: ", parent));
        String labelText = "Zoom level change when the ";
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            labelText += "\u2318";
        } else {
            labelText += "CTRL";
        }
        labelText += "+ALT keys are held down: ";
        final IntegerFieldEditor zoomDelta =
            new IntegerFieldEditor(PreferenceConstants.P_EDITOR_ZOOM_MODIFIED_DELTA, labelText, parent);
        zoomDelta.setValidRange(1, 100);
        addField(zoomDelta);

        addField(new HorizontalLineField(parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_SELECTED_NODE_HIGHLIGHT_CONNECTIONS,
            "Highlight the inport and outport connection lines of a selected node.", parent));
        addField(new ColorFieldEditor(PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_HIGHLIGHT_COLOR,
            "The color to use in highlighting connection lines:", parent));
        addField(new ColorFieldEditor(PreferenceConstants.P_EDITOR_SELECTED_NODE_FLOW_CONNECTION_HIGHLIGHT_COLOR,
            "The color to use in highlighting the flow variable connection lines:", parent));
        final IntegerFieldEditor highlightConnectionWidthChange
                = new IntegerFieldEditor(PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_WIDTH_DELTA,
                    "The change in width when highlighting connection lines:", parent);
        highlightConnectionWidthChange.setValidRange(-5, 10);
        highlightConnectionWidthChange.setTextLimit(2);
        addField(highlightConnectionWidthChange);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final IWorkbench workbench) {
        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        super.initialize();
        m_nodeLabelPrefix.setEnabled(m_emptyNodeLabel.getBooleanValue(), getFieldEditorParent());
    }


    private static class ZoomLevelsFieldEditor extends StringFieldEditor {
        ZoomLevelsFieldEditor(final String name, final String labelText, final Composite parent) {
            super(name, labelText, parent);
        }

        @Override
        protected boolean doCheckState() {
            final String[] levels = getStringValue().split(",");

            for (final String level : levels) {
                try {
                    Integer.parseInt(level.trim());
                } catch (final NumberFormatException nfe) {
                    setErrorMessage("Zoom levels must be integer values, with multiple separated by commas.");
                    return false;
                }
            }

            return true;
        }
    }
}
