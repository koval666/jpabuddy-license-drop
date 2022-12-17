package ru.teadev.jpabuddylicensedrop;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static com.pengrad.telegrambot.model.Chat.Type.Private;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetChatMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyDropService {

    private static final Long ADMIN_USER_ID = 303125770L;
    public static final String BOT_USERNAME = "@innotech_jpabuddy_key_drop_bot";

    @Value("${TELEGRAM_TOKEN}")
    String telegramToken;
    @Value("${REQUIRED_CHAT_ID}")
    String requiredChatId;

    private final HandleAdminMessage handleAdminMessage;
    private final HandleGroupMemberMessage handleGroupMemberMessage;

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

        if (isPrivateChat(message)) {

            Long userId = message.chat().id();

            if (isAdmin(userId)) {
                handleAdminMessage.execute(bot, message);

            } else if (isGroupMember(userId)) {
                handleGroupMemberMessage.executeWithCommandFilter(bot, message);

            } else {
                handleForeignMessage(userId);
            }

        } else {
            log.info("Not private chat call: " + message.chat().title());
            bot.execute(message(message.chat().id(),
                    "Для получения ключа напишите боту в личку:\n" + BOT_USERNAME));
            sendJoke(bot, message.chat().id());
        }
    }

    private void sendJoke(TelegramBot bot, Long chatId) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://rzhunemogu.ru/Rand.aspx?CType=1")
                .build();

        Mono<Joke> jokeMono = webClient.get()
                .acceptCharset(Charset.forName("windows-1251"))
                .retrieve()
                .bodyToMono(Joke.class);

        jokeMono.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .filter(Objects::nonNull)
                .subscribe(joke -> bot.execute(message(chatId, "Анекдот вспомнился:\n\n" + joke.content)));
    }

    @XmlRootElement(name = "root")
    static class Joke {
        @XmlElement
        String content;
    }

    private void handleForeignMessage(Long userId) {
        bot.execute(
                message(userId, "У вас нет доступа"));
    }

    private boolean isAdmin(Long userId) {
        return ADMIN_USER_ID.equals(userId);
    }

    private boolean isPrivateChat(Message message) {
        return message != null
                && message.chat() != null
                && Private.equals(message.chat().type());
    }

    private static SendMessage message(Long chatId, String text) {
        return new SendMessage(chatId, text)
                .replyMarkup(new ReplyKeyboardRemove());
    }

    private boolean isGroupMember(Long userId) {
        GetChatMember getChatMember = new GetChatMember(requiredChatId, userId);
        GetChatMemberResponse memberResponse = bot.execute(getChatMember);

        return memberResponse.chatMember() != null;
    }
}
