package com.googlecode.objectify.test;

import com.google.cloud.datastore.NullValue;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

// This class tests Objectify APIs handling of Embedded NullValue by comparing with Entitites
// written and read using raw Datastore API calls.
public class StrangeNullishTests extends TestBase {

	@Entity(name = "Sample")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Sample {
		@Id String name;
		Map<String, String> inner;
	}

	/**
	 * Verifies correctly loading NullValue embedded in a Map
	 */
	@Test
	public void loadSimplerNestedStructureWithNulls() {
		factory().register(Sample.class);

		final String rawDatastoreDocId = "EmbeddedMapNullValue_rawDatastoreDoc";
		final com.google.cloud.datastore.Key rawDatastoreKey = datastore().newKeyFactory()
				.setKind("Sample")
				.newKey(rawDatastoreDocId);

		final com.google.cloud.datastore.Entity rawSample =
				com.google.cloud.datastore.Entity.newBuilder(rawDatastoreKey)
						.set("inner", new NullValue())
						.build();

		// Save to Datastore
		datastore().put(rawSample);

		// Verify using Objectify `load` API
		final Sample sample = ofy().load().type(Sample.class).id(rawDatastoreDocId).now();
		// Just verifying this loads without NPE is success
	}
}
