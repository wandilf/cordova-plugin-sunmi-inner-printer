package com.sunmi.innerprinter;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.IWoyouService;
import com.sunmi.scanner.IScanInterface;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.BroadcastReceiver;

import android.graphics.Bitmap;

import android.os.IBinder;

import android.util.Base64;
import android.util.Log;

import com.sunmi.utils.BitmapUtils;
import com.sunmi.utils.ThreadPoolManager;
import com.sunmi.peripheral.printer.ExceptionConst;
import com.sunmi.peripheral.printer.InnerLcdCallback;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.InnerResultCallback;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import com.sunmi.peripheral.printer.WoyouConsts;

import java.util.Calendar;
import java.util.Date;

public class Printer extends CordovaPlugin {
    private static final String TAG = "SunmiPrinterPlugin";
    public static int NoSunmiPrinter = 0x00000000;
    public static int CheckSunmiPrinter = 0x00000001;
    public static int FoundSunmiPrinter = 0x00000002;
    public static int LostSunmiPrinter = 0x00000003;

    public int sunmiPrinter = CheckSunmiPrinter;
    private SunmiPrinterService woyouService;
    private BitmapUtils bitMapUtils;
    private PrinterStatusReceiver printerStatusReceiver = new PrinterStatusReceiver();
    private ScanReceiver scanReceiver = new ScanReceiver();
    private CallbackContext callbackContext;

    private InnerPrinterCallback innerPrinterCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            woyouService = service;
            checkSunmiPrinterService(service);
        }

        @Override
        protected void onDisconnected() {
            woyouService = null;
            sunmiPrinter = LostSunmiPrinter;
        }
    };

    private InnerResultCallback innerResultCallback = new InnerResultCallback() {
      @Override
      public void onRunResult(boolean isSuccess) {
        if (isSuccess) {
          LogTimeDiff(currentAction);
          callbackContext.success("");
        } else {
          callbackContext.error(isSuccess + "");
        }
      }

      @Override
      public void onReturnString(String result) {
        callbackContext.success(result);
      }

      @Override
      public void onRaiseException(int code, String msg) {
        callbackContext.error(msg);
      }

      @Override
      public void onPrintResult(int code, String msg) {
        callbackContext.success(msg);
      }
    };


    private IScanInterface scanInterface;
    private ServiceConnection conn = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        scanInterface = IScanInterface.Stub.asInterface(service);
        Log.i("setting", "Scanner Service Connected!");
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        Log.e("setting", "Scanner Service Disconnected!");
        scanInterface = null;
      }
    };

    public final static String OUT_OF_PAPER_ACTION = "woyou.aidlservice.jiuv5.OUT_OF_PAPER_ACTION";
    public final static String ERROR_ACTION = "woyou.aidlservice.jiuv5.ERROR_ACTION";
    public final static String NORMAL_ACTION = "woyou.aidlservice.jiuv5.NORMAL_ACTION";
    public final static String COVER_OPEN_ACTION = "woyou.aidlservice.jiuv5.COVER_OPEN_ACTION";
    public final static String COVER_ERROR_ACTION = "woyou.aidlservice.jiuv5.COVER_ERROR_ACTION";
    public final static String KNIFE_ERROR_1_ACTION = "woyou.aidlservice.jiuv5.KNIFE_ERROR_ACTION_1";
    public final static String KNIFE_ERROR_2_ACTION = "woyou.aidlservice.jiuv5.KNIFE_ERROR_ACTION_2";
    public final static String OVER_HEATING_ACITON = "woyou.aidlservice.jiuv5.OVER_HEATING_ACITON";
    public final static String FIRMWARE_UPDATING_ACITON = "woyou.aidlservice.jiuv5.FIRMWARE_UPDATING_ACITON";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
      super.initialize(cordova, webView);
      Log.i(TAG, "init sunmi plugin");

      Context applicationContext = this.cordova.getActivity().getApplicationContext();

      bitMapUtils = new BitmapUtils(applicationContext);

      try {
        boolean ret =  InnerPrinterManager.getInstance().bindService(applicationContext,
          innerPrinterCallback);
        if(!ret){
            sunmiPrinter = NoSunmiPrinter;
        }
        Log.i(TAG, "Bind print service result: " + ret);
      } catch (Exception e) {
        Log.i(TAG, "ERROR on bind print service: " + e.getMessage());
      }


      IntentFilter mFilter = new IntentFilter();
      mFilter.addAction(OUT_OF_PAPER_ACTION);
      mFilter.addAction(ERROR_ACTION);
      mFilter.addAction(NORMAL_ACTION);
      mFilter.addAction(COVER_OPEN_ACTION);
      mFilter.addAction(COVER_ERROR_ACTION);
      mFilter.addAction(KNIFE_ERROR_1_ACTION);
      mFilter.addAction(KNIFE_ERROR_2_ACTION);
      mFilter.addAction(OVER_HEATING_ACITON);
      mFilter.addAction(FIRMWARE_UPDATING_ACITON);

      applicationContext.registerReceiver(printerStatusReceiver, mFilter);
    }

    private Date startTime;
    private String currentAction;
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
      startTime = Calendar.getInstance().getTime();
      currentAction = action;
      this.callbackContext = callbackContext;
      if (action.equals("printerInit")) {
        printerInit(callbackContext);
        return true;
      } else if (action.equals("printerSelfChecking")) {
        printerSelfChecking(callbackContext);
        return true;
      } else if (action.equals("getPrinterSerialNo")) {
        getPrinterSerialNo(callbackContext);
        return true;
      } else if (action.equals("getPrinterVersion")) {
        getPrinterVersion(callbackContext);
        return true;
      } else if (action.equals("hasPrinter")) {
        hasPrinter(callbackContext);
        return true;
      } else if (action.equals("getPrintedLength")) {
        getPrintedLength(callbackContext);
        return true;
      } else if (action.equals("lineWrap")) {
        lineWrap(data.getInt(0), callbackContext);
        return true;
      } else if (action.equals("sendRAWData")) {
        sendRAWData(data.getString(0), callbackContext);
        return true;
      } else if (action.equals("setAlignment")) {
        setAlignment(data.getInt(0), callbackContext);
        return true;
      } else if (action.equals("setFontName")) {
        setFontName(data.getString(0), callbackContext);
        return true;
      } else if (action.equals("setFontSize")) {
        setFontSize((float) data.getDouble(0), callbackContext);
        return true;
      } else if (action.equals("printTextWithFont")) {
        printTextWithFont(data.getString(0), data.getString(1), (float) data.getDouble(2), callbackContext);
        return true;
      } else if (action.equals("printColumnsText")) {
        printColumnsText(data.getJSONArray(0), data.getJSONArray(1), data.getJSONArray(2), callbackContext);
        return true;
      } else if (action.equals("printBitmap")) {
        printBitmap(data.getString(0), data.getInt(1), data.getInt(2), callbackContext);
        return true;
      } else if (action.equals("printBarCode")) {
        printBarCode(data.getString(0), data.getInt(1), data.getInt(2), data.getInt(1), data.getInt(2), callbackContext);
        return true;
      } else if (action.equals("printQRCode")) {
        printQRCode(data.getString(0), data.getInt(1), data.getInt(2), callbackContext);
        return true;
      } else if (action.equals("printOriginalText")) {
        printOriginalText(data.getString(0), callbackContext);
        return true;
      } else if (action.equals("printString")) {
        printString(data.getString(0), callbackContext);
        return true;
      } else if (action.equals("printerStatusStartListener")) {
        printerStatusStartListener(callbackContext);
        return true;
      } else if (action.equals("printerStatusStopListener")) {
        printerStatusStopListener();
        return true;
      } else if (action.equals("initScanner")) {
        scannerInit(callbackContext);
        return true;
      } else if (action.equals("cutPaper")) {
        cutPaper(callbackContext);
        return true;
      }

      return false;
    }

    public void scannerInit(final CallbackContext callbackContext) {
      //scanner service
      Context applicationContext = this.cordova.getActivity().getApplicationContext();
      Intent intent = new Intent();
      intent.setPackage("com.sunmi.scanner");
      intent.setAction("com.sunmi.scanner.IScanInterface");

      applicationContext.startService(intent);
      applicationContext.bindService(intent, conn, Context.BIND_AUTO_CREATE);
      IntentFilter scanFilter = new IntentFilter();
      scanFilter.addAction("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED");
      applicationContext.registerReceiver(scanReceiver, scanFilter);
      scanReceiver.setCordova(this.cordova, this.webView);
      callbackContext.success("scanner init success");
    }


    public void printerInit(final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printerInit(new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void printerSelfChecking(final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printerSelfChecking(new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void getPrinterSerialNo(final CallbackContext callbackContext) {
      try {
        callbackContext.success(getPrinterSerialNo());
      } catch (Exception e) {
        Log.i(TAG, "ERROR: " + e.getMessage());
        callbackContext.error(e.getMessage());
      }
    }

    private String getPrinterSerialNo() throws Exception {
      final SunmiPrinterService printerService = woyouService;
      return printerService.getPrinterSerialNo();
    }

    public void getPrinterVersion(final CallbackContext callbackContext) {
      try {
        callbackContext.success(getPrinterVersion());
      } catch (Exception e) {
        Log.i(TAG, "ERROR: " + e.getMessage());
        callbackContext.error(e.getMessage());
      }
    }

    private String getPrinterVersion() throws Exception {
      final SunmiPrinterService printerService = woyouService;
      return printerService.getPrinterVersion();
    }

    public void getPrinterModal(final CallbackContext callbackContext) {
      try {
        callbackContext.success(getPrinterModal());
      } catch (Exception e) {
        Log.i(TAG, "ERROR: " + e.getMessage());
        callbackContext.error(e.getMessage());
      }
    }

    private String getPrinterModal() throws Exception {
      // Caution: This method is not fully test -- Januslo 2018-08-11
      final SunmiPrinterService printerService = woyouService;
      return printerService.getPrinterModal();
    }

    public void hasPrinter(final CallbackContext callbackContext) {
      try {
        callbackContext.success(hasPrinter());
      } catch (Exception e) {
        Log.i(TAG, "ERROR: " + e.getMessage());
        callbackContext.error(e.getMessage());
      }
    }

    private int hasPrinter() {
      //return sunmiPrinter != NoSunmiPrinter ? 1 : 0;
      final SunmiPrinterService printerService = woyouService;
      final boolean hasPrinterService = printerService != null;
      return hasPrinterService ? 1 : 0;
    }

    public void getPrintedLength(final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.getPrintedLength(new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void lineWrap(int n, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final int count = n;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.lineWrap(count, null);
            callbackContext.success("");
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    private void LogTimeDiff(String method) {
      try {
        Date now = Calendar.getInstance().getTime();
        long diff = now.getTime() - startTime.getTime();
        Log.i(TAG, "duration for " + method + ", " + diff + "ms");
      } catch (Exception ex) {
        Log.i(TAG, "ERROR: " + ex.getMessage());
      }
    }

    public void sendRAWData(String base64EncriptedData, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final byte[] d = Base64.decode(base64EncriptedData, Base64.DEFAULT);
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.sendRAWData(d, null);
            callbackContext.success("");
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void setAlignment(int alignment, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final int align = alignment;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.setAlignment(align, null);
            callbackContext.success("");
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void setFontName(String typeface, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final String tf = typeface;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.setFontName(tf, new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void setFontSize(float fontsize, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final float fs = fontsize;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.setFontSize(fs, null);
            callbackContext.success("");
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void cutPaper(final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.cutPaper(null);
            callbackContext.success("");
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void printTextWithFont(String text, String typeface, float fontsize, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final String txt = text;
      final String tf = typeface;
      final float fs = fontsize;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printTextWithFont(txt, tf, fs, null);
            callbackContext.success("");
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void printColumnsText(JSONArray colsTextArr, JSONArray colsWidthArr, JSONArray colsAlign,
        final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final String[] clst = new String[colsTextArr.length()];
      for (int i = 0; i < colsTextArr.length(); i++) {
        try {
          clst[i] = colsTextArr.getString(i);
        } catch (JSONException e) {
          clst[i] = "-";
          Log.i(TAG, "ERROR TEXT: " + e.getMessage());
        }
      }
      final int[] clsw = new int[colsWidthArr.length()];
      for (int i = 0; i < colsWidthArr.length(); i++) {
        try {
          clsw[i] = colsWidthArr.getInt(i);
        } catch (JSONException e) {
          clsw[i] = 1;
          Log.i(TAG, "ERROR WIDTH: " + e.getMessage());
        }
      }
      final int[] clsa = new int[colsAlign.length()];
      for (int i = 0; i < colsAlign.length(); i++) {
        try {
          clsa[i] = colsAlign.getInt(i);
        } catch (JSONException e) {
          clsa[i] = 0;
          Log.i(TAG, "ERROR ALIGN: " + e.getMessage());
        }
      }
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printColumnsText(clst, clsw, clsa, new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void printBitmap(String data, int width, int height, final CallbackContext callbackContext) {
      try {
        final SunmiPrinterService printerService = woyouService;
        byte[] decoded = Base64.decode(data, Base64.DEFAULT);
        final Bitmap bitMap = bitMapUtils.decodeBitmap(decoded, width, height);
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
          @Override
          public void run() {
            try {
              printerService.printBitmap(bitMap, null);
              callbackContext.success("");
            } catch (Exception e) {
              e.printStackTrace();
              Log.i(TAG, "ERROR: " + e.getMessage());
              callbackContext.error(e.getMessage());
            }
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
        Log.i(TAG, "ERROR: " + e.getMessage());
      }
    }

    public void printBarCode(String data, int symbology, int width, int height, int textPosition,
        final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final String d = data;
      final int s = symbology;
      final int h = height;
      final int w = width;
      final int tp = textPosition;

      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printBarCode(d, s, h, w, tp, new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void printQRCode(String data, int moduleSize, int errorLevel, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final String d = data;
      final int size = moduleSize;
      final int level = errorLevel;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printQRCode(d, size, level, null);
            callbackContext.success("");
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void printOriginalText(String text, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final String txt = text;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printOriginalText(txt, new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void commitPrinterBuffer() {
      final SunmiPrinterService printerService = woyouService;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.commitPrinterBuffer();
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
          }
        }
      });
    }

    public void enterPrinterBuffer(boolean clean) {
      final SunmiPrinterService printerService = woyouService;
      final boolean c = clean;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.enterPrinterBuffer(c);
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
          }
        }
      });
    }

    public void exitPrinterBuffer(boolean commit) {
      final SunmiPrinterService printerService = woyouService;
      final boolean com = commit;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.exitPrinterBuffer(com);
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
          }
        }
      });
    }

    public void printString(String message, final CallbackContext callbackContext) {
      final SunmiPrinterService printerService = woyouService;
      final String msgs = message;
      ThreadPoolManager.getInstance().executeTask(new Runnable() {
        @Override
        public void run() {
          try {
            printerService.printText(msgs, new InnerResultCallback() {
              @Override
              public void onRunResult(boolean isSuccess) {
                if (isSuccess) {
                  callbackContext.success("");
                } else {
                  callbackContext.error(isSuccess + "");
                }
              }

              @Override
              public void onReturnString(String result) {
                callbackContext.success(result);
              }

              @Override
              public void onRaiseException(int code, String msg) {
                callbackContext.error(msg);
              }

              @Override
              public void onPrintResult(int code, String msg) {
                  callbackContext.success(msg);
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
            callbackContext.error(e.getMessage());
          }
        }
      });
    }

    public void printerStatusStartListener(final CallbackContext callbackContext) {
      final PrinterStatusReceiver receiver = printerStatusReceiver;
      receiver.startReceiving(callbackContext);
    }

    public void printerStatusStopListener() {
      final PrinterStatusReceiver receiver = printerStatusReceiver;
      receiver.stopReceiving();
    }

    public void scanStartListener(final CallbackContext callbackContext) {
      final ScanReceiver receiver = scanReceiver;
      receiver.startReceiving(callbackContext);
    }

    public void scanStopListener() {
      final ScanReceiver receiver = scanReceiver;
      receiver.stopReceiving();
    }

    private void checkSunmiPrinterService(SunmiPrinterService service){
        boolean ret = false;
        try {
            ret = InnerPrinterManager.getInstance().hasPrinter(service);
        } catch (InnerPrinterException e) {
            e.printStackTrace();
        }
        sunmiPrinter = ret?FoundSunmiPrinter:NoSunmiPrinter;
    }
}
