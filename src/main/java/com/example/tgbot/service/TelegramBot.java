package com.example.tgbot.service;

import com.example.tgbot.config.BotConfig;

import com.example.tgbot.model.UserRepository;
import com.example.tgbot.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;

    static final String HELP_TEXT = "This bot is created to demonstrate Java Spring skills. if u have any questions contact me on " +
            "@bakuvi";
    final BotConfig botConfig;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "see collected data"));
        listOfCommands.add(new BotCommand("/deletedata", "delete ur collected data"));
        listOfCommands.add(new BotCommand("/help", "info how to use"));
        listOfCommands.add(new BotCommand("/settings", "set ur preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list" + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandRecieved(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                default:
                    sendMessage(chatId, "Sorry, command not recognized");
            }
        }

    }

    private  void registerUser(Message message) {
if(userRepository.findById(message.getChatId()).isEmpty()){
    Long chatId= message.getChatId();
    Chat chat= message.getChat();

    User user= new User();
    user.setChatId(chatId);
    user.setFirstName(chat.getFirstName());
    user.setLastName(chat.getLastName());
    user.setUserName(chat.getUserName());
    user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

    userRepository.save(user);
    log.info("user saved"+user);
}
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    private void startCommandRecieved(long chatId, String firstName) {
        String answer = "Hi, " + firstName + ", nice to meet u";
        log.info("Replied to user " + firstName);
        sendMessage(chatId, answer);

    }


    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }
}