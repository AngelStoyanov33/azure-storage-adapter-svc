package me.angelstoyanov.sporton.azure.storage.route;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.quarkus.runtime.annotations.RegisterForReflection;
import me.angelstoyanov.sporton.azure.storage.bean.StorageRequestHandler;
import me.angelstoyanov.sporton.azure.storage.config.AzureStorageConfig;
import me.angelstoyanov.sporton.azure.storage.entity.StorageEntity;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Native;

@Named("StorageRoute")
@RegisterForReflection(targets = {StorageRequestHandler.class, HttpOperationFailedException.class, StorageRoute.class, HttpStatus.class, IllegalArgumentException.class}, serialization = true)
@ApplicationScoped
public class StorageRoute extends RouteBuilder {

    @Inject
    protected AzureStorageConfig configuration;
    @Native
    private static final String ENTITY_ID_QUERY_PARAM_NAME = "entityId";
    @Native
    private static final String ENTITY_TYPE = "entityType";
    @Native
    private static final String DEFAULT_IMAGE_FORMAT = ".jpg";
    @Native
    private static final String DEFAULT_IMAGE_HTTP_CONTENT_TYPE = "image/jpeg";

    @Override
    public void configure() throws Exception {

        configureClient();
        this.getCamelContext().setManagementName("azure-storage-adapter-svc");


        onException(BlobStorageException.class)
                .process(exchange -> {
                    BlobStorageException e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, BlobStorageException.class);
                    int errorCode = e.getStatusCode();
                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, errorCode);
                        exchange.getIn().setBody(e.getServiceMessage());
                }).handled(true);

        onException(IllegalArgumentException.class)
                .process(exchange -> {
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_BAD_REQUEST);
                    exchange.getIn().setBody("Invalid request. Entity type is not supported.");
                }).handled(true);

        from("platform-http:/upload").routeId("[Azure Storage][Image] Upload")
                .convertBodyTo(byte[].class)
                .process(this::prepareStorageRequest)
                .to("azure-storage-blob://" +
                        configuration.getAccountName() +
                        "/default" +
                        "?blobName=blob" +
                        "&operation=uploadBlockBlob" +
                        "&serviceClient=#client")
                .process(exchange -> {
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_OK);
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
                    exchange.getIn().setBody("Image uploaded successfully.");
                });

        from("platform-http:/fetch").routeId("[Azure Storage][Image] Fetch")
                .convertBodyTo(byte[].class)
                .process(this::prepareStorageRequest)
                .to("azure-storage-blob://" +
                        configuration.getAccountName() +
                        "/default" +
                        "?blobName=blob" +
                        "&operation=getBlob" +
                        "&serviceClient=#client")
                .process(this::convertResponseBodyToImage);
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

    public void prepareStorageRequest(Exchange exchange) {
        String entityId = exchange.getIn().getHeader(ENTITY_ID_QUERY_PARAM_NAME, String.class);
        String entityType = exchange.getIn().getHeader(ENTITY_TYPE, String.class);
        String containerName = getContainerName(entityType);
        exchange.getIn().setHeader(BlobConstants.BLOB_NAME, entityId + DEFAULT_IMAGE_FORMAT);
        exchange.getIn().setHeader(BlobConstants.BLOB_CONTAINER_NAME, containerName);
    }

    public void convertResponseBodyToImage(Exchange exchange) throws IOException {
        InputStream inputStream = exchange.getMessage().getBody(InputStream.class);
        byte[] targetArray = IOUtils.toByteArray(inputStream);
        exchange.getMessage().setBody(targetArray, byte[].class);
        exchange.getMessage().setHeader("content-type", DEFAULT_IMAGE_HTTP_CONTENT_TYPE);
    }

    private String getContainerName(String entityType) {
        String containerName;
        if (StorageEntity.valueOf(entityType).equals(StorageEntity.IMAGE_COMMENT)) {
            containerName = configuration.getCommentsContainerName();
        } else if (StorageEntity.valueOf(entityType).equals(StorageEntity.IMAGE_PITCH)) {
            containerName = configuration.getPitchesContainerName();
        } else {
            throw new IllegalArgumentException("Invalid entity type");
        }
        return containerName;
    }
}
