package com.googlecode.objectify.impl.conv;

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
 * <pre>ObjectifyService.factory().getConversions().add(new BigDecimalLongConverter());</pre>
 * 
 * <p>The default factor of 1,000 is good for currency, which usually has 0-3 digits of precision past
 * the decimal point.  But you can pick any other factor appropriate to your application.</p>
 */
public class BigDecimalLongConverter extends SimpleConverterFactory<BigDecimal, Long>
{
	/** Default factor is 1000, which gives you three digits of precision past the decimal point */
	public static final long DEFAULT_FACTOR = 1000;
	
	/** */
	BigDecimal factor;
	
	/**
	 * Construct this converter with the default factor (1000), which can store three points of
	 * precision past the decimal point.
	 */
	public BigDecimalLongConverter()
	{
		this(DEFAULT_FACTOR);
	}
	
	/**
	 * Construct this with an arbitrary factor.  Powers of ten are highly recommended if you want to
	 * be able to interpret the numbers in the datastore viewer.
	 * 
	 * @param factor number multiplied by before storage and divided by on retrieval.
	 */
	public BigDecimalLongConverter(long factor)
	{
		super(BigDecimal.class);
		
		this.factor = new BigDecimal(factor);
	}
	
	@Override
	protected Converter<BigDecimal, Long> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<BigDecimal, Long>() {
			
			@Override
			public BigDecimal toPojo(Long value, ConverterLoadContext ctx) {
				return new BigDecimal(value).divide(factor);
			}
			
			@Override
			public Long toDatastore(BigDecimal value, ConverterSaveContext ctx) {
				return value.multiply(factor).longValueExact();
			}
		};
	}
}