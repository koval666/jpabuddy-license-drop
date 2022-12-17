package ru.teadev.jpabuddylicensedrop;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKey;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKeyRepository;
import ru.teadev.jpabuddylicensedrop.storage.Owner;

@Service
@RequiredArgsConstructor
public class HandleGroupMemberMessage {

    private static final String GET_KEY_COMMAND = "/get_key";

    private final LicenseKeyRepository licenseKeyRepository;

    @Transactional(rollbackFor = Exception.class)
    public void execute(TelegramBot bot, Message message) {
        if (!GET_KEY_COMMAND.equals(message.text())) {
            return;
        }

        User from = message.from();
        Long userId = from.id();

        LicenseKey ownedKey = licenseKeyRepository.findByOwner_UserId(userId);
        if (ownedKey != null) {
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

            bot.execute(createMessage(userId, freeKey.getKey()));
            bot.execute(createMessage(userId, generateAd()));

        } else {
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
