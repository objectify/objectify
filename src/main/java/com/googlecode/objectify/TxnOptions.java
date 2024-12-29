package com.googlecode.objectify;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Optional;

/**
 * <p>Options for a transaction. This deliberately has a record-like API, and someday when we baseline
 * a more modern java version will be converted to a record.</p>
 *
 * <p>This roughly approximates the protobuf TransactionOptions.</p>
 */
@Value
@RequiredArgsConstructor
@Accessors(fluent = true)
public class TxnOptions {
	private static final TxnOptions DEFAULT = new TxnOptions();
	public static TxnOptions deflt() {
		return DEFAULT;
	}

	/**
	 * Sets the transaction as readOnly. Allows some optimization by the datastore, but attempts
	 * to write data in the transaction will fail with a DatastoreException.
	 */
	boolean readOnly;

	/**
	 * <p>For viewing data in past history. Google's documentation says:</p>
	 *
	 * <p>This must be a microsecond precision timestamp within the past one hour, or
	 * if Point-in-Time Recovery is enabled, can additionally be a whole minute timestamp
	 * within the past 7 days.</p>
	 *
	 * <p>If set, readOnly must be true.</p>
	 */
	Optional<Instant> readTime;

	/**
	 * Number of tries that we may attempt on concurrency failure. Note that readOnly transactions
	 * aren't retried (they should never have concurrency failures).
	 */
	int limitTries;

	/**
	 * Construct with default options.
	 */
	public TxnOptions() {
		this(false, Optional.empty(), 200);
	}

	/** @return options that have the readOnly flag set to the specified value */
	public TxnOptions readOnly(final boolean readOnly) {
		return new TxnOptions(readOnly, readTime, limitTries);
	}

	/**
	 * Note that readOnly must be true for if this is enabled.
	 *
	 * @return options that have the readTime flag set to the specified value
	 */
	public TxnOptions readTime(final Optional<Instant> readTime) {
		return new TxnOptions(readOnly, readTime, limitTries);
	}

	/**
	 * Convenience method.
	 */
	public TxnOptions readTime(final Instant readTime) {
		return readTime(Optional.ofNullable(readTime));
	}

	/**
	 * Change the number of tries. Must be at least 1.
	 */
	public TxnOptions limitTries(final int limitTries) {
		Preconditions.checkArgument(limitTries >= 1, "limitTries must be at least 1");
		return new TxnOptions(readOnly, readTime, limitTries);
	}
}