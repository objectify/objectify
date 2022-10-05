package com.googlecode.objectify.util;

import com.google.cloud.datastore.Key;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyFormatTest {
	@Test
	void parsesAndFormatsOldStyleGAEKey() throws Exception {
		// Both of these get converted to the second form
		final String namespaceless = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWww";
		final String includesNamespace = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWwyiAQA";

		final Key key = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(namespaceless);
		final String restringifiedWithNamespace = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key);
		assertThat(restringifiedWithNamespace).isEqualTo(includesNamespace);

		final Key key2 = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(includesNamespace);
		assertThat(key).isEqualTo(key2);

		final String restringifiedWithNamespace2 = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key2);
		assertThat(restringifiedWithNamespace2).isEqualTo(includesNamespace);

		final String restringifiedWithoutNamespace = KeyFormat.NAMESPACELESS.formatOldStyleAppEngineKey(key2);
		assertThat(restringifiedWithoutNamespace).isEqualTo(namespaceless);
	}

	@Test
	void parsesAndFormatsOldStyleGAEKeyWithANamespace() throws Exception {
		final String hasNamespace = "ajNzfnRlc3QtcHJvamVjdC0yYTNhMzM2MS0xMDA2LTQ3NGItYWNmNy05NzJhMmUzYmE2ZmJyGgsSB1RyaXZpYWwYAQwLEgZIYXNSZWYYtwQMogEA";

		final Key key = KeyFormat.INSTANCE.parseOldStyleAppEngineKey(hasNamespace);
		final String restringifiedWithNamespace = KeyFormat.INSTANCE.formatOldStyleAppEngineKey(key);
		assertThat(restringifiedWithNamespace).isEqualTo(hasNamespace);

		assertThrows(IllegalArgumentException.class, () -> {
			KeyFormat.NAMESPACELESS.formatOldStyleAppEngineKey(key);
		});
	}
}