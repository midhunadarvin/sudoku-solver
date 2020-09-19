import {ToastAndroid} from 'react-native';

export const showToast = (message, duration = ToastAndroid.SHORT) => {
  ToastAndroid.show(message, duration);
};

export const showToastWithGravity = (
  message,
  duration = ToastAndroid.SHORT,
  gravity = ToastAndroid.CENTER,
) => {
  ToastAndroid.showWithGravity(message, duration, gravity);
};

export const showToastWithGravityAndOffset = () => {
  ToastAndroid.showWithGravityAndOffset(
    'A wild toast appeared!',
    ToastAndroid.LONG,
    ToastAndroid.BOTTOM,
    25,
    50,
  );
};
