package fr.openent.mediacentre;

import fr.openent.mediacentre.controller.MediacentreController;
import fr.openent.mediacentre.export.ExportTask;
import fr.wseduc.cron.CronTrigger;
import org.entcore.common.http.BaseServer;

import java.text.ParseException;

public class Mediacentre extends BaseServer {

	public static final String MEDIACENTRE_ADDRESS = "openent.mediacentre";

	@Override
	public void start() {
		super.start();
		addController(new MediacentreController());

		final String exportCron = container.config().getString("export-cron", "");

		try{
			new CronTrigger(vertx, exportCron).schedule(new ExportTask(vertx.eventBus(), container.logger()));
		}catch (ParseException e) {
			container.logger().fatal(e.getMessage(), e);
		}
	}

}
