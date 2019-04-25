/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.collocation.visat;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Geographic collocation action.
 *
 * @author Ralf Quast
 * @author Marco Peters
 */
@ActionID(category = "Processors", id = "org.esa.snap.collocation.visat.CollocationAction")
@ActionRegistration(displayName = "#CTL_CollocationAction_Text", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Raster/Geometric Operations", position = 10000),
        @ActionReference(path = "Toolbars/Processing Other", position = 40)
})
@NbBundle.Messages({
        "CTL_CollocationAction_Text=Collocate",
        "CTL_CollocationAction_Description=Collocate: creates a files which is a geographic collocation of two files."
})
public final class CollocationAction extends AbstractSnapAction implements Presenter.Menu, Presenter.Toolbar {

    private ModelessDialog dialog;
    private static final String SMALLICON = "org/esa/snap/collocation/docs/icons/Collocation.png";
    private static final String LARGEICON = "org/esa/snap/collocation/docs/icons/Collocation24.png";


    public CollocationAction() {
        putValue(NAME, Bundle.CTL_CollocationAction_Text());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_CollocationAction_Description());
        setHelpId(CollocationDialog.HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new CollocationDialog(getAppContext());
        }
        dialog.show();
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        menuItem.setIcon(null);
        return menuItem;
    }
    @Override
    public Component getToolbarPresenter() {
        JButton button = new JButton(this);
        button.setText(null);
        button.setIcon(ImageUtilities.loadImageIcon(LARGEICON,false));
        return button;
    }


}
