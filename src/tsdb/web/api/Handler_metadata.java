package tsdb.web.api;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONException;
import org.json.JSONWriter;

import tsdb.component.Region;
import tsdb.component.Sensor;
import tsdb.remote.GeneralStationInfo;
import tsdb.remote.PlotInfo;
import tsdb.remote.RemoteTsDB;
import tsdb.util.TimeUtil;
import tsdb.web.util.Web;

/**
 * get meta data of region 
 * @author woellauer
 *
 */
public class Handler_metadata extends MethodHandler {	
	private static final Logger log = LogManager.getLogger();

	public Handler_metadata(RemoteTsDB tsdb) {
		super(tsdb, "metadata.json");
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		response.setContentType("application/json;charset=utf-8");
		String regionName = request.getParameter("region");
		UserIdentity userIdentity = Web.getUserIdentity(baseRequest);
		if(regionName==null) {
			log.warn("missing region parameter");
			response.getWriter().write("missing region parameter");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} else {
			if(Web.isAllowed(userIdentity, regionName)) {
				Region region = tsdb.getRegionByName(regionName);
				if(region==null) {
					log.warn("region not found");
					response.getWriter().write("region not found");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				} else {
					JSONWriter json_output = new JSONWriter(response.getWriter());
					writeRegion(json_output, region);
					response.setStatus(HttpServletResponse.SC_OK);
				}
			} else {
				log.warn("no access to region "+regionName);
				response.getWriter().write("no access to region "+regionName);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		}		
	}

	private void writeRegion(JSONWriter json_output, Region region) throws JSONException, IOException {
		json_output.object();

		json_output.key("region");		
		json_output.object();
		json_output.key("id");
		json_output.value(region.name);
		json_output.key("name");
		json_output.value(region.longName);
		json_output.key("view_year_range");
		json_output.object();
		json_output.key("start");
		json_output.value(String.valueOf(TimeUtil.fastDateWriteYears(TimeUtil.oleMinutesToLocalDateTime(region.viewTimeRange.start).toLocalDate())));
		json_output.key("end");
		json_output.value(String.valueOf(TimeUtil.fastDateWriteYears(TimeUtil.oleMinutesToLocalDateTime(region.viewTimeRange.end).toLocalDate())));
		json_output.endObject();
		json_output.endObject();

		GeneralStationInfo[] generalStationInfos = tsdb.getGeneralStationsOfRegion(region.name);		
		json_output.key("general_stations");
		json_output.array();
		for(GeneralStationInfo generalStationInfo:generalStationInfos) {
			json_output.object();
			json_output.key("id");
			json_output.value(generalStationInfo.name);
			json_output.key("name");
			json_output.value(generalStationInfo.longName);
			json_output.endObject();
		}
		json_output.endArray();

		HashSet<String> sensorNameSet = new HashSet<String>();

		PlotInfo[] plotInfos = tsdb.getPlots();
		json_output.key("plots");
		json_output.array();
		for(PlotInfo plotInfo:plotInfos) {
			if(region.name.equals(plotInfo.generalStationInfo.region.name)) {
				String[] sensorNames = tsdb.getSensorNamesOfPlot(plotInfo.name);
				json_output.object();
				json_output.key("id");
				json_output.value(plotInfo.name);
				json_output.key("general_station");
				json_output.value(plotInfo.generalStationInfo.name);
				json_output.key("sensor_names");
				json_output.array();
				for(String sensorName:sensorNames) {
					sensorNameSet.add(sensorName);
					json_output.value(sensorName);
				}
				json_output.endArray();
				json_output.endObject();
			}
		}
		json_output.endArray();

		Sensor[] sensors = tsdb.getSensors();

		json_output.key("sensors");
		json_output.array();
		for(Sensor sensor:sensors) {
			if(sensorNameSet.contains(sensor.name)) {
				json_output.object();
				json_output.key("id");
				json_output.value(sensor.name);
				json_output.endObject();
			}
		}
		json_output.endArray();


		json_output.endObject();
	}
}