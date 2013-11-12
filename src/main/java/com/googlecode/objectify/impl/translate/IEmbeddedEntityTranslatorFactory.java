package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.googlecode.objectify.IEmbeddedEntity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.reflect.Type;

/**
 * Implementation for translating any IEmbeddedEntity descendant into EmbeddedEntity
 *
 * @author Huseyn Guliyev<husayt@gmail.com>
 */
public class IEmbeddedEntityTranslatorFactory extends ValueTranslatorFactory<IEmbeddedEntity, EmbeddedEntity> {
    public IEmbeddedEntityTranslatorFactory() {
        super(IEmbeddedEntity.class);
    }

    @Override
    protected ValueTranslator<IEmbeddedEntity, EmbeddedEntity> createSafe(Path path, Property property, Type type, CreateContext ctx) {
        final Class<? extends IEmbeddedEntity<?>> clazz = (Class<? extends IEmbeddedEntity<?>>) GenericTypeReflector.erase(type);
        final ObjectifyFactory fact = ctx.getFactory();

        return new ValueTranslator<IEmbeddedEntity, EmbeddedEntity>(path, EmbeddedEntity.class) {
            @Override
            protected IEmbeddedEntity loadValue(EmbeddedEntity value, LoadContext ctx) {
                final IEmbeddedEntity<?> construct = fact.construct(clazz);
                return construct.setFieldsFrom(value);
            }

            @Override
            protected EmbeddedEntity saveValue(IEmbeddedEntity value, SaveContext ctx) {
                return value.createEmbeddedEntity();
            }
        };
    }
}
