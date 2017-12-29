package com.googlecode.objectify.util;

import com.google.cloud.datastore.Key;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class KeyFormatTest {
	@Test
	void parsesAndFormatsOldStyleGAEKey() throws Exception {
		final String urlsafeKey = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWww";

		final Key key = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(urlsafeKey);
		final String restringified = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key);

		assertThat(restringified).isEqualTo(urlsafeKey);
	}
}