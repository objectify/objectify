package com.googlecode.objectify.util;

import com.google.cloud.datastore.Key;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class KeyFormatTest {
	@Test
	void parsesAndFormatsOldStyleGAEKey() throws Exception {
		// Both of these get converted to the second form
		final String urlsafeKey1 = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWww";
		final String urlsafeKey2 = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWwyiAQA";

		final Key key = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(urlsafeKey1);
		final String restringified = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key);
		assertThat(restringified).isEqualTo(urlsafeKey2);

		final Key key2 = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(urlsafeKey2);
		final String restringified2 = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key2);
		assertThat(restringified2).isEqualTo(urlsafeKey2);
	}
}