package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.helper.impl.PaginatorHelperImpl;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

import static fr.openent.mediacentre.constants.GarConstants.*;

public class DataServiceStudentImpl1d extends DataServiceBaseImpl implements DataService {
    private PaginatorHelperImpl paginator;
    private String entId;
    private String source;

    DataServiceStudentImpl1d(String entId, String source, JsonObject config, String strDate) {
        this.entId = entId;
        this.source = source;
        xmlExportHelper = new XmlExportHelperImpl(entId, source, config, STUDENT_ROOT, STUDENT_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
    }

    /**
     * Export Data to folder
     * - Export Students identities
     * - Export Students Mefs
     *
     * @param handler response handler
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        final JsonArray mefs = new fr.wseduc.webutils.collections.JsonArray();
        getAndProcessStudentsInfo(0, mefs, studentsResult -> {
            if (validResponse(studentsResult, handler)) {
                if (validResponse(processStudentsMefs(mefs), handler)) {
                    xmlExportHelper.closeFile();
                    handler.handle(new Either.Right<String, JsonObject>(
                            new JsonObject().put(
                                    FILE_LIST_KEY,
                                    xmlExportHelper.getFileList()
                            )));
                }
            }
        });
    }

    /**
     * Process students info, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessStudentsInfo(int skip, JsonArray mefs, final Handler<Either<String, JsonObject>> handler) {
        getStudentsInfoFromNeo4j(skip, studentResults -> {
            if (validResponseNeo4j(studentResults, handler)) {
                final JsonArray students = studentResults.right().getValue();
                populateMef(mefs, students);
                Either<String, JsonObject> result = processStudentsInfo(students);
                if (studentResults.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessStudentsInfo(skip + PaginatorHelperImpl.LIMIT, mefs, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceStudentImpl@getAndProcessStudentsInfo] Failed to process");
            }
        });
    }

    private void populateMef(JsonArray mefs, final JsonArray students) {
        if (!students.isEmpty()) {
            students.forEach(student -> {
                if (student instanceof JsonObject) {
                    final JsonObject fields = (JsonObject) student;
                    populateMef(fields, mefs);
                }
            });
        }
    }

    private void populateMef(final JsonObject fields, JsonArray mefs) {
        final String userLevel = StringUtils.trimToNull(fields.getString("level"));
        final JsonArray uais = fields.getJsonArray("profiles");
        //export not empty Mef
        if (userLevel != null) {
            final String[] elems = userLevel.split("\\$");
            if (elems.length > 1) {
                final String mefCode = StringUtils.trimToNull(elems[0]);
                if (mefCode != null && uais != null && !uais.isEmpty()) {
                    uais.forEach(uai -> {
                        if (uai instanceof String) {
                            final String strUai = (String) uai;
                            if (StringUtils.trimToNull(strUai) != null) {
                                final JsonObject jo = new JsonObject();
                                jo.put(STRUCTURE_UAI, strUai);
                                jo.put(PERSON_ID, fields.getString(PERSON_ID));
                                jo.put(MEF_CODE_1D, mefCode);
                                mefs.add(jo);
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Get students infos from Neo4j
     * Set fields as requested by xsd, except for structures
     *
     * @param handler results
     */
    private void getStudentsInfoFromNeo4j(int skip, Handler<Either<String, JsonArray>> handler) {
        String query = "match (u:User)-[:IN]->(pg:ProfileGroup {filter:'Student'})-[:DEPENDS]->(s:Structure {source:'" + this.source + "'}) " +
                "where HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) " +
                "OPTIONAL MATCH (u:User)-[:ADMINISTRATIVE_ATTACHMENT]->(sr:Structure {source:'" + this.source + "'}) " +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) ";
        String dataReturn = "return distinct " +
                "u.id  as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_PATRO_NAME + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "coalesce(u.otherNames, [u.firstName]) as `" + PERSON_OTHER_NAMES + "`, " +
                //TODO GARPersonCivilite
                "sr.UAI as `" + PERSON_STRUCT_ATTACH + "`, " +
                "u.birthDate as `" + PERSON_BIRTH_DATE + "`, " +
                "collect(distinct s.UAI) as profiles, u.level as level " +
                "order by " + "`" + PERSON_ID + "`";


        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);
        paginator.neoStream(query, params, skip, handler);
    }

    /**
     * Process students info
     * Add structures in arrays to match xsd
     *
     * @param students Array of students from Neo4j
     */
    private Either<String, JsonObject> processStudentsInfo(JsonArray students) {
        try {
            for (Object o : students) {
                if (!(o instanceof JsonObject)) continue;

                JsonObject student = (JsonObject) o;
                JsonArray profiles = student.getJsonArray("profiles", null);
                if (profiles == null || profiles.size() == 0) {
                    log.warn("Mediacentre : Student with no profile for export, id "
                            + student.getString("u.id", "unknown"));
                    continue;
                }

                processProfiles(student, STUDENT_PROFILE, null);

                if (isMandatoryFieldsAbsent(student, STUDENT_NODE_MANDATORY)) {
                    log.warn("Mediacentre : mandatory attribut for Student : " + student);
                    continue;
                }

                reorganizeNodes(student);

                xmlExportHelper.saveObject(STUDENT_NODE, student);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing students Info : " + e.getMessage());
        }
    }

    /**
     * XSD specify precise order for xml tags
     *
     * @param student
     */
    private void reorganizeNodes(JsonObject student) {
        JsonObject personCopy = student.copy();
        student.clear();
        student.put(PERSON_ID, personCopy.getValue(PERSON_ID));
        student.put(PERSON_PROFILES, personCopy.getValue(PERSON_PROFILES));
        student.put(PERSON_PATRO_NAME, personCopy.getValue(PERSON_PATRO_NAME));
        student.put(PERSON_NAME, personCopy.getValue(PERSON_NAME));
        student.put(PERSON_FIRST_NAME, personCopy.getValue(PERSON_FIRST_NAME));
        student.put(PERSON_OTHER_NAMES, personCopy.getValue(PERSON_OTHER_NAMES));
        //TODO GARPersonCivilite
        student.put(PERSON_STRUCT_ATTACH, personCopy.getValue(PERSON_STRUCT_ATTACH));
        student.put(PERSON_STRUCTURE, personCopy.getValue(PERSON_STRUCTURE));
        student.put(PERSON_BIRTH_DATE, personCopy.getValue(PERSON_BIRTH_DATE));
    }

    /**
     * Process mefs info
     *
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String, JsonObject> processStudentsMefs(JsonArray mefs) {
        Either<String, JsonObject> event = processSimpleArray(mefs, PERSON_MEF_1D, PERSON_MEF_NODE_MANDATORY_1D);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing students mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }
}
