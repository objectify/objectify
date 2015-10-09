package com.googlecode.objectify.impl.translate.opt;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

import java.math.BigDecimal;

/**
 * <p>This a simple strategy for storing BigDecimal in the datastore.  BigDecimalLongConverter multiplies
 * by a fixed factor and stores the result as a Long.  This is appropriate for monetary and other (relatively)
 * small values with fixed decimal precision.</p>
 *
 * <p>This is one possible strategy and not appropriate for all uses of BigDecimal - especially very large
 * values which cannot fit in a Long.  For this reason, the converter is not installed by default.  You can
 * Install this converter at the same time you perform registration:</p>
 *
 * <pre>ObjectifyService.factory().getTranslators().add(new BigDecimalLongTranslatorFactory());</pre>
 *
 * <p>The default factor of 1,000 is good for currency, which usually has 0-3 digits of precision past
 * the decimal point.  But you can pick any other factor appropriate to your application.</p>
 *
 * <p>All custom translators must be registered *before* entity classes are registered.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BigDecimalLongTranslatorFactory extends ValueTranslatorFactory<BigDecimal, Long>
{
	/** Default factor is 1000, which gives you three digits of precision past the decimal point */
	public static final long DEFAULT_FACTOR = 1000;

	/** */
	private BigDecimal factor;

	/**
	 * Construct this converter with the default factor (1000), which can store three points of
	 * precision past the decimal point.
	 */
	public BigDecimalLongTranslatorFactory()
	{
		this(DEFAULT_FACTOR);
	}

	/**
	 * Construct this with an arbitrary factor.  Powers of ten are highly recommended if you want to
	 * be able to interpret the numbers in the datastore viewer.
	 *
	 * @param factor number multiplied by before storage and divided by on retrieval.
	 */
	public BigDecimalLongTranslatorFactory(long factor) {
		super(BigDecimal.class);

		this.factor = new BigDecimal(factor);
	}

	@Override
	protected ValueTranslator<BigDecimal, Long> createValueTranslator(TypeKey<BigDecimal> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<BigDecimal, Long>(Long.class) {
			@Override
			protected BigDecimal loadValue(Long value, LoadContext ctx, Path path) throws SkipException {
				return new BigDecimal(value).divide(factor);
			}

			@Override
			protected Long saveValue(BigDecimal value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.multiply(factor).longValueExact();
			}
		};
	}
}
