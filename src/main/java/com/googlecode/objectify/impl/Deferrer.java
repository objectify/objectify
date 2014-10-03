package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.ResultNow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all the logic of deferring operations
 */
public class Deferrer {

	/** */
	private final Objectify ofy;

	/** */
	private final Session session;

	/** Values of null mean "delete" */
	private Map<Key<?>, Object> operations = new HashMap<>();

	public Deferrer(Objectify ofy, Session session) {
		this.ofy = ofy;
		this.session = session;
	}

	public void deferSave(Key<Object> key, Object entity) {
		session.addValue(key, entity);
		operations.put(key, entity);
	}

	public void deferDelete(Key<?> key) {
		session.addValue(key, null);
		operations.put(key, null);
	}

	public void flush() {
		if (operations.isEmpty())
			return;

		// Sort into two batch operations: one for save, one for delete.
		List<Object> saves = new ArrayList<>();
		List<Key<?>> deletes = new ArrayList<>();

		for (Map.Entry<Key<?>, Object> entry : operations.entrySet()) {
			if (entry.getValue() == null)
				deletes.add(entry.getKey());
			else
				saves.add(entry.getValue());
		}

		// Do this async so we get parallelism, but force it to be complete in the end.

		Result<?> savesFuture = new ResultNow<>(null);
		Result<?> deletesFuture = new ResultNow<>(null);

		if (!saves.isEmpty())
			savesFuture = ofy.save().entities(saves);

		if (!deletes.isEmpty())
			deletesFuture = ofy.delete().keys(deletes);

		savesFuture.now();
		deletesFuture.now();
	}
}
