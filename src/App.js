/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React from 'react';
import 'react-native-gesture-handler';
import {NavigationContainer} from '@react-navigation/native';
import {createStackNavigator} from '@react-navigation/stack';

import HomeScreen from './Screens/HomeScreen/HomeScreen';
import CameraScreen from './Screens/CameraScreen/CameraScreen';
import {LogBox} from 'react-native';

const Stack = createStackNavigator();

const App = () => {
  LogBox.ignoreAllLogs(true);
  return (
    <>
      <NavigationContainer>
        <Stack.Navigator>
          <Stack.Screen
            name="Home"
            component={HomeScreen}
            options={{title: 'Sudoku Solver'}}
          />
          <Stack.Screen name="Camera" component={CameraScreen} />
        </Stack.Navigator>
      </NavigationContainer>
    </>
  );
};

export default App;
