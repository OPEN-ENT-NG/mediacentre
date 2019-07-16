package fr.openent.mediacentre;

import fr.openent.mediacentre.controller.MediacentreController;
import fr.openent.mediacentre.controller.SettingController;
import fr.openent.mediacentre.export.ExportTask;
import fr.openent.mediacentre.export.impl.ExportWorker;
import fr.wseduc.cron.CronTrigger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

import java.text.ParseException;

public class Mediacentre extends BaseServer {

	public static final String MEDIACENTRE_ADDRESS = "openent.mediacentre";
	public static boolean demo;
	public static JsonObject CONFIG;

	@Override
	public void start() throws Exception {
		super.start();
		final EventBus eb = getEventBus(vertx);

		addController(new MediacentreController(vertx, config));
		addController(new SettingController(eb));


		final String exportCron = config.getString("export-cron", "");
		demo = config.getBoolean("demo", false);
		CONFIG = config;

		vertx.deployVerticle(ExportWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));

		try{
			new CronTrigger(vertx, exportCron).schedule(new ExportTask(vertx.eventBus()));
		}catch (ParseException e) {
			log.fatal(e.getMessage(), e);
		}
	}

}
