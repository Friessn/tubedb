package tsdb.run;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.NavigableSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.jdbcx.JdbcDataSource;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

import tsdb.TsDB;
import tsdb.TsDBFactory;

public class H2DataReader {
	private static final Logger log = LogManager.getLogger();

	/*public static void main(String[] args) {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:c:/h2_storage/h2_storage");

		try(Connection connection = ds.getConnection()) {
			log.info("connected");
			Statement statement = connection.createStatement();
			String tsName = "AEG01"+"_"+"Ta_200";
			ResultSet rs = statement.executeQuery("SELECT * FROM "+tsName);
			while(rs.next()) {
				//log.info(rs.getInt(1));
			}

		} catch (SQLException e) {
			e.printStackTrace();
			log.error(e);
		}
		log.info("closed");
	}*/

	private final TsDB tsdb;

	private long total_count = 0;

	public static void main(String[] args) {		
		TsDB tsdb = TsDBFactory.createDefault();
		H2DataReader h2DateReader = new H2DataReader(tsdb);
		h2DateReader.readAll();
		tsdb.close();		
	}

	public H2DataReader(TsDB tsdb) {
		this.tsdb = tsdb;
	}

	public void readAll() {

		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:c:/h2_storage/h2_storage");

		try(Connection connection = ds.getConnection()) {


			NavigableSet<String> stationNames = tsdb.streamStorage.getStationNames();	

			long timeStartImport = System.currentTimeMillis();
			try {
				for(String stationName:stationNames) {

					if(stationName.equals("HET38") || stationName.equals("HET44") ) { //very slow reads  reason: unknown
						continue;
					}
					try {
						String[] sensorNames = tsdb.streamStorage.getSensorNames(stationName);
						for(String sensorName:sensorNames) {
							try(Statement statement = connection.createStatement()) {
								readSeries(statement, stationName, sensorName);
							}
						}
					} catch(Exception e) {
						e.printStackTrace();
						log.error(e);
					}
				}
			} catch (Exception e) {
				log.error(e);
			}
			long timeEndImport = System.currentTimeMillis();
			log.info((timeEndImport-timeStartImport)/1000+" s Export "+total_count+" count");

		} catch(Exception e) {
			log.error(e);
		} 
	}

	private void readSeries(Statement statement, String stationName, String sensorName) throws SQLException {		
		String tsName = stationName+"_"+sensorName;
		log.info("read "+tsName);
		try(ResultSet rs = statement.executeQuery("SELECT * FROM "+tsName)) {
			while(rs.next()) {
				rs.getInt(1);
				total_count++;
				//log.info(rs.getInt(1));
			}
		}

	}

}