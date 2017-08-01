/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.api.editor.gutter;

/**
 * Interface for components that manage a gutter.
 */
public interface HasGutter {

    /**
     * Returns the gutter.
     *
     * @return the gutter
     */
    Gutter getGutter(GutterType type);

    enum GutterType {
        BREACKPOINT, VCS_MARK
    }
}
