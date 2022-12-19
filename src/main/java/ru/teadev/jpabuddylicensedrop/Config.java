package ru.teadev.jpabuddylicensedrop;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class Config {

    @Value("${bot.admin.user-id:303125770}")
    private Long adminUserId;

    @Value("${bot.token}")
    private String telegramToken;
    @Value("${bot.username:@innotech_jpabuddy_key_drop_bot}")
    private String botUsername;
    @Value("${bot.required-chat-id}")
    private String requiredChatId;

    @Value("${bot.input.cancel-command:Отмена}")
    private String cancelCommand;
    @Value("${bot.input.get-key-command:/get_key}")
    private String getKeyCommand;
    @Value("${bot.input.separator:\n}")
    private String inputSeparator;

    @Value("${bot.message.maxLength:4000}")
    private Integer messageMaxLength;

}
