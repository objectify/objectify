package com.googlecode.objectify.util;

import com.google.cloud.datastore.Key;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class KeyFormatTest {
	@Test
	void parsesAndFormatsOldStyleGAEKey() throws Exception {
		// Both of these get converted to the second form
		final String namespaceless = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWww";
		final String includesNamespace = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWwyiAQA";

		final Key key = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(namespaceless);
		final String restringifiedWithNamespace = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key);
		assertThat(restringifiedWithNamespace).isEqualTo(namespaceless);

		final Key key2 = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(includesNamespace);
		assertThat(key).isEqualTo(key2);
	}

	@Test
	void parsesAndFormatsOldStyleGAEKeyAustralianEdition() throws Exception {
		final String includesNamespace = "ahVmfnJheXdoaXRlLXByb2R1Y3Rpb25yKwsSC19haF9TRVNTSU9OIhpfYWhzLS01Tnh0OGlGQ3UxZDFGaklscm1CUQw";

		final Key key2 = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(includesNamespace);
		assertThat(key2.getProjectId()).isEqualTo("raywhite-production");
	}

	@Test
	void parsesAndFormatsOldStyleGAEKeyWithANamespace() throws Exception {
		// This should stay as-is
		final String hasNamespace = "agxzfm1haWxmb29nYWVyMQsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEghXb3JrZmxvdxiAgJaV__usCgw";

		final Key key = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(hasNamespace);
		final String restringifiedWithNamespace = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key);
		assertThat(restringifiedWithNamespace).isEqualTo(hasNamespace);
	}
}