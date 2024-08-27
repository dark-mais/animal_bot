package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.processor.UpdateProcessor;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final TelegramBot telegramBot;
    private final UpdateProcessor updateProcessor;

    @Autowired
    public TelegramBotUpdatesListener(TelegramBot telegramBot, UpdateProcessor updateProcessor) {
        this.telegramBot = telegramBot;
        this.updateProcessor = updateProcessor;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
        logger.info("TelegramBotUpdatesListener initialized and ready to receive updates.");
    }

    @Override
    public int process(List<Update> updates) {
        logger.info("Received {} updates.", updates.size());
        updateProcessor.process(updates);
        logger.info("Finished processing updates.");
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}