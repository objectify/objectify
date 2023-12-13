package com.googlecode.objectify.util;

import com.google.cloud.datastore.Key;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

/**
 * The new Cloud SDK has a new format for "url safe strings". This code lets us read and write the old-style
 * GAE standard web safe string format.
 *
 * @author Fred Wulff
 */
public enum KeyFormat {
    INSTANCE;

    // We build the descriptor for the App Engine Onestore Reference type in code
    // to avoid depending on the App Engine libraries.
    //
    // Since everybody serializes this everywhere in the form of websafe strings
    // we don't have to worry about the format changing from under us.
    //
    // Format was copied from https://github.com/golang/appengine/blob/master/internal/datastore/datastore_v3.proto
    private final Descriptors.FileDescriptor keyDescriptor;

    @SneakyThrows
    KeyFormat() {
        this.keyDescriptor = Descriptors.FileDescriptor.buildFrom(DescriptorProtos.FileDescriptorProto.newBuilder()
                .addMessageType(makeElement())
                .addMessageType(makePath())
                .addMessageType(makeReference())
                .build(), new Descriptors.FileDescriptor[]{});
    }

    private DescriptorProto makeElement() {
        return DescriptorProtos.DescriptorProto.newBuilder()
                .setName("Element")
                .addField(
                        DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
                                .setName("type")
                                .setNumber(2)
                                .build()
                )
                .addField(
                        DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64)
                                .setName("id")
                                .setNumber(3)
                                .build()
                )
                .addField(
                        DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
                                .setName("name")
                                .setNumber(4)
                                .build()
                ).build();
    }

    private DescriptorProto makePath() {
        return DescriptorProtos.DescriptorProto.newBuilder()
                .setName("Path")
                .addField(
                        DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP)
                                .setName("Element")
                                .setTypeName("Element")
                                .setNumber(1)
                                .build()
                ).build();
    }

    private DescriptorProto makeReference() {
        return DescriptorProto.newBuilder()
                .setName("Reference")
                .addField(
                        FieldDescriptorProto.newBuilder()
                                .setLabel(Label.LABEL_REQUIRED)
                                .setType(Type.TYPE_STRING)
                                .setName("app")
                                .setNumber(13)
                                .build()
                )
                .addField(
                        FieldDescriptorProto
                                .newBuilder()
                                .setLabel(Label.LABEL_OPTIONAL)
                                .setType(Type.TYPE_STRING)
                                .setName("name_space")
                                .setNumber(20)
                                .build()
                )
                .addField(
                        FieldDescriptorProto.newBuilder()
                                .setLabel(Label.LABEL_REQUIRED)
                                .setType(Type.TYPE_MESSAGE)
                                .setTypeName("Path")
                                .setName("path")
                                .setNumber(14)
                                .build()
                ).build();
    }

    public Key parseOldStyleAppEngineKey(final String urlsafeKey) throws InvalidProtocolBufferException {
        final Descriptors.Descriptor referenceDescriptor = keyDescriptor.findMessageTypeByName("Reference");
        byte[] userKey = BaseEncoding.base64Url().decode(urlsafeKey);
        final DynamicMessage userKeyMessage = DynamicMessage.newBuilder(referenceDescriptor).mergeFrom(userKey).build();
        String app = (String) userKeyMessage.getField(referenceDescriptor.findFieldByName("app"));

        if (app.startsWith("s~") || app.startsWith("f~") || app.startsWith("a~")) {
            app = app.substring(2);
        }

        final String namespace = (String) userKeyMessage.getField(referenceDescriptor.findFieldByName("name_space"));
        final DynamicMessage path = (DynamicMessage) userKeyMessage.getField(referenceDescriptor.findFieldByName("path"));
        final Descriptors.Descriptor pathDescriptor = keyDescriptor.findMessageTypeByName("Path");
        final Descriptors.Descriptor elementDescriptor = keyDescriptor.findMessageTypeByName("Element");

        final Descriptors.FieldDescriptor elementFieldDescriptor = pathDescriptor.findFieldByName("Element");
        final int elementCount = path.getRepeatedFieldCount(elementFieldDescriptor);

        Key.Builder keyBuilder = null;
        for (int i = 0; i < elementCount; i++) {
            final DynamicMessage element = (DynamicMessage) path.getRepeatedField(elementFieldDescriptor, i);
            final String type = (String) element.getField(elementDescriptor.findFieldByName("type"));
            final Long id = (Long) element.getField(elementDescriptor.findFieldByName("id"));
            final String name = (String) element.getField(elementDescriptor.findFieldByName("name"));
            if (keyBuilder == null) {
                if (name != null && !"".equals(name)) {
                    keyBuilder = Key.newBuilder(app, type, name);
                } else {
                    keyBuilder = Key.newBuilder(app, type, id);
                }
            } else {
                if (name != null && !"".equals(name)) {
                    keyBuilder = Key.newBuilder(keyBuilder.build(), type, name);
                } else {
                    keyBuilder = Key.newBuilder(keyBuilder.build(), type, id);
                }
            }
        }

        if (keyBuilder != null) {
            if (namespace != null) {
                keyBuilder.setNamespace(namespace);
            }
            return keyBuilder.build();
        } else {
            return null;
        }
    }

    public String formatOldStyleAppEngineKey(Key key) {
        final Descriptors.Descriptor referenceDescriptor = keyDescriptor.findMessageTypeByName("Reference");
        final DynamicMessage.Builder keyMessageBuilder = DynamicMessage.newBuilder(referenceDescriptor);
        String fullProjectId = key.getProjectId();
        if (!fullProjectId.startsWith("s~")) {
            fullProjectId = "s~" + fullProjectId;
        }
        keyMessageBuilder.setField(referenceDescriptor.findFieldByName("app"), fullProjectId);

        if (key.getNamespace() != null && !key.getNamespace().isEmpty()) {
            final FieldDescriptor namespaceDescriptor = referenceDescriptor.findFieldByName("name_space");
            keyMessageBuilder.setField(namespaceDescriptor, key.getNamespace());
        }

        final Descriptors.Descriptor elementDescriptor = keyDescriptor.findMessageTypeByName("Element");

        final List<DynamicMessage> elementMessages = new ArrayList<>();
        do {
            final DynamicMessage.Builder elementMessageBuilder = DynamicMessage.newBuilder(elementDescriptor);
            elementMessageBuilder.setField(elementDescriptor.findFieldByName("type"), key.getKind());
            if (key.getName() != null) {
                elementMessageBuilder.setField(elementDescriptor.findFieldByName("name"), key.getName());
            } else {
                elementMessageBuilder.setField(elementDescriptor.findFieldByName("id"), key.getId());
            }
            elementMessages.add(0, elementMessageBuilder.build());
        } while ((key = key.getParent()) != null);

        final Descriptors.Descriptor pathDescriptor = keyDescriptor.findMessageTypeByName("Path");
        final DynamicMessage.Builder pathBuilder = DynamicMessage.newBuilder(pathDescriptor);
        for (final DynamicMessage elementMessage: elementMessages) {
            pathBuilder.addRepeatedField(pathDescriptor.findFieldByName("Element"), elementMessage);
        }
        keyMessageBuilder.setField(referenceDescriptor.findFieldByName("path"), pathBuilder.build());
        return BaseEncoding.base64Url().omitPadding().encode(keyMessageBuilder.build().toByteArray());
    }
}
