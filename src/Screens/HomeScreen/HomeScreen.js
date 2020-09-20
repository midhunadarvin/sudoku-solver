import React, {Component} from 'react';
import {SafeAreaView, View, StatusBar, Button, Image} from 'react-native';
import ImagePicker from 'react-native-image-picker';
import {checkForBlurryImage, scanSudoku} from '../../utils/ImageUtils';
import {showToast} from '../../utils/Toast';
import {styles} from './Styles';
import FastImage from 'react-native-fast-image';

export default class HomeScreen extends Component {
  state = {
    result: null,
  };

  goToCamera = () => {
    this.props.navigation.navigate('Camera');
  };

  pickImage = () => {
    // More info on all the options is below in the API Reference... just some common use cases shown here
    const options = {
      title: 'Select Image',
      storageOptions: {
        skipBackup: true,
        path: 'downloads',
      },
    };

    /**
     * The first arg is the options object for customization (it can also be null or omitted for default options),
     * The second arg is the callback which sends object: response (more info in the API Reference)
     */
    ImagePicker.launchImageLibrary(options, async (response) => {
      console.log('Response = ', response);

      if (response.didCancel) {
        console.log('User cancelled image picker');
      } else if (response.error) {
        console.log('ImagePicker Error: ', response.error);
      } else if (response.customButton) {
        console.log('User tapped custom button: ', response.customButton);
      } else {
        // You can also display the image using data:
        this.imageBase64Data = response.data;
        const result = await scanSudoku(this.imageBase64Data);
        const source = {uri: `data:image/gif;base64, ${result}`};
        this.setState({
          image: source,
        });
      }
    });
  };

  render() {
    return (
      <>
        <StatusBar barStyle="dark-content" />
        <SafeAreaView>
          <View
            contentInsetAdjustmentBehavior="automatic"
            style={styles.layoutView}>
            {this.state.image && (
              <View style={styles.imageContainer}>
                <FastImage
                  style={styles.image}
                  source={this.state.image}
                  resizeMode={FastImage.resizeMode.contain}
                />
              </View>
            )}
            <View>
              <View style={styles.buttonContainer}>
                <Button title="Scan sudoku" onPress={this.goToCamera} />
              </View>
              <View style={styles.buttonContainer}>
                <Button title="Pick Picture" onPress={this.pickImage} />
              </View>
            </View>
          </View>
        </SafeAreaView>
      </>
    );
  }
}
