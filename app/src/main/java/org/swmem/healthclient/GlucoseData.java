package org.swmem.healthclient;

/**
 *
 *  The Data what comes from Device.
 *
 * Created by HyunJae on 2016. 7. 22..
 */
public class GlucoseData {

    private double rawData;
    private double convertedData;
    private double temperature;
    private String deviceID;
    private String date;
    private String type;


    // 컨버팅이 되어있는지 확인하는 불리언값
    private boolean convert;

    // 데이터베이스에 존재하는지 확인하는 값.
    private boolean inDataBase;


    public boolean isModifed() {
        return isModifed;
    }

    public void setModifed(boolean modifed) {
        isModifed = modifed;
    }

    // 값이 바뀌었는지 확인하는 값.
    private boolean isModifed;


    GlucoseData(){

        convert = false;
    }

    public GlucoseData(double rawData, double convertedData, double temperature,
                       String deviceID, String date, String type,
                       boolean convert, boolean inDataBase, boolean isModifed) {
        this.rawData = rawData;
        this.convertedData = convertedData;
        this.temperature = temperature;
        this.deviceID = deviceID;
        this.date = date;
        this.type = type;
        this.convert = convert;
        this.inDataBase = inDataBase;
        this.isModifed = isModifed;
    }

    GlucoseData(double rawData, double temperature){
        setRawData(rawData);
        setTemperature(temperature);
        convert = false;
    }

    GlucoseData(double rawData, double convertedData, double temperature){
        setRawData(rawData);
        setConvertedData(convertedData);
        setTemperature(temperature);

    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public boolean isInDataBase() {
        return inDataBase;
    }

    public void setInDataBase(boolean inDataBase) {
        this.inDataBase = inDataBase;
    }


    public boolean isConverted() {
        return convert;
    }

    public void setConvert(boolean convert) {
        this.convert = convert;
    }


    public double getRawData() {
        return rawData;
    }

    public void setRawData(double rawData) {
        this.rawData = rawData;
    }

    public double getConvertedData() {
        return convertedData;
    }

    public void setConvertedData(double convertedData) {
        this.convertedData = convertedData;
        setConvert(true);
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }


    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
