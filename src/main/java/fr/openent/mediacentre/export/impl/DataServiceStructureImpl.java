package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.DataService;
import fr.openent.mediacentre.helper.impl.PaginatorHelperImpl;
import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static fr.openent.mediacentre.constants.GarConstants.*;

public class DataServiceStructureImpl extends DataServiceBaseImpl implements DataService {

    private PaginatorHelperImpl paginator;
    private JsonObject config;
    private String entId;
    private Boolean hasAcademyPrefix;

    DataServiceStructureImpl(String entId, JsonObject config, String strDate) {
        this.entId = entId;
        this.config = config;
        xmlExportHelper = new XmlExportHelperImpl(entId, config, STRUCTURE_ROOT, STRUCTURE_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
        hasAcademyPrefix = this.config.containsKey("academy-prefix") && !"".equals(this.config.getString("academy-prefix").trim());
    }

    /**
     * Export Data to folder
     * - Export Structures info and build mapStructures with mapping between structures ID and UAI
     * - Export Structures Mefs
     * - Export Structures fields of study
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {

        getAndProcessStructuresInfo(structInfoResults -> {
            if (validResponse(structInfoResults, handler)) {

                getAndProcessStructuresMefs(structMefsResults -> {
                    if (validResponse(structMefsResults, handler)) {

                        getAndProcessStructuresFos(structFosResults -> {
                            if (validResponse(structFosResults, handler)) {

                                xmlExportHelper.closeFile();
                                handler.handle(new Either.Right<String, JsonObject>(
                                        new JsonObject().put(
                                                FILE_LIST_KEY,
                                                xmlExportHelper.getFileList()
                                        )));

                            }
                        });

                    }
                });
            }
        });
    }

    /**
     * Process structure info, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessStructuresInfo(final Handler<Either<String, JsonObject>> handler) {

        getStucturesInfoFromNeo4j(structResults -> {
            if (validResponseNeo4j(structResults, handler)) {
                Either<String, JsonObject> result = processStructuresInfo(structResults.right().getValue());
                handler.handle(result);
            } else {
                log.error("[DataServiceStructureImpl@getAndProcessStructuresInfo] Failed to process");
            }
        });
    }

    /**
     * Process structure mefs, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessStructuresMefs(final Handler<Either<String, JsonObject>> handler) {

        getStucturesMefsFromNeo4j(structResults -> {
            if (validResponseNeo4j(structResults, handler)) {
                Either<String, JsonObject> result = processStucturesMefs(structResults.right().getValue());
                handler.handle(result);
            } else {
                log.error("[DataServiceStructureImpl@getAndProcessStructuresMefs] Failed to process");
            }
        });
    }

    /**
     * Process structure fields of study, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessStructuresFos(final Handler<Either<String, JsonObject>> handler) {

        getStucturesFosFromNeo4j(structResults -> {
            if (validResponseNeo4j(structResults, handler)) {
                getFosLabelFromNeo4j(structResults.right().getValue(), structResultsWithLabel -> {
                    Either<String, JsonObject> result = processStucturesFos(structResultsWithLabel.right().getValue());
                    handler.handle(result);
                });

            } else {
                log.error("[DataServiceStructureImple@getAndProcessStructureFos] Failed to process");
            }
        });
    }

    private void getFosLabelFromNeo4j(JsonArray fosList, Handler<Either<String, JsonArray>> handler) {
        String query2 = "MATCH (fos:FieldOfStudy) " +
                "RETURN fos.externalId as id, fos.name as name ORDER BY fos.externalId";
        neo4j.execute(query2, new JsonObject(), res2 -> {
            if (res2.body() != null && res2.body().containsKey("result")) {
                JsonArray fieldOfStudyResult = res2.body().getJsonArray("result");
                Map<String, String> fieldOfStudyLabels = new HashMap<>();
                fieldOfStudyResult.forEach((entry2) -> {

                    if (entry2 instanceof JsonObject) {
                        JsonObject field = (JsonObject) entry2;
                        String id = field.getString("id", "");
                        String name = field.getString("name", "");

                        if (!id.isEmpty() && !name.isEmpty()) {
                            if (hasAcademyPrefix) {
                                id = id.replaceFirst("(" + this.config.getString("academy-prefix") + ")-", "");
                            }
                            fieldOfStudyLabels.put(id, name);
                        }
                    }
                });

                String query = "MATCH (s:Structure)<-[:SUBJECT]-(sub:Subject) " +
                        "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                        "RETURN s.UAI as UAI, sub.code as code, sub.label as label";
                neo4j.execute(query, new JsonObject().put("entId", entId), res -> {
                    if (res.body() != null && res.body().containsKey("result")) {
                        JsonArray queryResult = res.body().getJsonArray("result");

                        Map<String, Map<String, String>> labelsByCodeUai = new HashMap<>();
                        queryResult.forEach((entry) -> {
                            if (entry instanceof JsonObject) {
                                JsonObject field = (JsonObject) entry;
                                String uai = field.getString("UAI", "");
                                String code = field.getString("code", "");
                                String label = field.getString("label", "");
                                if (!uai.isEmpty() && !code.isEmpty() && !label.isEmpty()) {
                                    if (!labelsByCodeUai.containsKey(uai)) {
                                        labelsByCodeUai.put(uai, new HashMap<>());
                                    }
                                    if (hasAcademyPrefix) {
                                        code = code.replaceFirst("(" + this.config.getString("academy-prefix") + ")-", "");
                                    }
                                    labelsByCodeUai.get(uai).put(code, label);
                                }
                            }
                        });

                        for (int i = 0; i < fosList.size(); i++) {
                            JsonObject entry = fosList.getJsonObject(i);
                            if (entry.containsKey(STRUCTURE_UAI) && entry.containsKey(STUDYFIELD_CODE)) {
                                String UIA = entry.getString(STRUCTURE_UAI);
                                String fosCode = entry.getString(STUDYFIELD_CODE);
                                if (labelsByCodeUai.containsKey(UIA) && labelsByCodeUai.get(UIA).containsKey(fosCode)) {
                                    entry.put(STUDYFIELD_DESC, labelsByCodeUai.get(UIA).get(fosCode));
                                } else {
                                    if (fieldOfStudyLabels.containsKey(fosCode)) {
                                        entry.put(STUDYFIELD_DESC, fieldOfStudyLabels.get(fosCode));
                                    } else {
                                        entry.put(STUDYFIELD_DESC, "MATIERE " + fosCode);
                                    }
                                }
                            }
                        }
                    }
                    handler.handle(new Either.Right<>(fosList));
                });

            }
        });
    }

    /**
     * Get structures infos from Neo4j
     *
     * @param handler results
     */
    private void getStucturesInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) " +
                "WHERE HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports ";
// Don't export optional attachment structure attribute
//                "OPTIONAL MATCH (g2:ManualGroup{name:\\\"\" + CONTROL_GROUP + \"\\\"})-[:DEPENDS]->(s2:Structure)<-[:HAS_ATTACHMENT]-(s:Structure) ";
        String dataReturn = "RETURN distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "s.name as `" + STRUCTURE_NAME + "`, " +
//                "collect(distinct s2.UAI)[0]  as `" + STRUCTURE_RATTACH + "`, " +
                "s.contract  as `" + STRUCTURE_CONTRACT + "`, " +
                "s.phone  as `" + STRUCTURE_PHONE + "`, " +
                //TODO GARStructureTelephone
                "s.externalId  as structid " +
                "order by " + "`" + STRUCTURE_UAI + "`";

        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process structures info
     * Update general map with mapping between structures ID and UAI
     *
     * @param structures Array of structures from Neo4j
     */
    private Either<String, JsonObject> processStructuresInfo(JsonArray structures) {
        try {
            //clean mapStructures before process structures.
            mapStructures.clear();
            for (Object o : structures) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject structure = (JsonObject) o;

                if (isMandatoryFieldsAbsent(structure, STRUCTURE_NODE_MANDATORY)) continue;

                updateMap(structure);
                xmlExportHelper.saveObject(STRUCTURE_NODE, structure);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing structures Info : " + e.getMessage());
        }
    }

    /**
     * Update mapStructures with ID and UAI of structure
     * Then remove ID from object
     *
     * @param structure object with structure info
     */
    private void updateMap(JsonObject structure) {
        String structId = structure.getString("structid");
        String structUAI = structure.getString(STRUCTURE_UAI);
        mapStructures.put(structId, structUAI);
        structure.remove("structid");
    }


    /**
     * Get structures mefs from Neo4j
     * For each structure :
     * Each student has one mef attached
     * Each teacher can have many mefs attached
     *
     * @param handler results
     */
    private void getStucturesMefsFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String queryStudentsMefs = "MATCH (n:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
                "WHERE exists(n.module) AND  NOT(has(n.deleteDate)) AND NOT(HAS(n.disappearanceDate))" +
                " AND HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "n.module as `" + MEF_CODE + "`, " +
                "n.moduleName as `" + MEF_DESCRIPTION + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + MEF_CODE + "` " +
                "UNION ";
        String queryTeachersMefs = "MATCH (n:User)-[:IN|DEPENDS*1..2]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "where exists(n.modules) and not has(n.deleteDate) " +
                "AND NOT(HAS(n.disappearanceDate)) AND HAS(s.exports) AND ('GAR-' + {entId}) IN s.exports " +
                "with s,n " +
                "unwind n.modules as rows " +
                "with s, split(rows,\"$\") as modules " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "modules[1] as `" + MEF_CODE + "`, " +
                "modules[2] as `" + MEF_DESCRIPTION + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + MEF_CODE + "` ";

        String query = queryStudentsMefs + queryTeachersMefs;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process structures mefs
     *
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String, JsonObject> processStucturesMefs(JsonArray mefs) {

        Either<String, JsonObject> event = processSimpleArray(mefs, MEF_NODE, MEF_NODE_MANDATORY);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing structures mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }

    /**
     * Get structures fields of study from Neo4j
     * - Structure FOS codes may be prefixed by ACADEMY-
     * - Students FOS codes and description are lists in two different fields and must be mapped
     *
     * @param handler results
     */
    private void getStucturesFosFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String condition;
        if (hasAcademyPrefix) {
            condition = "CASE WHEN sub.code =~ '(" + this.config.getString("academy-prefix") + ")-[A-Z0-9-]+' THEN reduce(v=sub.code, prefix in split('" +
                    this.config.getString("academy-prefix") +"', '|') | replace(v, prefix + '-', '')) ELSE sub.code END as codelist";
        } else {
            condition = "split(sub.code,\"-\") as codelist";
        }


        String queryStructureFos = "MATCH (sub:Subject)-[:SUBJECT]->(s:Structure)" +
                "WHERE HAS(s.exports) AND sub.code =~ '^(.*-)?([0-9]{2})([A-Z0-9]{4})$' AND ('GAR-' + {entId}) IN s.exports " +
                "with s, sub.label as label, " + condition +
                " return distinct s.UAI as `" + STRUCTURE_UAI + "`, toUpper(" +
                (hasAcademyPrefix ? "codelist" : "codelist[size(codelist)-1]") + ") as `" + STUDYFIELD_CODE + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + STUDYFIELD_CODE + "` " +
                "UNION ";
        String queryStudentFos = "MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "where exists (u.fieldOfStudy) AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) AND HAS(s.exports) " +
                "AND ('GAR-' + {entId}) IN s.exports " +
                "with s, u.fieldOfStudy as fos " +
                "with s, " +
                "reduce(x=[], idx in range(0,size(fos)-1) | x + {code:fos[idx]}) as rows " +
                "unwind rows as row " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "toUpper(row.code) as `" + STUDYFIELD_CODE + "` " +
                "order by `" + STRUCTURE_UAI + "` , `" + STUDYFIELD_CODE + "` ";


        String query = queryStructureFos + queryStudentFos;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT).put("entId", entId);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process structures fields of study
     *
     * @param fos Array of fis from Neo4j
     */
    private Either<String, JsonObject> processStucturesFos(JsonArray fos) {
        Either<String, JsonObject> event = processSimpleArray(fos, STUDYFIELD_NODE, STUDYFIELD_NODE_MANDATORY);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing structures fos : " + event.left().getValue());
        } else {
            return event;
        }
    }
}
