package com.googlecode.objectify.test;

import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.testng.annotations.Test;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 */
public class EmbedCollectionTests extends TestBase
{

	@Entity(name = "ChatRoom")
	@Unindex
	@Cache
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ChatRoomEntity {
		@Id
		private String chatRoomId;
		private List<ChatEntry> chatEntries = new ArrayList<ChatEntry>();
	}

	@Embed
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ChatEntry implements Serializable {
		private int chatEntryId;
		private String playerId;
		private String message;
		private Date discussiondate;
	}

	@Test
	public void loadAndSaveInATransaction() throws Exception {

		fact().register(ChatRoomEntity.class);

		final ChatRoomEntity room = new ChatRoomEntity();
		room.setChatRoomId("room1");
		room.getChatEntries().add(new ChatEntry(123, "player1", "hello", new Date()));
		room.getChatEntries().add(new ChatEntry(456, "player2", "hello", new Date()));

		ofy().save().entity(room);

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				final ChatRoomEntity r = ofy().load().type(ChatRoomEntity.class).id("room1").now();
				ofy().save().entity(r).now();
			}
		});
	}
}
