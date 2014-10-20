package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Protobuf;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

// For testing, use protobufs that are already baked into the protobuf
// library. This way, we don't need to add a dependency on protoc.
import com.google.protobuf.DescriptorProtos;

/**
 * Tests of the {@code @Protobuf} annotation
 */
public class ProtobufTests extends TestBase {
	@Entity
	@Cache
	public static class HasProto {
		@Id public Long id;
		@Protobuf public DescriptorProtos.FieldDescriptorProto proto;
	}

	@Test
	public void testSimpleProtobuf() throws Exception {
		fact().register(HasProto.class);

		HasProto hp = new HasProto();
		hp.proto = DescriptorProtos.FieldDescriptorProto.newBuilder()
			.setName("foo")
			.setNumber(57)
			.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
			.build();

		HasProto fetched = ofy().saveClearLoad(hp);
		assert fetched.proto.equals(hp.proto);
	}

	// TODO: Check that the blob being stored is actually a serialized protobuf
	// that could be parsed in other languages too. For example,
	// DescriptorProtos.FieldDescriptorProto.parseFrom(data) should work.
}
