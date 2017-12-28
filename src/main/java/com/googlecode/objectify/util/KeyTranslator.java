package com.googlecode.objectify.util;

import com.google.api.client.util.Base64;
import com.google.cloud.datastore.Key;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates between the Cloud Datastore and App Engine Datastore
 * key representations.
 */
public enum KeyTranslator {
    INSTANCE;

    // We build the descriptor for the App Engine Onestore Reference type in code
    // to avoid depending on the App Engine libraries.
    //
    // Since everybody serializes this everywhere in the form of websafe strings
    // we don't have to worry about the format changing from under us.
    //
    // Format was copied from https://github.com/golang/appengine/blob/master/internal/datastore/datastore_v3.proto
    private Descriptors.FileDescriptor keyDescriptor;

    KeyTranslator() {
        try {
            keyDescriptor = initializeFileDescriptor();
        } catch (Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
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

    public <T> com.googlecode.objectify.Key<T> parseAppEngineUrlsafeKey(String urlsafeKey) throws InvalidProtocolBufferException {
        Descriptors.Descriptor referenceDescriptor = KeyTranslator.INSTANCE.keyDescriptor.findMessageTypeByName("Reference");
        byte[] userKey = Base64.decodeBase64(urlsafeKey);
        DynamicMessage userKeyMessage = DynamicMessage.newBuilder(referenceDescriptor).mergeFrom(userKey).build();
        String app = (String) userKeyMessage.getField(referenceDescriptor.findFieldByName("app"));
        // TODO(frew): Does Google Cloud Datastore have the concept of namespace?
        // String namespace = (String) userKeyMessage.getField(referenceDescriptor.findFieldByName("name_space"));
        DynamicMessage path = (DynamicMessage) userKeyMessage.getField(referenceDescriptor.findFieldByName("path"));
        Descriptors.Descriptor pathDescriptor = KeyTranslator.INSTANCE.keyDescriptor.findMessageTypeByName("Path");
        Descriptors.Descriptor elementDescriptor = KeyTranslator.INSTANCE.keyDescriptor.findMessageTypeByName("Element");

        Descriptors.FieldDescriptor elementFieldDescriptor = pathDescriptor.findFieldByName("Element");
        int elementCount = path.getRepeatedFieldCount(elementFieldDescriptor);
        Key key = null;
        for (int i = 0; i < elementCount; i++) {
            DynamicMessage element = (DynamicMessage) path.getRepeatedField(elementFieldDescriptor, i);
            String type = (String) element.getField(elementDescriptor.findFieldByName("type"));
            Long id = (Long) element.getField(elementDescriptor.findFieldByName("id"));
            String name = (String) element.getField(elementDescriptor.findFieldByName("name"));
            if (key == null) {
                if (name != null && !"".equals(name)) {
                    key = Key.newBuilder(app, type, name).build();
                } else {
                    key = Key.newBuilder(app, type, id).build();
                }
            } else {
                if (name != null && !"".equals(name)) {
                    key = Key.newBuilder(key, type, name).build();
                } else {
                    key = Key.newBuilder(key, type, id).build();
                }
            }
        }

        return com.googlecode.objectify.Key.create(key);
    }

    public String generateAppEngineUrlSafeKey(com.googlecode.objectify.Key<?> key) {
        Descriptors.Descriptor referenceDescriptor = KeyTranslator.INSTANCE.keyDescriptor.findMessageTypeByName("Reference");
        DynamicMessage.Builder keyMessageBuilder = DynamicMessage.newBuilder(referenceDescriptor);
        keyMessageBuilder.setField(referenceDescriptor.findFieldByName("app"), key.getRaw().getProjectId());
        Descriptors.Descriptor elementDescriptor = KeyTranslator.INSTANCE.keyDescriptor.findMessageTypeByName("Element");

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

        Descriptors.Descriptor pathDescriptor = KeyTranslator.INSTANCE.keyDescriptor.findMessageTypeByName("Path");
        DynamicMessage.Builder pathBuilder = DynamicMessage.newBuilder(pathDescriptor);
        for (DynamicMessage elementMessage: elementMessages) {
            pathBuilder.addRepeatedField(pathDescriptor.findFieldByName("Element"), elementMessage);
        }
        keyMessageBuilder.setField(referenceDescriptor.findFieldByName("path"), pathBuilder.build());
        return Base64.encodeBase64URLSafeString(keyMessageBuilder.build().toByteArray());
    }

    public static void main(String[] args) throws Descriptors.DescriptorValidationException, InvalidProtocolBufferException {
        String urlsafeKey = "agxzfm1haWxmb29nYWVyKAsSDE9yZ2FuaXphdGlvbiIKc3RyZWFrLmNvbQwLEgRVc2VyGJneWww";
        System.out.println(KeyTranslator.INSTANCE.parseAppEngineUrlsafeKey(urlsafeKey));
        System.out.println(
                KeyTranslator.INSTANCE.generateAppEngineUrlSafeKey(
                        KeyTranslator.INSTANCE.parseAppEngineUrlsafeKey(urlsafeKey)));
    }
}
