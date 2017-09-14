package com.googlecode.objectify.test;

import com.googlecode.objectify.ObjectifyOptions;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.impl.ObjectifyImpl;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

public class Transaction2Tests extends TestBase
{
    @Override
    protected ObjectifyOptions getObjectifyOptions() {
        return super.getObjectifyOptions().cache(false);
    }

    /**
     * This is a somewhat clunky way to test this, and requires making impl.getCache() public,
     * but it gets the job done.
     */
    @Test
    public void transactionalObjectifyInheritsCacheSetting() throws Exception {
        ofy().transact(new VoidWork() {
            @Override
            public void vrun() {
                // Test in _and out_ of a transaction
                assert !ofy().getCache();
                assert !ofy().transactionless().getCache();
            }
        });
    }
}
