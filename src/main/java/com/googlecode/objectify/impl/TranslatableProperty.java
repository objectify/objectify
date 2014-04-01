package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.impl.translate.UsesExistingValue;
import com.googlecode.objectify.util.LogUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Associates a Property with a Translator and provides a more convenient interface.
 */
public class TranslatableProperty<P, D> {
	/** */
	private static final Logger log = Logger.getLogger(TranslatableProperty.class.getName());

	/** */
	protected Property property;
	protected Translator<P, D> translator;

	/** */
	public TranslatableProperty(Property prop, Translator<P, D> trans) {
		this.property = prop;
		this.translator = trans;
	}

	/** */
	public Property getProperty() { return this.property; }

	/** This is easier to debug if we have a string value */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + property.getName() + ")";
	}

	/**
	 * Gets the appropriate value from the container and sets it on the appropriate field of the pojo.
	 * TODO: figure out what to do with collections and maps etc, things that need to be preserved
	 */
	public void executeLoad(PropertyContainer container, Object onPojo, LoadContext ctx, Path containerPath) {
		try {
			D value = getPropertyFromContainer(container, containerPath);

			if (translator instanceof UsesExistingValue) {
				P existingValue = (P)property.get(onPojo);
				ctx.setExistingValue(existingValue);
			}

			setValue(onPojo, value, ctx, containerPath);
		} catch (SkipException ex) {
			// No prob, skip this one
		}
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
	 * Set this raw datastore value on the relevant property of the pojo, doing whatever translations are necessary.
	 */
	public void setValue(Object pojo, D value, LoadContext ctx, Path containerPath) throws SkipException {
		Path propertyPath = containerPath.extend(property.getName());
		P loaded = translator.load(value, ctx, propertyPath);

		setOnPojo(pojo, loaded, ctx, propertyPath);
	}

	/**
	 * Get the value for the property and translate it into datastore format.
	 */
	public D getValue(Object pojo, SaveContext ctx, Path containerPath) {
		@SuppressWarnings("unchecked")
		P value = (P)property.get(pojo);

		return translator.save(value, false, ctx, containerPath.extend(property.getName()));
	}

	/**
	 * Gets the appropriate field value from the pojo and puts it in the container at the appropriate prop name
	 * and with the appropriate indexing.
	 *
	 * @param onPojo is the parent pojo which holds the property we represent
	 * @param containerPath is the path to the container; each property will extend this path.
	 * @param index is the default state of indexing up to this point
	 */
	public void executeSave(Object onPojo, PropertyContainer container, boolean index, SaveContext ctx, Path containerPath) {
		if (property.isSaved(onPojo)) {
			// Look for an override on indexing
			Boolean propertyIndexInstruction = property.getIndexInstruction(onPojo);
			if (propertyIndexInstruction != null)
				index = propertyIndexInstruction;

			P value = (P)property.get(onPojo);
			try {
				Path propPath = containerPath.extend(property.getName());
				Object propValue = translator.save(value, index, ctx, propPath);

				setContainerProperty(container, property.getName(), propValue, index);
			}
			catch (SkipException ex) {
				// No problem, do nothing
			}
		}
	}

	/** Utility method */
	private void setContainerProperty(PropertyContainer entity, String propertyName, Object value, boolean index) {
		if (index)
			entity.setProperty(propertyName, value);
		else
			entity.setUnindexedProperty(propertyName, value);
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

				value = (D)container.getProperty(name);
				foundName = name;
			}
		}

		if (foundName == null)
			throw new SkipException();
		else
			return value;
	}
}