import {Platform} from 'react-native';
import OpenCV from '../NativeModules/OpenCV';

export async function checkForBlurryImage(imageAsBase64) {
  return new Promise((resolve) => {
    if (Platform.OS === 'android') {
      OpenCV.checkForBlurryImage(
        imageAsBase64,
        () => {
          // error handling
        },
        (msg) => {
          resolve(msg);
        },
      );
    } else {
      OpenCV.checkForBlurryImage(imageAsBase64, (error, dataArray) => {
        resolve(dataArray[0]);
      });
    }
  });
}

export async function scanSudoku(imageAsBase64) {
  return new Promise((resolve) => {
    if (Platform.OS === 'android') {
      OpenCV.scanSudoku(
        imageAsBase64,
        () => {
          // error handling
        },
        (result) => {
          resolve(result);
        },
      );
    } else {
      OpenCV.checkForBlurryImage(imageAsBase64, (error, dataArray) => {
        resolve(dataArray[0]);
      });
    }
  });
}
