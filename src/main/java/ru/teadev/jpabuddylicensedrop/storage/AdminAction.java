package ru.teadev.jpabuddylicensedrop.storage;

import java.util.Arrays;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public
enum AdminAction {
    ADD("Добавить ключи", true),
    REMOVE("Удалить ключи", true),
    GIVEN_COUNT("Сколько выданных ключей", false),
    GIVEN_LIST("Выданные ключи", false),
    FREE_COUNT("Сколько свободных ключей", false),
    FREE_LIST("Свободные ключи", false),
    GET_KEY_AS_USER("Получить ключ как юзер", false);


    @NonNull
    @Getter
    final String message;
    @Getter
    final boolean needAnswer;

    @Nullable
    public static AdminAction findByMessage(@Nullable String message) {
        for (AdminAction value : values()) {
            if (value.getMessage().equals(message)) {
                return value;
            }
        }
        return null;
    }

    @NonNull
    public static String[] getMessages() {
        return Arrays.stream(values())
                .map(AdminAction::getMessage)
                .toArray(String[]::new);
    }
}
