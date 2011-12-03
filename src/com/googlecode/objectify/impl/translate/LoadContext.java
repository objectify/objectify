package com.googlecode.objectify.impl.translate;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.engine.LoadBatch;

/** 
 * The context of a load or save operation to a single entity. 
 */
public class LoadContext
{
	/** The objectify instance */
	Objectify ofy;
	
	/** */
	LoadBatch batch;
	
	/** Lazily created, but executed at the end of done() */
	List<Runnable> delayed;
	
	/** */
	public LoadContext(Objectify ofy, LoadBatch batch)
	{
		this.ofy = ofy;
		this.batch = batch;
	}
	
	/** */
	public Objectify getObjectify() { return this.ofy; }
	
	/** 
	 * Call this when a load process completes.  Executes anything in the batch and then executes any delayed operations. 
	 */
	public void done() {
		batch.execute();
		
		if (delayed != null)
			for (Runnable run: delayed)
				run.run();
	}
	
	/**
	 * Create a Ref for the key, and maybe initialize the value depending on the load annotation and the current
	 * state of load groups.
	 */
	public <T> Ref<T> makeRef(Load load, Key<T> key) {
		Ref<T> ref = Ref.create(key);
		
		if (batch.shouldLoad(load))
			batch.loadRef(ref);
		
		return ref;
	}
	
	/**
	 * Create an entity reference object for the key.  If not loaded, the reference will be a simple
	 * partial entity.  If loaded, the return value will be a Result<?> that produces a loaded instance.
	 * 
	 * @param clazz is the type of the partial to generate, or base class of the result (in either case it is the fied type)
	 * @return either a partial entity or Result<?> that will produce a loaded instance.
	 */
	public Object makeReference(Load load, Class<?> clazz, com.google.appengine.api.datastore.Key key) {
		if (batch.shouldLoad(load)) {
			// back into the batch, magically enqueueing a pending!  
			return batch.getResult(Key.create(key));
		} else {
			// Make a partial
			Object instance = ofy.getFactory().construct(clazz);
			
			@SuppressWarnings("unchecked")
			EntityMetadata<Object> meta = (EntityMetadata<Object>)ofy.getFactory().getMetadata(clazz);
			meta.getKeyMetadata().setKey(instance, key, this);
			
			return instance;
		}
	}
	
	/**
	 * Delays an operation until the context is done().
	 */
	public void delay(Runnable runnable) {
		if (this.delayed == null)
			this.delayed = new ArrayList<Runnable>();
		
		this.delayed.add(runnable);
	}
}