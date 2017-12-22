package com.googlecode.objectify.impl;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.Populator;
import com.googlecode.objectify.impl.translate.Recycles;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Synthetic;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.util.LogUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Associates a Property with a Translator and provides a more convenient interface.
 */
@RequiredArgsConstructor
@Slf4j
public class PropertyPopulator<P, D> implements Populator<P> {
	/** */
	@Getter
	private final Property property;

	private final Translator<P, D> translator;

	/** */
	public LoadConditions getLoadConditions() {
		return new LoadConditions(property.getAnnotation(Load.class), property.getAnnotation(Parent.class));
	}

	/** This is easier to debug if we have a string value */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + property.getName() + ")";
	}

	/**
	 * Gets the appropriate value from the container and sets it on the appropriate field of the pojo.
	 */
	@Override
	public void load(final FullEntity<?> container, final LoadContext ctx, final Path containerPath, final P intoPojo) {
		try {
			if (translator instanceof Recycles)
				ctx.recycle(property.get(intoPojo));

			final Value<D> value = (translator instanceof Synthetic)
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
	private Value<D> getPropertyFromContainer(final FullEntity<?> container, final Path containerPath) {
		String foundName = null;
		Value<D> value = null;

		for (String name: property.getLoadNames()) {
			if (container.contains(name)) {
				if (foundName != null)
					throw new IllegalStateException("Collision trying to load field; multiple name matches for '"
							+ property.getName() + "' at '" + containerPath.extend(foundName) + "' and '" + containerPath.extend(name) + "'");

				//noinspection unchecked
				value = container.getValue(name);
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
	public void setValue(final Object pojo, final Value<D> value, final LoadContext ctx, final Path containerPath) throws SkipException {
		final Path propertyPath = containerPath.extend(property.getName());
		final P loaded = translator.load(value, ctx, propertyPath);

		setOnPojo(pojo, loaded, ctx, propertyPath);
	}

	/**
	 * Sets the property on the pojo to the value. The value should already be translated.
	 * TODO: Sensitive to the value possibly being a Result<?> wrapper, in which case it enqueues the set operation until the loadcontext is done.
	 */
	private void setOnPojo(final Object pojo, final P value, final LoadContext ctx, final Path path) {
		if (log.isTraceEnabled())
			log.trace(LogUtils.msg(path, "Setting property " + property.getName() + " to " + value));

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
	public void save(final P onPojo, boolean index, final SaveContext ctx, final Path containerPath, final FullEntity.Builder<?> into) {
		if (property.isSaved(onPojo)) {
			// Look for an override on indexing
			final Boolean propertyIndexInstruction = property.getIndexInstruction(onPojo);
			if (propertyIndexInstruction != null)
				index = propertyIndexInstruction;

			@SuppressWarnings("unchecked")
			final P value = (P)property.get(onPojo);
			try {
				final Path propPath = containerPath.extend(property.getName());
				final Value<D> propValue = translator.save(value, index, ctx, propPath);

				into.set(property.getName(), propValue);
			}
			catch (SkipException ex) {
				// No problem, do nothing
			}
		}
	}

	/**
	 * Get the value for the property and translate it into datastore format.
	 */
	public Value<D> getValue(final Object pojo, final SaveContext ctx, final Path containerPath) {
		@SuppressWarnings("unchecked")
		final P value = (P)property.get(pojo);

		return translator.save(value, false, ctx, containerPath.extend(property.getName()));
	}

}