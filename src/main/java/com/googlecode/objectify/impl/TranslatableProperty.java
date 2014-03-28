package com.googlecode.objectify.impl;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.impl.translate.CollectionTranslatorFactory.CollectionListNodeTranslator;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.EmbedMapTranslatorFactory.MapMapNodeTranslator;
import com.googlecode.objectify.impl.translate.MapifyTranslatorFactory.MapifyListNodeTranslator;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.util.LogUtils;

/**
 * Associates a Property with a Translator and provides a more convenient interface.
 */
public class TranslatableProperty<T> {
	/** */
	private static final Logger log = Logger.getLogger(TranslatableProperty.class.getName());

	/** */
	protected Property property;
	protected Translator<T> translator;

	/** */
	public TranslatableProperty(Property prop, Translator<T> trans) {
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
	 * Gets the appropriate value from the node and sets it on the appropriate field of the pojo.
	 * @param node is the container node for the property
	 * @param onPojo is the container object for the property
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void executeLoad(Node node, Object onPojo, LoadContext ctx) {
		Node actual = getChild(node, property);

		// We only execute if there is a real node.  Note that even a null value in the data will have a real
		// EntityNode with a propertyValue of null, so this is a legitimate test for data in the source Entity
		if (actual != null) {
			try {
				// We have a couple special cases - for collection/map fields we would like to preserve the original
				// instance, if one exists.  It might have been initialized with custom comparators, etc.
				T value;
				if (translator instanceof CollectionListNodeTranslator && actual.hasList()) {
					Collection<Object> coll = (Collection<Object>)this.property.get(onPojo);
					CollectionListNodeTranslator collTranslator = (CollectionListNodeTranslator)translator;
					value = (T)collTranslator.loadListIntoExistingCollection(actual, ctx, coll);
				}
				else if (translator instanceof MapMapNodeTranslator && actual.hasMap()) {
					Map map = (Map)this.property.get(onPojo);
					MapMapNodeTranslator mapTranslator = (MapMapNodeTranslator)translator;
					value = (T)mapTranslator.loadMapIntoExistingMap(actual, ctx, map);
				}
				else if (translator instanceof MapifyListNodeTranslator && actual.hasList()) {
					Map map = (Map)this.property.get(onPojo);
					MapifyListNodeTranslator mapTranslator = (MapifyListNodeTranslator)translator;
					value = (T)mapTranslator.loadListIntoExistingMap(actual, ctx, map);
				}
				else {
					value = translator.load(actual, ctx);
				}

				setOnPojo(onPojo, value, ctx, node.getPath());
			}
			catch (SkipException ex) {
				// No prob, skip this one
			}
		}
	}

	/**
	 * Sets the property on the pojo to the value.  However, sensitive to the value possibly being a Result<?>
	 * wrapper, in which case it enqueues the set operation until the loadcontext is done.
	 */
	private void setOnPojo(final Object pojo, final T value, final LoadContext ctx, final Path path) {
		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(path, "Setting property " + property.getName() + " to " + value));

		property.set(pojo, value);
	}

	/** */
	public void setValue(Object pojo, Object value, LoadContext ctx) {
		T obj = translator.load(Node.of(value), ctx);
		setOnPojo(pojo, obj, ctx, Path.root());
	}

	/** Comes out in datastore format */
	public Object getValue(Object pojo) {
		@SuppressWarnings("unchecked")
		T value = (T)property.get(pojo);
		return translator.save(value, false, new SaveContext(null), Path.root()).getPropertyValue();
	}

	/**
	 * Gets the appropriate field value from the pojo and puts it in the container at the appropriate prop name
	 * and with the appropriate indexing.
	 *
	 * @param onPojo is the parent pojo which holds the property we represent
	 * @param containerPath is the path to the container; each property will extend this path.
	 * @param index is the default state of indexing up to this point
	 */
	public void executeSave(Object onPojo, PropertyContainer container, Path containerPath, boolean index, SaveContext ctx) {
		if (property.isSaved(onPojo)) {
			// Look for an override on indexing
			Boolean propertyIndexInstruction = property.getIndexInstruction(onPojo);
			if (propertyIndexInstruction != null)
				index = propertyIndexInstruction;

			T value = (T)property.get(onPojo);
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
	 * Gets a child node from a parent, detecting collisions and throwing an exception if one is found.
	 *
	 * @param parent is the collection in which to look
	 * @param names is a list of names to look for in the parent
	 * @return one child which has a name in the parent
	 * @throws IllegalStateException if there are multiple name matches
	 */
	private Node getChild(Node parent, Property prop) {
		Node child = null;

		for (String name: prop.getLoadNames()) {
			Node child2 = parent.get(name);

			if (child != null && child2 != null)
				throw new IllegalStateException("Collision trying to load field; multiple name matches for '"
						+ prop.getName() + "' at '" + child.getPath() + "' and '" + child2.getPath() + "'");

			if (child2 != null)
				child = child2;
		}

		return child;
	}
}