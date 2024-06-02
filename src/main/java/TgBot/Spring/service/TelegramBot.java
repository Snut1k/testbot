package TgBot.Spring.service;

import TgBot.Spring.config.BotConfig;
import TgBot.Spring.model.User;
import TgBot.Spring.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.util.TimeStamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    final BotConfig config;

    static final String HELP_TEXT = "This bot is for calulating health parameters\n\n"+
            "You can add and change parameters using menu on the left or by teping command in the message box\n\n"+
            "/mydata command shows you your presonal data \n\n"+
            "Type /help to see this message again";
    public TelegramBot(BotConfig config){
        this.config = config; // вернуться
        List<BotCommand> menuList = new ArrayList(); // bot command класс из бибилотеги телеграмма
        menuList.add(new BotCommand("/start", "get welcome message"));
        menuList.add( new BotCommand("/mydata", "get a person data stored"));
        menuList.add( new BotCommand("/delete","delete person data"));
        menuList.add( new BotCommand("/help", "info about bot"));
        menuList.add(new BotCommand("/settings", "set a preferences"));
        try{
            this.execute(new SetMyCommands(menuList, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e){
            log.error("Error setting bot command list: "+ e.getMessage());
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName() ;
    }
    @Override
    public String getBotToken(){
        return config.getToken();
    }
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

             switch (messageText){
                 case "/start":

                     registerUser(update.getMessage());
                     startCommandRecived(chatId, update.getMessage().getChat().getFirstName());
                     break;

                 case "/help":
                     sendMessage(chatId, HELP_TEXT);
                     break;
                 case "water":
                     sendMessage(chatId,"is wet");
                     break;
                 case "register":
                     register(chatId);
                     break;

                 default: sendMessage(chatId, "Command does not exits");

             }
        }
        else if(update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if(callbackData.equals("YES_BUTTON")){
                String text = "You pressed Yes";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                message.setMessageId((int)messageId);

                try {
                    execute(message);
                }
                catch (TelegramApiException e){
                    log.error("Error occurred "+ e.getMessage());
                }
            }
            else if(callbackData.equals("NO_BUTTON")){
                String text = "You pressed No";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                message.setMessageId((int)messageId);

                try {
                    execute(message);
                }
                catch (TelegramApiException e){
                    log.error("Error occurred "+ e.getMessage());
                }
            }
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you want to register?");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>(); // список списков для хранения кнопок
        List<InlineKeyboardButton> rowInLine = new ArrayList<>() ; // список с кнопками для ряда
        var yesButton= new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("YES_BUTTON");

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON");// !!! лучше иcпользовать константы

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInline.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInline);

        try { // !!! провести рефакторинг
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Error occurred "+ e.getMessage());
        }

    }

    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()){

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            // long currentTimeMillis = System.currentTimeMillis();
            LocalDateTime localDateTime = LocalDateTime.now();
            System.out.println("Current Timestamp: " + localDateTime + " milliseconds");
            TimeStamp stamp = new TimeStamp();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(localDateTime);

            userRepository.save(user);
            log.info("user saved "+user);
        }
    }

    private void startCommandRecived(Long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, "+  name +", nice to meet you!"+" :blush:");
        //String answer = "Hi, "+  name +", nice to meet you!";
        sendMessage(chatId, answer);
        log.info("replied to user: "+ name);
    }
    private void sendMessage(long chatId, String textTpSend) {
        SendMessage message=new SendMessage();
        message.setChatId(String.valueOf(chatId) );
        message.setText(textTpSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("weather");
        row.add("water");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("register");
        row.add("show my data");
        row.add("delete my data");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Error occurred "+ e.getMessage());
        }

    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }



    //WebHook -уведомляется по  сообщению  TelegramLongPollingBot - сам периобчиески проверяет есть ли сообщения

}
