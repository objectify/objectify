package com.googlecode.objectify;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;

/**
 * <p>The default options are:</p>
 *
 * <ul>
 * <li>Do NOT begin a transaction.</li>
 * <li>DO use a global cache.</li>
 * <li>Use STRONG consistency.</li>
 * <li>Apply no deadline to calls.</li>
 * </ul>
 */
public class ObjectifyOptions {

    private boolean cache = true;
    private Consistency consistency = Consistency.STRONG;
    private Double deadline;
    private boolean mandatoryTransactions = false;

    public ObjectifyOptions() {
    }

    public boolean cache() {
        return cache;
    }

    /**
     * <p>Provides a new Objectify instance which uses (or doesn't use) a 2nd-level memcache.
     * If true, Objectify will obey the @Cache annotation on entity classes,
     * saving entity data to the GAE memcache service.  Fetches from the datastore
     * for @Cache entities will look in the memcache service first.  This cache
     * is shared across all versions of your application across the entire GAE
     * cluster.</p>
     *
     * <p>Objectify instances are cache(true) by default.</p>
     *
     * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
     * current command object.</b></p>
     */
    public ObjectifyOptions cache(boolean cache) {
        this.cache = cache;
        return this;
    }

    public Consistency consistency() {
        return consistency;
    }

    /**
     * <p>Provides a new Objectify instance with the specified Consistency.  Generally speaking, STRONG consistency
     * provides more consistent results more slowly; EVENTUAL consistency produces results quickly but they
     * might be out of date.  See the
     * <a href="http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/ReadPolicy.Consistency.html">Appengine Docs</a>
     * for more explanation.</p>
     *
     * <p>The new instance will inherit all other characteristics (transaction, cache policy, session cache contents, etc)
     * from this instance.</p>
     *
     * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
     * current command object.</b></p>
     *
     * @param policy the consistency policy to use.  STRONG load()s are more consistent but EVENTUAL load()s
     *  are faster.
     */
    public ObjectifyOptions consistency(Consistency policy) {
        this.consistency = policy;
        return this;
    }

    public Double deadline() {
        return deadline;
    }

    /**
     * <p>Provides a new Objectify instance with a limit, in seconds, for datastore calls.  If datastore calls take longer
     * than this amount, a timeout exception will be thrown.</p>
     *
     * <p>The new instance will inherit all other characteristics (transaction, cache policy, session cache contents, etc)
     * from this instance.</p>
     *
     * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
     * current command object.</b></p>
     *
     * @param value - limit in seconds, or null to indicate no deadline (other than the standard whole request deadline of 30s/10m).
     */
    public ObjectifyOptions deadline(Double value) {
        this.deadline = value;
        return this;
    }

    public boolean mandatoryTransactions() {
        return mandatoryTransactions;
    }

    /**
     * <p>Provides a new Objectify instance which throws an exception whenever save() or delete() is
     * called from outside a transaction context. This is a reasonable sanity check for most business
     * workloads; you may wish to enable it globally by overriding ObjectifyFactory.begin() to
     * twiddle this flag on the returned object.</p>
     *
     * <p>Objectify instances are mandatoryTransactions(false) by default.</p>
     *
     * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
     * current command object.</b></p>
     */
    public ObjectifyOptions mandatoryTransactions(boolean mandatoryTransactions) {
        this.mandatoryTransactions = mandatoryTransactions;
        return this;
    }
}
