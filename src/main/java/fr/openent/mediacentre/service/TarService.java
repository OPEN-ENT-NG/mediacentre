package fr.openent.mediacentre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.io.File;

public interface TarService {
    void compress(String name, File directory, Handler<Either<String, JsonObject>> handler);
}
