package fr.openent.gar.export.impl;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.vertx.java.busmods.BusModBase;

import java.util.Calendar;

public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {

    ExportImpl export = null;
    private Calendar lastExportTime = Calendar.getInstance();

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        final String action = message.body().getString("action", "");
        if ("exportAndSend".equals(action)) {
            export();
        }
    }

    private void export() {
        Calendar now = Calendar.getInstance();
        if(this.export == null || ( (now.getTimeInMillis() - lastExportTime.getTimeInMillis()) / 1000 / 3600) > 1 )  {
            this.lastExportTime = now;
            this.export = new ExportImpl(vertx,
                    s -> export = null
            );
        }
    }


}
