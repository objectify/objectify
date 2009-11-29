/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.apphosting.api.ApiProxy;

/**
 * See:  http://code.google.com/appengine/docs/java/howto/unittesting.html
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TestEnvironment implements ApiProxy.Environment
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestEnvironment.class);

	@Override
	public String getAppId()
	{
		return "test";
	}

	@Override
	public String getRequestNamespace()
	{
		return "";
	}

	@Override
	public String getVersionId()
	{
		return "1.0";
	}

	@Override
	public Map<String, Object> getAttributes()
	{
		return new HashMap<String, Object>();
	}

	@Override
	public String getAuthDomain()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getEmail()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAdmin()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLoggedIn()
	{
		throw new UnsupportedOperationException();
	}
}