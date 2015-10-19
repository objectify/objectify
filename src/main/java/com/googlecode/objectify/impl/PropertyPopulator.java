package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.Populator;
import com.googlecode.objectify.impl.translate.Recycles;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Synthetic;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.util.DatastoreUtils;
import com.googlecode.objectify.util.LogUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Associates a Property with a Translator and provides a more convenient interface.
 */
public class PropertyPopulator<P, D> implements Populator<P> {
	/** */
	private static final Logger log = Logger.getLogger(PropertyPopulator.class.getName());

	/** */
	protected Property property;
	protected Translator<P, D> translator;

	/** */
	public PropertyPopulator(Property prop, Translator<P, D> trans) {
		this.property = prop;
		this.translator = trans;
	}

	/** */
	public Property getProperty() { return this.property; }

	/** */
	public LoadConditions getLoadConditions() { return new LoadConditions(property.getAnnotation(Load.class), property.getAnnotation(Parent.class)); }

	/** This is easier to debug if we have a string value */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + property.getName() + ")";
	}

	/**
	 * Gets the appropriate value from the container and sets it on the appropriate field of the pojo.
	 */
	@Override
	public void load(PropertyContainer container, LoadContext ctx, Path containerPath, Object intoPojo) {
		try {
			if (translator instanceof Recycles)
				ctx.recycle(property.get(intoPojo));

			D value = (translator instanceof Synthetic)
				? null
				: getPropertyFromContainer(container, containerPath);	// will throw SkipException if property not present

			setValue(intoPojo, value, ctx, containerPath);
		}
		catch (SkipException ex) {
			// Irrelevant
		}
	}

	/**
	 * Gets the relevant property from the container, detecting alsoload collisions.
	 *
	 * @return the value obtained from the container
	 * @throws IllegalStateException if there are multiple alsoload name matches
	 */
	private D getPropertyFromContainer(PropertyContainer container, Path containerPath) {
		String foundName = null;
		D value = null;

		for (String name: property.getLoadNames()) {
			if (container.hasProperty(name)) {
				if (foundName != null)
					throw new IllegalStateException("Collision trying to load field; multiple name matches for '"
							+ property.getName() + "' at '" + containerPath.extend(foundName) + "' and '" + containerPath.extend(name) + "'");

				//noinspection unchecked
				value = (D)container.getProperty(name);
				foundName = name;
			}
		}

		if (foundName == null)
			throw new SkipException();
		else
			return value;
	}

	/**
	 * Set this raw datastore value on the relevant property of the pojo, doing whatever translations are necessary.
	 */
	public void setValue(Object pojo, D value, LoadContext ctx, Path containerPath) throws SkipException {
		Path propertyPath = containerPath.extend(property.getName());
		P loaded = translator.load(value, ctx, propertyPath);

		setOnPojo(pojo, loaded, ctx, propertyPath);
	}

	/**
	 * Sets the property on the pojo to the value. The value should already be translated.
	 * TODO: Sensitive to the value possibly being a Result<?> wrapper, in which case it enqueues the set operation until the loadcontext is done.
	 */
	private void setOnPojo(Object pojo, P value, LoadContext ctx, Path path) {
		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(path, "Setting property " + property.getName() + " to " + value));

		property.set(pojo, value);
	}

	/**
	 * Gets the appropriate field value from the pojo and puts it in the container at the appropriate prop name
	 * and with the appropriate indexing.
	 * @param onPojo is the parent pojo which holds the property we represent
	 * @param index is the default state of indexing up to this point
	 * @param containerPath is the path to the container; each property will extend this path.
	 */
	@Override
	public void save(Object onPojo, boolean index, SaveContext ctx, Path containerPath, PropertyContainer into) {
		if (property.isSaved(onPojo)) {
			// Look for an override on indexing
			Boolean propertyIndexInstruction = property.getIndexInstruction(onPojo);
			if (propertyIndexInstruction != null)
				index = propertyIndexInstruction;

			@SuppressWarnings("unchecked")
			P value = (P)property.get(onPojo);
			try {
				Path propPath = containerPath.extend(property.getName());
				Object propValue = translator.save(value, index, ctx, propPath);

				DatastoreUtils.setContainerProperty(into, property.getName(), propValue, index, ctx, propPath);
			}
			catch (SkipException ex) {
				// No problem, do nothing
			}
		}
	}

	/**
	 * Get the value for the property and translate it into datastore format.
	 */
	public D getValue(Object pojo, SaveContext ctx, Path containerPath) {
		@SuppressWarnings("unchecked")
		P value = (P)property.get(pojo);

		return translator.save(value, false, ctx, containerPath.extend(property.getName()));
	}

}