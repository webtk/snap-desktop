/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.smart.configurator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Insets;

/**
 * @author muhammad.bc.
 */

@OptionsPanelController.SubRegistration(
        location = "PerformancePreferences",
        displayName = "#OptionsCategory_Name_Tilepoint_tab",
        keywords = "#OptionsCategory_Keywords_Tilepoint_tab_Optim",
        keywordsCategory = "#OptionsCategory_Name_Tilepoint_tab"
)
@org.openide.util.NbBundle.Messages({
        "OptionsCategory_Name_Tilepoint_tab=Tile size",
        "OptionsCategory_Keywords_Tilepoint_tab_Optim=Tile point optimization smart configurator"
})
public class TilePointOptionController extends DefaultConfigController {
    public static final String SNAP_IMAGE_TILE_SIZE = "snap.image.tileSize";
    public static final String SNAP_IMAGE_FORCE_TILE_SIZE = "snap.image.forceTileSize";
    public static final String SNAP_IMAGE_TILE_FORMAT_SIZE = "snap.image";

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("tile_size");
    }


    @Override
    protected PropertySet createPropertySet() {
        return createPropertySet(new TiepointBean());
    }

    @Override
    protected JPanel createPanel(BindingContext context) {

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.SOUTH);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        JPanel mainPanel = new JPanel(tableLayout);


        Property tileSize = context.getPropertySet().getProperty(SNAP_IMAGE_TILE_SIZE);
        Property forceTileSize = context.getPropertySet().getProperty(SNAP_IMAGE_FORCE_TILE_SIZE);
        Property imageFormatTileSize = context.getPropertySet().getProperty(SNAP_IMAGE_TILE_FORMAT_SIZE);

        PropertyEditorRegistry editorRegistry = PropertyEditorRegistry.getInstance();
        JComponent[] tileSizeComponent = editorRegistry.findPropertyEditor(tileSize.getDescriptor()).createComponents(tileSize.getDescriptor(), context);
        JComponent[] forceTileSizeComponent = editorRegistry.findPropertyEditor(forceTileSize.getDescriptor()).createComponents(forceTileSize.getDescriptor(), context);
        JComponent[] imageFormatTileSizeComponent = editorRegistry.findPropertyEditor(imageFormatTileSize.getDescriptor()).createComponents(imageFormatTileSize.getDescriptor(), context);

        mainPanel.add(tileSizeComponent[1]);
        mainPanel.add(tileSizeComponent[0]);

        mainPanel.add(forceTileSizeComponent[1]);
        mainPanel.add(forceTileSizeComponent[0]);

        mainPanel.add(imageFormatTileSizeComponent[1]);
        mainPanel.add(imageFormatTileSizeComponent[0]);
        tableLayout.setTableFill(TableLayout.Fill.VERTICAL);
        mainPanel.setBorder(new TitledBorder(""));
        mainPanel.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(mainPanel, BorderLayout.CENTER);

        return parent;
    }

    @Override
    protected BindingContext getBindingContext() {
        BindingContext bindingContext = new BindingContext();

        return bindingContext;
    }

    static class TiepointBean {

        @Preference(label = "Force tile size", key = SNAP_IMAGE_FORCE_TILE_SIZE)
        int forceTileSize;

        @Preference(label = "Tile size", key = SNAP_IMAGE_TILE_SIZE)
        int tileSize;

        @Preference(label = "Tile size", key = SNAP_IMAGE_TILE_FORMAT_SIZE, valueSet = {"JPEG", "BIG TIFF"})
        String imageTileSize;
    }
}
