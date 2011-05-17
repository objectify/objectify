package com.googlecode.objectify.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests that bulk gets return results having the same order as the argument
 */
public class BulkGetTests extends TestBase
{
    /** */
	@Test
    @SuppressWarnings("unchecked")
    public void testOrderOfBulkGet()
    {
        Objectify ofy = this.fact.begin();

        Trivial t1 = new Trivial("foo", 5);
        Trivial t2 = new Trivial("bar", 6);

        Key<Trivial> k1 = ofy.put(t1);
        Key<Trivial> k2 = ofy.put(t2);

        // get k1, then k2
        Map<Key<Trivial>,Trivial> map = ofy.get(Arrays.asList(k1, k2));
        assert sameList(Arrays.asList(k1, k2), map.keySet());

        // get k2, then k1
        map = ofy.get(Arrays.asList(k2, k1));
        assert sameList(Arrays.asList(k2, k1), map.keySet());

    }

    private boolean sameList(List<Key<Trivial>> l1, Collection<Key<Trivial>> l2) {
        return Arrays.equals(l1.toArray(), l2.toArray());
    }
}
