package fr.openent.mediacentre.export.impl;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {

    private ExportImpl export = null;
    private Calendar lastExportTime = Calendar.getInstance();


    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        final String action = message.body().getString("action", "");
        switch (action) {
            case "exportAndSend":
                export(message.body().getString("entId"));
                break;
        }
    }

    private void export(final String entId) {
        Calendar now = Calendar.getInstance();
        if(this.export == null || ( (now.getTimeInMillis() - lastExportTime.getTimeInMillis()) / 1000 / 3600) > 1 )  {
            this.lastExportTime = now;
            if (entId != null) {
                this.export = new ExportImpl(vertx, entId, new Handler<String>() {
                    @Override
                    public void handle(String s) {
                        export = null;
                    }
                });
            } else {
                export(0);
            }
        }
    }

    private void export(final int index) {
        //export for each entId
        final List<Object> ids = new ArrayList<>(config.getJsonObject("id-ent", new JsonObject()).getMap().values());
        if (index < ids.size()) {
            this.export = new ExportImpl(vertx, (String)ids.get(index), new Handler<String>() {
                @Override
                public void handle(String s) {
                    export(index+1);
                }
            });
        } else {
            export = null;
        }
    }
}
