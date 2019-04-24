/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.ui.mosaic;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Mosaicing action.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Operators", id = "org.esa.snap.core.gpf.ui.mosaic.MosaicAction")
@ActionRegistration(displayName = "#CTL_MosaicAction_Name", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Raster/Geometric Operations", position = 10000),
        @ActionReference(path = "Toolbars/Processing Other", position = 60)
})
@NbBundle.Messages({
        "CTL_MosaicAction_Name=Mosaic",
        "CTL_MosaicAction_Description=Mosaic: creates a file which is an aggregate of multiple files."
})
public final class MosaicAction extends AbstractSnapAction implements Presenter.Menu, Presenter.Toolbar {

    private ModelessDialog dialog;

    private static final String SMALLICON = "org/esa/snap/core/gpf/docs/gpf/icons/Mosaic.png";
    private static final String LARGEICON = "org/esa/snap/core/gpf/docs/gpf/icons/Mosaic24.png";


    public MosaicAction() {
        putValue(NAME, Bundle.CTL_MosaicAction_Name());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_MosaicAction_Description());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new MosaicDialog(Bundle.CTL_MosaicAction_Name(),
                                      "mosaicAction", getAppContext());
        }
        dialog.show();
    }

    @Override
    public boolean isEnabled() {
        return GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Mosaic") != null;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(this);
        menuItem.setIcon(null);
        return menuItem;
    }
    @Override
    public Component getToolbarPresenter() {
        JToggleButton toggleButton = new JToggleButton(this);
        toggleButton.setText(null);
        toggleButton.setIcon(ImageUtilities.loadImageIcon(LARGEICON,false));
        return toggleButton;
    }
}
