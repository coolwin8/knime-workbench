/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 4, 2020 (hornm): created
 */
package org.knime.workbench.browser;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface NodeDescBrowserWrapper {

    void init(Composite parent);

    /**
     * @return
     */
    boolean setFocus();

    /**
     * @return
     */
    Display getDisplay();

    /**
     * @return
     */
    boolean isDisposed();

    /**
     * @param html
     */
    void setText(String html);

    /**
     * @param l
     */
    void addLocationListener(LocationListener l);

    /**
     * @return
     */
    Composite getComposite();

    public static Optional<NodeDescBrowserWrapper> getRegisteredNodeDescBrowserWrapper() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.knime.workbench.editor.BrowserWrapper");
        CheckUtils.checkState(point != null, "Invalid extension point: %s",
            "org.knime.workbench.editor.BrowserWrapper");
        Optional<IConfigurationElement> el =
            Arrays.stream(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements())).findFirst();
        if (el.isPresent()) {
            try {
                return Optional.of((NodeDescBrowserWrapper)el.get().createExecutableExtension("class"));
            } catch (CoreException e) {
                NodeLogger.getLogger(NodeDescBrowserWrapper.class)
                    .error("Unable to instantiate browser via BrowserWrapper extension point", e);
                throw new RuntimeException(e);
            }
        } else {
            return Optional.empty();
        }
    }

}
