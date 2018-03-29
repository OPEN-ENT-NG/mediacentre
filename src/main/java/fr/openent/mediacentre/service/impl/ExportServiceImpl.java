package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.service.DataService;
import fr.openent.mediacentre.service.ExportService;
import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.HashMap;
import java.util.Map;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;


public class ExportServiceImpl implements ExportService{

    private final Container container;



    public ExportServiceImpl(Container container) {
        this.container = container;
    }

    @Override
    public void test(final Handler<Either<String, JsonObject>> handler) {

        DataService studentService = new DataServiceStudentImpl(container);
        final DataService teacherService = new DataServiceTeacherImpl(container);
        studentService.exportData(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> result) {
                if(result.isRight()) {
                    teacherService.exportData(new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                            handler.handle(stringJsonObjectEither);
                        }
                    });
                } else {
                    handler.handle(result);
                }
            }
        });
    }
}
