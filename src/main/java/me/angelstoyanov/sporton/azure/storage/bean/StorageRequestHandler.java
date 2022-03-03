package me.angelstoyanov.sporton.azure.storage.bean;

import io.quarkus.runtime.annotations.RegisterForReflection;
import me.angelstoyanov.sporton.azure.storage.config.AzureStorageConfig;
import me.angelstoyanov.sporton.azure.storage.entity.StorageEntity;
import org.apache.camel.Exchange;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.commons.io.IOUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Native;

@Named("StorageRequestHandler")
@RegisterForReflection(targets = {IllegalArgumentException.class, StorageRequestHandler.class}, serialization = true)
@ApplicationScoped
@Deprecated(forRemoval = true)
public class StorageRequestHandler {

    @Native
    private static final String ENTITY_ID_QUERY_PARAM_NAME = "entityId";
    @Native
    private static final String ENTITY_TYPE = "entityType";
    @Native
    private static final String DEFAULT_IMAGE_FORMAT = ".jpg";
    @Native
    private static final String DEFAULT_IMAGE_HTTP_CONTENT_TYPE = "image/jpeg";

    @Inject
    protected AzureStorageConfig configuration;
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
