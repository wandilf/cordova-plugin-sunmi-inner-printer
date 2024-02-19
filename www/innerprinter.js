exec = require("cordova/exec");

module.exports = {
  printerInit: function (onSuccess, onError) {
    //  return new Promise(function (onSuccess, onError) {
    exec(onSuccess, onError, "Printer", "printerInit", []);
    //    });
  },
  printerSelfChecking: function (onSuccess, onError) {
    // return new Promise(function (resolve, reject) {
    exec(onSuccess, onError, "Printer", "printerSelfChecking", []);
    // });
  },
  getPrinterSerialNo: function (onSuccess, onError) {
    //  return new Promise(function (resolve, reject) {
    exec(onSuccess, onError, "Printer", "getPrinterSerialNo", []);
    // });
  },
  getPrinterVersion: function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "getPrinterVersion", []);
    });
  },
  isPaperPresent: function (onSuccess, onError) {
    // return new Promise(function (onSuccess, onError) {
    exec(onSuccess, onError, "Printer", "isPaperPresent", []);
    // });
  },
  getPrintedLength: function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "getPrintedLength", []);
    });
  },
  lineWrap: function (count) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "lineWrap", [count]);
    });
  },
  sendRAWData: function (base64Data) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "sendRAWData", [base64Data]);
    });
  },
  setAlignment: function (alignment) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "setAlignment", [alignment]);
    });
  },
  setFontName: function (typeface) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "setFontName", [typeface]);
    });
  },
  setFontSize: function (fontSize) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "setFontSize", [fontSize]);
    });
  },
  cutPaper: function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "cutPaper", []);
    });
  },
  printTextWithFont: function (text, typeface, fontSize) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "printTextWithFont", [
        text,
        typeface,
        fontSize,
      ]);
    });
  },
  printColumnsText: function (colsTextArr, colsWidthArr, colsAlign) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "printColumnsText", [
        colsTextArr,
        colsWidthArr,
        colsAlign,
      ]);
    });
  },
  printRawText: function (base64Data, width, height, success, error) {
    // return new Promise(function (resolve, reject) {
    exec(success, error, "Printer", "printRawText", [
      base64Data,
      width,
      height,
    ]);
    // });
  },
  printBarCode: function (barCodeData, symbology, width, height, textPosition) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "printBarCode", [
        barCodeData,
        symbology,
        width,
        height,
        textPosition,
      ]);
    });
  },
  printQRCode: function (qrCodeData, moduleSize, errorLevel) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "printQRCode", [
        qrCodeData,
        moduleSize,
        errorLevel,
      ]);
    });
  },
  printOriginalText: function (text) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "printOriginalText", [text]);
    });
  },
  printString: function (text) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "Printer", "printString", [text]);
    });
  },
  printerStatusStartListener: function (onSuccess, onError) {
    exec(onSuccess, onError, "Printer", "printerStatusStartListener", []);
  },
  printerStatusStopListener: function () {
    exec(
      function () { },
      function () { },
      "Printer",
      "printerStatusStopListener",
      []
    );
  },
  initScanner: function (onSuccess, onError) {
    exec(onSuccess, onError, "Printer", "initScanner", []);
  },
};
