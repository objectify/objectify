/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOpenTelemetryOptions;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.LocalDatastoreExtension;
import com.googlecode.objectify.test.util.LocalMemcacheExtension;
import com.googlecode.objectify.test.util.MockitoExtension;
import com.googlecode.objectify.util.Closeable;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * A manual test. Disabled because it requires some setup. Enable it then:
 *
 * docker run -d --name jaeger \
 *   -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
 *   -p 5775:5775/udp \
 *   -p 6831:6831/udp \
 *   -p 6832:6832/udp \
 *   -p 5778:5778 \
 *   -p 16686:16686 \
 *   -p 14268:14268 \
 *   -p 14250:14250 \
 *   -p 9411:9411 \
 *   jaegertracing/all-in-one:latest
 *
 *  Then visit http://localhost:16686 to see the events land.
 */
@ExtendWith({
	MockitoExtension.class,
	LocalDatastoreExtension.class,
	LocalMemcacheExtension.class,
})
@Disabled
class OtelTests {
	private Closeable rootService;

	@BeforeEach
	void setUp(final LocalDatastoreHelper helper) {
		final JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
			.setEndpoint("http://localhost:14250")
			.build();

		final Resource resource = Resource.getDefault().merge(
			Resource.create(
				Attributes.of(AttributeKey.stringKey("service.name"), "objectify-test-service")
			)
		);

		final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
			.setResource(resource)
			.build();

		final OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
			.setTracerProvider(tracerProvider)
			.buildAndRegisterGlobal();

		final DatastoreOpenTelemetryOptions otelOptions = DatastoreOpenTelemetryOptions.newBuilder().setOpenTelemetry(otel).build();
		final DatastoreOptions datastoreOptions = helper.getOptions().toBuilder().setOpenTelemetryOptions(otelOptions).build();

		final Datastore datastore = datastoreOptions.getService();

		ObjectifyService.init(new ObjectifyFactory(datastore));

		rootService = ObjectifyService.begin();
	}

	@AfterEach
	void tearDown() {
		rootService.close();
	}

	@Test
	void simpleOperationsToCheckOtelByHand() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> k = ofy().save().entity(triv).now();

		ofy().clear();
		final Trivial fetched = ofy().load().key(k).now();

		ofy().clear();
		final List<Trivial> listed = ofy().load().type(Trivial.class).filter("someString", "foo").list();

		ofy().delete().entity(fetched).now();
	}
}