package fr.openent.mediacentre.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public interface ExportService {

    void test(Handler<Either<String,JsonObject>> handler);
}
