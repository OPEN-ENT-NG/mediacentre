package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.export.ExportService;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ExportServiceImpl implements ExportService{

    private final Container container;



    public ExportServiceImpl(Container container) {
        this.container = container;
    }




    @Override
    public void launchExport(final Message<JsonObject> message) {
        launchExport(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> exportResult) {
                JsonObject json;
                if(exportResult.isLeft()) {
                    json = (new JsonObject())
                            .putString("status", "error")
                            .putString("message", exportResult.left().getValue());
                } else {
                    json = exportResult.right().getValue();
                }
                message.reply(json.putString("status", "ok"));
            }
        });
    }

    @Override
    public void launchExport(final Handler<Either<String, JsonObject>> handler) {


        String strDate = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());

        final Queue<DataService> dataServiceQueue = new ConcurrentLinkedQueue<>();
        dataServiceQueue.add(new DataServiceStructureImpl(container, strDate));
        dataServiceQueue.add(new DataServiceStudentImpl(container, strDate));
        dataServiceQueue.add(new DataServiceTeacherImpl(container, strDate));
        dataServiceQueue.add(new DataServiceGroupImpl(container, strDate));
        dataServiceQueue.add(new DataServiceRespImpl(container, strDate));

        processExport(dataServiceQueue, handler);
    }

    private void processExport(final Queue<DataService> dataServiceQueue,
                               final Handler<Either<String, JsonObject>> handler) {
        if(!dataServiceQueue.isEmpty()) {
            DataService service = dataServiceQueue.poll();
            service.exportData(new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> exportResult) {
                    if(exportResult.isRight()) {
                        processExport(dataServiceQueue, handler);
                    } else {
                        handler.handle(exportResult);
                    }
                }
            });
        } else {
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
        }
    }
}
