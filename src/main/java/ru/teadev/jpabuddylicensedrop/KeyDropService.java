package ru.teadev.jpabuddylicensedrop;

import java.util.List;
import javax.annotation.PostConstruct;

import static com.pengrad.telegrambot.model.Chat.Type.Private;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetChatMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyDropService {

    private static final Long ADMIN_USER_ID = 303125770L;
    private static final String GET_KEY_COMMAND = "/get_key";

    @Value("${TELEGRAM_TOKEN}")
    String telegramToken;
    @Value("${REQUIRED_CHAT_ID}")
    String requiredChatId;

    private final HandleAdminMessage handleAdminMessage;

    private TelegramBot bot;

    @PostConstruct
    void initBot() {
        bot = new TelegramBot(telegramToken);

        bot.setUpdatesListener(new UpdatesListener() {
            @Override
            public int process(List<Update> updates) {

                for (Update update : updates) {
                    Message message = update.message();
                    try {
                        handleMessage(message);
                    } catch (Exception e) {
                        bot.execute(
                                message(message.chat().id(), "Ошибка " + e.getClass().getSimpleName()));
                        log.error(e.getMessage(), e);
                    }
                }

                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }
        });
    }

    private void handleMessage(Message message) {

        if (isChatPrivate(message)) {

            Long userId = message.chat().id();

            if (isAdmin(userId)) {
                handleAdminMessage.execute(bot, message);

            } else if (isGroupMember(userId)) {
                handelGroupMemberMessage(userId);

            } else {
                handleForeignMessage(userId);
            }
        }
    }


    private void handelGroupMemberMessage(Long userId) {
        bot.execute(
                message(userId, "Ваш ключ:\n12345"));
    }

    private void handleForeignMessage(Long userId) {
        bot.execute(
                message(userId, "У вас нет доступа"));
    }

    private boolean isAdmin(Long userId) {
        return ADMIN_USER_ID.equals(userId);
    }

    private boolean isChatPrivate(Message message) {
        return message != null
                && message.chat() != null
                && Private.equals(message.chat().type());
    }

    private static SendMessage message(Long chatId, String text) {
        return new SendMessage(chatId, text)
                .disableWebPagePreview(true);
    }

    private boolean isGroupMember(Long userId) {
        GetChatMember getChatMember = new GetChatMember(requiredChatId, userId);
        GetChatMemberResponse memberResponse = bot.execute(getChatMember);

        return memberResponse.chatMember() != null;
    }
}
