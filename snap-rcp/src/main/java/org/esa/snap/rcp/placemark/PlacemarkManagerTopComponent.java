package org.esa.snap.rcp.placemark;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.beam.dataio.placemark.PlacemarkIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.FloatCellEditor;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.color.ColorTableCellEditor;
import org.esa.beam.framework.ui.color.ColorTableCellRenderer;
import org.esa.beam.framework.ui.product.AbstractPlacemarkTableModel;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.util.SelectionChangeSupport;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.openide.util.HelpCtx;
import org.openide.windows.TopComponent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * @author Tonio Fincke
 */
public class PlacemarkManagerTopComponent extends TopComponent implements HelpCtx.Provider {

    public static final String PROPERTY_KEY_IO_DIR = "pin.io.dir";

    private final PlacemarkDescriptor placemarkDescriptor;
    private final Preferences preferences;

    private SnapApp snapApp;
    private final HashMap<Product, Band[]> productToSelectedBands;
    private final HashMap<Product, TiePointGrid[]> productToSelectedGrids;

    private Product product;
    private JTable placemarkTable;
    private PlacemarkListener placemarkListener;
    private Band[] selectedBands;
    private TiePointGrid[] selectedGrids;
    private boolean synchronizingPlacemarkSelectedState;
    private AbstractPlacemarkTableModel placemarkTableModel;
    private String prefixTitle;
    private PlacemarkManagerButtons buttonPane;
    private ProductSceneView currentView;
    private final SelectionChangeListener selectionChangeHandler;

    public PlacemarkManagerTopComponent(PlacemarkDescriptor placemarkDescriptor, TableModelFactory modelFactory) {
        this.placemarkDescriptor = placemarkDescriptor;
        snapApp = SnapApp.getDefault();
        preferences = snapApp.getPreferences();
        productToSelectedBands = new HashMap<>(50);
        productToSelectedGrids = new HashMap<>(50);
        placemarkTableModel = modelFactory.createTableModel(placemarkDescriptor, product, null, null);
        selectionChangeHandler = new ViewSelectionChangeHandler();
        initUI();
    }

    public void initUI() {
        setLayout(new BorderLayout());
        prefixTitle = getTitle();
        placemarkTable = new JTable(placemarkTableModel);
        placemarkTable.setRowSorter(new TableRowSorter<>(placemarkTableModel));
        placemarkTable.setName("placemarkTable");
        placemarkTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        placemarkTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        placemarkTable.setRowSelectionAllowed(true);
        // IMPORTANT: We set ReorderingAllowed=false, because we export the
        // table model AS IS to a flat text file.
        placemarkTable.getTableHeader().setReorderingAllowed(false);

        ToolTipSetter toolTipSetter = new ToolTipSetter();
        placemarkTable.addMouseMotionListener(toolTipSetter);
        placemarkTable.addMouseListener(toolTipSetter);
        placemarkTable.addMouseListener(new PopupListener());
        placemarkTable.getSelectionModel().addListSelectionListener(new PlacemarkTableSelectionHandler());
        updateTableModel();

        final TableColumnModel columnModel = placemarkTable.getColumnModel();
        columnModel.addColumnModelListener(new ColumnModelListener());

        JScrollPane tableScrollPane = new JScrollPane(placemarkTable);
        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.add(tableScrollPane, BorderLayout.CENTER);

        buttonPane = new PlacemarkManagerButtons(this);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        content.add(BorderLayout.CENTER, mainPane);
        content.add(BorderLayout.EAST, buttonPane);
        Component southExtension = getSouthExtension();
        if (southExtension != null) {
            content.add(BorderLayout.SOUTH, southExtension);
        }
        content.setPreferredSize(new Dimension(420, 200));

        setCurrentView(snapApp.getSelectedProductSceneView());
        setProduct(snapApp.getSelectedProduct());
        snapApp.addProductSceneViewSelectionChangeListener(new ProductSceneViewSelectionChangeListener());
        snapApp.addProductNodeSelectionChangeListener(new ProductSelectionListener());
        snapApp.getProductManager().addListener(new ProductRemovedListener());
        updateUIState();
        add(content, BorderLayout.CENTER);
    }

    void applyFilteredGrids() {
        if (product != null) {
            Band[] allBands = product.getBands();
            TiePointGrid[] allGrids = product.getTiePointGrids();
            BandChooser bandChooser = new BandChooser(SwingUtilities.getWindowAncestor(this),
                                                      "Available Bands And Tie Point Grids",
                                                      getHelpId(), false,
                                                      allBands, selectedBands, allGrids, selectedGrids, true);
            if (bandChooser.show() == ModalDialog.ID_OK) {
                selectedBands = bandChooser.getSelectedBands();
                selectedGrids = bandChooser.getSelectedTiePointGrids();
                productToSelectedBands.put(product, selectedBands);
                productToSelectedGrids.put(product, selectedGrids);
                updateTableModel();
            }
        }
    }

    PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    private void setCurrentView(ProductSceneView sceneView) {
        if (sceneView != currentView) {
            if (currentView != null) {
                currentView.getSelectionContext().removeSelectionChangeListener(selectionChangeHandler);
            }
            currentView = sceneView;
            if (currentView != null) {
                currentView.getSelectionContext().addSelectionChangeListener(selectionChangeHandler);
                setProduct(currentView.getProduct());
            } else {
                setProduct(null);
            }
        }
    }

    protected final Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        if (this.product == product) {
            return;
        }
        Product oldProduct = this.product;
        if (oldProduct != null) {
            oldProduct.removeProductNodeListener(placemarkListener);
        }
        this.product = product;
        selectedBands = productToSelectedBands.get(this.product);
        selectedGrids = productToSelectedGrids.get(this.product);
        if (this.product != null) {
            setDisplayName(getTitle() + " - " + this.product.getProductRefString());
            if (placemarkListener == null) {
                placemarkListener = new PlacemarkListener();
            }
            this.product.addProductNodeListener(placemarkListener);
        } else {
            setDisplayName(getTitle());
        }

        updateTableModel();
        updatePlacemarkTableSelectionFromView();
        updateUIState();
    }

    protected String getTitle() {
        return "";
    }

    protected String getHelpId() {
        return null;
    }

    protected Component getSouthExtension() {
        return null;
    }

    private void updateTableModel() {
        placemarkTableModel.setProduct(product);
        placemarkTableModel.setSelectedBands(selectedBands);
        placemarkTableModel.setSelectedGrids(selectedGrids);
        addCellRenderer(placemarkTable.getColumnModel());
        addCellEditor(placemarkTable.getColumnModel());

    }

    protected void addCellRenderer(TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000")));
        columnModel.getColumn(1).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000")));
        columnModel.getColumn(2).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000000")));
        columnModel.getColumn(3).setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("0.000000")));
        columnModel.getColumn(4).setCellRenderer(new ColorTableCellRenderer());
        columnModel.getColumn(5).setCellRenderer(new RightAlignmentTableCellRenderer());
    }

    protected void addCellEditor(TableColumnModel columnModel) {
        final FloatCellEditor pixelCellEditor = new FloatCellEditor();
        columnModel.getColumn(0).setCellEditor(pixelCellEditor);
        columnModel.getColumn(1).setCellEditor(pixelCellEditor);
        columnModel.getColumn(2).setCellEditor(new FloatCellEditor(-180, 180));
        columnModel.getColumn(3).setCellEditor(new FloatCellEditor(-90, 90));
        columnModel.getColumn(4).setCellEditor(new ColorTableCellEditor());
//        ColorComboBox comboBox = new ColorComboBox();
//        comboBox.setColorValueVisible(true);
//        comboBox.setColorIconVisible(true);
//        comboBox.setInvalidValueAllowed(false);
//        comboBox.setAllowDefaultColor(true);
//        comboBox.setAllowMoreColors(true);
//        columnModel.getColumn(4).setCellEditor(new ColorCellEditor(comboBox));
//        columnModel.getColumn(4).setCellEditor(new ColorCE());
    }

    private ProductSceneView getSceneView() {
        final ProductSceneView selectedProductSceneView = snapApp.getSelectedProductSceneView();
        if (selectedProductSceneView == null && product != null) {
            final Band[] bands = product.getBands();
            for (Band band : bands) {
                ProductSceneViewTopComponent productSceneViewTopComponent = getProductSceneViewTopComponent(band);
                if (productSceneViewTopComponent != null) {
                    return productSceneViewTopComponent.getView();
                }
            }
            final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
            for (TiePointGrid tiePointGrid : tiePointGrids) {
                ProductSceneViewTopComponent productSceneViewTopComponent = getProductSceneViewTopComponent(tiePointGrid);
                if(productSceneViewTopComponent != null) {
                    return productSceneViewTopComponent.getView();
                }
            }
        }
        return selectedProductSceneView;
    }

    //copied from TimeSeriesManagerForm
    private ProductSceneViewTopComponent getProductSceneViewTopComponent(RasterDataNode raster) {
        return WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .filter(topComponent -> raster == topComponent.getView().getRaster())
                .findFirst()
                .orElse(null);
    }

    private Placemark getPlacemarkAt(final int selectedRow) {
        Placemark placemark = null;
        if (product != null) {
            if (selectedRow > -1 && selectedRow < getPlacemarkGroup(product).getNodeCount()) {
                placemark = getPlacemarkGroup(product).get(selectedRow);
            }
        }
        return placemark;
    }

    void newPin() {
        Guardian.assertNotNull("product", product);
        String[] uniquePinNameAndLabel = PlacemarkNameFactory.createUniqueNameAndLabel(placemarkDescriptor, product);
        Placemark newPlacemark = Placemark.createPointPlacemark(placemarkDescriptor, uniquePinNameAndLabel[0],
                                                                uniquePinNameAndLabel[1],
                                                                "",
                                                                new PixelPos(0, 0), null,
                                                                product.getGeoCoding());
        if (PlacemarkDialog.showEditPlacemarkDialog(
                SwingUtilities.getWindowAncestor(this), product, newPlacemark, placemarkDescriptor)) {
            makePlacemarkNameUnique(newPlacemark);
            updateUIState();
        }
    }

    void copyActivePlacemark() {
        Guardian.assertNotNull("product", product);
        Placemark activePlacemark = getSelectedPlacemark();
        Guardian.assertNotNull("activePlacemark", activePlacemark);
        Placemark newPlacemark = Placemark.createPointPlacemark(activePlacemark.getDescriptor(),
                                                                "copy_of_" + activePlacemark.getName(),
                                                                activePlacemark.getLabel(),
                                                                activePlacemark.getDescription(),
                                                                activePlacemark.getPixelPos(),
                                                                activePlacemark.getGeoPos(),
                                                                activePlacemark.getProduct().getGeoCoding());
        newPlacemark.setStyleCss(activePlacemark.getStyleCss());
        if (PlacemarkDialog.showEditPlacemarkDialog(
                SwingUtilities.getWindowAncestor(this), product, newPlacemark, placemarkDescriptor)) {
            makePlacemarkNameUnique(newPlacemark);
            updateUIState();
        }
    }

    private ProductNodeGroup<Placemark> getPlacemarkGroup(Product product) {
        return placemarkDescriptor.getPlacemarkGroup(product);
    }

    void editActivePin() {
        Guardian.assertNotNull("product", product);
        Placemark activePlacemark = getSelectedPlacemark();
        Guardian.assertNotNull("activePlacemark", activePlacemark);
        if (PlacemarkDialog.showEditPlacemarkDialog(SwingUtilities.getWindowAncestor(this), product, activePlacemark,
                                                    placemarkDescriptor)) {
            makePlacemarkNameUnique(activePlacemark);
            updateUIState();
        }
    }

    void removeSelectedPins() {
        final List<Placemark> placemarks = getSelectedPlacemarks();
        int i = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
                                              MessageFormat.format(
                                                      "Do you really want to remove {0} selected {1}(s)?\n" +
                                                              "This action can not be undone.",
                                                      placemarks.size(), placemarkDescriptor.getRoleLabel()),
                                              MessageFormat.format("{0} - Remove {1}s", getTitle(),
                                                                   placemarkDescriptor.getRoleLabel()),
                                              JOptionPane.OK_CANCEL_OPTION);
        if (i == JOptionPane.OK_OPTION) {
            int selectedRow = placemarkTable.getSelectedRow();
            for (Placemark placemark : placemarks) {
                getPlacemarkGroup(product).remove(placemark);
            }
            if (selectedRow >= getPlacemarkGroup(product).getNodeCount()) {
                selectedRow = getPlacemarkGroup(product).getNodeCount() - 1;
            }
            if (selectedRow >= 0) {
                placemarkTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
            }
            updateUIState();
        }
    }

    private int getNumSelectedPlacemarks() {
        int[] rowIndexes = placemarkTable.getSelectedRows();
        return rowIndexes != null ? rowIndexes.length : 0;
    }

    private Placemark getSelectedPlacemark() {
        int rowIndex = placemarkTable.getSelectedRow();
        if (rowIndex >= 0) {
            final int modelIndex = placemarkTable.convertRowIndexToModel(rowIndex);
            return placemarkTableModel.getPlacemarkAt(modelIndex);
        }
        return null;
    }

    private List<Placemark> getSelectedPlacemarks() {
        List<Placemark> placemarkList = new ArrayList<>();
        int[] sortedRowIndexes = placemarkTable.getSelectedRows();
        if (sortedRowIndexes != null) {
            for (int rowIndex : sortedRowIndexes) {
//                int modelRowIndex = placemarkTable.getActualRowAt(rowIndex);
                int modelRowIndex = placemarkTable.convertRowIndexToModel(rowIndex);
                placemarkList.add(placemarkTableModel.getPlacemarkAt(modelRowIndex));
            }
        }
        return placemarkList;
    }

    void zoomToActivePin() {
        Guardian.assertNotNull("product", product);
        Placemark activePlacemark = getSelectedPlacemark();
        Guardian.assertNotNull("activePlacemark", activePlacemark);
        final ProductSceneView view = getSceneView();
        final PixelPos imagePos = activePlacemark.getPixelPos();  // in image coordinates on Level 0, can be null
        if (view != null && imagePos != null) {
            final ImageLayer layer = view.getBaseImageLayer();
            final AffineTransform imageToModelTransform = layer.getImageToModelTransform(0);
            final Point2D modelPos = imageToModelTransform.transform(imagePos, null);
            view.zoom(modelPos.getX(), modelPos.getY(), view.getZoomFactor());
            updateUIState();
        }
    }

    // }} Actions
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private void makePlacemarkNameUnique(Placemark newPlacemark) {
        if (makePlacemarkNameUnique0(newPlacemark, product)) {
            SnapDialogs.showWarning(MessageFormat.format("{0} has been renamed to ''{1}'',\n" +
                                                                 "because a {2} with the former name already exists.",
                                                         StringUtils.firstLetterUp(placemarkDescriptor.getRoleLabel()),
                                                         newPlacemark.getName(),
                                                         placemarkDescriptor.getRoleLabel()));
        }
    }

    protected void updateUIState() {
        boolean productSelected = product != null;
        int numSelectedPins = 0;
        if (productSelected) {
            updatePlacemarkTableSelectionFromView();
            numSelectedPins = getNumSelectedPlacemarks();
            setDisplayName(prefixTitle + " - " + product.getDisplayName());
        } else {
            setDisplayName(prefixTitle);
        }

        placemarkTable.setEnabled(productSelected);
        buttonPane.updateUIState(productSelected, placemarkTable.getRowCount(), numSelectedPins);
    }

    private void updatePlacemarkTableSelectionFromView() {
        if (!synchronizingPlacemarkSelectedState) {
            try {
                synchronizingPlacemarkSelectedState = true;
                if (product != null) {
                    Placemark[] placemarks = placemarkTableModel.getPlacemarks();
                    for (int i = 0; i < placemarks.length; i++) {
                        if (i < placemarkTable.getRowCount()) {
                            Placemark placemark = placemarks[i];
                            int sortedRowAt = placemarkTable.convertRowIndexToView(i);
                            boolean selected = isPlacemarkSelectedInView(placemark);
                            if (selected != placemarkTable.isRowSelected(sortedRowAt)) {
                                if (selected) {
                                    placemarkTable.getSelectionModel().addSelectionInterval(sortedRowAt, sortedRowAt);
                                } else {
                                    placemarkTable.getSelectionModel().removeSelectionInterval(sortedRowAt,
                                                                                               sortedRowAt);
                                }
                            }
                        }
                    }
                }
                placemarkTable.revalidate();
                placemarkTable.repaint();
            } finally {
                synchronizingPlacemarkSelectedState = false;
            }
        }
    }

    void importPlacemarks(boolean allPlacemarks) {
        List<Placemark> placemarks;
        try {
            placemarks = loadPlacemarksFromFile();
        } catch (IOException e) {
            e.printStackTrace();
            SnapDialogs.showError(MessageFormat.format("I/O error, failed to import {0}s:\n{1}",  /*I18N*/
                                                       placemarkDescriptor.getRoleLabel(), e.getMessage()));
            return;
        }
        if (placemarks.isEmpty()) {
            return;
        }
        addPlacemarksToProduct(placemarks, product, allPlacemarks);
    }

    void addPlacemarksToProduct(List<Placemark> placemarks, Product targetProduct, boolean allPlacemarks) {
        final GeoCoding geoCoding = targetProduct.getGeoCoding();
        final boolean canGetPixelPos = geoCoding != null && geoCoding.canGetPixelPos();
        final boolean isPin = placemarkDescriptor instanceof PinDescriptor;

        int numPinsOutOfBounds = 0;
        int numPinsRenamed = 0;
        int numInvalids = 0;

        for (Placemark placemark : placemarks) {
            if (makePlacemarkNameUnique0(placemark, targetProduct)) {
                numPinsRenamed++;
            }

            final PixelPos pixelPos = placemark.getPixelPos();
            boolean productContainsPixelPos = targetProduct.containsPixel(pixelPos);
            if (!canGetPixelPos && isPin && !productContainsPixelPos) {
                numInvalids++;
                continue;
            }

            // from here on we only handle GCPs and valid Pins

            if (canGetPixelPos) {
                placemarkDescriptor.updatePixelPos(geoCoding, placemark.getGeoPos(), pixelPos);
            }

            if (!productContainsPixelPos && isPin) {
                numPinsOutOfBounds++;
            } else {
                getPlacemarkGroup(targetProduct).add(placemark);
                placemark.setPixelPos(pixelPos);
            }

            if (!allPlacemarks) {
                break; // import only the first one
            }
        }

        String intoProductMessage = "";
        if (targetProduct != product) {
            intoProductMessage = "into product " + targetProduct.getDisplayName() + "\n";
        }
        if (numInvalids > 0) {
            SnapDialogs.showWarning(MessageFormat.format(
                    "One or more {0}s have not been imported,\n{1}because they can not be assigned to a product without a geo-coding.",
                    placemarkDescriptor.getRoleLabel(), intoProductMessage));
        }
        if (numPinsRenamed > 0) {
            SnapDialogs.showWarning(MessageFormat.format(
                    "One or more {0}s have been renamed,\n{1}because their former names are already existing.",
                    placemarkDescriptor.getRoleLabel(), intoProductMessage));
        }
        if (numPinsOutOfBounds > 0) {
            if (numPinsOutOfBounds == placemarks.size()) {
                SnapDialogs.showError(
                        MessageFormat.format(
                                "No {0}s have been imported,\n{1}because their pixel positions\nare outside the product''s bounds.",
                                placemarkDescriptor.getRoleLabel(), intoProductMessage)
                );
            } else {
                SnapDialogs.showError(
                        MessageFormat.format(
                                "{0} {1}s have not been imported,\n{2}because their pixel positions\nare outside the product''s bounds.",
                                numPinsOutOfBounds, placemarkDescriptor.getRoleLabel(), intoProductMessage)
                );
            }
        }
    }

    private boolean isPlacemarkSelectedInView(Placemark placemark) {
        boolean selected = false;
        final ProductSceneView sceneView = getSceneView();
        if (sceneView != null) {
            if (getPlacemarkDescriptor() instanceof PinDescriptor) {
                selected = sceneView.isPinSelected(placemark);
            } else {
                selected = sceneView.isGcpSelected(placemark);
            }
        }
        return selected;
    }

    private boolean makePlacemarkNameUnique0(Placemark placemark, Product targetProduct) {
        ProductNodeGroup<Placemark> placemarkGroup = getPlacemarkGroup(targetProduct);
        if (placemarkGroup.get(placemark.getName()) == placemark) {
            return false;
        }
        String name0 = placemark.getName();
        String name = name0;
        int id = 1;
        while (placemarkGroup.contains(name)) {
            name = name0 + "_" + id;
            id++;
        }
        if (!name0.equals(name)) {
            placemark.setName(name);
            return true;
        }
        return false;
    }

    private List<Placemark> loadPlacemarksFromFile() throws IOException {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        String roleLabel = StringUtils.firstLetterUp(placemarkDescriptor.getRoleLabel());
        fileChooser.setDialogTitle("Import " + roleLabel + "s"); /*I18N*/
        setComponentName(fileChooser, "Import");
        fileChooser.addChoosableFileFilter(PlacemarkIO.createTextFileFilter());
        fileChooser.setFileFilter(PlacemarkIO.createPlacemarkFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        int result = fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                GeoCoding geoCoding = null;
                if (product != null) {
                    geoCoding = product.getGeoCoding();
                }
                return PlacemarkIO.readPlacemarks(new FileReader(file), geoCoding, placemarkDescriptor);
            }
        }
        return Collections.emptyList();
    }

    void exportPlacemarks() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle(MessageFormat.format("Export {0}(s)",
                                                        StringUtils.firstLetterUp(placemarkDescriptor.getRoleLabel())));   /*I18N*/
        setComponentName(fileChooser, "Export_Selected");
        fileChooser.addChoosableFileFilter(PlacemarkIO.createTextFileFilter());
        fileChooser.setFileFilter(PlacemarkIO.createPlacemarkFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, placemarkDescriptor.getRoleName()));
        int result = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                final Boolean overwriteDecision = SnapDialogs.requestOverwriteDecision(prefixTitle, file);
                if (overwriteDecision == null || !overwriteDecision) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                BeamFileFilter beamFileFilter = fileChooser.getBeamFileFilter();
                String fileExtension = FileUtils.getExtension(file);
                if (fileExtension == null || !StringUtils.contains(beamFileFilter.getExtensions(), fileExtension)) {
                    file = FileUtils.ensureExtension(file, beamFileFilter.getDefaultExtension());
                }
                try {
                    if (beamFileFilter.getFormatName().equals(
                            PlacemarkIO.createPlacemarkFileFilter().getFormatName())) {

                        final List<Placemark> placemarkList = getPlacemarksForExport();
                        PlacemarkIO.writePlacemarksFile(new FileWriter(file), placemarkList);
                    } else {
                        try (Writer writer = new FileWriter(file)) {
                            writePlacemarkDataTableText(writer);
                        }
                    }
                } catch (IOException ioe) {
                    SnapDialogs.showError(String.format("I/O Error.\n   Failed to export %ss.\n%s",
                                          placemarkDescriptor.getRoleLabel(), ioe.getMessage()));
                    ioe.printStackTrace();
                }
            }
        }
    }

    private List<Placemark> getPlacemarksForExport() {
        boolean noPlacemarksSelected = placemarkTable.getSelectionModel().isSelectionEmpty();
        final List<Placemark> placemarkList;
        if (noPlacemarksSelected) {
            placemarkList = Arrays.asList(placemarkTableModel.getPlacemarks());
        } else {
            placemarkList = getSelectedPlacemarks();
        }
        return placemarkList;
    }

    void transferPlacemarks() {
        // ask for destination product
        Product thisProduct = getProduct();
        Product[] allProducts = snapApp.getProductManager().getProducts();
        if (allProducts.length < 2 || thisProduct == null) {
            return;
        }
        Product[] allOtherProducts = new Product[allProducts.length - 1];
        int allOtherProductsIndex = 0;
        for (int i = 0; i < allProducts.length; i++) {
            Product product = allProducts[i];
            if (product != thisProduct) {
                allOtherProducts[allOtherProductsIndex++] = product;
            }
        }
        ProductChooser productChooser = new ProductChooser(
                snapApp.getMainFrame(),
                prefixTitle,
                getHelpId(),
                allOtherProducts,
                null);
        int buttonID = productChooser.show();
        System.out.println("buttonID = " + buttonID);
        if (buttonID == AbstractDialog.ID_OK) {
            // copy placemarks
            List<Placemark> placemarks = getPlacemarksForExport();
            Product[] selectedProducts = productChooser.getSelectedProducts();
            for (Product selectedProduct : selectedProducts) {
                List<Placemark> placemarksCopy = new ArrayList<>(placemarks.size());
                for (Placemark placemark : placemarks) {
                    Placemark newPlacemark = Placemark.createPointPlacemark(placemark.getDescriptor(),
                                                                            placemark.getName(),
                                                                            placemark.getLabel(),
                                                                            placemark.getDescription(),
                                                                            placemark.getPixelPos(),
                                                                            placemark.getGeoPos(),
                                                                            selectedProduct.getGeoCoding());
                    newPlacemark.setStyleCss(placemark.getStyleCss());
                    placemarksCopy.add(newPlacemark);
                }
                addPlacemarksToProduct(placemarksCopy, selectedProduct, true);
            }
        }
    }

    private void setComponentName(JComponent component, String name) {
        component.setName(getClass().getName() + "." + name);
    }

    void exportPlacemarkDataTable() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle(MessageFormat.format("Export {0} Data Table",  /*I18N*/
                                                        StringUtils.firstLetterUp(placemarkDescriptor.getRoleLabel())));
        setComponentName(fileChooser, "Export_Data_Table");
        fileChooser.setFileFilter(PlacemarkIO.createTextFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, "Data"));
        int result = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                final Boolean overwriteDecision = SnapDialogs.requestOverwriteDecision(prefixTitle, file);
                if (overwriteDecision == null || !overwriteDecision) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                file = FileUtils.ensureExtension(file, PlacemarkIO.FILE_EXTENSION_FLAT_TEXT);
                try {
                    try (Writer writer = new FileWriter(file)) {
                        writePlacemarkDataTableText(writer);
                    }
                } catch (IOException ignored) {
                    SnapDialogs.showError(MessageFormat.format("I/O Error.\nFailed to export {0} data table.",  /*I18N*/
                                                         placemarkDescriptor.getRoleLabel()));
                }
            }
        }
    }

    private void writePlacemarkDataTableText(final Writer writer) {

        final String[] standardColumnNames = placemarkTableModel.getStandardColumnNames();
        final int columnCountMin = standardColumnNames.length;
        final int columnCount = placemarkTableModel.getColumnCount();
        String[] additionalColumnNames = new String[columnCount - columnCountMin];
        for (int i = 0; i < additionalColumnNames.length; i++) {
            additionalColumnNames[i] = placemarkTableModel.getColumnName(columnCountMin + i);
        }

        List<Placemark> placemarkList = new ArrayList<>();
        List<Object[]> valueList = new ArrayList<>();
        for (int sortedRow = 0; sortedRow < placemarkTable.getRowCount(); ++sortedRow) {
            ListSelectionModel selectionModel = placemarkTable.getSelectionModel();
            if (selectionModel.isSelectionEmpty() || selectionModel.isSelectedIndex(sortedRow)) {
                final int modelRow = placemarkTable.convertRowIndexToModel(sortedRow);
                placemarkList.add(placemarkTableModel.getPlacemarkAt(modelRow));
                Object[] values = new Object[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    values[col] = placemarkTableModel.getValueAt(modelRow, col);
                }
                valueList.add(values);
            }
        }

        PlacemarkIO.writePlacemarksWithAdditionalData(writer,
                                                      placemarkDescriptor.getRoleLabel(),
                                                      product.getName(),
                                                      placemarkList,
                                                      valueList,
                                                      standardColumnNames,
                                                      additionalColumnNames);
    }

    private void setIODir(File dir) {
        if (preferences != null && dir != null) {
            preferences.put(PROPERTY_KEY_IO_DIR, dir.getPath());
        }
    }

    private File getIODir() {
        File dir = SystemUtils.getUserHomeDir();
        if (preferences != null) {
            dir = new File(preferences.get(PROPERTY_KEY_IO_DIR, dir.getPath()));
        }
        return dir;
    }

    private class PlacemarkListener implements ProductNodeListener {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Placemark && sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(
                    product)) {
                updateUIState();
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Placemark && sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(
                    product)) {
                updateUIState();
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Placemark && sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(
                    product)) {
                placemarkTableModel.addPlacemark((Placemark) sourceNode);
                updateUIState();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Placemark && sourceNode.getOwner() == placemarkDescriptor.getPlacemarkGroup(
                    product)) {
                placemarkTableModel.removePlacemark((Placemark) sourceNode);
                updateUIState();
            }
        }

    }

    private class ToolTipSetter extends MouseInputAdapter {

        private int _rowIndex;

        private ToolTipSetter() {
            _rowIndex = -1;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            _rowIndex = -1;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int rowIndex = placemarkTable.rowAtPoint(e.getPoint());
            if (rowIndex != _rowIndex) {
                _rowIndex = rowIndex;
                if (_rowIndex >= 0 && _rowIndex < placemarkTable.getRowCount()) {
                    GeoPos geoPos = getPlacemarkAt(placemarkTable.convertRowIndexToModel(_rowIndex)).getGeoPos();
                    if (geoPos != null) {
                        placemarkTable.setToolTipText(geoPos.getLonString() + " / " + geoPos.getLatString());
                    }
                }
            }
        }

    }

    private static class ColumnModelListener implements TableColumnModelListener {

        @Override
        public void columnAdded(TableColumnModelEvent e) {
            int minWidth;
            final int index = e.getToIndex();
            switch (index) {
                case 0:
                case 1:
                    minWidth = 60;
                    break;
                default:
                    minWidth = 80;
            }
            TableColumnModel columnModel = (TableColumnModel) e.getSource();
            columnModel.getColumn(index).setPreferredWidth(minWidth);
            columnModel.getColumn(index).setCellRenderer(new RightAlignmentTableCellRenderer());
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    }

    private class PopupListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            action(e);
        }

        private void action(MouseEvent e) {
            if (e.isPopupTrigger()) {

                if (getNumSelectedPlacemarks() > 0) {
                    final JPopupMenu popupMenu = new JPopupMenu();
                    final JMenuItem menuItem;
                    menuItem = new JMenuItem("Copy selected data to clipboard");
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            final StringWriter stringWriter = new StringWriter();
                            writePlacemarkDataTableText(stringWriter);
                            String text = stringWriter.toString();
                            text = text.replaceAll("\r\n", "\n");
                            text = text.replaceAll("\r", "\n");
                            SystemUtils.copyToClipboard(text);
                        }
                    });

                    popupMenu.add(menuItem);
                    final Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), placemarkTable);
                    popupMenu.show(placemarkTable, point.x, point.y);
                }
            }
        }
    }

    private class ProductSceneViewSelectionChangeListener implements SelectionChangeSupport.Listener<ProductSceneView> {

        @Override
        public void selected(ProductSceneView first, ProductSceneView... more) {
            setCurrentView(first);
        }

        @Override
        public void deselected(ProductSceneView first, ProductSceneView... more) {
            if (first == currentView) {
                setCurrentView(null);
            }
        }

    }

    private class ProductRemovedListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            //do nothing
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            if (product == getProduct()) {
                setProduct(null);
                productToSelectedBands.remove(product);
                productToSelectedGrids.remove(product);
            }
        }

    }

    private class ProductSelectionListener implements SelectionChangeSupport.Listener<ProductNode> {

        @Override
        public void selected(ProductNode first, ProductNode... more) {
            setProduct(first.getProduct());
        }

        @Override
        public void deselected(ProductNode first, ProductNode... more) {
            //do nothing
        }

    }


    private class PlacemarkTableSelectionHandler implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting() || e.getFirstIndex() == -1 || synchronizingPlacemarkSelectedState) {
                return;
            }

            try {
                synchronizingPlacemarkSelectedState = true;
                Placemark[] placemarks = placemarkTableModel.getPlacemarks();
                ArrayList<Placemark> selectedPlacemarks = new ArrayList<>();
                for (int i = 0; i < placemarks.length; i++) {
                    Placemark placemark = placemarks[i];
                    int sortedIndex = placemarkTable.convertRowIndexToView(i);
                    if (placemarkTable.isRowSelected(sortedIndex)) {
                        selectedPlacemarks.add(placemark);
                    }
                }
                ProductSceneView sceneView = getSceneView();
                if (sceneView != null) {
                    Placemark[] placemarkArray = selectedPlacemarks.toArray(new Placemark[selectedPlacemarks.size()]);
                    if (getPlacemarkDescriptor() instanceof PinDescriptor) {
                        sceneView.selectPins(placemarkArray);
                    } else {
                        sceneView.selectGcps(placemarkArray);
                    }
                }
            } finally {
                updateUIState();
                synchronizingPlacemarkSelectedState = false;
            }

        }
    }

    private class ViewSelectionChangeHandler implements SelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            if (synchronizingPlacemarkSelectedState) {
                return;
            }
            final ProductSceneView sceneView = getSceneView();
            if (sceneView != null) {
                Layer layer = sceneView.getSelectedLayer();
                if (layer instanceof VectorDataLayer) {
                    VectorDataLayer vectorDataLayer = (VectorDataLayer) layer;
                    if (vectorDataLayer.getVectorDataNode() == getProduct().getPinGroup().getVectorDataNode() ||
                            vectorDataLayer.getVectorDataNode() == getProduct().getGcpGroup().getVectorDataNode()) {
                        updateUIState();
                    }
                }
            }
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
        }
    }

    private static class RightAlignmentTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                                                              column);
            label.setHorizontalAlignment(JLabel.RIGHT);
            return label;


        }
    }

    //copied from MaskTable
//    private static class ColorCE extends ColorCellEditor {
//        @Override
//        protected ColorExComboBox createColorComboBox() {
//        protected ColorComboBox createColorComboBox() {
//            ColorExComboBox comboBox = super.createColorComboBox();
//            ColorComboBox comboBox = super.createColorComboBox();
//            comboBox.setColorValueVisible(true);
//            comboBox.setColorIconVisible(true);
//            comboBox.setInvalidValueAllowed(false);
//            comboBox.setAllowDefaultColor(true);
//            comboBox.setAllowMoreColors(true);
//            return comboBox;
//        }
//    }

    //copied from MaskTable
//    private static class ColorCR extends ColorCellRenderer {
//        @Override
//        public Component getTableCellRendererComponent(JTable table, Object value,
//                                                       boolean isSelected, boolean hasFocus, int row, int column) {
//            setColorIconVisible(true);
//            setColorValueVisible(true);
//            setCrossBackGroundStyle(true);
//            return super.getTableCellRendererComponent(table, value,
//                                                       isSelected, hasFocus, row, column);
//        }
//    }

}