package com.google.appengine.api.users;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore User class
 */
public class User_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, User instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static User instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		String email = streamReader.readString();
		String auth = streamReader.readString();
		String userid = streamReader.readString();
		return new User(email, auth, userid);
	}

	public static void serialize(SerializationStreamWriter streamWriter, User instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getEmail());
		streamWriter.writeString(instance.getAuthDomain());
		streamWriter.writeString(instance.getUserId());
	}
}
