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
package org.knime.workbench.cef.browser;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.knime.workbench.browser.IntroPageBrowserWrapper;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class BrowserEditor extends EditorPart implements IntroPageBrowserWrapper {

    static final String BROWSER_EDITOR_ID = "org.knime.workbench.cef.browser";

    private Browser m_browser;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        if (input instanceof BrowserEditorInput) {
            BrowserEditorInput browserInput = (BrowserEditorInput)input;
            setPartName(browserInput.getName());
            setTitleToolTip(browserInput.getToolTipText());
            setSite(site);
            setInput(input);
        } else {
            throw new PartInitException("Unsupport browser input");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        BrowserEditorInput editorInput = (BrowserEditorInput)getEditorInput();
        m_browser = new Browser(parent, SWT.NONE);
        m_browser.setUrl(editorInput.getUrl().toString());
    }

    /**
     * @return
     */
    public void close() {
        if(m_browser != null) {
            m_browser.dispose();
        }
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        if (m_browser != null) {
            m_browser.setFocus();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doSaveAs() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean execute(final String jsCall) {
        if (m_browser != null) {
            return m_browser.execute(jsCall);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        if (m_browser != null) {
            m_browser.refresh();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLocationListener(final LocationListener l) {
        if (m_browser != null) {
            m_browser.removeLocationListener(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLocationListener(final LocationListener l) {
        if (m_browser != null) {
            m_browser.addLocationListener(l);
        }
    }

}
