package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

public class EmbeddedNullContainerTests extends TestBase {

	@Test
	void testLoadingEmbeddedMapNullValue() {
		factory().register(Sample.class);

		List<Value> valueList = new ArrayList<>();
		valueList.add(new Value(null, null));

		Map<String, List<Value>> values = new HashMap<String, List<Value>>();
		values.put("k1", valueList);

		final Sample sample = new Sample("testKey", values);
		final Sample retrieved = saveClearLoad(sample);

		assertThat(sample.values).isEqualTo(retrieved.values);
	}


	@Ignore("This test fails as follows:"
		+ "missing keys\n"
		+ "for key         : k1\n"
		+ "---\n"
		+ "expected      : {k1=null}\n"
		+ "but was       : {}\n"
		+ "Possible root-cause: encoding the document on `save` is probably broken")
	@Test
	void testLoadingNullListValue() {
		factory().register(Sample.class);

		Map<String, List<Value>> values = new HashMap<String, List<Value>>();
		values.put("k1", null);

		final Sample sample = new Sample("testKey", values);
		final Sample retrieved = saveClearLoad(sample);

		assertThat(retrieved.values).isEqualTo(sample.values);
	}


	@Entity(name = "Sample")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Sample {

		@Id
		String name;
		Map<String, List<Value>> values;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Value {

		String primitiveField;
		Map<String, String> structuredField;
	}
}
