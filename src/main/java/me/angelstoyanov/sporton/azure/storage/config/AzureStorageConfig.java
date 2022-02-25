package me.angelstoyanov.sporton.azure.storage.config;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import javax.enterprise.context.ApplicationScoped;

@StaticInitSafe
@ConfigMapping(prefix = "sporton.storage.azure")
@RegisterForReflection
@ApplicationScoped
public interface AzureStorageConfig {

    @WithName("account.name")
    String accountName();

    @WithName("account.key")
    String accountKey();

    @WithName("container.name.comments")
    String commentsContainerName();

    @WithName("container.name.pitches")
    String pitchesContainerName();
}
