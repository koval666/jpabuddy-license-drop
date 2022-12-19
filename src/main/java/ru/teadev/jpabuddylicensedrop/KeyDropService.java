package ru.teadev.jpabuddylicensedrop;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static com.pengrad.telegrambot.model.Chat.Type.Private;
import static java.text.MessageFormat.format;
import static java.util.Optional.ofNullable;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.ChatMember.Status;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetChatMemberResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyDropService {

    private static final List<Status> CHAT_MEMBER_STATUSES =
            List.of(Status.member, Status.administrator, Status.creator);

    private final Config config;

    private final HandleAdminMessage handleAdminMessage;
    private final HandleGroupMemberMessage handleGroupMemberMessage;

    private TelegramBot bot;

    @PostConstruct
    void initBot() {
        bot = new TelegramBot(config.getTelegramToken());

        bot.setUpdatesListener(new UpdatesListener() {
            @Override
            public int process(List<Update> updates) {

                for (Update update : updates) {
                    Message message = update.message();
                    try {
                        if (message != null) {
                            handleMessage(message);
                        }
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

    private void handleMessage(@NonNull Message message) {

        if (isPrivateChat(message)) {

            Long userId = message.chat().id();
            String text = message.text();
            String username = ofNullable(message.from()).map(User::username).orElse(null);

            if (isAdmin(userId)) {
                log.info(format("Admin message processing:\ntext: {0}\nauthor: @{1}", text, username));
                handleAdminMessage.execute(bot, message);

            } else if (isGroupMember(userId)) {
                log.info(format("Group member message processing:\ntext: {0}\nauthor: @{1}", text, username));
                handleGroupMemberMessage.executeWithCommandFilter(bot, message);

            } else {
                log.info("Foreign user message from: @" + username);
                handleForeignMessage(userId);
            }

        } else if ((config.getGetKeyCommand() + config.getBotUsername()).equals(message.text())) {
            log.info("Not private chat call: " + message.chat().title());
            bot.execute(message(message.chat().id(),
                    "Для получения ключа напишите боту в личку:\n" + config.getBotUsername()));
//            sendJoke(bot, message.chat().id());
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
        return config.getAdminUserId().equals(userId);
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
        GetChatMember getChatMember = new GetChatMember(config.getRequiredChatId(), userId);
        GetChatMemberResponse memberResponse = bot.execute(getChatMember);
        ChatMember chatMember = memberResponse.chatMember();

        return chatMember != null
                && chatMember.status() != null
                && CHAT_MEMBER_STATUSES.contains(chatMember.status());
    }
}
