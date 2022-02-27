package me.angelstoyanov.sporton.azure.storage.route;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.quarkus.runtime.annotations.RegisterForReflection;
import me.angelstoyanov.sporton.azure.storage.bean.StorageRequestHandler;
import me.angelstoyanov.sporton.azure.storage.config.AzureStorageConfig;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.http.HttpStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named("StorageRoute")
@RegisterForReflection(targets = {HttpOperationFailedException.class, StorageRoute.class, HttpStatus.class}, serialization = true)
@ApplicationScoped
public class StorageRoute extends RouteBuilder {

    @Inject
    protected AzureStorageConfig configuration;

    @Override
    public void configure() throws Exception {

        configureClient();

        from("platform-http:/upload").routeId("[Azure Storage][Image] Upload")
                .convertBodyTo(byte[].class)
                .bean(StorageRequestHandler.class, "prepareStorageRequest")
                .to("azure-storage-blob://" +
                        configuration.getAccountName() +
                        "/default" +
                        "?blobName=blob" +
                        "&operation=uploadBlockBlob" +
                        "&serviceClient=#client")
                .convertBodyTo(String.class);

        from("platform-http:/fetch").routeId("[Azure Storage][Image] Fetch")
                .convertBodyTo(byte[].class)
                .bean(StorageRequestHandler.class, "prepareStorageRequest")
                .to("azure-storage-blob://" +
                        configuration.getAccountName() +
                        "/default" +
                        "?blobName=blob" +
                        "&operation=getBlob" +
                        "&serviceClient=#client")
                .bean(StorageRequestHandler.class, "convertResponseBodyToImage");
    }

    private void configureClient() {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(
                configuration.getAccountName(),
                configuration.getAccountKey());
        String uri = String.format("https://%s.blob.core.windows.net", configuration.getAccountName());
        BlobServiceClient client = new BlobServiceClientBuilder()
                .endpoint(uri)
                .credential(credential)
                .buildClient();

        this.getContext().getRegistry().bind("client", client);
    }
}
