package com.google.appengine.api.users;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
public class User implements Serializable, Comparable<User>
{
	static final long serialVersionUID = 8691571286358652288L;
	
	private String email;
	private String authDomain;
	private String userId;

	@SuppressWarnings("unused")
	private User()
	{
	}

	public User(String email, String authDomain)
	{
		this(email, authDomain, null);
	}

	public User(String email, String authDomain, String userId)
	{
		if (email == null)
			throw new NullPointerException("email must be specified");
		if (authDomain == null)
		{
			throw new NullPointerException("authDomain must be specified");
		}
		else
		{
			this.email = email;
			this.authDomain = authDomain;
			this.userId = userId;
			return;
		}
	}

	public String getNickname()
	{
		int indexOfDomain = email.indexOf("@" + authDomain);
		if (indexOfDomain == -1)
			return email;
		else
			return email.substring(0, indexOfDomain);
	}

	public String getAuthDomain()
	{
		return authDomain;
	}

	public String getEmail()
	{
		return email;
	}

	public String getUserId()
	{
		return userId;
	}

	public String toString()
	{
		return email;
	}

	public boolean equals(Object object)
	{
		if (!(object instanceof User))
		{
			return false;
		}
		else
		{
			User user = (User) object;
			return user.email.equals(email) && user.authDomain.equals(authDomain);
		}
	}

	public int hashCode()
	{
		return 17 * email.hashCode() + authDomain.hashCode();
	}

	public int compareTo(User user)
	{
		return email.compareTo(user.email);
	}

}
