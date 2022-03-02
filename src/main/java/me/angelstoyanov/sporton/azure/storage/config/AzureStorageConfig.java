package me.angelstoyanov.sporton.azure.storage.config;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import javax.enterprise.context.ApplicationScoped;

@ConfigMapping(prefix = "sporton.storage.azure")
@RegisterForReflection
@ApplicationScoped
public interface AzureStorageConfig {

    @WithName("account.name")
    String getAccountName();

    @WithName("account.key")
    String getAccountKey();

    @WithName("container.name.comments")
    String getCommentsContainerName();

    @WithName("container.name.pitches")
    String getPitchesContainerName();
}
