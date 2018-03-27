package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.helper.XmlExportHelper;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.service.DataService;
import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import org.w3c.dom.Document;
import static fr.openent.mediacentre.constants.GarConstants.*;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceStudentImpl implements DataService{

    private Document doc = null;
    private XmlExportHelper xmlExportHelper;
    private final Logger log;
    private final String CONTROL_GROUP;

    private Neo4j neo4j = Neo4j.getInstance();

    DataServiceStudentImpl(Container container) {
        super();
        xmlExportHelper = new XmlExportHelperImpl(container, STUDENT_ROOT, STUDENT_FILE_PARAM);
        this.log = container.logger();
        this.CONTROL_GROUP = container.config().getString("control-group", "GAR");
    }

    /**
     * Export Data to folder
     * - Export Students identities
     * - Export Students Mefs
     * - Export Students teaching modules
     * @param path folder path
     * @param handler response handler
     */
    @Override
    public void exportData(String path, final Handler<Either<String, JsonObject>> handler) {

        getStudentsInfoFromNeo4j(
                new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> studentsResult) {
                if(studentsResult.isLeft()) {
                    handler.handle(new Either.Left<String, JsonObject>(studentsResult.left().getValue()));
                } else {

                    processStudentsInfo(studentsResult.right().getValue());
                    getStudentsMefFromNeo4j(
                            new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> mefsResult) {
                            if(mefsResult.isLeft()) {
                                handler.handle(new Either.Left<String, JsonObject>(mefsResult.left().getValue()));
                            } else {

                                processStudentsMefs(mefsResult.right().getValue());
                                getStudentsStudyfieldsFromNeo4j(
                                        new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> modulesResult) {
                                                if(modulesResult.isLeft()) {
                                                    handler.handle(new Either.Left<String, JsonObject>(modulesResult.left().getValue()));
                                                } else {

                                                    processStudentsStudyfields(modulesResult.right().getValue());
                                                    xmlExportHelper.closeFile();
                                                    handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
                                                }
                                            }
                                        }
                                );
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Get students infos from Neo4j
     * Set fields as requested by xsd, except for structures
     * @param handler results
     */
    private void getStudentsInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "match (u:User)-[IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}), " +
                "(p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup) " +
                "where p.name = 'Student' " +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(sr:Structure) ";
        String dataReturn = "return distinct u.id  as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_PATRO_NAME + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.otherNames as `" + PERSON_OTHER_NAMES + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "sr.UAI as `" + PERSON_STRUCT_ATTACH + "`, " +
                "u.birthDate as `" + PERSON_BIRTH_DATE + "`, " +
                "collect(distinct s.UAI) as profiles " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process students info
     * Add structures in arrays to match xsd
     * @param students Array of students from Neo4j
     */
    private void processStudentsInfo(JsonArray students) {
        for(Object o : students) {
            if(!(o instanceof JsonObject)) continue;

            JsonObject student = (JsonObject) o;
            JsonArray profiles = student.getArray("profiles", null);
            if(profiles == null || profiles.size() == 0) {
                log.error("Mediacentre : Student with no profile for export, id "
                        + student.getString("u.id", "unknown"));
                continue;
            }

            JsonArray garProfiles = new JsonArray();
            JsonArray garEtabs = new JsonArray();
            for(Object o2 : profiles) {
                if(!(o2 instanceof String)) continue;
                String structure = (String)o2;

                garEtabs.addString(structure);

                JsonObject garProfile = new JsonObject();
                garProfile.putString(STRUCTURE_UAI, structure);
                garProfile.putString(PERSON_PROFILE, STUDENT_PROFILE);
                garProfiles.addObject(garProfile);
            }
            student.putArray(PERSON_PROFILES, garProfiles);
            student.putArray(PERSON_STRUCTURE, garEtabs);
            student.removeField("profiles");
            xmlExportHelper.saveObject(STUDENT_NODE, student);
        }
    }

    /**
     * Get students mefs from Neo4j
     * @param handler results
     */
    private void getStudentsMefFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-" +
                "(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) ";
        String dataReturn = "where p.name = 'Student' " +
                "return distinct u.id as `" + PERSON_ID + "`, " +
                "u.module as `" + MEF_CODE + "`, " +
                "s.UAI as `" + STRUCTURE_UAI + "` " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }


    private void processStudentsMefs(JsonArray mefs) {
        for(Object o : mefs) {
            if (!(o instanceof JsonObject)) continue;
            xmlExportHelper.saveObject(PERSON_MEF, (JsonObject)o);
        }
    }

    /**
     * Get students modules from Neo4j
     * @param handler results
     */
    private void getStudentsStudyfieldsFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-(u:User)" +
                "-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "where p.name = 'Student' ";
        String dataReturn = "with u,s " +
                "unwind u.fieldOfStudy as fos " +
                "return distinct u.id as `" + PERSON_ID + "`, " +
                "s.UAI as `" + STRUCTURE_UAI + "`, " +
                "fos as `" + STUDYFIELD_CODE + "` " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }


    private void processStudentsStudyfields(JsonArray studyfields) {
        for(Object o : studyfields) {
            if (!(o instanceof JsonObject)) continue;
            xmlExportHelper.saveObject(STUDENT_STUDYFIELD, (JsonObject)o);
        }
    }
}
