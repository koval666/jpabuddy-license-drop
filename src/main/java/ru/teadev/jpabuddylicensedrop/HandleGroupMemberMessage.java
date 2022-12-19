package ru.teadev.jpabuddylicensedrop;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKey;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKeyRepository;
import ru.teadev.jpabuddylicensedrop.storage.Owner;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleGroupMemberMessage {

    private final Config config;
    private final LicenseKeyRepository licenseKeyRepository;

    @Transactional(rollbackFor = Exception.class)
    public void executeWithCommandFilter(TelegramBot bot, Message message) {
        if (!config.getGetKeyCommand().equals(message.text())) {
            log.info("Skip message, wrong text command: " + message.text());
            bot.execute(createMessage(message.from().id(), "Используйте команду " + config.getGetKeyCommand()));
            return;
        }

        execute(bot, message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(TelegramBot bot, Message message) {

        User from = message.from();
        Long userId = from.id();

        LicenseKey ownedKey = licenseKeyRepository.findByOwner_UserId(userId);
        if (ownedKey != null) {
            log.info("Send owned key: " + ownedKey);
            bot.execute(createMessage(userId, ownedKey.getKey()));
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
            bot.execute(createMessage(userId, freeKey.getKey()));
            bot.execute(createMessage(userId, generateAd()));

        } else {
            log.info("Send no free key message");
            bot.execute(createMessage(userId, "Нет доступных ключей"));
        }
    }

    private String generateAd() {
        return "Поставьте 5 звезд JpaBuddy!\n\n" +
                "https://plugins.jetbrains.com/plugin/15075-jpa-buddy/reviews";
    }

    private SendMessage createMessage(Long chatId, String text) {
        return new SendMessage(chatId, text).replyMarkup(new ReplyKeyboardRemove());
    }

}
