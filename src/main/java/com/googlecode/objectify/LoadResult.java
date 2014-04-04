package com.googlecode.objectify;



/**
 * <p>Enhances the basic Result<?> with some additional methods useful when loading data.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadResult<T> implements Result<T>
{
	private final Key<T> key;
	private final Result<T> result;

	public LoadResult(Key<T> key, Result<T> result) {
		this.key = key;
		this.result = result;
	}

	/**
	 * Obtain the loaded value now.
	 */
	@Override
	public T now() {
		return result.now();
	}

	/**
	 * Like now(), but throws NotFoundException instead of returning null.
	 * @throws NotFoundException if the loaded value was not found
	 */
	public final T safe() throws NotFoundException {
		T t = now();
		if (t == null)
			throw new NotFoundException(key);
		else
			return t;
	}
}