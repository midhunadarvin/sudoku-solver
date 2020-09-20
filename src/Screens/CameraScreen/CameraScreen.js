import React, {Component} from 'react';
import {View, Text, Image, TouchableOpacity} from 'react-native';
import {RNCamera as Camera} from 'react-native-camera';
import Toast from 'react-native-easy-toast';

import styles from './Styles';
import CircleWithinCircle from '../../assets/svg/CircleWithinCircle';
import {showToast} from '../../utils/Toast';
import {checkForBlurryImage} from '../../utils/ImageUtils';

export default class CameraScreen extends Component {
  constructor() {
    super();
    this.toast = React.createRef();
    this.takePicture = this.takePicture.bind(this);
    this.proceedWithCheckingBlurryImage = this.proceedWithCheckingBlurryImage.bind(
      this,
    );
    this.repeatPhoto = this.repeatPhoto.bind(this);
    this.usePhoto = this.usePhoto.bind(this);
  }

  state = {
    cameraPermission: false,
    photoAsBase64: {
      content: '',
      isPhotoPreview: false,
      photoPath: '',
    },
  };

  async takePicture() {
    if (this.camera) {
      const options = {quality: 0.5, base64: true};
      const data = await this.camera.takePictureAsync(options);
      this.setState({
        ...this.state,
        photoAsBase64: {
          content: data.base64,
          isPhotoPreview: true,
          photoPath: data.uri,
        },
      });
    }
  }

  proceedWithCheckingBlurryImage(content) {
    return new Promise((resolve, reject) => {
      checkForBlurryImage(content)
        .then((blurryPhoto) => {
          if (blurryPhoto) {
            showToast('Photo is blurred!');
          }
          showToast('Photo is clear!');
          resolve(true);
        })
        .catch((err) => {
          console.log('err', err);
          reject(err);
        });
    });
  }

  repeatPhoto() {
    this.setState({
      ...this.state,
      photoAsBase64: {
        content: '',
        isPhotoPreview: false,
        photoPath: '',
      },
    });
  }

  async usePhoto() {
    // do something, e.g. navigate
    await this.proceedWithCheckingBlurryImage(this.state.photoAsBase64.content);
    this.props.navigation.pop();
  }

  render() {
    if (this.state.photoAsBase64.isPhotoPreview) {
      return (
        <View style={styles.container}>
          <Toast ref={(ref) => (this.toast = ref)} position="center" />
          <Image
            source={{
              uri: `data:image/png;base64,${this.state.photoAsBase64.content}`,
            }}
            style={styles.imagePreview}
          />
          <View style={styles.repeatPhotoContainer}>
            <TouchableOpacity onPress={this.repeatPhoto}>
              <Text style={styles.photoPreviewRepeatPhotoText}>
                Repeat photo
              </Text>
            </TouchableOpacity>
          </View>
          <View style={styles.usePhotoContainer}>
            <TouchableOpacity onPress={this.usePhoto}>
              <Text style={styles.photoPreviewUsePhotoText}>Use photo</Text>
            </TouchableOpacity>
          </View>
        </View>
      );
    }

    return (
      <View style={styles.container}>
        <Camera
          ref={(cam) => {
            this.camera = cam;
          }}
          style={styles.preview}
          androidCameraPermissionOptions={{
            title: 'Permission to use camera',
            message: 'We need your permission to use your camera phone',
          }}>
          <View style={styles.takePictureContainer}>
            <TouchableOpacity onPress={this.takePicture}>
              <View>
                <CircleWithinCircle />
              </View>
            </TouchableOpacity>
          </View>
        </Camera>
        <Toast ref={(ref) => (this.toast = ref)} position="center" />
      </View>
    );
  }
}
