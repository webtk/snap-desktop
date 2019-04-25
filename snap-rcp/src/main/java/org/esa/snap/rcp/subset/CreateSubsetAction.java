/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.subset;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.util.MultiSizeIssue;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.product.ProductSubsetDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This action opens a product subset dialog with the initial spatial bounds
 * taken from the currently visible image area, if any.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Tools", id = "CreateSubsetAction")
@ActionRegistration(displayName = "#CTL_CreateSubsetAction_Name", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Raster", position = 50),
        @ActionReference(path = "Toolbars/Processing Other", position = 50)
})
@NbBundle.Messages({
        "CTL_CreateSubsetAction_Name=Subset",
        "CTL_CreateSubsetAction_Title=Subset: crop a file (spatial, subsample, raster) to create a new file ( default boundaries are the current view)"
})
public class CreateSubsetAction extends AbstractAction implements LookupListener, Presenter.Menu, Presenter.Toolbar{

    static int subsetNumber;

    private final ProductNode sourceNode;

    private final Lookup lookup;
    private final Lookup.Result<ProductSceneView> viewResult;

    private static final String SMALLICON = "org/esa/snap/rcp/icons/Create_subset.png";
    private static final String LARGEICON = "org/esa/snap/rcp/icons/Create_subset24.png";


    public CreateSubsetAction() {
        this(null);
    }

    protected CreateSubsetAction(Lookup lookup) {
        this(lookup, null);
    }

    public CreateSubsetAction(Lookup lookup, ProductNode sourceNode) {
        this.sourceNode = sourceNode;
        putValue(ACTION_COMMAND_KEY, getClass().getName());
//        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_CreateSubsetAction_Name());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_CreateSubsetAction_Title());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        this.viewResult = this.lookup.lookupResult(ProductSceneView.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent ignored) {
        Product product = SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        if (product != null) {
            createSubset(product, getInitialBounds(product));
        }
    }

    public static void createSubset(Product sourceProduct, Rectangle bounds) {
        if (MultiSizeIssue.isMultiSize(sourceProduct)) {
            final Product resampledProduct = MultiSizeIssue.maybeResample(sourceProduct);
            //todo use resampled product to call subsetDialog from here using the code below - tf 20160314
//            if (resampledProduct != null) {
//                sourceProduct = resampledProduct;
//            } else {
//                return;
//            }
            return;
        }

        final String subsetName = "subset_" + CreateSubsetAction.subsetNumber + "_of_" + sourceProduct.getName();
        final ProductSubsetDef initSubset = new ProductSubsetDef();
        initSubset.setRegion(bounds);
        initSubset.setNodeNames(sourceProduct.getBandNames());
        initSubset.addNodeNames(sourceProduct.getTiePointGridNames());
        initSubset.setIgnoreMetadata(false);
        final ProductSubsetDialog subsetDialog = new ProductSubsetDialog(SnapApp.getDefault().getMainFrame(),
                                                                         sourceProduct, initSubset);
        if (subsetDialog.show() != ProductSubsetDialog.ID_OK) {
            return;
        }
        final ProductSubsetDef subsetDef = subsetDialog.getProductSubsetDef();
        if (subsetDef == null) {
            Dialogs.showInformation(Bundle.CTL_CreateSubsetFromViewAction_Title(),
                                    "No product subset created.",
                                    null);
            return;
        }
        try {
            final Product subset = sourceProduct.createSubset(subsetDef, subsetName,
                                                              sourceProduct.getDescription());
            SnapApp.getDefault().getProductManager().addProduct(subset);
            CreateSubsetAction.subsetNumber++;
        } catch (Exception e) {
            final String msg = "An error occurred while creating the product subset:\n" +
                               e.getMessage();
            SnapApp.getDefault().handleError(msg, e);
        }
    }

    private Rectangle getInitialBounds(Product product) {
        Rectangle bounds = null;
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view != null && view.getProduct() == product) {
            bounds = view.getVisibleImageBounds();
        }
        return bounds;
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

    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();
    }

    protected void updateEnabledState() {
        super.setEnabled(!viewResult.allInstances().isEmpty());
    }


}
