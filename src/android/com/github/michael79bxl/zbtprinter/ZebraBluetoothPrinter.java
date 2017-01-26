package com.github.michael79bxl.zbtprinter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import com.zebra.sdk.printer.discovery.*;
import com.zebra.sdk.comm.*;
import com.zebra.sdk.printer.*;

public class ZebraBluetoothPrinter extends CordovaPlugin {

    private static final String LOG_TAG = "ZebraBluetoothPrinter";
    //String mac = "AC:3F:A4:1D:7A:5C";

    public ZebraBluetoothPrinter() {
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("print")) {
            try {
                String mac = args.getString(0);
                String msg = args.getString(1);
                sendData(callbackContext, mac, msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        if (action.equals("find")) {
            try {
                findPrinter(callbackContext);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public void findPrinter(final CallbackContext callbackContext) {
      try {
          NetworkDiscoverer.findPrinters(new DiscoveryHandlerLinkOsOnly(new DiscoveryHandler() {

              List<DiscoveredPrinter> printers = new ArrayList<DiscoveredPrinter>();

              public void foundPrinter(DiscoveredPrinter printer) {
                  // if(printer instanceof DiscoveredPrinterBluetooth) {
                  //    JSONObject printerObj = new JSONObject();
                  //    try {
                  //      printerObj.put("address", printer.address);
                  //      printerObj.put("friendlyName", ((DiscoveredPrinterBluetooth) printer).friendlyName);
                  //      callbackContext.success(printerObj);
                  //    } catch (JSONException e) {
                  //    }
                  // } else {
                  //   String macAddress = printer.address;
                  //   //I found a printer! I can use the properties of a Discovered printer (address) to make a Bluetooth Connection
                  //   callbackContext.success(macAddress);
                  // }
                printers.add(printer);
              }

              public void discoveryFinished() {
                  //Discovery is done
                callbackContext.success(printers.toString());
              }

              public void discoveryError(String message) {
                  //Error during discovery
                  callbackContext.error(message);
              }
          }));
      } catch (Exception e) {
          e.printStackTrace();
      }
    }

    /*
     * This will send data to be printed by the bluetooth printer
     */
    void sendData(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

                    // Verify the printer is ready to print
                    if (isPrinterReady(thePrinterConn)) {

                        // Open the connection - physical connection is established here.
                        thePrinterConn.open();

                        // Send the data to printer as a byte array.
//                        thePrinterConn.write("^XA^FO0,20^FD^FS^XZ".getBytes());
                        thePrinterConn.write(msg.getBytes());


                        // Make sure the data got to the printer before closing the connection
                        Thread.sleep(500);

                        // Close the insecure connection to release resources.
                        thePrinterConn.close();
                        callbackContext.success("Done");
                    } else {
            callbackContext.error("Printer is not ready");
          }
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }

    private Boolean isPrinterReady(Connection connection) throws ConnectionException, ZebraPrinterLanguageUnknownException {
        Boolean isOK = false;
        connection.open();
        // Creates a ZebraPrinter object to use Zebra specific functionality like getCurrentStatus()
        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
        ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer);

        PrinterStatus printerStatus = (linkOsPrinter != null) ? linkOsPrinter.getCurrentStatus() : printer.getCurrentStatus();
        if (printerStatus.isReadyToPrint) {
            isOK = true;
        } else if (printerStatus.isPaused) {
            throw new ConnectionException("Cannot print because the printer is paused");
        } else if (printerStatus.isHeadOpen) {
            throw new ConnectionException("Cannot print because the printer media door is open");
        } else if (printerStatus.isPaperOut) {
            throw new ConnectionException("Cannot print because the paper is out");
        } else {
            throw new ConnectionException("Cannot print");
        }
        return isOK;
    }

    public final static String specialChars = "ÜüÁáÉéÍíÓóÚúÑñÅÆØåæøÇçÃãÕõÂâÊêÔôªºÀà²³";

    public final static byte[] specialCharsCode = new byte[] { (byte) 0xDC, (byte) 0xFC, (byte) 0xC1, (byte) 0xE1, (byte) 0xC9, (byte) 0xE9,
          (byte) 0xCD, (byte) 0xED, (byte) 0xD3, (byte) 0xF3, (byte) 0xDA, (byte) 0xFA, (byte) 0xD1, (byte) 0xF1, (byte) 0xC5, (byte) 0xC6, (byte) 0xD8,
          (byte) 0xE5, (byte) 0xE6, (byte) 0xF8, (byte) 0xC7, (byte) 0xE7, (byte) 0xC3, (byte) 0xE3, (byte) 0xD5, (byte) 0xF5, (byte) 0xC2, (byte) 0xE2, (byte) 0xCA, (byte) 0xEA,
          (byte) 0xD4, (byte) 0xF4, (byte) 0xAA, (byte) 0xBA, (byte) 0xC0, (byte) 0xE0, (byte) 0xB2, (byte) 0xB3 };

    public static byte[] stringToBytes(String s) {
      int i, l, i_especial;
      byte b;
      byte[] b_arr;
      String s_sub;

      if(s == null)
          return null;
      if((l = s.length()) < 1)
        return new byte[0];

      b_arr = new byte[l];
      for(i = 0; i < l; i++)
      {
        s_sub= s.substring(i, i + 1);
        i_especial= specialChars.indexOf(s_sub);
        if(i_especial < 0)
            b = (s_sub.getBytes())[0];
        else
            b = specialCharsCode[i_especial];
        b_arr[i]= b;
      }

      return b_arr;
    }
}