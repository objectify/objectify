package com.googlecode.objectify.impl.translate;

import java.util.Collections;
import java.util.List;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.Upgrade;
import com.googlecode.objectify.util.IdentityMultimapList;

/**
 * The context of a save operation; might involve multiple entities (eg, batch save).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SaveContext
{
	/** The objectify instance */
	Objectify ofy;

	/** The current root entity; will change as multiple entities are loaded */
	Object currentRoot;

	/**
	 * Potential upgrades found during the save process; key is the pojo entity itself (its identity).
	 * The problem is that when saving, the id may not be set yet, so we can't track keys.
	 */
	IdentityMultimapList<Object, Upgrade> upgrades = new IdentityMultimapList<Object, Upgrade>();

	/** */
	public SaveContext(Objectify ofy) {
		this.ofy = ofy;
	}

	/** */
	public Objectify getObjectify() { return this.ofy; }

	/** Sets the current root entity, not its key! */
	public void setCurrentRoot(Object rootEntity) {
		this.currentRoot = rootEntity;
	}

	/** */
	public void registerUpgrade(Property prop, Ref<?> ref) {
		upgrades.add(currentRoot, new Upgrade(prop, ref));
	}

	/** @return an empty list if no upgrades found */
	public List<Upgrade> getUpgrades(Object pojo) {
		List<Upgrade> list = upgrades.get(pojo);
		if (list == null)
			return Collections.emptyList();
		else
			return list;
	}
}