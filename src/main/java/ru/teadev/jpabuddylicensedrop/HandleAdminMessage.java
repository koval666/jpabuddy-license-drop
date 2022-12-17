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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teadev.jpabuddylicensedrop.storage.AdminAction;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKey;
import ru.teadev.jpabuddylicensedrop.storage.LicenseKeyRepository;
import ru.teadev.jpabuddylicensedrop.storage.UserState;
import ru.teadev.jpabuddylicensedrop.storage.UserStateRepository;

@Service
@RequiredArgsConstructor
public class HandleAdminMessage {

    public static final String CANCEL_COMMAND = "Отмена";
    public static final String KEY_INPUT_SEPARATOR = "\n";
    public static final int MESSAGE_LENGTH_LIMIT = 4000;

    private final LicenseKeyRepository licenseKeyRepository;
    private final UserStateRepository userStateRepository;


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
                        messageWithActionButtons(fromId, "Привет админ!"));
            }

        } else if (CANCEL_COMMAND.equalsIgnoreCase(text)) {
            bot.execute(messageWithActionButtons(fromId, "Отменено"));
            userState.setPreviousAction(null);
            userStateRepository.save(userState);

        } else {
            switch (previousAction) {
                case ADD: {
                    saveKeys(text);
                    bot.execute(messageWithActionButtons(fromId, "Ключи добавлены"));
                }
                break;
                case REMOVE: {
                    long removed = removeKeys(text);
                    bot.execute(messageWithActionButtons(fromId, "Удалено " + removed + " ключей"));
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

        Arrays.stream(text.split(KEY_INPUT_SEPARATOR))
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

        List<String> keyForRemove = Arrays.stream(text.split(KEY_INPUT_SEPARATOR))
                .collect(Collectors.toList());

        return licenseKeyRepository.deleteByKeyIn(keyForRemove);
    }

    private void executeAction(TelegramBot bot, AdminAction action, Message message) {
        switch (action) {

            case ADD:
            case REMOVE: {
                SendMessage sendMessage = createMessage(
                        message.chat().id(),
                        "Введите ключи, разделитель - новая строка")
                        .replyMarkup(new ReplyKeyboardMarkup(CANCEL_COMMAND).resizeKeyboard(true));
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

            default:
                throw new IllegalStateException("Unexpected value: " + action);
        }
    }

    private static List<String> licenseStrings(List<LicenseKey> licenseKeys) {
        if (licenseKeys.isEmpty()) {
            return List.of("[]");
        }

        List<String> keyStrings = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        for (LicenseKey licenseKey : licenseKeys) {
            int beforeLength = stringBuilder.length();
            String licenseKeyString = licenseKey.toString();

            if (licenseKeyString.length() > MESSAGE_LENGTH_LIMIT) {
                throw new RuntimeException("Too long single license key string");

            } else if (beforeLength + licenseKeyString.length() > MESSAGE_LENGTH_LIMIT) {
                keyStrings.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(licenseKeyString);
                stringBuilder.append(KEY_INPUT_SEPARATOR);

            } else {

                stringBuilder.append(licenseKeyString);
                stringBuilder.append(KEY_INPUT_SEPARATOR);
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
