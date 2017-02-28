/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   24 Feb 2017 (albrecht): created
 */
package org.knime.workbench.editor2.subnode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.wizard.WizardViewCreator;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.wizard.WizardPageManager;
import org.knime.js.core.JSONViewContent;
import org.knime.js.core.JSONWebNode;
import org.knime.js.core.JSONWebNodePage;
import org.knime.js.core.JSONWebNodePageConfiguration;
import org.knime.js.core.JavaScriptViewCreator;
import org.knime.workbench.editor2.subnode.SubnodeViewableModel.SubnodeViewValue;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public class SubnodeViewableModel implements ViewableModel, WizardNode<JSONWebNodePage, SubnodeViewValue> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SubnodeViewableModel.class);

    private JSONWebNodePage m_page;
    private SubnodeViewValue m_value;

    private final WorkflowManager m_wfm;
    private final String m_viewName;
    private String m_viewPath;
    private final JavaScriptViewCreator<JSONWebNodePage, SubnodeViewValue> m_viewCreator;

    /**
     * Creates a new instance of this viewable model
     * @param nodeContainer the subnode container
     * @param viewName the name of the view
     * @throws IOException on view model creation error
     *
     */
    public SubnodeViewableModel(final SubNodeContainer nodeContainer, final String viewName) throws IOException {
        m_wfm = nodeContainer.getParent();
        m_viewName = viewName;
        m_viewCreator = new SubnodeWizardViewCreator();
        final WizardPageManager wpm = WizardPageManager.of(m_wfm);
        m_page = wpm.createWizardPage(nodeContainer.getID());
        nodeContainer.addNodeStateChangeListener(new NodeStateChangeListener() {

            @Override
            public void stateChanged(final NodeStateEvent state) {
                if (nodeContainer.getNodeContainerState().isExecuted()) {
                    try {
                        m_page = wpm.createWizardPage(nodeContainer.getID());
                    } catch (IOException e) {
                        reset();
                    }
                } else {
                    reset();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationError validateViewValue(final SubnodeViewValue viewContent) {
        //TODO this needs refactoring in WizardExecutionController
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadViewValue(final SubnodeViewValue viewContent, final boolean useAsDefault) {
        // TODO this also needs refactoring in WizardExecutionController

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveCurrentValue(final NodeSettingsWO content) { /* not used */ }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONWebNodePage getViewRepresentation() {
        return m_page;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnodeViewValue getViewValue() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONWebNodePage createEmptyViewRepresentation() {
        Map<String, JSONWebNode> emptyNode = new HashMap<String, JSONWebNode>();
        return new JSONWebNodePage(new JSONWebNodePageConfiguration(), emptyNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnodeViewValue createEmptyViewValue() {
        return new SubnodeViewValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavascriptObjectID() {
        /* no id present */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewHTMLPath() {
        boolean create = false;
        if (m_viewPath == null || m_viewPath.isEmpty()) {
            // view is not created
            create = true;
        } else {
            // check if file still exists, create otherwise
            File viewFile = new File(m_viewPath);
            if (!viewFile.exists()) {
                create = true;
            }
        }
        if (create) {
            try {
                m_viewPath = m_viewCreator.createWebResources(m_viewName, getViewRepresentation(), getViewValue());
            } catch (IOException e) {
                LOGGER.error("Creating view HTML failed: " + e.getMessage(), e);
            }
        }
        return m_viewPath;
    }

    private void reset() {
        m_page = createEmptyViewRepresentation();
        m_value = createEmptyViewValue();
        if (m_viewPath != null) {
            try {
                File viewFile = new File(m_viewPath);
                if (viewFile.exists()) {
                    viewFile.delete();
                }
            } catch (Exception e) { /* do nothing */ }
        }
        m_viewPath = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WizardViewCreator<JSONWebNodePage, SubnodeViewValue> getViewCreator() {
        return m_viewCreator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHideInWizard() {
        /* TODO no implementation possible at the moment
         * this needs to be configurable for nested subnodes, etc
         */
        return false;
    }

    //TODO decide if this belongs here. Could also be sth. like PageViewValue in org.knime.js.core
    static class SubnodeViewValue extends JSONViewContent {

        private Map<String, String> m_viewValues;

        /**
         * @return the viewValues
         */
        public Map<String, String> getViewValues() {
            return m_viewValues;
        }

        /**
         * @param viewValues the viewValues to set
         */
        public void setViewValues(final Map<String, String> viewValues) {
            m_viewValues = viewValues;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void saveToNodeSettings(final NodeSettingsWO settings) { /* nothing to save */ }

        /**
         * {@inheritDoc}
         */
        @Override
        public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException { /* nothing to load */ }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            SubnodeViewValue other = (SubnodeViewValue)obj;
            return new EqualsBuilder()
                    .append(m_viewValues, other.m_viewValues)
                    .isEquals();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(m_viewValues)
                    .toHashCode();
        }

    }

    private static class SubnodeWizardViewCreator extends JavaScriptViewCreator<JSONWebNodePage, SubnodeViewValue> {

        public SubnodeWizardViewCreator() {
            super(null);
            setWebTemplate(createSubnodeWebTemplate());
        }

        private WebTemplate createSubnodeWebTemplate() {
            List<WebResourceLocator> locators = new ArrayList<WebResourceLocator>();
            String pluginName = "org.knime.js.core";
            boolean isDebug = isDebug();
            String requireJS = isDebug ? "org/knime/debug/require.js" : "org/knime/core/require.js";
            String jQuery = isDebug ? "js-lib/jQuery/jquery-1.11.3.js" : "js-lib/jQuery/jquery-1.11.3.min.js";
            String bsJS = isDebug ? "js-lib/bootstrap/3_3_6/debug/js/bootstrap.js" : "js-lib/bootstrap/3_3_6/min/js/bootstrap.min.js";
            String bsCSS = isDebug ? "js-lib/bootstrap/3_3_6/debug/css/bootstrap.css" : "js-lib/bootstrap/3_3_6/min/css/bootstrap.min.css";
            String iframeResizer = isDebug ? "org/knime/debug/iframeResizer/iframeResizer.js" : "org/knime/core/iframeResizer/iframeResizer.js";
            String pageBuilder = isDebug ? "org/knime/debug/knime_pagebuilder.js" : "org/knime/core/knime_pagebuilder.js";

            locators.add(new WebResourceLocator(pluginName, requireJS, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, jQuery, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, bsJS, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, bsCSS, WebResourceType.CSS));
            locators.add(new WebResourceLocator(pluginName, iframeResizer, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, pageBuilder, WebResourceType.JAVASCRIPT));
            String namespace = "KnimePageLoader";
            return new DefaultWebTemplate(locators.toArray(new WebResourceLocator[0]), namespace, "init", "validate",
                "getPageValues", "setValidationError");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String createInitJSViewMethodCall(final JSONWebNodePage viewRepresentation, final SubnodeViewValue viewValue) {
            String jsonViewRepresentation = getViewRepresentationJSONString(viewRepresentation);
            StringBuilder builder = new StringBuilder();
            String escapedRepresentation = jsonViewRepresentation.replace("\\", "\\\\").replace("'", "\\'");
            String repParseCall = "var parsedRepresentation = JSON.parse('" + escapedRepresentation + "');";
            builder.append(repParseCall);
            String initMethod = getWebTemplate().getInitMethodName();
            String initCall = getNamespacePrefix() + initMethod + "(parsedRepresentation, null, null, " + isDebug() + ");";
            builder.append(initCall);
            return builder.toString();
        }
    }





}
