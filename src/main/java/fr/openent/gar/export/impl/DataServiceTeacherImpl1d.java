package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.helper.impl.PaginatorHelperImpl;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static fr.openent.mediacentre.constants.GarConstants.*;

public class DataServiceTeacherImpl1d extends DataServiceBaseImpl implements DataService {
    private PaginatorHelperImpl paginator;
    private String entId;
    private String source;

    DataServiceTeacherImpl1d(String entId, String source, JsonObject config, String strDate) {
        this.entId = entId;
        this.source = source;
        xmlExportHelper = new XmlExportHelperImpl(entId, source, config, TEACHER_ROOT, TEACHER_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
    }

    /**
     * Export Data to folder
     * - Export Teachers identities
     * - Export Teachers Mefs
     *
     * @param handler response handler
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        //fixme : no modules IN 1D, perhaps level in futur like student
        final JsonArray modules = new fr.wseduc.webutils.collections.JsonArray();
        getAndProcessTeachersInfoFromNeo4j(0, modules, resultTeachers -> {
            if (validResponse(resultTeachers, handler)) {
                if (validResponse(processTeachersMefs(modules), handler)) {
                    xmlExportHelper.closeFile();
                    handler.handle(new Either.Right<String, JsonObject>(
                            new JsonObject().put(
                                    FILE_LIST_KEY,
                                    xmlExportHelper.getFileList()
                            )));
                } else {
                    log.error("[DataServiceTeacherImpl@exportData] Failed to process");
                }
            } else {
                log.error("[DataServiceTeacherImpl@exportData] Failed to process");
            }
        });
    }

    private void getAndProcessTeachersInfoFromNeo4j(int skip, JsonArray modules,
                                                    final Handler<Either<String, JsonObject>> handler) {
        getTeachersInfoFromNeo4j(skip, teacherInfos -> {
            if (validResponseNeo4j(teacherInfos, handler)) {
                final JsonArray teachers = teacherInfos.right().getValue();
                populateModules(modules, teachers);
                Either<String, JsonObject> result = processTeachersInfo(teachers);

                if (teacherInfos.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessTeachersInfoFromNeo4j(skip + PaginatorHelperImpl.LIMIT, modules, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceTeacherImpl@getAndProcessTeachersInfoFromNeo4j] Failed to process");
            }
        });
    }

    private void populateModules(JsonArray modules, final JsonArray teachers) {
        if (!teachers.isEmpty()) {
            teachers.forEach(teacher -> {
                if (teacher instanceof JsonObject) {
                    final JsonObject fields = (JsonObject) teacher;
                    final JsonArray userModules = fields.getJsonArray("modules");
                    if (userModules != null && !userModules.isEmpty()) {
                        userModules.forEach(module -> {
                            if (module instanceof String) {
                                final String[] mods = (StringUtils.trimToBlank((String) module)).split("\\$");
                                //export not empty Mef only for Gar Structure
                                if (mods.length > 1 && StringUtils.trimToNull(mapStructures.get(mods[0])) != null &&
                                        StringUtils.trimToNull(mods[1]) != null) {
                                    final JsonObject jo = new JsonObject();
                                    jo.put(STRUCTURE_UAI, mapStructures.get(mods[0]));
                                    jo.put(PERSON_ID, fields.getString(PERSON_ID));
                                    jo.put(MEF_CODE_1D, mods[1]);
                                    modules.add(jo);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * Get teachers infos from Neo4j
     * Set fields as requested by xsd, except for structures
     *
     * @param handler results
     */
    private void getTeachersInfoFromNeo4j(int skip, Handler<Either<String, JsonArray>> handler) {
        String query = "match (u:User)-[:IN]->(pg:ProfileGroup {filter:'Teacher'})-[:DEPENDS]->(s:Structure {source:'" + this.source + "'}) " +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) " +
                // ADMINISTRATIVE ATTACHMENT can reference non GAR exported structure
                "OPTIONAL MATCH (u:User)-[:ADMINISTRATIVE_ATTACHMENT]->(sr:Structure) ";
        String dataReturn = "return distinct u.id  as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_PATRO_NAME + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "coalesce(u.otherNames, [u.firstName]) as `" + PERSON_OTHER_NAMES + "`, " +
                //TODO GARPersonCivilite
                "collect(distinct sr.UAI)[0] as `" + PERSON_STRUCT_ATTACH + "`, " +
                "u.birthDate as `" + PERSON_BIRTH_DATE + "`, " +
                "u.functions as functions, u.modules as modules, " +
                "collect(distinct s.UAI) as profiles " +
                "order by " + "`" + PERSON_ID + "`";

        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);
        paginator.neoStream(query, params, skip, handler);
    }

    /**
     * Process teachers info
     * Add structures in arrays to match xsd
     *
     * @param teachers Array of teachers from Neo4j
     */
    private Either<String, JsonObject> processTeachersInfo(JsonArray teachers) {
        try {
            for (Object o : teachers) {
                if (!(o instanceof JsonObject)) continue;

                JsonObject teacher = (JsonObject) o;
                JsonArray profiles = teacher.getJsonArray("profiles", null);
                if (profiles == null || profiles.size() == 0) {
                    log.error("Mediacentre : Teacher with no profile or function for export, id "
                            + teacher.getString("u.id", "unknown"));
                    continue;
                }

                Map<String, String> userStructProfiles = new HashMap<>();

                processFunctions(teacher, userStructProfiles);
                processProfiles(teacher, TEACHER_PROFILE, userStructProfiles);

                if (isMandatoryFieldsAbsent(teacher, TEACHER_NODE_MANDATORY)) {
                    log.warn("Mediacentre : mandatory attribut for Teacher : " + teacher);
                    continue;
                }

                reorganizeNodes(teacher);

                xmlExportHelper.saveObject(TEACHER_NODE, teacher);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing teachers Info : " + e.getMessage());
        }
    }

    /**
     * XSD specify precise order for xml tags
     *
     * @param teacher
     */
    private void reorganizeNodes(JsonObject teacher) {
        JsonObject personCopy = teacher.copy();
        teacher.clear();
        teacher.put(PERSON_ID, personCopy.getValue(PERSON_ID));
        teacher.put(PERSON_PROFILES, personCopy.getValue(PERSON_PROFILES));
        teacher.put(PERSON_PATRO_NAME, personCopy.getValue(PERSON_PATRO_NAME));
        teacher.put(PERSON_NAME, personCopy.getValue(PERSON_NAME));
        teacher.put(PERSON_FIRST_NAME, personCopy.getValue(PERSON_FIRST_NAME));
        teacher.put(PERSON_OTHER_NAMES, personCopy.getValue(PERSON_OTHER_NAMES));
        //TODO GARPersonCivilite
        teacher.put(PERSON_STRUCT_ATTACH, personCopy.getValue(PERSON_STRUCT_ATTACH));
        teacher.put(PERSON_STRUCTURE, personCopy.getValue(PERSON_STRUCTURE));
        if (personCopy.getValue(PERSON_BIRTH_DATE) != null && !"".equals(personCopy.getValue(PERSON_BIRTH_DATE))) {
            teacher.put(PERSON_BIRTH_DATE, personCopy.getValue(PERSON_BIRTH_DATE));
        }
        teacher.put(TEACHER_POSITION_1D, personCopy.getValue(TEACHER_POSITION_1D));
    }

    /**
     * Process teachers functions
     * Calc profile for Documentalist functions
     * Teacher function is in form structID$functionCode$functionDesc$roleCode and must be splited
     * and analyzed
     * Documentalists have specific role and profile
     *
     * @param teacher   to process functions for
     * @param structMap map between structures ID and profile
     */
    private void processFunctions(JsonObject teacher, Map<String, String> structMap) {
        JsonArray functions = teacher.getJsonArray("functions", null);
        if (functions == null || functions.size() == 0) {
            return;
        }

        JsonArray garFunctions = new fr.wseduc.webutils.collections.JsonArray();
        for (Object o : functions) {
            if (!(o instanceof String)) continue;
            String[] arrFunction = ((String) o).split("\\$");
            if (arrFunction.length < 4) continue;
            String structID = arrFunction[0];
            if (!mapStructures.containsKey(structID)) {
                continue;
            }
            String structUAI = mapStructures.get(structID);
            String functionCode = arrFunction[1];
            String functionDesc = arrFunction[2];
            String roleCode = arrFunction[3];
            String profileType = TEACHER_PROFILE;
            if (DOCUMENTALIST_CODE.equals(functionCode) && DOCUMENTALIST_DESC.equals(functionDesc)) {
                profileType = DOCUMENTALIST_PROFILE;
            }
            structMap.put(structUAI, profileType);

            JsonObject function = new JsonObject();
            function.put(STRUCTURE_UAI, structUAI);
            function.put(POSITION_CODE_1D, roleCode);
            garFunctions.add(function);
        }
        teacher.put(TEACHER_POSITION_1D, garFunctions);
        teacher.remove("functions");
    }

    /**
     * Process mefs info
     *
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String, JsonObject> processTeachersMefs(JsonArray mefs) {
        Either<String, JsonObject> event = processSimpleArray(mefs, PERSON_MEF_1D, PERSON_MEF_NODE_MANDATORY_1D);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing teacher mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }
}
