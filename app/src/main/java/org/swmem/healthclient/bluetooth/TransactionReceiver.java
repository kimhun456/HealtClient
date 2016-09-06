/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.swmem.healthclient.bluetooth;

import android.os.Handler;

/**
 * Parse stream and extract accel data
 * @author Administrator
 */
public class TransactionReceiver {
	private static final String TAG = "TransactionReceiver";
	
	private static final int PARSE_MODE_ERROR = 0;
	private static final int PARSE_MODE_WAIT_START_BYTE = 1;
	private static final int PARSE_MODE_WAIT_COMMAND = 2;
	private static final int PARSE_MODE_WAIT_DATA = 3;
	private static final int PARSE_MODE_WAIT_END_BYTE = 4;
	private static final int PARSE_MODE_COMPLETED = 101;
	
	private Handler mHandler = null;
	
	
	
	public TransactionReceiver(Handler h) {
		mHandler = h;
		reset();
	}
	
	
	/**
	 * Reset transaction receiver.
	 */
	public void reset() {
	}
	
	/**
	 * Set bytes to parse
	 * This method automatically calls parseStream()
	 * @param buffer	
	 * @param count
	 */
	public void setByteArray(byte[] buffer, int count) {
		parseStream(buffer, count);
	}
	
	/**
	 * After parsing bytes received, transaction receiver makes object instance.
	 * This method returns parsed results
	 * @return	Object		parsed object
	 */
	public Object getObject() {
		// TODO: return what you want
		return null;
	}

	/**
	 * Caching received stream and parse byte array
	 * @param buffer		byte array to parse
	 * @param count			byte array size
	 */
	public void parseStream(byte[] buffer, int count) {
		if(buffer != null && buffer.length > 0 && count > 0) {
			for(int i=0; i < buffer.length && i < count; i++) {
				
				// Parse received data
				// Protocol description -----------------------------------------------------------
				// Describe brief info about protocol
				
				// TODO: parse buffer
				
				
			}	// End of for loop
		}	// End of if()
	}	// End of parseStream()


	public static class CommandParser {

        public static final int COMMAND_NONE = -1;
        public static final int COMMAND_THINGSPEAK = 1;

        private static final String THINGSPEAK_PREFIX = "thingspeak:";
        private static final String COMMAND_SUFFIX = "[*]";

        private StringBuilder mReceivedString;
        private int mCommand = COMMAND_NONE;
        private String mParameters;

        public CommandParser() {
            mReceivedString = new StringBuilder();
        }


        public int setString(String message) {
            mReceivedString.append(message);
            return checkCommand();
        }

        public int getCommand() {
            return mCommand;
        }

        public String getParameterString() {
            return mParameters;
        }

        public void resetParser() {
            mCommand = COMMAND_NONE;
            mParameters = null;
        }

        private int checkCommand() {
            int prefixIndex = mReceivedString.lastIndexOf(THINGSPEAK_PREFIX);
            if(prefixIndex > -1) {
                //Logs.d("# found thingspeak prefix");
                // Check if there is suffix string [*]
                int suffixIndex = mReceivedString.lastIndexOf(COMMAND_SUFFIX);
                if(suffixIndex > -1) {
                    //Logs.d("# found [*] suffix");
                    if(prefixIndex + THINGSPEAK_PREFIX.length() <= suffixIndex) {
                        // cut the parameter string from buffer
                        mCommand = COMMAND_THINGSPEAK;
                        mParameters = mReceivedString.substring(prefixIndex + THINGSPEAK_PREFIX.length(), suffixIndex);
                        mReceivedString = new StringBuilder();
                        //Logs.d("# parameters = "+mParameters);
                        return COMMAND_THINGSPEAK;
                    }
                }
            } else {

            }

            // Nothing found... Empty buffer if it's too big
            if(mReceivedString.length() > 1000) {
                mReceivedString = new StringBuilder(mReceivedString.substring(mReceivedString.length() - 200));
            }

            mCommand = COMMAND_NONE;

            return COMMAND_NONE;
        }




    }
}
