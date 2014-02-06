package com.googlecode.objectify.util.cmd;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.QueryKeys;
import com.googlecode.objectify.cmd.SimpleQuery;

import java.util.List;

/**
 * Simple wrapper/decorator for a SimpleQuery.  Use it like this:
 * {@code class MySimpleQuery<T> extends SimpleQueryWrapper<MyQuery<T>, T>}
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SimpleQueryWrapper<H extends SimpleQueryWrapper<H, T>, T> implements SimpleQuery<T>, Cloneable {
    /** */
    SimpleQuery<T> base;

    /** */
    public SimpleQueryWrapper(SimpleQuery<T> base) {
        this.base = base;
    }

    @Override
    public H filterKey(String condition, Object value) {
        H next = this.clone();
        next.base = base.filterKey(condition, value);
        return next;
    }

    @Override
    public SimpleQuery<T> filterKey(Object value) {
        H next = this.clone();
        next.base = base.filterKey(value);
        return next;
    }

    @Override
    public H ancestor(Object keyOrEntity) {
        H next = this.clone();
        next.base = base.ancestor(keyOrEntity);
        return next;
    }

    @Override
    public H limit(int value) {
        H next = this.clone();
        next.base = base.limit(value);
        return next;
    }

    @Override
    public H offset(int value) {
        H next = this.clone();
        next.base = base.offset(value);
        return next;
    }

    @Override
    public H reverse() {
        H next = this.clone();
        next.base = base.reverse();
        return next;
    }

    @Override
    public H startAt(Cursor value) {
        H next = this.clone();
        next.base = base.startAt(value);
        return next;
    }

    @Override
    public H endAt(Cursor value) {
        H next = this.clone();
        next.base = base.endAt(value);
        return next;
    }

    @Override
    public String toString() {
        return base.toString();
    }

    @Override
    public LoadResult<T> first() {
        return base.first();
    }

    @Override
    public int count() {
        return base.count();
    }

    @Override
    public QueryResultIterable<T> iterable() {
        return base.iterable();
    }

    @Override
    public List<T> list() {
        return base.list();
    }

    @Override
    public H chunk(int value) {
        H next = this.clone();
        next.base = base.chunk(value);
        return next;
    }

    @Override
    public H chunkAll() {
        H next = this.clone();
        next.base = base.chunkAll();
        return next;
    }

    @Override
    public H distinct(boolean value) {
        H next = this.clone();
        next.base = base.distinct(value);
        return next;
    }

    @Override
    public H hybrid(boolean force) {
        H next = this.clone();
        next.base = base.hybrid(force);
        return next;
    }

    @Override
    public QueryResultIterator<T> iterator() {
        return base.iterator();
    }

    @Override
    public QueryKeys<T> keys() {
        return base.keys();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @SuppressWarnings("unchecked")
    protected H clone() {
        try {
            return (H) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // impossible
        }
    }
}
