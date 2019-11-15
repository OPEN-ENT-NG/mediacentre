package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.export.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExportServiceImpl implements ExportService{

    private final JsonObject config;

    public ExportServiceImpl(JsonObject config) {
        this.config = config;
    }

    @Override
    public void launchExport(final String entId, final Handler<Either<String, JsonObject>> handler) {

        String strDate = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());

        final Queue<DataService> dataServiceQueue = new ConcurrentLinkedQueue<>();
        dataServiceQueue.add(new DataServiceStructureImpl(entId, config, strDate));
        dataServiceQueue.add(new DataServiceStudentImpl(entId, config, strDate));
        dataServiceQueue.add(new DataServiceTeacherImpl(entId, config, strDate));
        dataServiceQueue.add(new DataServiceGroupImpl(entId, config, strDate));
        dataServiceQueue.add(new DataServiceRespImpl(entId, config, strDate));

        JsonArray fileList = new fr.wseduc.webutils.collections.JsonArray();
        processExport(dataServiceQueue, fileList, handler);
    }

    private void processExport(final Queue<DataService> dataServiceQueue,
                               final JsonArray fileList,
                               final Handler<Either<String, JsonObject>> handler) {
        DataService service = dataServiceQueue.poll();
        if(service != null) {
            service.exportData(new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> exportResult) {
                    if(exportResult.isRight()) {

                        JsonArray fileListResult = exportResult.right().getValue().getJsonArray(DataService.FILE_LIST_KEY);
                        if(fileListResult != null) {
                            for (Object o : fileListResult) {
                                fileList.add(o);
                            }
                        }
                        processExport(dataServiceQueue, fileList, handler);
                    } else {
                        handler.handle(exportResult);
                    }
                }
            });
        } else {
            doReporting(fileList, handler);
        }
    }

    private void doReporting(final JsonArray fileList, final Handler<Either<String, JsonObject>> handler) {

        if(fileList.size() == 0) {
            handler.handle(new Either.Left<String, JsonObject>("No file created"));
        } else  {
            JsonObject msgContent = new JsonObject();
            msgContent.put("path", fileList);
            //TODO faire le zip
            msgContent.put("zipfile", "");
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
        }

    }
}
