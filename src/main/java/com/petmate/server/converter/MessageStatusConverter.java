package com.petmate.server.converter;

import com.petmate.server.enums.MessageStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MessageStatusConverter implements AttributeConverter<MessageStatus, String> {

    @Override
    public String convertToDatabaseColumn(MessageStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public MessageStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MessageStatus.valueOf(dbData);
    }
}
