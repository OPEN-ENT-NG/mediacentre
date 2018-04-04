package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.service.DataService;
import fr.openent.mediacentre.service.ExportService;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.text.SimpleDateFormat;
import java.util.Date;


public class ExportServiceImpl implements ExportService{

    private final Container container;



    public ExportServiceImpl(Container container) {
        this.container = container;
    }

    @Override
    public void test(final Handler<Either<String, JsonObject>> handler) {


        String strDate = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());
        final DataService studentService = new DataServiceStudentImpl(container, strDate);
        final DataService teacherService = new DataServiceTeacherImpl(container, strDate);
        final DataService structureService = new DataServiceStructureImpl(container, strDate);
        final DataService groupService = new DataServiceGroupImpl(container, strDate);

        structureService.exportData(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> resultStructure) {
                if(resultStructure.isRight()) {
                    studentService.exportData(new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> resultStudent) {
                            if(resultStudent.isRight()) {
                                teacherService.exportData(new Handler<Either<String, JsonObject>>() {
                                    @Override
                                    public void handle(Either<String, JsonObject> resultTeacher) {
                                        if(resultTeacher.isRight()) {
                                            groupService.exportData(new Handler<Either<String, JsonObject>>() {
                                                @Override
                                                public void handle(Either<String, JsonObject> resultGroups) {
                                                    handler.handle(resultGroups);
                                                }
                                            });
                                        }
                                    }
                                });
                            } else {
                                handler.handle(resultStudent);
                            }
                        }
                    });
                } else {
                    handler.handle(resultStructure);
                }
            }
        });
    }
}
