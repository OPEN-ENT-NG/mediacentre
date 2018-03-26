package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.service.ExportService;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public class ExportServiceImpl implements ExportService{

    @Override
    public void test(Handler<Either<String, JsonObject>> handler) {
        handler.handle(new Either.Left<String, JsonObject>("KO"));
    }
}
