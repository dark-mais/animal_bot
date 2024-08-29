package pro.sky.telegrambot.processor;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.handler.CommandHandler;

@Component
public class UpdateProcessor {

    private final Logger logger = LoggerFactory.getLogger(UpdateProcessor.class);
    private final List<CommandHandler> commandHandlers;

    @Autowired
    public UpdateProcessor(List<CommandHandler> commandHandlers) {
        this.commandHandlers = commandHandlers;
    }

    public void process(List<Update> updates) {
        for (Update update : updates) {
            logger.info("Processing update: {}", update);

            if (update.callbackQuery() != null) {
                CallbackQuery callbackQuery = update.callbackQuery();
                String data = callbackQuery.data();

                logger.info("Received CallbackQuery with data: {}", data);

                if (data != null) {
                    commandHandlers.stream()
                                   .filter(h -> h.canHandle(data))
                                   .findFirst()
                                   .ifPresent(handler -> {
                                       logger.info("Found handler for callback data: {}", data);
                                       //if (handler instanceof SelectPetCommandHandler) {
                                           handler.handleCallbackQuery(callbackQuery);
                                       //}
                                   });
                }


            } else if (update.message() != null) {
                Message message = update.message();
                String text = message.text();
                logger.info("Received message: {}", text);

                Optional<CommandHandler> handler = commandHandlers.stream()
                        .filter(h -> h.canHandle(text))
                        .findFirst();

                handler.ifPresentOrElse(h -> h.handle(message),
                        () -> logger.warn("No handler found for command: {}", text));
            }
        }
    }
}