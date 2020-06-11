package fr.openent.mediacentre.export;

import fr.openent.mediacentre.Mediacentre;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExportTask implements Handler<Long> {
    private final EventBus eb;
    private final Logger log = LoggerFactory.getLogger(ExportTask.class);

    public ExportTask(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void handle(Long event) {
        log.info("export launched");
        eb.send(Mediacentre.MEDIACENTRE_ADDRESS,
                new JsonObject().put("action", "export"),
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if ("ok".equals(event.body().getString("status"))) {
                    log.info("export succeeded");
                }
            }
        }));
    }
}
