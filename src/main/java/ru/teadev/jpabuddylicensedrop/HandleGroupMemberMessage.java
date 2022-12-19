package ru.teadev.jpabuddylicensedrop;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKey;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKeyRepository;
import ru.teadev.jpabuddylicensedrop.storage.Owner;
import ru.teadev.jpabuddylicensedrop.storage.UserAction;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleGroupMemberMessage {

    private final Config config;
    private final LicenseKeyRepository licenseKeyRepository;

    @Transactional(rollbackFor = Exception.class)
    public void executeWithActionFilter(TelegramBot bot, Message message) {
        String text = message.text();
        Long fromId = message.from().id();

        UserAction action = UserAction.findByMessage(text);

        if (action != null) {
            executeAction(bot, action, message);

        } else {
            bot.execute(
                    messageWithActionButtons(fromId, "Выберите действие"));
        }
    }

    private void executeAction(@NonNull TelegramBot bot,
                               @NonNull UserAction action,
                               @NonNull Message message) {
        switch (action) {
            case GET_KEY:
                execute(bot, message.from());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + action);
        }
    }

    private SendMessage messageWithActionButtons(Long chatId, String text) {
        ReplyKeyboardMarkup replyKeyboardMarkup = null;

        for (UserAction action : UserAction.values()) {
            String message = action.getMessage();
            if (replyKeyboardMarkup == null) {
                replyKeyboardMarkup = new ReplyKeyboardMarkup(message).resizeKeyboard(true);
            } else {
                replyKeyboardMarkup.addRow(message);
            }
        }

        return new SendMessage(chatId, text)
                .replyMarkup(replyKeyboardMarkup);
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(TelegramBot bot, User from) {

        Long userId = from.id();

        LicenseKey ownedKey = licenseKeyRepository.findByOwner_UserId(userId);
        if (ownedKey != null) {
            log.info("Send owned key: " + ownedKey);
            bot.execute(messageWithActionButtons(userId, ownedKey.getKey()));
            return;
        }


        LicenseKey freeKey = licenseKeyRepository.findFirstByOwner_UserIdNull();

        if (freeKey != null) {
            freeKey.setOwner(
                    Owner.builder()
                            .userId(userId)
                            .username(from.username())
                            .firstname(from.firstName())
                            .lastname(from.lastName())
                            .build());
            licenseKeyRepository.save(freeKey);

            log.info("Send free key: " + freeKey);
            bot.execute(messageWithActionButtons(userId, freeKey.getKey()));
            bot.execute(messageWithActionButtons(userId, generateAd()));

        } else {
            log.info("Send no free key message");
            bot.execute(messageWithActionButtons(userId, "Нет доступных ключей"));
        }
    }

    private String generateAd() {
        return "Поставьте 5 звезд JpaBuddy!\n\n" +
                "https://plugins.jetbrains.com/plugin/15075-jpa-buddy/reviews";
    }

}
