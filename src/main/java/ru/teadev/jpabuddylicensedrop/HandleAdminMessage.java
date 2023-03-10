package ru.teadev.jpabuddylicensedrop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teadev.jpabuddylicensedrop.storage.AdminAction;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKey;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKeyRepository;
import ru.teadev.jpabuddylicensedrop.storage.UserState;
import ru.teadev.jpabuddylicensedrop.storage.UserStateRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleAdminMessage {

    private final Config config;

    private final LicenseKeyRepository licenseKeyRepository;
    private final UserStateRepository userStateRepository;
    @Autowired
    private HandleGroupMemberMessage handleGroupMemberMessage;


    @Transactional(rollbackFor = Exception.class)
    public void execute(TelegramBot bot, Message message) {

        String text = message.text();
        Long fromId = message.from().id();

        UserState userState = userStateRepository.findById(fromId)
                .orElseGet(() -> new UserState(fromId));

        AdminAction previousAction = userState.getPreviousAction();

        if (previousAction == null) {
            AdminAction action = AdminAction.findByMessage(text);

            if (action != null) {
                executeAction(bot, action, message);

                if (action.isNeedAnswer()) {
                    userState.setPreviousAction(action);
                    userStateRepository.save(userState);
                }

            } else {
                bot.execute(
                        messageWithActionButtons(fromId, "???????????? ??????????!"));
            }

        } else if (config.getCancelCommand().equalsIgnoreCase(text)) {
            bot.execute(messageWithActionButtons(fromId, "????????????????"));
            userState.setPreviousAction(null);
            userStateRepository.save(userState);

        } else {
            switch (previousAction) {
                case ADD: {
                    saveKeys(text);
                    bot.execute(messageWithActionButtons(fromId, "?????????? ??????????????????"));
                }
                break;
                case REMOVE: {
                    long removed = removeKeys(text);
                    bot.execute(messageWithActionButtons(fromId, "?????????????? " + removed + " ????????????"));
                }
                break;
                default:
                    throw new IllegalStateException("Unexpected value: " + previousAction);
            }

            userState.setPreviousAction(null);
            userStateRepository.save(userState);
        }
    }

    private void saveKeys(String text) {
        if (text == null) {
            return;
        }

        Arrays.stream(text.split(config.getInputSeparator()))
                .map(key -> {
                    LicenseKey licenseKey = new LicenseKey();
                    licenseKey.setKey(key);
                    return licenseKey;
                })
                .forEach(licenseKeyRepository::save);
    }

    private long removeKeys(String text) {
        if (text == null) {
            return 0;
        }

        List<String> keyForRemove = Arrays.stream(text.split(config.getInputSeparator()))
                .collect(Collectors.toList());

        return licenseKeyRepository.deleteByKeyIn(keyForRemove);
    }

    private void executeAction(TelegramBot bot, AdminAction action, Message message) {
        switch (action) {

            case ADD:
            case REMOVE: {
                SendMessage sendMessage = createMessage(
                        message.chat().id(),
                        "?????????????? ??????????, ?????????????????????? - ?????????? ????????????")
                        .replyMarkup(new ReplyKeyboardMarkup(config.getCancelCommand()).resizeKeyboard(true));
                bot.execute(sendMessage);
            }
            break;

            case GIVEN_COUNT: {
                long count = licenseKeyRepository.countByOwner_UserIdNotNull();
                bot.execute(
                        messageWithActionButtons(message.chat().id(), String.valueOf(count)));
            }
            break;

            case GIVEN_LIST: {
                List<LicenseKey> userIdNotNull = licenseKeyRepository.findByOwner_UserIdNotNull();
                for (String licenseString : licenseStrings(userIdNotNull)) {
                    bot.execute(
                            messageWithActionButtons(message.chat().id(), licenseString));
                }
            }
            break;

            case FREE_COUNT: {
                long count = licenseKeyRepository.countByOwner_UserIdNull();
                bot.execute(
                        messageWithActionButtons(message.chat().id(), String.valueOf(count)));
            }
            break;

            case FREE_LIST: {
                List<LicenseKey> userIdNull = licenseKeyRepository.findByOwner_UserIdNull();
                for (String licenseString : licenseStrings(userIdNull)) {
                    bot.execute(
                            messageWithActionButtons(message.chat().id(), licenseString));
                }
            }
            break;

            case GET_KEY_AS_USER:
                handleGroupMemberMessage.execute(bot, message.from());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + action);
        }
    }

    private List<String> licenseStrings(List<LicenseKey> licenseKeys) {
        if (licenseKeys.isEmpty()) {
            return List.of("[]");
        }

        List<String> keyStrings = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        for (LicenseKey licenseKey : licenseKeys) {
            int beforeLength = stringBuilder.length();
            String licenseKeyString = licenseKey.toString();

            if (licenseKeyString.length() > config.getMessageMaxLength()) {
                throw new RuntimeException("Too long single license key string");

            } else if (beforeLength + licenseKeyString.length() > config.getMessageMaxLength()) {
                keyStrings.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(licenseKeyString);
                stringBuilder.append(config.getInputSeparator());

            } else {

                stringBuilder.append(licenseKeyString);
                stringBuilder.append(config.getInputSeparator());
            }
        }

        keyStrings.add(stringBuilder.toString());

        return keyStrings;
    }

    private SendMessage messageWithActionButtons(Long chatId, String text) {
        ReplyKeyboardMarkup replyKeyboardMarkup = null;

        for (AdminAction action : AdminAction.values()) {
            String message = action.getMessage();
            if (replyKeyboardMarkup == null) {
                replyKeyboardMarkup = new ReplyKeyboardMarkup(message).resizeKeyboard(true);
            } else {
                replyKeyboardMarkup.addRow(message);
            }
        }

        return createMessage(chatId, text)
                .replyMarkup(replyKeyboardMarkup);
    }

    private SendMessage createMessage(Long chatId, String text) {
        return new SendMessage(chatId, text)
                .disableWebPagePreview(true);
    }

}
