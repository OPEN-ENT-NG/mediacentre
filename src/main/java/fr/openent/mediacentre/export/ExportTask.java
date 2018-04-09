package fr.openent.mediacentre.export;

import fr.openent.mediacentre.Mediacentre;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class ExportTask implements Handler<Long> {


    private final EventBus eb;
    private final Logger log;

    public ExportTask(EventBus eb, Logger log) {
        this.eb = eb;
        this.log = log;
    }

    @Override
    public void handle(Long event) {
        log.info("export launched");
        eb.send(Mediacentre.MEDIACENTRE_ADDRESS,
                new JsonObject().putString("action", "export"),
                new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if ("ok".equals(event.body().getString("status"))) {
                    log.info("export succeeded");
                }
            }
        });
    }
}
