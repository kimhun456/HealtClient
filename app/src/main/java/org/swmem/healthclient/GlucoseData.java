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
    private String sensorID;
    private String date;


    // 컨버팅이 되어있는지 확인하는 불리언값
    private boolean convert;

    // 데이터베이스에 존재하는지 확인하는 값.
    private boolean inDataBase;


    GlucoseData(){

        convert = false;
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


    public String getSensorID() {
        return sensorID;
    }

    public void setSensorID(String sensorID) {
        this.sensorID = sensorID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
