package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressPanel;
import org.esa.snap.product.library.v2.database.DerbyDAL;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by jcoravu on 19/8/2019.
 */
public class DownloadProductTimerRunnable extends AbstractProgressTimerRunnable<Path> {

    private final String dataSourceName;
    private final RepositoryProduct productToDownload;
    private final JComponent parentComponent;
    private final QueryProductResultsPanel productResultsPanel;
    private final ProductRepositoryDownloader dataSourceProductDownloader;
    private final Path targetFolderPath;

    public DownloadProductTimerRunnable(ProgressPanel progressPanel, int threadId, String dataSourceName,
                                        ProductRepositoryDownloader dataSourceProductDownloader, RepositoryProduct productToDownload,
                                        Path targetFolderPath, QueryProductResultsPanel productResultsPanel, JComponent parentComponent) {

        super(progressPanel, threadId, 500);

        this.dataSourceName = dataSourceName;
        this.targetFolderPath = targetFolderPath;
        this.dataSourceProductDownloader = dataSourceProductDownloader;
        this.productToDownload = productToDownload;
        this.parentComponent = parentComponent;
        this.productResultsPanel = productResultsPanel;
    }

    @Override
    protected Path execute() throws Exception {
        notifyDownloadingProgressValueLater((short)0);

        ProgressListener progressListener = new ProgressListener() {
            @Override
            public void notifyProgress(short progressPercent) {
                notifyDownloadingProgressValueLater(progressPercent);
            }
        };
        Path productPath = this.dataSourceProductDownloader.download(this.productToDownload, this.targetFolderPath, progressListener);

        // successfully downloaded the product
        notifyDownloadingProgressValueLater((short)100);

        DerbyDAL.saveProduct(this.productToDownload, productPath);

        return productPath;
    }

    @Override
    public void stopRunning() {
        super.stopRunning();

        this.dataSourceProductDownloader.cancel();
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product from '" + this.dataSourceName + "'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.parentComponent, "Failed to download the product from " + this.dataSourceName + ".", "Error");
    }

    private void notifyDownloadingProgressValueLater(short progressPercent) {
        GenericRunnable<Short> runnable = new GenericRunnable<Short>(progressPercent) {
            @Override
            protected void execute(Short progressPercentValue) {
                if (isCurrentProgressPanelThread()) {
                    productResultsPanel.setProductDownloadPercent(productToDownload, progressPercentValue);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
