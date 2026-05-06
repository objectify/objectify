package com.googlecode.objectify.test.util.valkey;

import glide.api.GlideClient;
import glide.api.models.configuration.GlideClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots a Valkey 9.0 container once per JVM and exposes a {@link GlideClient} that connects
 * to it. {@code FLUSHALL} runs before each test so cached state from prior tests cannot leak.
 */
@Slf4j
public class LocalValkeyExtension implements BeforeAllCallback, BeforeEachCallback {

	private static final DockerImageName IMAGE = DockerImageName.parse("valkey/valkey:9.0");

	@Override
	public void beforeAll(final ExtensionContext context) throws Exception {
		if (getClient(context) != null) {
			return;
		}

		log.info("Starting Valkey container");

		@SuppressWarnings("resource")
		final GenericContainer<?> container = new GenericContainer<>(IMAGE).withExposedPorts(6379);
		container.start();

		final GlideClient client = GlideClient.createClient(
				GlideClientConfiguration.builder()
						.address(NodeAddress.builder()
								.host(container.getHost())
								.port(container.getMappedPort(6379))
								.build())
						.requestTimeout(5000)
						.build()
		).get();

		final Namespace ns = Namespace.GLOBAL;
		context.getRoot().getStore(ns).put(GenericContainer.class, container);
		context.getRoot().getStore(ns).put(GlideClient.class, client);
	}

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final GlideClient client = getClient(context);
		client.customCommand(new String[]{"FLUSHALL"}).get();
	}

	public static GlideClient getClient(final ExtensionContext context) {
		return context.getRoot().getStore(Namespace.GLOBAL).get(GlideClient.class, GlideClient.class);
	}
}
