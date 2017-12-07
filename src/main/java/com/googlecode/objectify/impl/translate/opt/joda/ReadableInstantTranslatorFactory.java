package com.googlecode.objectify.impl.translate.opt.joda;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.time.ReadableInstant;

import java.lang.invoke.MethodHandle;


/**
 * Converts Joda ReadableInstants (DateTime, DateMidnight, etc) into Timestamp
 *
 * <p>All custom translators must be registered *before* entity classes are registered.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ReadableInstantTranslatorFactory extends ValueTranslatorFactory<ReadableInstant, Timestamp>
{
	public ReadableInstantTranslatorFactory() {
		super(ReadableInstant.class);
	}

	@Override
	protected ValueTranslator<ReadableInstant, Timestamp> createValueTranslator(final TypeKey<ReadableInstant> tk, final CreateContext ctx, final Path path) {
		final Class<?> clazz = tk.getTypeAsClass();

		// All the Joda instants have a constructor that will accept a Date
		final MethodHandle ctor = TypeUtils.getConstructor(clazz, Object.class);

		return new ValueTranslator<ReadableInstant, Timestamp>(ValueType.TIMESTAMP) {
			@Override
			protected ReadableInstant loadValue(final Value<Timestamp> value, final LoadContext ctx, final Path path) throws SkipException {
				return TypeUtils.invoke(ctor, value.get().toSqlTimestamp().getTime());
			}

			@Override
			protected Value<Timestamp> saveValue(final ReadableInstant value, final SaveContext ctx, final Path path) throws SkipException {
				return TimestampValue.of(Timestamp.of(value.toInstant().toDate()));
			}
		};
	}
}