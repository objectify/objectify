package com.googlecode.objectify.impl.cmd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.util.DatastoreIntrospector;
import com.googlecode.objectify.util.SimpleFutureWrapper;

/**
 * Implementation for when we start a transaction.  Maintains a separate session, but then copies all
 * data into the original session on successful commit.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImplTxn extends ObjectifyImpl
{
	/** The transaction to use.  If null, do not use transactions. */
	protected Result<Transaction> txn;
	
	/** The session is a simple hashmap */
	protected Session parentSession;

	/**
	 */
	public ObjectifyImplTxn(ObjectifyImpl parent) {
		super(parent);
		
		// There is no overhead for XG transactions on a single entity group, so there is
		// no good reason to ever have withXG false when on the HRD.
		Future<Transaction> fut = createAsyncDatastoreService().beginTransaction(TransactionOptions.Builder.withXG(DatastoreIntrospector.SUPPORTS_XG));
		txn = new ResultAdapter<Transaction>(fut);
		
		// Transactions get a new session, but are still linked to the old one
		parentSession = session;
		session = new Session();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	public Transaction getTxn() {
		return (Transaction)Proxy.newProxyInstance(txn.now().getClass().getClassLoader(), new Class[] { Transaction.class }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().equals("commit")) {
					txn.now().commit();
					parentSession.addAll(session);
					return null;	// void
				} else if (method.getName().equals("commitAsync")) {
					return new SimpleFutureWrapper<Void, Void>(txn.now().commitAsync()) {
						@Override
						protected Void wrap(Void paramK) throws Exception {
							parentSession.addAll(session);
							return paramK;
						}
					};
				} else {
					return method.invoke(txn.now(), args);
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.ObjectifyImpl#getTxnRaw()
	 */
	@Override
	public Transaction getTxnRaw() {
		return this.txn.now();
	}
}