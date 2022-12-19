package ru.teadev.jpabuddylicensedrop.storage;

import java.util.Arrays;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public
enum UserAction {

    GET_KEY("Получить ключ", false);


    @NonNull
    @Getter
    final String message;
    @Getter
    final boolean needAnswer;

    @Nullable
    public static UserAction findByMessage(@Nullable String message) {
        for (UserAction value : values()) {
            if (value.getMessage().equals(message)) {
                return value;
            }
        }
        return null;
    }

    @NonNull
    public static String[] getMessages() {
        return Arrays.stream(values())
                .map(UserAction::getMessage)
                .toArray(String[]::new);
    }
}
