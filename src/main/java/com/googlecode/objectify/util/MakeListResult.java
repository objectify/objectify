package com.googlecode.objectify.util;

import com.google.common.collect.Lists;

import java.util.List;


/**
 * Simple ResultTranslator that converts from an Iterable to a List.
 */
public class MakeListResult<T> extends ResultTranslator<Iterable<T>, List<T>>
{
	private static final long serialVersionUID = 1L;

	public MakeListResult(Iterable<T> wrapped) {
		super(wrapped);
	}

	@Override
	protected List<T> translate(Iterable<T> from) {
		return Lists.newArrayList(from);
	}
}
