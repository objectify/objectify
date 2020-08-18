package com.googlecode.objectify.util;

import com.google.api.client.util.Base64;
import com.google.cloud.datastore.Key;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
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
    private Descriptors.FileDescriptor keyDescriptor;

    @SneakyThrows
    KeyFormat() {
        keyDescriptor = initializeFileDescriptor();
    }

    private Descriptors.FileDescriptor initializeFileDescriptor() throws Descriptors.DescriptorValidationException {
        DescriptorProtos.DescriptorProto elementDescriptor = DescriptorProtos.DescriptorProto.newBuilder()
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

        DescriptorProtos.DescriptorProto pathDescriptor = DescriptorProtos.DescriptorProto.newBuilder()
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

        DescriptorProtos.DescriptorProto referenceDescriptor = DescriptorProtos.DescriptorProto.newBuilder()
                .setName("Reference")
                .addField(
                        DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
                                .setName("app")
                                .setNumber(13)
                                .build())
                .addField(
                        DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
                                .setName("name_space")
                                .setNumber(20)
                                .build())
                .addField(
                        DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                                .setTypeName("Path")
                                .setName("path")
                                .setNumber(14)
                                .build())
                .build();

        return Descriptors.FileDescriptor.buildFrom(DescriptorProtos.FileDescriptorProto.newBuilder()
                .addMessageType(elementDescriptor)
                .addMessageType(pathDescriptor)
                .addMessageType(referenceDescriptor)
                .build(), new Descriptors.FileDescriptor[]{});
    }

    public Key parseOldStyleAppEngineKey(final String urlsafeKey) throws InvalidProtocolBufferException {
        Descriptors.Descriptor referenceDescriptor = keyDescriptor.findMessageTypeByName("Reference");
        byte[] userKey = Base64.decodeBase64(urlsafeKey);
        DynamicMessage userKeyMessage = DynamicMessage.newBuilder(referenceDescriptor).mergeFrom(userKey).build();
        String app = (String) userKeyMessage.getField(referenceDescriptor.findFieldByName("app"));
        if (app.startsWith("s~")) {
            app = app.substring(2);
        }
        String namespace = (String) userKeyMessage.getField(referenceDescriptor.findFieldByName("name_space"));
        DynamicMessage path = (DynamicMessage) userKeyMessage.getField(referenceDescriptor.findFieldByName("path"));
        Descriptors.Descriptor pathDescriptor = keyDescriptor.findMessageTypeByName("Path");
        Descriptors.Descriptor elementDescriptor = keyDescriptor.findMessageTypeByName("Element");

        Descriptors.FieldDescriptor elementFieldDescriptor = pathDescriptor.findFieldByName("Element");
        int elementCount = path.getRepeatedFieldCount(elementFieldDescriptor);
        Key.Builder keyBuilder = null;
        for (int i = 0; i < elementCount; i++) {
            DynamicMessage element = (DynamicMessage) path.getRepeatedField(elementFieldDescriptor, i);
            String type = (String) element.getField(elementDescriptor.findFieldByName("type"));
            Long id = (Long) element.getField(elementDescriptor.findFieldByName("id"));
            String name = (String) element.getField(elementDescriptor.findFieldByName("name"));
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
        Descriptors.Descriptor referenceDescriptor = keyDescriptor.findMessageTypeByName("Reference");
        DynamicMessage.Builder keyMessageBuilder = DynamicMessage.newBuilder(referenceDescriptor);
        String fullProjectId = key.getProjectId();
        if (!fullProjectId.startsWith("s~")) {
            fullProjectId = "s~" + fullProjectId;
        }
        keyMessageBuilder.setField(referenceDescriptor.findFieldByName("app"), fullProjectId);
        keyMessageBuilder.setField(referenceDescriptor.findFieldByName("name_space"), key.getNamespace());
        Descriptors.Descriptor elementDescriptor = keyDescriptor.findMessageTypeByName("Element");

        List<DynamicMessage> elementMessages = new ArrayList<>();
        do {
            DynamicMessage.Builder elementMessageBuilder = DynamicMessage.newBuilder(elementDescriptor);
            elementMessageBuilder.setField(elementDescriptor.findFieldByName("type"), key.getKind());
            if (key.getName() != null) {
                elementMessageBuilder.setField(elementDescriptor.findFieldByName("name"), key.getName());
            } else {
                elementMessageBuilder.setField(elementDescriptor.findFieldByName("id"), key.getId());
            }
            elementMessages.add(0, elementMessageBuilder.build());
        } while ((key = key.getParent()) != null);

        Descriptors.Descriptor pathDescriptor = keyDescriptor.findMessageTypeByName("Path");
        DynamicMessage.Builder pathBuilder = DynamicMessage.newBuilder(pathDescriptor);
        for (DynamicMessage elementMessage: elementMessages) {
            pathBuilder.addRepeatedField(pathDescriptor.findFieldByName("Element"), elementMessage);
        }
        keyMessageBuilder.setField(referenceDescriptor.findFieldByName("path"), pathBuilder.build());
        return Base64.encodeBase64URLSafeString(keyMessageBuilder.build().toByteArray());
    }
}
