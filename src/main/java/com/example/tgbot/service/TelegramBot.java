package com.example.tgbot.service;

import com.example.tgbot.config.BotConfig;
import com.example.tgbot.model.Joke;
import com.example.tgbot.model.JokeRepository;
import com.example.tgbot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    public static final String ERROR_OCCURED = "Error occured: ";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    JokeRepository jokeRepository;

    static final String HELP_TEXT = "This bot is created to demonstrate Java Spring skills. if u have any questions contact me on " + "@bakuvi";

    private BotConfig botConfig;

    static final int MAX_JOKE_ID_MINUS_ONE = 3772;
    static final String NEXT_JOKE = "NEXT_JOKE";


    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/joke", "get random joke"));
        listOfCommands.add(new BotCommand("/help", "info how to use"));
        listOfCommands.add(new BotCommand("/settings", "set ur preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (messageText.contains("/send") && botConfig.getBotOwnerId() == chatId) {
                var textToSend = messageText.substring(messageText.indexOf(" "));

                var user = userRepository.findById(botConfig.getTestUserId()).get();
                sendMessage(user.getChatId(), textToSend);
            } else {
                switch (messageText) {
                    case "/start" -> {
                        showStart(chatId, update.getMessage().getChat().getFirstName());
//                        ObjectMapper objectMapper = new ObjectMapper();
//                        TypeFactory typeFactory = objectMapper.getTypeFactory();
//                        try {
//                            List<Joke> jokeList = objectMapper.readValue(new File("/Users/bakuvi/IdeaProjects/TgBot/db/stupidstuff.json"),
//                                    typeFactory.constructCollectionType(List.class, Joke.class));
//                            jokeRepository.saveAll(jokeList);
//                        } catch (IOException e) {
//                            log.error(Arrays.toString(e.getStackTrace()));
//                        }
                    }
                    case "/joke" -> {
                        var joke = getRandomJoke();

                        joke.ifPresent(random -> addButtonAndSendMessage(random.getBody(), chatId));

                    }
                    case "/help" -> {
                        sendMessage(chatId, HELP_TEXT);
                    }
                    default -> {
                        sendMessage(chatId, "Sorry, command not recognized");
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(NEXT_JOKE)) {
                var joke = getRandomJoke();
                joke.ifPresent(randomJoke -> addButtonAndSendMessage(randomJoke.getBody(), chatId));

            }
        }
    }

    private Optional<Joke> getRandomJoke() {
        var r = new Random();
        var randomId = r.nextInt(MAX_JOKE_ID_MINUS_ONE) + 1;
        return jokeRepository.findById(randomId);

    }

    private void addButtonAndSendMessage(String joke, long chatId) {
        SendMessage message = new SendMessage();
        message.setText(joke);
        message.setChatId(chatId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setCallbackData(NEXT_JOKE);
        inlineKeyboardButton.setText("next joke");
        rowInline.add(inlineKeyboardButton);
        rowsInline.add(rowInline);
        markup.setKeyboard(rowsInline);
        message.setReplyMarkup(markup);
        executeMessage(message);

    }

//    private void registerUser(Message message) {
//
//        if (userRepository.findById(message.getChatId()).isEmpty()) {
//            Long chatId = message.getChatId();
//            Chat chat = message.getChat();
//
//            User user = new User();
//            user.setChatId(chatId);
//            user.setFirstName(chat.getFirstName());
//            user.setLastName(chat.getLastName());
//            user.setUserName(chat.getUserName());
//            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
//
//            userRepository.save(user);
//            log.info("user saved" + user);
//        }
//    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    private void showStart(long chatId, String firstName) {
        String answer = "Hi, " + firstName + ", nice to meet u";
        log.info("Replied to user " + firstName);
        sendMessage(chatId, answer);

    }


    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        executeMessage(message);
    }


    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURED + e.getMessage());
        }
    }
}