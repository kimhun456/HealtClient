package org.swmem.healthclient;

/**
 * Created by HyunJae on 2016. 7. 15..
 *
 * This is Util class
 *
 *  1) implements the algorithm about data parsing
 */
public class Util {

    private static Util util;

    Util getInstance(){
        if(util == null){
            util = new Util();
        }

        return util;
    }






}
