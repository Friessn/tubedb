package tsdb.remote;

import static tsdb.util.AssumptionCheck.throwNull;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.Yaml;

import com.opencsv.CSVWriter;

import tsdb.component.Region;
import tsdb.component.Sensor;
import tsdb.iterator.ProjectionFillIterator;
import tsdb.util.AggregationInterval;
import tsdb.util.DataQuality;
import tsdb.util.TimeUtil;
import tsdb.util.TsEntry;
import tsdb.util.Util;
import tsdb.util.iterator.TimestampSeries;

/**
 * Creates Zip-files of sets of time series
 * @author woellauer
 *
 */
public class ZipExport {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Platform neutral line separator (windows style)
	 */
	private static final String LINE_SEPARATOR = "\r\n";
	private static final LineBreak LINE_BREAK = LineBreak.WIN; //"\r\n"

	private final RemoteTsDB tsdb;

	private Consumer<String> cbPrintLine = null;

	private final Region region;
	private String[] sensorNames;
	private final String[] plotIDs;
	private final AggregationInterval aggregationInterval;
	private final DataQuality dataQuality;
	private final boolean interpolated;
	private final boolean allInOne;
	private final boolean desc_sensor;
	private final boolean desc_plot;
	private final boolean desc_settings;
	private final boolean col_plotid;
	private final boolean col_timestamp;
	private final boolean col_datetime;
	private final boolean col_qualitycounter;
	private final boolean write_header;
	private final Long startTimestamp;
	private final Long endTimestamp;

	private int processedPlots = 0;

	public ZipExport(RemoteTsDB tsdb, Region region, String[] sensorNames, String[] plotIDs,AggregationInterval aggregationInterval,DataQuality dataQuality,boolean interpolated, boolean allinone, boolean desc_sensor, boolean desc_plot, boolean desc_settings, boolean col_plotid, boolean col_timestamp, boolean col_datetime, boolean write_header, Long startTimestamp, Long endTimestamp, boolean col_qualitycounter) {
		throwNull(tsdb);
		this.tsdb = tsdb;

		this.region = region;
		if(aggregationInterval == AggregationInterval.RAW) {
			this.sensorNames = sensorNames;
		} else {
			ArrayList<String> sensorNameList = new ArrayList<String>();
			try {
				Sensor[] allSensors = tsdb.getSensors();
				if(allSensors!=null) {
					Map<String, Sensor> allSensorsMap = Arrays.stream(allSensors).collect(Collectors.toMap(Sensor::getName, Function.identity()));
					for(String sensorName:sensorNames) {
						if(allSensorsMap.containsKey(sensorName)) {
							if(allSensorsMap.get(sensorName).isAggregable()) {
								sensorNameList.add(sensorName);
							}
						}
					}
					this.sensorNames = sensorNameList.toArray(new String[0]);
				} else {
					this.sensorNames = sensorNames;
				}
			} catch (RemoteException e) {
				log.warn(e);
				this.sensorNames = sensorNames;
			}
		}

		this.plotIDs = plotIDs;
		this.aggregationInterval = aggregationInterval;
		this.dataQuality = dataQuality;
		this.interpolated = interpolated;
		this.allInOne = allinone;
		this.desc_sensor = desc_sensor;
		this.desc_plot = desc_plot;
		this.desc_settings = desc_settings;
		this.col_plotid = col_plotid;
		this.col_timestamp = col_timestamp;
		this.col_datetime = col_datetime;
		this.write_header = write_header;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.col_qualitycounter = col_qualitycounter;
	}

	public boolean createZipFile(String filename) {
		FileOutputStream fileOutputStream;
		try {
			printLine("create file: "+filename);
			fileOutputStream = new FileOutputStream(filename);
			boolean ret = writeToStream(fileOutputStream);
			fileOutputStream.close();
			printLine("...finished");
			return ret;
		} catch (IOException e) {
			log.error(e);
			return false;
		}

	}


	public boolean writeToStream(OutputStream outputstream) {
		printLine("start export...");
		printLine("");
		printLine("sensorNames       "+Util.arrayToString(sensorNames));
		if(Util.empty(sensorNames)) {
			return false;
		}
		if(Util.empty(plotIDs)) {
			return false;
		}
		printLine("plots "+plotIDs.length);
		printLine("");

		try {
			ZipOutputStream zipOutputStream = new ZipOutputStream(outputstream);
			zipOutputStream.setComment("TubeDB time series data");
			zipOutputStream.setLevel(9);

			if(desc_settings) {
				/*zipOutputStream.putNextEntry(new ZipEntry("processing_settings.txt"));
				PrintStream printStream = new PrintStream(zipOutputStream, false);
				write_settings_TXT(printStream);
				printStream.flush();*/
				zipOutputStream.putNextEntry(new ZipEntry("processing_settings.yaml"));
				OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream);
				BufferedWriter bufferedWriter = new BufferedWriter(writer);
				write_settings_YAML(bufferedWriter);
				bufferedWriter.flush();
				writer.flush();
			}

			if(desc_sensor) {
				/*zipOutputStream.putNextEntry(new ZipEntry("sensor_description.txt"));
				PrintStream printStream = new PrintStream(zipOutputStream, false);
				write_sensor_description_TXT(printStream);
				printStream.flush();*/
				zipOutputStream.putNextEntry(new ZipEntry("sensor_description.csv"));
				OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream);
				BufferedWriter bufferedWriter = new BufferedWriter(writer);
				write_sensor_description_CSV(bufferedWriter);
				bufferedWriter.flush();
				writer.flush();
			}

			if(desc_plot) {
				/*zipOutputStream.putNextEntry(new ZipEntry("plot_description.txt"));
				PrintStream printStream = new PrintStream(zipOutputStream, false);
				write_plot_description_TXT(printStream);
				printStream.flush();*/
				zipOutputStream.putNextEntry(new ZipEntry("plot_description.csv"));
				OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream);
				BufferedWriter bufferedWriter = new BufferedWriter(writer);
				write_plot_description_CSV(bufferedWriter);
				bufferedWriter.flush();
				writer.flush();
			}

			if(allInOne) {				
				zipOutputStream.putNextEntry(new ZipEntry("plots.csv"));
				PrintStream csvOut = new PrintStream(zipOutputStream,false);
				if(write_header) {
					writeCSVHeader(csvOut);
				}
				processedPlots = 0;
				for(String plotID:plotIDs) {
					printLine("processing plot "+plotID);
					try {
						String[] schema = tsdb.getValidSchemaWithVirtualSensors(plotID, sensorNames);
						if(!Util.empty(schema)) {
							TimestampSeries timeseries = tsdb.plot(null,plotID, schema, aggregationInterval, dataQuality, interpolated, startTimestamp, endTimestamp);
							if(timeseries!=null) {								
								writeTimeseries(timeseries,plotID,csvOut);								
							} else {
								printLine("not processed: "+plotID);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						log.error(e);
						printLine("ERROR "+e);
					}
					processedPlots++;
				}
				csvOut.flush();				
			} else {
				processedPlots = 0;
				for(String plotID:plotIDs) {
					printLine("processing plot "+plotID);
					try {
						String[] schema = tsdb.getValidSchemaWithVirtualSensors(plotID, sensorNames);
						if(!Util.empty(schema)) {
							TimestampSeries timeseries = tsdb.plot(null,plotID, schema, aggregationInterval, dataQuality, interpolated, startTimestamp, endTimestamp);
							if(timeseries!=null) {
								zipOutputStream.putNextEntry(new ZipEntry(plotID+".csv"));
								PrintStream csvOut = new PrintStream(zipOutputStream,false);
								if(write_header) {
									writeCSVHeader(csvOut);
								}
								writeTimeseries(timeseries,plotID,csvOut);
								csvOut.flush();
							} else {
								printLine("not processed: "+plotID);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						log.error(e);
						printLine("ERROR "+e);
					}
					processedPlots++;
				}				
			}
			zipOutputStream.finish();
			printLine("");
			printLine("...finished");
			return true;
		} catch (IOException e) {
			log.warn(e);
			printLine("ERROR "+e);
			return false;
		}		
	}	

	@Deprecated
	private void write_settings_TXT(PrintStream printStream) {
		printStream.print("Settings that were used to create this time series archive file:"+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);
		printStream.print("creation date: "+LocalDateTime.now()+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);
		printStream.print("sensor names ("+sensorNames.length+") : "+Util.arrayToString(sensorNames)+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);
		printStream.print("plot names ("+plotIDs.length+") : "+Util.arrayToString(plotIDs)+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);
		printStream.print("time steps : "+aggregationInterval.getText()+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);
		printStream.print("quality checks : "+dataQuality.getText()+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);
		if(interpolated) {
			printStream.print("interpolate missing data"+LINE_SEPARATOR);
			printStream.print(LINE_SEPARATOR);
		} else {
			printStream.print("no interpolation used"+LINE_SEPARATOR);
			printStream.print(LINE_SEPARATOR);
		}
		if(allInOne) {
			printStream.print("write all plots into one CSV-File"+LINE_SEPARATOR);
			printStream.print(LINE_SEPARATOR);
		} else {
			printStream.print("for each plot write into separate CSV-File"+LINE_SEPARATOR);
			printStream.print(LINE_SEPARATOR);			
		}

	}

	private void write_settings_YAML(BufferedWriter bufferedWriter) {		
		try {
			Map<String,Object> map = new LinkedHashMap<String,Object>();
			map.put("creation date", LocalDateTime.now().toString());

			Map<String, Object> regionMap = new LinkedHashMap<String,Object>();
			regionMap.put("id", region.name);
			regionMap.put("name", region.longName);
			map.put("region", regionMap);

			try {
				HashSet<String> plotSet = new HashSet<>(Arrays.asList(plotIDs));			
				TreeSet<GeneralStationInfo> generals = new TreeSet<GeneralStationInfo>();
				PlotInfo[] plotInfos = tsdb.getPlots();
				for(PlotInfo plotInfo:plotInfos) {
					if(plotSet.contains(plotInfo.name) && !generals.contains(plotInfo.generalStationInfo)) {
						generals.add(plotInfo.generalStationInfo);
					}
				}
				ArrayList<Object> generalList = new ArrayList<Object>();
				for(GeneralStationInfo general:generals) {
					Map<String, Object> generalMap = new LinkedHashMap<String,Object>();
					generalMap.put("id", general.name);
					generalMap.put("name", general.longName);
					generalList.add(generalMap);
				}
				map.put("groups", generalList);
			} catch(Exception e) {
				log.error(e);
			}

			map.put("plots", plotIDs);
			map.put("sensors", sensorNames);


			String timeStart = startTimestamp==null?"*":TimeUtil.oleMinutesToText(startTimestamp);
			String timeEnd = endTimestamp==null?"*":TimeUtil.oleMinutesToText(endTimestamp);
			Map<String,Object> timeMap = new LinkedHashMap<String,Object>();
			timeMap.put("start", timeStart);
			timeMap.put("end", timeEnd);
			map.put("time interval", timeMap);

			map.put("aggregation", aggregationInterval.getText());
			map.put("quality check", dataQuality.getText());
			map.put("interpolation", interpolated);

			List<String> columnlist = new ArrayList<String>();
			if(col_plotid) columnlist.add("plotID");
			if(col_timestamp) columnlist.add("timestamp");
			if(col_datetime) columnlist.add("datetime");
			if(col_qualitycounter) columnlist.add("qualitycounter");
			map.put("data columns", columnlist);

			map.put("data header", write_header);			
			map.put("all plots in one file", allInOne);

			List<String> filelist = new ArrayList<String>();
			if(desc_sensor) filelist.add("sensor description");
			if(desc_plot) filelist.add("plot description");
			if(desc_settings) filelist.add("processing settings");

			map.put("additional files", filelist);

			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			options.setLineBreak(LINE_BREAK);
			Yaml yaml = new Yaml(options);
			yaml.dump(map, bufferedWriter);
		} catch(Exception e) {
			log.error(e);
		}

	}

	public void setPrintCallback(Consumer<String> callback) {
		this.cbPrintLine = callback;
	}

	private void printLine(String s) {
		if(cbPrintLine!=null) {
			cbPrintLine.accept(s);
		}
	}

	private void writeCSVHeader(PrintStream csvOut) {
		StringBuilder stringbuilder = new StringBuilder();
		boolean isFirst = true;
		if(col_plotid) {
			stringbuilder.append("plotID");
			isFirst = false;
		}

		if(col_timestamp) {
			if(!isFirst) {
				stringbuilder.append(',');				
			}
			stringbuilder.append("timestamp");
			isFirst = false;
		}

		if(col_datetime) {
			if(!isFirst) {
				stringbuilder.append(',');				
			}
			stringbuilder.append("datetime");
			isFirst = false;
		}
		for(String name:sensorNames) {
			if(!isFirst) {
				stringbuilder.append(',');
			}
			stringbuilder.append(name);
			isFirst = false;
		}
		if(col_qualitycounter) {
			if(!isFirst) {
				stringbuilder.append(',');				
			}
			stringbuilder.append("qualitycounter");
			isFirst = false;
		}
		csvOut.print(stringbuilder+LINE_SEPARATOR);
	}

	@Deprecated
	private void write_sensor_description_TXT(PrintStream printStream) {
		printStream.print("sensors:\t"+sensorNames.length+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);
		for(int i=0;i<sensorNames.length;i++) {
			printStream.print((i+1)+". sensor:\t"+sensorNames[i]+LINE_SEPARATOR);
			try {
				Sensor sensor = tsdb.getSensor(sensorNames[i]);
				if(sensor!=null) {
					printStream.print("description:\t"+sensor.description+LINE_SEPARATOR);
					printStream.print("unit:\t\t"+sensor.unitDescription+LINE_SEPARATOR);
				}
			} catch (RemoteException e) {
				log.error(e);
			}
			printStream.print(LINE_SEPARATOR);
		}		
	}

	private void write_sensor_description_CSV(BufferedWriter bufferedWriter) {
		try {
			@SuppressWarnings("resource") //don't close stream
			CSVWriter csvWriter = new CSVWriter(bufferedWriter, ',', '"', LINE_SEPARATOR);
			csvWriter.writeNext(new String[]{"name", "description", "unit"}, false);
			for(String sensorName:sensorNames) {
				String sensorDescription = "";
				String sensorUnit = "";
				try {
					Sensor sensor = tsdb.getSensor(sensorName);
					if(sensor!=null) {
						sensorDescription = sensor.description;
						sensorUnit = sensor.unitDescription;
					}
				} catch (Exception e) {
					log.error(e);
				}
				csvWriter.writeNext(new String[]{sensorName, sensorDescription, sensorUnit}, false);
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	@Deprecated
	private void write_plot_description_TXT(PrintStream printStream) {
		printStream.print("plots:\t"+plotIDs.length+LINE_SEPARATOR);
		printStream.print("in region:\t"+region.longName+LINE_SEPARATOR);
		printStream.print(LINE_SEPARATOR);

		try {
			PlotInfo[] plotInfos = tsdb.getPlots();
			Map<String,PlotInfo> map = new HashMap<String,PlotInfo>();
			for(PlotInfo plotInfo:plotInfos) {
				map.put(plotInfo.name, plotInfo);
			}

			for(int i=0;i<plotIDs.length;i++) {
				printStream.print((i+1)+". plot:\t"+plotIDs[i]+LINE_SEPARATOR);

				PlotInfo plotInfo = map.get(plotIDs[i]);
				if(plotInfo!=null) {
					printStream.print("category:\t"+plotInfo.generalStationInfo.longName+LINE_SEPARATOR);
					if(Double.isFinite(plotInfo.geoPosLatitude)) {
						printStream.print("Latitude:\t"+plotInfo.geoPosLatitude+LINE_SEPARATOR);
					}
					if(Double.isFinite(plotInfo.geoPosLongitude)) {
						printStream.print("Longitude:\t"+plotInfo.geoPosLongitude+LINE_SEPARATOR);
					}
					if(Float.isFinite(plotInfo.elevation)) {
						printStream.print("Elevation:\t"+plotInfo.elevation+LINE_SEPARATOR);
					}
				}

				printStream.print(LINE_SEPARATOR);
			}
		} catch (RemoteException e) {
			log.error(e);
		}
	}

	private void write_plot_description_CSV(BufferedWriter bufferedWriter) {		
		try {
			@SuppressWarnings("resource") //don't close stream
			CSVWriter csvWriter = new CSVWriter(bufferedWriter, ',', '"', LINE_SEPARATOR);
			PlotInfo[] plotInfos = tsdb.getPlots();
			Map<String,PlotInfo> map = new HashMap<String,PlotInfo>();
			for(PlotInfo plotInfo:plotInfos) {
				map.put(plotInfo.name, plotInfo);
			}

			csvWriter.writeNext(new String[]{"plot","general","region","lat","lon","elevation"}, false);

			for(int i=0;i<plotIDs.length;i++) {
				PlotInfo plotInfo = map.get(plotIDs[i]);
				double lat = plotInfo.geoPosLatitude;
				double lon = plotInfo.geoPosLongitude;
				double elevation = plotInfo.elevation;
				csvWriter.writeNext(new String[]{
						plotInfo.name,
						plotInfo.generalStationInfo.name,
						plotInfo.generalStationInfo.region.name,
						Double.isFinite(lat)?Double.toString(lat):"",
								Double.isFinite(lon)?Double.toString(lon):"",
										Double.isFinite(elevation)?Double.toString(elevation):""
				}, false);
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	private void writeTimeseries(TimestampSeries timeseries, String plotID, PrintStream csvOut) {		
		ProjectionFillIterator it = new ProjectionFillIterator(timeseries.tsIterator(), sensorNames);
		while(it.hasNext()) {
			TsEntry entry = it.next();
			boolean isFirst = true;
			StringBuilder s = new StringBuilder();
			if(col_plotid) {
				s.append(plotID);
				isFirst = false;
			}
			if(col_timestamp) {
				if(!isFirst) {
					s.append(',');
				}
				s.append(entry.timestamp);
				isFirst = false;
			}
			if(col_datetime) {
				if(!isFirst) {
					s.append(',');
				}
				s.append(TimeUtil.oleMinutesToText(entry.timestamp));
				isFirst = false;
			}
			Formatter formater = new Formatter(s,Locale.ENGLISH);
			for(int i=0;i<sensorNames.length;i++) {
				float v = entry.data[i];
				if(Float.isNaN(v)) {
					if(isFirst) {
						formater.format("NA");
						isFirst = false;
					} else {
						formater.format(",NA");
					}	
				} else {
					if(isFirst) {
						formater.format("%.2f", v);
						isFirst = false;
					} else {
						formater.format(",%.2f", v);
					}
				}
			}
			if(col_qualitycounter) {
				if(!isFirst) {
					s.append(',');
				}
				s.append(entry.qualityCountersToString());
				isFirst = false;
			}
			s.append(LINE_SEPARATOR);
			csvOut.print(s);
			formater.close();			
		}		
	}

	public int getProcessedPlots() {
		return processedPlots;
	}
}
